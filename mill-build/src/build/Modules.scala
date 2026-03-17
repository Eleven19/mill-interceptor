package build

import mill.*
import mill.javalib.NativeImageModule
import mill.scalalib.*
import mill.scalalib.PublishModule
import mill.scalalib.SonatypeCentralPublishModule

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

trait CommonScalaTestModule extends ScalaModule {
  private val unsafeMemoryAccessOption = "--sun-misc-unsafe-memory-access=allow"

  override def forkArgs = Task {
    super.forkArgs() ++ Seq(unsafeMemoryAccessOption)
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
}

trait MavenPluginSupport extends mill.Module with PublishModule with SonatypeCentralPublishModule {
  this: CommonScalaModule =>
  private val defaultPublishVersion = "0.0.0-SNAPSHOT"
  private val goalPrefix = "mill-interceptor"
  private val descriptorGoal = "describe"
  private val descriptorImplementation =
    "io.eleven19.mill.interceptor.maven.plugin.mojo.DescribeMojo"

  def publishVersion = Task.Input {
    sys.env.getOrElse("MILLI_PUBLISH_VERSION", defaultPublishVersion)
  }

  def publishArtifactSummary = Task {
    Seq(s"${pomSettings().organization}:${artifactId()}:${publishVersion()}")
  }

  private def pluginDescriptor(
      organization: String,
      description: String,
      publishedArtifactId: String,
      version: String
  ): String =
    s"""<?xml version="1.0" encoding="UTF-8"?>
<plugin>
  <name>Mill Interceptor Maven Plugin</name>
  <description>$description</description>
  <groupId>$organization</groupId>
  <artifactId>$publishedArtifactId</artifactId>
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
    val pom = pomSettings()
    val descriptorDir = Task.dest / "META-INF" / "maven"
    os.makeDir.all(descriptorDir)
    os.write.over(
      descriptorDir / "plugin.xml",
      pluginDescriptor(pom.organization, pom.description, artifactId(), publishVersion()),
      createFolders = true
    )
    PathRef(Task.dest)
  }

  def publishedPluginJar = Task {
    publishArtifactsPayload()()
      .collectFirst { case (subPath, path) if subPath.last.endsWith(".jar") => path }
      .getOrElse {
        throw new IllegalStateException("Unable to locate the published Maven plugin jar payload.")
      }
  }

  def publishedPluginPom = Task {
    publishArtifactsPayload()()
      .collectFirst { case (subPath, path) if subPath.last.endsWith(".pom") => path }
      .getOrElse {
        throw new IllegalStateException("Unable to locate the published Maven plugin pom payload.")
      }
  }

  override def compileResources = Task {
    super.compileResources() ++ Seq(generatedPluginResources())
  }

  override def pomPackagingType = "maven-plugin"

  trait MavenPluginItestModule extends ScalaTests with CommonScalaTestModule with TestModule.ZioTest {
    override def forkEnv = Task {
      super.forkEnv() ++ Map(
        "MILL_INTERCEPTOR_MAVEN_PLUGIN_JAR" -> MavenPluginSupport.this.publishedPluginJar().path.toString,
        "MILL_INTERCEPTOR_MAVEN_PLUGIN_POM" -> MavenPluginSupport.this.publishedPluginPom().path.toString
      )
    }
  }
}
