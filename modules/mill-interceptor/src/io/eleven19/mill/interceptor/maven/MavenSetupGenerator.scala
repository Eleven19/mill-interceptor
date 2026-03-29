package io.eleven19.mill.interceptor.maven

import os.Path
import java.nio.file.Files

final case class GeneratedSetupFile(path: os.Path, content: String) derives CanEqual

object MavenSetupGenerator:

    final private case class PlannedFile(
        path: os.Path,
        content: String
    ) derives CanEqual

    private val extensionGroupId    = "io.eleven19.mill-interceptor"
    private val extensionArtifactId = "mill-interceptor-maven-plugin"

    def generate(
        startPath: os.Path,
        options: MavenSetupOptions,
        extensionVersion: String
    ): Either[IllegalArgumentException, List[GeneratedSetupFile]] =
        for
            repoRoot <- detectRepoRoot(startPath)
            files     = plannedFiles(repoRoot, options.format, extensionVersion)
            _        <- validateWritable(files, options.force)
            _         = if !options.dryRun then writeFiles(files)
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
        repoRoot: os.Path,
        format: MavenSetupFormat,
        extensionVersion: String
    ): List[PlannedFile] =
        val cfgFile = configFileName(format)
        List(
            PlannedFile(
                repoRoot / ".mvn" / "extensions.xml",
                renderExtensionsXml(extensionVersion)
            ),
            PlannedFile(
                repoRoot / cfgFile,
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
    ): Either[IllegalArgumentException, Unit] =
        files.foldLeft[Either[IllegalArgumentException, Unit]](Right(())) { (acc, file) =>
            acc.flatMap { _ =>
                if !os.exists(file.path) || force then Right(())
                else Left(new IllegalArgumentException(s"Refusing to overwrite existing file: ${file.path}"))
            }
        }

    private def writeFiles(files: List[PlannedFile]): Unit =
        files.foreach { file =>
            os.makeDir.all(file.path / os.up)
            os.write(file.path, file.content)
        }

    private def detectRepoRoot(startPath: os.Path): Either[IllegalArgumentException, os.Path] =
        val normalizedStart = startPath.toNIO.toAbsolutePath.normalize
        @annotation.tailrec
        def loop(current: java.nio.file.Path | Null): Either[IllegalArgumentException, os.Path] =
            current match
                case null =>
                    Left(new IllegalArgumentException(
                        s"Could not find repo root from ${startPath}. Expected a parent containing .git"
                    ))
                case path if path.resolve(".git").toFile.exists() => Right(os.Path(path))
                case path                                         => loop(path.getParent)

        loop(normalizedStart)
