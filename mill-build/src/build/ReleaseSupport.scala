package build

import mill.*
import mill.javalib.NativeImageModule
import mill.scalalib.JavaModule

import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

trait ReleaseSupport extends mill.Module { this: NativeImageModule & JavaModule =>
  private val toolName = "mill-interceptor"
  private val mavenGroup = "io.github.eleven19.mill-interceptor"
  private val libraryArtifact = "milli"
  private val distArtifact = "milli-dist"
  private val defaultVersionToken = "@MILLI_DEFAULT_VERSION@"
  private val supportedReleaseTargets = Seq(
    "x86_64-unknown-linux-gnu",
    "aarch64-unknown-linux-gnu",
    "x86_64-apple-darwin",
    "aarch64-apple-darwin",
    "x86_64-pc-windows-msvc"
  )

  protected def isWindowsTarget(target: String): Boolean =
    target.endsWith("windows-msvc")

  protected def validatedTarget(target: String): String =
    if supportedReleaseTargets.contains(target) then target
    else throw new IllegalArgumentException(s"Unsupported release target: $target")

  protected def executableNameFor(target: String): String =
    if isWindowsTarget(target) then s"$toolName.exe" else toolName

  protected def archiveExtensionFor(target: String): String =
    if isWindowsTarget(target) then "zip" else "tar.gz"

  private def assetNameFor(version: String, target: String): String =
    s"$toolName-v$version-$target.${archiveExtensionFor(target)}"

  private def distAssetNameFor(version: String): String =
    s"$toolName-dist-v$version.jar"

  private def nativeArtifactNameFor(target: String): String =
    validatedTarget(target) match
      case "x86_64-unknown-linux-gnu" => "milli-native-linux-amd64"
      case "aarch64-unknown-linux-gnu" => "milli-native-linux-aarch64"
      case "x86_64-apple-darwin" => "milli-native-macos-amd64"
      case "aarch64-apple-darwin" => "milli-native-macos-aarch64"
      case "x86_64-pc-windows-msvc" => "milli-native-windows-amd64"

  protected def writeZip(source: os.Path, entryName: String, destination: os.Path): Unit =
    val output = new ZipOutputStream(new FileOutputStream(destination.toIO))
    try
      val entry = new ZipEntry(entryName)
      output.putNextEntry(entry)
      output.write(os.read.bytes(source))
      output.closeEntry()
    finally output.close()

  private def validatedLauncherOs(launcherOs: String): String =
    launcherOs match
      case "unix" | "windows" => launcherOs
      case other => throw new IllegalArgumentException(s"Unsupported launcher OS: $other")

  private def launcherFileNameFor(launcherOs: String): String =
    validatedLauncherOs(launcherOs) match
      case "unix" => "milli"
      case "windows" => "milli.bat"

  private def launcherTemplatePath(taskDest: os.Path, launcherOs: String): os.Path =
    taskDest / os.up / os.up / "launcher" / launcherFileNameFor(launcherOs)

  def releaseTargets = Task {
    supportedReleaseTargets
  }

  def publishGroup = Task {
    mavenGroup
  }

  def publishLibraryArtifact = Task {
    libraryArtifact
  }

  def publishAssemblyArtifact = Task {
    distArtifact
  }

  def publishNativeArtifacts = Task {
    supportedReleaseTargets.map(target => s"$target=${nativeArtifactNameFor(target)}")
  }

  def releaseExecutableName(target: String) = Task.Command {
    executableNameFor(validatedTarget(target))
  }

  def releaseArchiveExtension(target: String) = Task.Command {
    archiveExtensionFor(validatedTarget(target))
  }

  def releaseAssetName(version: String, target: String) = Task.Command {
    assetNameFor(version, validatedTarget(target))
  }

  def releaseArchive(version: String, target: String) = Task.Command {
    val checkedTarget = validatedTarget(target)
    val stageDir = Task.dest / "stage"
    val executableName = executableNameFor(checkedTarget)
    val archivePath = Task.dest / assetNameFor(version, checkedTarget)
    val stagedExecutable = stageDir / executableName

    os.makeDir.all(stageDir)
    os.copy.over(nativeImage().path, stagedExecutable, createFolders = true)

    if !isWindowsTarget(checkedTarget) then
      os.perms.set(stagedExecutable, "rwxr-xr-x")

    if isWindowsTarget(checkedTarget) then
      writeZip(stagedExecutable, executableName, archivePath)
    else
      os.proc(
        "tar",
        "-C",
        stageDir.toString,
        "-czf",
        archivePath.toString,
        executableName
      ).call(check = true)

    PathRef(archivePath)
  }

  def releaseAssemblyAssetName(version: String) = Task.Command {
    distAssetNameFor(version)
  }

  def releaseAssembly(version: String) = Task.Command {
    val destination = Task.dest / distAssetNameFor(version)
    os.copy.over(assembly().path, destination, createFolders = true)
    PathRef(destination)
  }

  def releaseLauncherName(launcherOs: String) = Task.Command {
    launcherFileNameFor(launcherOs)
  }

  def releaseLauncher(version: String, launcherOs: String) = Task.Command {
    val checkedOs = validatedLauncherOs(launcherOs)
    val destination = Task.dest / launcherFileNameFor(checkedOs)
    val template = launcherTemplatePath(Task.dest, checkedOs)
    val contents = os.read(template).replace(defaultVersionToken, version)

    os.write.over(destination, contents, createFolders = true)

    if checkedOs == "unix" then
      os.perms.set(destination, "rwxr-xr-x")

    PathRef(destination)
  }
}
