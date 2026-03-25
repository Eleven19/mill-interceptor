package io.eleven19.mill.interceptor.maven

import kyo.*
import kyo.test.KyoSpecDefault
import zio.test.*
import java.lang.System as JSystem

object MavenSetupGeneratorSpec extends KyoSpecDefault:

    private def tempPath(name: String): Path =
        Path("out", "mill-interceptor-tests", name)

    private def generateSuccess(
        startPath: Path,
        options: MavenSetupOptions,
        extensionVersion: String = "1.2.3"
    ): List[GeneratedSetupFile] < Sync =
        Abort.run[IllegalArgumentException](MavenSetupGenerator.generate(startPath, options, extensionVersion)).map {
            case kyo.Result.Success(value) => value
            case other                     => throw new AssertionError(s"Expected success but got: $other")
        }

    def spec: Spec[Any, Any] = suite("MavenSetupGenerator")(
        suite("render")(
            test("extensions xml uses published coordinates") {
                val xml = MavenSetupGenerator.renderExtensionsXml("1.2.3")
                Sync.defer(
                    assertTrue(xml.contains("io.eleven19.mill-interceptor")) &&
                    assertTrue(xml.contains("mill-interceptor-maven-plugin")) &&
                    assertTrue(xml.contains("1.2.3"))
                )
            },
            test("yaml starter is the default file content") {
                val yaml = MavenSetupGenerator.renderStarterConfig(MavenSetupFormat.Yaml)
                Sync.defer(
                    assertTrue(yaml.contains("# Optional overrides for the Maven extension baseline")) &&
                    assertTrue(yaml.contains("scalafmt:"))
                )
            }
        ),
        suite("generate")(
            test("writes extensions xml and yaml starter config into the repo root") {
                val repoRoot = tempPath(s"maven-setup-${JSystem.nanoTime()}")
                val nested   = Path(repoRoot, "modules", "app")
                val xmlPath  = Path(repoRoot, ".mvn", "extensions.xml")
                val yamlPath = Path(repoRoot, "mill-interceptor.yaml")

                for
                    _ <- repoRoot.removeAll
                    _ <- nested.mkDir
                    _ <- Path(repoRoot, ".git").mkDir
                    generated <- generateSuccess(
                        nested,
                        MavenSetupOptions()
                    )
                    xmlExists <- xmlPath.exists
                    yamlExists <- yamlPath.exists
                    xml <- xmlPath.read
                    yaml <- yamlPath.read
                    _ <- repoRoot.removeAll
                yield
                    assertTrue(generated.map(_.path).toSet == Set(xmlPath, yamlPath)) &&
                    assertTrue(xmlExists) &&
                    assertTrue(yamlExists) &&
                    assertTrue(xml.contains("1.2.3")) &&
                    assertTrue(yaml.contains("scalafmt:"))
            },
            test("dry run reports planned files without writing them") {
                val repoRoot = tempPath(s"maven-setup-dry-run-${JSystem.nanoTime()}")
                val nested   = Path(repoRoot, "modules", "app")
                val xmlPath  = Path(repoRoot, ".mvn", "extensions.xml")
                val yamlPath = Path(repoRoot, "mill-interceptor.yaml")

                for
                    _ <- repoRoot.removeAll
                    _ <- nested.mkDir
                    _ <- Path(repoRoot, ".git").mkDir
                    generated <- generateSuccess(
                        nested,
                        MavenSetupOptions(dryRun = true)
                    )
                    xmlExists <- xmlPath.exists
                    yamlExists <- yamlPath.exists
                    _ <- repoRoot.removeAll
                yield
                    assertTrue(generated.map(_.path).toSet == Set(xmlPath, yamlPath)) &&
                    assertTrue(!xmlExists) &&
                    assertTrue(!yamlExists)
            },
            test("existing files fail without force") {
                val repoRoot = tempPath(s"maven-setup-existing-${JSystem.nanoTime()}")
                val nested   = Path(repoRoot, "modules", "app")
                val xmlPath  = Path(repoRoot, ".mvn", "extensions.xml")

                for
                    _ <- repoRoot.removeAll
                    _ <- nested.mkDir
                    _ <- Path(repoRoot, ".git").mkDir
                    _ <- Path(repoRoot, ".mvn").mkDir
                    _ <- xmlPath.write("existing")
                    result <- Abort.run[IllegalArgumentException](
                        MavenSetupGenerator.generate(nested, MavenSetupOptions(), extensionVersion = "1.2.3")
                    )
                    _ <- repoRoot.removeAll
                yield result match
                    case kyo.Result.Error(ex: IllegalArgumentException) =>
                        assertTrue(ex.getMessage.contains("overwrite existing file"))
                    case _ => assertTrue(false)
            },
            test("force overwrites existing files") {
                val repoRoot = tempPath(s"maven-setup-force-${JSystem.nanoTime()}")
                val nested   = Path(repoRoot, "modules", "app")
                val xmlPath  = Path(repoRoot, ".mvn", "extensions.xml")

                for
                    _ <- repoRoot.removeAll
                    _ <- nested.mkDir
                    _ <- Path(repoRoot, ".git").mkDir
                    _ <- Path(repoRoot, ".mvn").mkDir
                    _ <- xmlPath.write("existing")
                    _ <- generateSuccess(
                        nested,
                        MavenSetupOptions(force = true)
                    )
                    xml <- xmlPath.read
                    _ <- repoRoot.removeAll
                yield assertTrue(xml.contains("1.2.3"))
            }
        )
    )
