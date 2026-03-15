package build

import mill.*
import mill.javalib.NativeImageModule
import mill.scalalib.JavaModule
import mill.scalalib.PublishModule
import mill.scalalib.publish.{Artifact, Developer, License, PomSettings, PublishInfo, VersionControl}

trait PublishSupport extends PublishModule { this: JavaModule & NativeImageModule & ReleaseSupport =>
  private val defaultPublishVersion = "0.0.0-SNAPSHOT"
  private val nativePublishModules = Seq(
    "x86_64-unknown-linux-gnu" -> "milli-native-linux-amd64",
    "aarch64-unknown-linux-gnu" -> "milli-native-linux-aarch64",
    "x86_64-apple-darwin" -> "milli-native-macos-amd64",
    "aarch64-apple-darwin" -> "milli-native-macos-aarch64",
    "x86_64-pc-windows-msvc" -> "milli-native-windows-amd64"
  )

  private def nativeArtifactModuleNameFor(target: String): String =
    nativePublishModules.collectFirst { case (`target`, artifactId) => artifactId }.getOrElse {
      throw new IllegalArgumentException(s"Unsupported native publish target: $target")
    }

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
      s"${publishGroup()}:${publishDistArtifact()}:$version"
    ) ++ publishNativeArtifacts().map { entry =>
      val artifact = entry.split("=", 2)(1)
      s"${publishGroup()}:$artifact:$version"
    }
  }

  trait PublishedAssetModule extends mill.Module with PublishModule {
    def publishedArtifactId: String
    def publishedExt: String

    def publishVersion = PublishSupport.this.publishVersion
    def pomSettings = PublishSupport.this.pomSettings

    override def artifactMetadata = Task {
      Artifact(publishGroup(), publishedArtifactId, publishVersion())
    }
  }

  object distPublish extends PublishedAssetModule {
    def publishedArtifactId = "milli-dist"
    def publishedExt = "jar"

    override def publishArtifacts = Task {
      val version = PublishSupport.this.publishVersion()
      val destination = Task.dest / s"artifact.$publishedExt"
      os.copy.over(PublishSupport.this.assembly().path, destination, createFolders = true)
      mill.scalalib.PublishModule.PublishData(
        artifactMetadata(),
        Seq(
          pom() -> s"$publishedArtifactId-$version.pom",
          PathRef(destination) -> s"$publishedArtifactId-$version.$publishedExt"
        )
      )
    }
  }

  trait NativePublishModule extends PublishedAssetModule {
    def releaseTarget: String

    def publishedArtifactId = nativeArtifactModuleNameFor(releaseTarget)
    def publishedExt = archiveExtensionFor(releaseTarget)

    override def publishArtifacts = Task {
      val checkedTarget = validatedTarget(releaseTarget)
      val version = PublishSupport.this.publishVersion()
      val destination = Task.dest / s"artifact.$publishedExt"
      val stageDir = destination / os.up / "stage"
      val executableName = executableNameFor(checkedTarget)
      val stagedExecutable = stageDir / executableName

      os.makeDir.all(stageDir)
      os.copy.over(PublishSupport.this.nativeImage().path, stagedExecutable, createFolders = true)

      if !isWindowsTarget(checkedTarget) then
        os.perms.set(stagedExecutable, "rwxr-xr-x")

      if isWindowsTarget(checkedTarget) then
        writeZip(stagedExecutable, executableName, destination)
      else
        os.proc(
          "tar",
          "-C",
          stageDir.toString,
          "-czf",
          destination.toString,
          executableName
        ).call(check = true)

      mill.scalalib.PublishModule.PublishData(
        artifactMetadata(),
        Seq(
          pom() -> s"$publishedArtifactId-$version.pom",
          PathRef(destination) -> s"$publishedArtifactId-$version.$publishedExt"
        )
      )
    }
  }

  object nativeLinuxAmd64Publish extends NativePublishModule {
    val releaseTarget = "x86_64-unknown-linux-gnu"
  }

  object nativeLinuxAarch64Publish extends NativePublishModule {
    val releaseTarget = "aarch64-unknown-linux-gnu"
  }

  object nativeMacosAmd64Publish extends NativePublishModule {
    val releaseTarget = "x86_64-apple-darwin"
  }

  object nativeMacosAarch64Publish extends NativePublishModule {
    val releaseTarget = "aarch64-apple-darwin"
  }

  object nativeWindowsAmd64Publish extends NativePublishModule {
    val releaseTarget = "x86_64-pc-windows-msvc"
  }
}
