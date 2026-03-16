package build

import mill.*
import mill.javalib.NativeImageModule
import mill.scalalib.*
import mill.scalalib.PublishModule
import mill.scalalib.SonatypeCentralPublishModule
import mill.scalalib.publish.{Developer, License, PomSettings, VersionControl}

trait CommonScalaModule extends ScalaModule with scalafmt.ScalafmtModule {
  override def jvmVersion = Task {
    "graalvm-java25:25.0.1"
  }

  override def jvmIndexVersion = Task {
    "latest.release"
  }

  override def scalaVersion = Task {
    "3.8.2"
  }

  override def scalacOptions = Task {
    Seq(
      "-Wvalue-discard",
      "-Wnonunit-statement",
      "-Wconf:msg=(unused.*value|discarded.*value|pure.*statement):error",
      "-language:strictEquality"
    )
  }
}

trait InterceptorModule
    extends CommonScalaModule
    with mill.contrib.scoverage.ScoverageModule
    with NativeImageModule
    with ReleaseSupport
    with PublishSupport {
  override def scoverageVersion = Task {
    "2.5.2"
  }

  override def mvnDeps = Task {
    super.mvnDeps() ++ Seq(
      mvn"com.github.alexarchambault::case-app::2.1.0",
      mvn"com.outr::scribe::3.18.0",
      mvn"io.getkyo::kyo-core::1.0-RC1",
      mvn"io.getkyo::kyo-direct:1.0-RC1",
      mvn"io.getkyo::kyo-prelude:1.0-RC1"
    )
  }

  override def nativeImageOptions = Task {
    super.nativeImageOptions() ++ Seq("--no-fallback")
  }

  object test extends ScoverageTests with TestModule.ZioTest {
    override def mvnDeps = Task {
      super.mvnDeps() ++ Seq(
        mvn"dev.zio::zio-test::2.1.24",
        mvn"dev.zio::zio-test-sbt::2.1.24",
        mvn"io.getkyo::kyo-zio-test::1.0-RC1"
      )
    }
  }

  object itest extends ScalaTests with TestModule.Junit5 {
    override def mvnDeps = Task {
      super.mvnDeps() ++ Seq(
        mvn"org.scalameta::munit::1.0.4",
        mvn"io.cucumber::cucumber-scala:8.25.1",
        mvn"io.cucumber:cucumber-junit-platform-engine:7.20.1",
        mvn"org.junit.platform:junit-platform-suite:1.11.4",
        mvn"org.junit.platform:junit-platform-suite-engine:1.11.4",
        mvn"org.junit.jupiter:junit-jupiter-engine:5.11.4"
      )
    }
  }
}

