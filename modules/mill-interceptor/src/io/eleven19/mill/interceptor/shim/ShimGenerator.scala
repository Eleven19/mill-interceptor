package io.eleven19.mill.interceptor.shim

import os.Path
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import scala.util.Try

/** Outcome of a single shim-file generation attempt. */
final case class GeneratedShim(path: os.Path, tool: BuildTool, platform: String) derives CanEqual

/** Configuration for shim generation. */
final case class ShimGenerateOptions(
    tools: List[BuildTool],
    wrapper: Boolean,
    outputDir: os.Path,
    version: String
) derives CanEqual

object ShimGenerateOptions:

    val default: ShimGenerateOptions = ShimGenerateOptions(
        tools = BuildTool.all,
        wrapper = false,
        outputDir = os.Path(java.nio.file.Paths.get(".")),
        version = "latest"
    )
end ShimGenerateOptions

/** Core logic for generating platform-specific shim scripts. */
object ShimGenerator:

    /** Generate all shim scripts for the given options.
      *
      * For each build tool, generates both a Unix script (no extension) and a Windows script (.cmd). Returns a
      * `List[GeneratedShim]`.
      */
    def generate(options: ShimGenerateOptions): List[GeneratedShim] =
        if !os.exists(options.outputDir) then os.makeDir.all(options.outputDir)
        options.tools.flatMap { tool =>
            val baseName = tool.scriptName(options.wrapper)
            val unix = generateFile(
                options.outputDir,
                baseName,
                ShimTemplate.unix(tool, options.version),
                tool,
                "unix"
            )
            val windows = generateFile(
                options.outputDir,
                s"$baseName.cmd",
                ShimTemplate.windows(tool, options.version),
                tool,
                "windows"
            )
            List(unix, windows)
        }

    /** Generate the content for a Unix shim script without writing to disk. */
    def unixContent(tool: BuildTool, version: String): String =
        ShimTemplate.unix(tool, version)

    /** Generate the content for a Windows shim script without writing to disk. */
    def windowsContent(tool: BuildTool, version: String): String =
        ShimTemplate.windows(tool, version)

    private def generateFile(
        outputDir: os.Path,
        fileName: String,
        content: String,
        tool: BuildTool,
        platform: String
    ): GeneratedShim =
        val filePath = outputDir / fileName
        os.write(filePath, content)
        setUnixExecutableIfPossible(filePath, platform)
        GeneratedShim(filePath, tool, platform)

    private def setUnixExecutableIfPossible(filePath: os.Path, platform: String): Unit =
        if platform == "unix" then
            val _: Try[Unit] = Try {
                val perms = Files.getPosixFilePermissions(filePath.toNIO)
                perms.add(PosixFilePermission.OWNER_EXECUTE)
                perms.add(PosixFilePermission.GROUP_EXECUTE)
                perms.add(PosixFilePermission.OTHERS_EXECUTE)
                Files.setPosixFilePermissions(filePath.toNIO, perms)
                ()
            }
end ShimGenerator
