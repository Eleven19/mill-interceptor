package io.eleven19.mill.interceptor.maven

import kyo.*
import java.nio.file.Files

final case class GeneratedSetupFile(path: Path, content: String) derives CanEqual

object MavenSetupGenerator:

    final private case class PlannedFile(
        path: Path,
        absolutePath: java.nio.file.Path,
        content: String
    ) derives CanEqual

    private val extensionGroupId    = "io.eleven19.mill-interceptor"
    private val extensionArtifactId = "mill-interceptor-maven-plugin"

    def generate(
        startPath: Path,
        options: MavenSetupOptions,
        extensionVersion: String
    ): List[GeneratedSetupFile] < (Sync & Abort[IllegalArgumentException]) =
        for
            repoRoot <- detectRepoRoot(startPath)
            files = plannedFiles(repoRoot, options.format, extensionVersion)
            _ <- validateWritable(files, options.force)
            _ <- if options.dryRun then Kyo.unit else writeFiles(files)
        yield files.map(file => GeneratedSetupFile(file.path, file.content))

    def renderExtensionsXml(extensionVersion: String): String =
        val safeVersion = extensionVersion
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        s"""<?xml version="1.0" encoding="UTF-8"?>
           |<extensions xmlns="http://maven.apache.org/EXTENSIONS/1.0.0"
           |            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           |            xsi:schemaLocation="http://maven.apache.org/EXTENSIONS/1.0.0 https://maven.apache.org/xsd/core-extensions-1.0.0.xsd">
           |  <extension>
           |    <groupId>$extensionGroupId</groupId>
           |    <artifactId>$extensionArtifactId</artifactId>
           |    <version>$safeVersion</version>
           |  </extension>
           |</extensions>
           |""".stripMargin

    def renderStarterConfig(format: MavenSetupFormat): String =
        format match
            case MavenSetupFormat.Yaml =>
                """# Optional overrides for the Maven extension baseline.
                  |# The common Maven lifecycle works with the built-in defaults.
                  |# Uncomment and edit entries below when your Mill build needs custom mappings.
                  |
                  |validate:
                  |  scalafmtEnabled: true
                  |
                  |# lifecycle:
                  |#   compile:
                  |#     - __.compile
                  |
                  |# goals:
                  |#   inspect-plan:
                  |#     - app.inspectPlan
                  |
                  |# Module-local overrides can live in:
                  |#   <module>/mill-interceptor.yaml
                  |#   <module>/.config/mill-interceptor/config.yaml
                  |
                  |# Maven install/deploy require a Mill PublishModule-capable surface.
                  |""".stripMargin
            case MavenSetupFormat.Pkl =>
                """// Optional overrides for the Maven extension baseline.
                  |// The common Maven lifecycle works with the built-in defaults.
                  |
                  |validate {
                  |  scalafmtEnabled = true
                  |}
                  |
                  |// lifecycle {
                  |//   compile = List("__.compile")
                  |// }
                  |
                  |// goals {
                  |//   ["inspect-plan"] = List("app.inspectPlan")
                  |// }
                  |
                  |// Module-local overrides can live in:
                  |//   <module>/mill-interceptor.pkl
                  |//   <module>/.config/mill-interceptor/config.pkl
                  |// Maven install/deploy require a Mill PublishModule-capable surface.
                  |""".stripMargin

    private def plannedFiles(
        repoRoot: java.nio.file.Path,
        format: MavenSetupFormat,
        extensionVersion: String
    ): List[PlannedFile] =
        val cfgFile = configFileName(format)
        List(
            PlannedFile(
                Path(".mvn", "extensions.xml"),
                repoRoot.resolve(".mvn").resolve("extensions.xml"),
                renderExtensionsXml(extensionVersion)
            ),
            PlannedFile(
                Path(cfgFile),
                repoRoot.resolve(cfgFile),
                renderStarterConfig(format)
            )
        )

    private def configFileName(format: MavenSetupFormat): String =
        format match
            case MavenSetupFormat.Yaml => "mill-interceptor.yaml"
            case MavenSetupFormat.Pkl  => "mill-interceptor.pkl"

    private def validateWritable(
        files: List[PlannedFile],
        force: Boolean
    ): Unit < (Sync & Abort[IllegalArgumentException]) =
        Kyo.foreachDiscard(files) { file =>
            for
                exists <- Sync.defer(Files.exists(file.absolutePath))
                _ <-
                    if !exists || force then Kyo.unit
                    else Abort.fail(new IllegalArgumentException(s"Refusing to overwrite existing file: ${file.path}"))
            yield ()
        }

    private def writeFiles(files: List[PlannedFile]): Unit < Sync =
        Kyo.foreachDiscard(files) { file =>
            Sync.defer {
                val parent = file.absolutePath.getParent
                if parent != null then
                    val _ = Files.createDirectories(parent)
                val _ = Files.writeString(file.absolutePath, file.content)
                ()
            }
        }

    private def detectRepoRoot(startPath: Path): java.nio.file.Path < (Sync & Abort[IllegalArgumentException]) =
        Abort.catching[IllegalArgumentException] {
            Sync.defer {
                val normalizedStart = startPath.toJava.toAbsolutePath.normalize
                @annotation.tailrec
                def loop(current: java.nio.file.Path | Null): java.nio.file.Path =
                    current match
                        case null =>
                            throw new IllegalArgumentException(
                                s"Could not find repo root from ${startPath}. Expected a parent containing .git"
                            )
                        case path if path.resolve(".git").toFile.exists() => path
                        case path                                         => loop(path.getParent)

                loop(normalizedStart)
            }
        }
