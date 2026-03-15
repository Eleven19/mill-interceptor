package io.github.eleven19.mill.interceptor.shim

import java.nio.file.{Files, Path, Paths}
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
        outputDir = Paths.get("."),
        version = "latest"
    )
end ShimGenerateOptions

/** Core logic for generating platform-specific shim scripts. */
object ShimGenerator:

    /** Generate all shim scripts for the given options.
      *
      * For each build tool, generates both a Unix script (no extension) and a Windows script (.cmd).
      */
    def generate(options: ShimGenerateOptions): List[GeneratedShim] =
        options.tools.flatMap { tool =>
            val baseName = tool.scriptName(options.wrapper)
            List(
                generateFile(options.outputDir, baseName, ShimTemplate.unix(tool, options.version), tool, "unix"),
                generateFile(
                    options.outputDir,
                    s"$baseName.cmd",
                    ShimTemplate.windows(tool, options.version),
                    tool,
                    "windows"
                )
            )
        }

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
    ): GeneratedShim =
        Files.createDirectories(outputDir)
        val filePath = outputDir.resolve(fileName)
        Files.writeString(filePath, content)

        // Try to set executable permission on Unix platforms
        if platform == "unix" then
            val _ : Try[Path] = Try {
                val perms = Files.getPosixFilePermissions(filePath)
                perms.add(PosixFilePermission.OWNER_EXECUTE)
                perms.add(PosixFilePermission.GROUP_EXECUTE)
                perms.add(PosixFilePermission.OTHERS_EXECUTE)
                Files.setPosixFilePermissions(filePath, perms)
            }

        GeneratedShim(filePath, tool, platform)
end ShimGenerator
