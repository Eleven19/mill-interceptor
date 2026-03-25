package io.eleven19.mill.interceptor.maven

import kyo.*

final case class GeneratedSetupFile(path: Path, content: String) derives CanEqual

object MavenSetupGenerator:

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
        yield files

    def renderExtensionsXml(extensionVersion: String): String =
        s"""<extensions xmlns=\"http://maven.apache.org/EXTENSIONS/1.0.0\"\n|            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n|            xsi:schemaLocation=\"http://maven.apache.org/EXTENSIONS/1.0.0 https://maven.apache.org/xsd/core-extensions-1.0.0.xsd\">\n|  <extension>\n|    <groupId>$extensionGroupId</groupId>\n|    <artifactId>$extensionArtifactId</artifactId>\n|    <version>$extensionVersion</version>\n|  </extension>\n|</extensions>\n|""".stripMargin

    def renderStarterConfig(format: MavenSetupFormat): String =
        format match
            case MavenSetupFormat.Yaml =>
                """# Optional overrides for the Maven extension baseline.
                  |# The common Maven lifecycle works with the built-in defaults.
                  |# Uncomment and edit entries below when your Mill build needs custom mappings.
                  |
                  |scalafmt:
                  |  enabled: true
                  |
                  |# lifecycle:
                  |#   compile:
                  |#     targets:
                  |#       - __.compile
                  |
                  |# Module-local overrides can live in:
                  |#   .config/mill-interceptor/config.yaml
                  |
                  |# Maven install/deploy require a Mill PublishModule-capable surface.
                  |""".stripMargin
            case MavenSetupFormat.Pkl =>
                """// Optional overrides for the Maven extension baseline.
                  |// The common Maven lifecycle works with the built-in defaults.
                  |
                  |scalafmt {
                  |  enabled = true
                  |}
                  |
                  |// lifecycle {
                  |//   compile {
                  |//     targets = ["__.compile"]
                  |//   }
                  |// }
                  |
                  |// Module-local overrides can live in .config/mill-interceptor/config.pkl.
                  |// Maven install/deploy require a Mill PublishModule-capable surface.
                  |""".stripMargin

    private def plannedFiles(
        repoRoot: Path,
        format: MavenSetupFormat,
        extensionVersion: String
    ): List[GeneratedSetupFile] =
        List(
            GeneratedSetupFile(Path(repoRoot, ".mvn", "extensions.xml"), renderExtensionsXml(extensionVersion)),
            GeneratedSetupFile(Path(repoRoot, configFileName(format)), renderStarterConfig(format))
        )

    private def configFileName(format: MavenSetupFormat): String =
        format match
            case MavenSetupFormat.Yaml => "mill-interceptor.yaml"
            case MavenSetupFormat.Pkl  => "mill-interceptor.pkl"

    private def validateWritable(
        files: List[GeneratedSetupFile],
        force: Boolean
    ): Unit < (Sync & Abort[IllegalArgumentException]) =
        Kyo.foreachDiscard(files) { file =>
            for
                exists <- file.path.exists
                _ <-
                    if !exists || force then Kyo.unit
                    else Abort.fail(new IllegalArgumentException(s"Refusing to overwrite existing file: ${file.path}"))
            yield ()
        }

    private def writeFiles(files: List[GeneratedSetupFile]): Unit < Sync =
        Kyo.foreachDiscard(files) { file =>
            for
                _ <- ensureParentDirectory(file.path)
                _ <- file.path.write(file.content)
            yield ()
        }

    private def ensureParentDirectory(path: Path): Unit < Sync =
        Sync.defer {
            val parent = path.toJava.getParent
            if parent != null then
                val _ = java.nio.file.Files.createDirectories(parent)
            ()
        }

    private def detectRepoRoot(startPath: Path): Path < (Sync & Abort[IllegalArgumentException]) =
        Abort.catching[IllegalArgumentException] {
            Sync.defer {
                val normalizedStart = startPath.toJava.normalize
                @annotation.tailrec
                def loop(current: java.nio.file.Path | Null): Path =
                    current match
                        case null =>
                            throw new IllegalArgumentException(
                                s"Could not find repo root from ${startPath}. Expected a parent containing .git"
                            )
                        case path if path.resolve(".git").toFile.exists() => Path(path.toString)
                        case path                                         => loop(path.getParent)

                loop(normalizedStart)
            }
        }
