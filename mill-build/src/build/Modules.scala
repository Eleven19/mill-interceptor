package build

import mill.*
import mill.javalib.NativeImageModule
import mill.scalalib.*

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

trait MavenPluginModule extends CommonScalaModule {
  override def mvnDeps = Task {
    super.mvnDeps() ++ Seq(
      mvn"com.outr::scribe::3.18.0",
      mvn"io.getkyo::kyo-core::1.0-RC1",
      mvn"io.getkyo::kyo-direct:1.0-RC1",
      mvn"io.getkyo::kyo-prelude:1.0-RC1"
    )
  }

  object test extends ScalaTests with TestModule.ZioTest {
    override def mvnDeps = Task {
      super.mvnDeps() ++ Seq(
        mvn"dev.zio::zio-test::2.1.24",
        mvn"dev.zio::zio-test-sbt::2.1.24",
        mvn"io.getkyo::kyo-zio-test::1.0-RC1"
      )
    }
  }
}