trait MavenPluginModule
    extends CommonScalaModule
    with PublishModule
    with SonatypeCentralPublishModule {
  private val defaultPublishVersion = "0.0.0-SNAPSHOT"
  private val publishGroupId = "io.eleven19.mill-interceptor"
  private val publishArtifactId = "mill-interceptor-maven-plugin"
  private val pluginDescription =
    "A Maven plugin for bridging Maven lifecycle executions into Mill interceptor workflows."
  private val goalPrefix = "mill-interceptor"
  private val descriptorGoal = "describe"
  private val descriptorImplementation =
    "io.eleven19.mill.interceptor.maven.plugin.mojo.DescribeMojo"

  override def artifactId = Task {
    publishArtifactId
  }

  override def mvnDeps = Task {
    super.mvnDeps() ++ Seq(
      mvn"com.outr::scribe::3.18.0",
      mvn"io.getkyo::kyo-core::1.0-RC1",
      mvn"io.getkyo::kyo-direct:1.0-RC1",
      mvn"io.getkyo::kyo-prelude:1.0-RC1",
      mvn"org.apache.maven:maven-plugin-api:3.9.9",
      mvn"org.apache.maven.plugin-tools:maven-plugin-annotations:3.15.2"
    )
  }

  def publishGroup = Task {
    publishGroupId
  }

  def publishVersion = Task.Input {
    sys.env.getOrElse("MILLI_PUBLISH_VERSION", defaultPublishVersion)
  }

  def pomSettings = Task {
    PomSettings(
      description = pluginDescription,
      organization = publishGroup(),
      url = "https://github.com/Eleven19/mill-interceptor",
      licenses = Seq(License.`Apache-2.0`),
      versionControl = VersionControl.github("Eleven19", "mill-interceptor"),
      developers = Seq(
        Developer(
          id = "DamianReeves",
          name = "Damian Reeves",
          url = "https://github.com/DamianReeves"
        )
      )
    )
  }

  def publishArtifactSummary = Task {
    Seq(s"${publishGroup()}:${artifactId()}:${publishVersion()}")
  }

  private def pluginDescriptor(version: String): String =
    s"""<?xml version="1.0" encoding="UTF-8"?>
<plugin>
  <name>Mill Interceptor Maven Plugin</name>
  <description>$pluginDescription</description>
  <groupId>$publishGroupId</groupId>
  <artifactId>$publishArtifactId</artifactId>
  <version>$version</version>
  <goalPrefix>$goalPrefix</goalPrefix>
  <isolatedRealm>false</isolatedRealm>
  <inheritedByDefault>true</inheritedByDefault>
  <mojos>
    <mojo>
      <goal>$descriptorGoal</goal>
      <description>Placeholder goal for validating Maven plugin packaging and execution.</description>
      <implementation>$descriptorImplementation</implementation>
      <language>java</language>
      <instantiationStrategy>per-lookup</instantiationStrategy>
      <executionStrategy>once-per-session</executionStrategy>
      <threadSafe>true</threadSafe>
      <parameters/>
      <configuration/>
    </mojo>
  </mojos>
</plugin>
"""

  def generatedPluginResources = Task {
    val descriptorDir = Task.dest / "META-INF" / "maven"
    os.makeDir.all(descriptorDir)
    os.write.over(
      descriptorDir / "plugin.xml",
      pluginDescriptor(publishVersion()),
      createFolders = true
    )
    PathRef(Task.dest)
  }

  def publishedPluginJar = Task {
    publishArtifacts()
      .payload
      .collectFirst { case (path, fileName) if fileName.endsWith(".jar") => path }
      .getOrElse {
        throw new IllegalStateException("Unable to locate the published Maven plugin jar payload.")
      }
  }

  def publishedPluginPom = Task {
    publishArtifacts()
      .payload
      .collectFirst { case (path, fileName) if fileName.endsWith(".pom") => path }
      .getOrElse {
        throw new IllegalStateException("Unable to locate the published Maven plugin pom payload.")
      }
  }

  override def compileResources = Task {
    super.compileResources() ++ Seq(generatedPluginResources())
  }

  override def pomPackagingType = "maven-plugin"

  object test extends ScalaTests with TestModule.ZioTest {
    override def mvnDeps = Task {
      super.mvnDeps() ++ Seq(
        mvn"dev.zio::zio-test::2.1.24",
        mvn"dev.zio::zio-test-sbt::2.1.24",
        mvn"io.getkyo::kyo-zio-test::1.0-RC1"
      )
    }
  }

  object itest extends ScalaTests with TestModule.ZioTest {
    override def forkEnv = Task {
      super.forkEnv() ++ Map(
        "MILL_INTERCEPTOR_MAVEN_PLUGIN_JAR" -> MavenPluginModule.this.publishedPluginJar().path.toString,
        "MILL_INTERCEPTOR_MAVEN_PLUGIN_POM" -> MavenPluginModule.this.publishedPluginPom().path.toString
      )
    }

    override def mvnDeps = Task {
      super.mvnDeps() ++ Seq(
        mvn"dev.zio::zio-test::2.1.24",
        mvn"dev.zio::zio-test-sbt::2.1.24",
        mvn"io.getkyo::kyo-zio-test::1.0-RC1"
      )
    }
  }
}
