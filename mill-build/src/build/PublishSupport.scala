package build

import mill.*
import mill.scalalib.JavaModule
import mill.scalalib.PublishModule
import mill.scalalib.publish.{Developer, License, PomSettings, VersionControl}

trait PublishSupport extends PublishModule { this: JavaModule & ReleaseSupport =>
  private val defaultPublishVersion = "0.0.0-SNAPSHOT"

  override def artifactId = Task {
    publishLibraryArtifact()
  }

  def publishVersion = Task.Input {
    sys.env.getOrElse("MILLI_PUBLISH_VERSION", defaultPublishVersion)
  }

  def pomSettings = Task {
    PomSettings(
      description = "A tool for intercepting other build tools using mill.",
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
    val version = publishVersion()
    Seq(
      s"${publishGroup()}:${publishLibraryArtifact()}:$version",
      s"${publishGroup()}:${publishAssemblyArtifact()}:$version"
    ) ++ publishNativeArtifacts().map { entry =>
      val artifact = entry.split("=", 2)(1)
      s"${publishGroup()}:$artifact:$version"
    }
  }
}
