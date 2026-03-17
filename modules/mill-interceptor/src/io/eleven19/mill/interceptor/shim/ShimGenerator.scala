package io.eleven19.mill.interceptor.shim

import kyo.*
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import scala.util.Try

/** Outcome of a single shim-file generation attempt. */
final case class GeneratedShim(path: Path, tool: BuildTool, platform: String) derives CanEqual

/** Configuration for shim generation. */
final case class ShimGenerateOptions(
    tools: List[BuildTool],
    wrapper: Boolean,
    outputDir: Path,
    version: String
) derives CanEqual

object ShimGenerateOptions:

    val default: ShimGenerateOptions = ShimGenerateOptions(
        tools = BuildTool.all,
        wrapper = false,
        outputDir = Path("."),
        version = "latest"
    )
end ShimGenerateOptions

/** Core logic for generating platform-specific shim scripts.
  *
  * All file I/O operations are suspended via Kyo's `Sync` effect so callers can compose them with other effects like
  * `Log`.
  */
object ShimGenerator:

    /** Generate all shim scripts for the given options.
      *
      * For each build tool, generates both a Unix script (no extension) and a Windows script (.cmd). Returns a
      * `List[GeneratedShim]` suspended in `Sync`.
      */
    def generate(options: ShimGenerateOptions): List[GeneratedShim] < Sync =
        for
            outputDirExists <- options.outputDir.exists
            _               <- if outputDirExists then Kyo.unit else options.outputDir.mkDir
            generated <- Kyo.foreach(options.tools) { tool =>
                val baseName = tool.scriptName(options.wrapper)
                for
                    unix <- generateFile(
                        options.outputDir,
                        baseName,
                        ShimTemplate.unix(tool, options.version),
                        tool,
                        "unix"
                    )
                    windows <- generateFile(
                        options.outputDir,
                        s"$baseName.cmd",
                        ShimTemplate.windows(tool, options.version),
                        tool,
                        "windows"
                    )
                yield List(unix, windows)
            }
        yield generated.flatten

    /** Generate the content for a Unix shim script without writing to disk. */
    def unixContent(tool: BuildTool, version: String): String =
        ShimTemplate.unix(tool, version)

    /** Generate the content for a Windows shim script without writing to disk. */
    def windowsContent(tool: BuildTool, version: String): String =
        ShimTemplate.windows(tool, version)

    private def generateFile(
        outputDir: Path,
        fileName: String,
        content: String,
        tool: BuildTool,
        platform: String
    ): GeneratedShim < Sync =
        val filePath = Path(outputDir, fileName)
        for
            _ <- filePath.write(content)
            _ <- setUnixExecutableIfPossible(filePath, platform)
        yield GeneratedShim(filePath, tool, platform)

    private def setUnixExecutableIfPossible(filePath: Path, platform: String): Unit < Sync =
        Sync.defer {
            if platform == "unix" then
                val _: Try[Unit] = Try {
                    val perms = Files.getPosixFilePermissions(filePath.toJava)
                    perms.add(PosixFilePermission.OWNER_EXECUTE)
                    perms.add(PosixFilePermission.GROUP_EXECUTE)
                    perms.add(PosixFilePermission.OTHERS_EXECUTE)
                    Files.setPosixFilePermissions(filePath.toJava, perms)
                    ()
                }
            ()
        }
end ShimGenerator
