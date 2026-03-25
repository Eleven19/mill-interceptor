package io.eleven19.mill.interceptor.maven

import kyo.*
import kyo.test.KyoSpecDefault
import zio.test.*
import java.lang.System as JSystem
import java.io.StringReader
import java.nio.file.{Files, Paths}
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

object MavenSetupGeneratorSpec extends KyoSpecDefault:

    private def tempPath(name: String): Path =
        Path("out", "mill-interceptor-tests", name)

    private def fsPath(path: Path): java.nio.file.Path =
        path.toJava.toAbsolutePath.normalize

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
                Sync.defer {
                    val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    builder.parse(InputSource(StringReader(xml)))
                    assertTrue(xml.contains("io.eleven19.mill-interceptor")) &&
                    assertTrue(xml.contains("mill-interceptor-maven-plugin")) &&
                    assertTrue(xml.contains("1.2.3")) &&
                    assertTrue(!xml.contains("\\\""))
                }
            },
            test("extensions xml escapes XML-special characters in the version") {
                val xml = MavenSetupGenerator.renderExtensionsXml("1.0&beta<2>")
                Sync.defer {
                    val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    builder.parse(InputSource(StringReader(xml)))
                    assertTrue(xml.contains("1.0&amp;beta&lt;2&gt;")) &&
                    assertTrue(!xml.contains("1.0&beta<2>"))
                }
            },
            test("yaml starter is the default file content") {
                val yaml = MavenSetupGenerator.renderStarterConfig(MavenSetupFormat.Yaml)
                Sync.defer(
                    assertTrue(yaml.contains("# Optional overrides for the Maven extension baseline")) &&
                    assertTrue(yaml.contains("validate:")) &&
                    assertTrue(yaml.contains("scalafmtEnabled: true")) &&
                    assertTrue(yaml.contains("#   compile:")) &&
                    assertTrue(yaml.contains("#     - __.compile"))
                )
            },
            test("pkl starter uses the plugin config schema") {
                val pkl = MavenSetupGenerator.renderStarterConfig(MavenSetupFormat.Pkl)
                Sync.defer(
                    assertTrue(pkl.contains("validate {")) &&
                    assertTrue(pkl.contains("scalafmtEnabled = true")) &&
                    assertTrue(pkl.contains("""//   compile = List("__.compile")"""))
                )
            }
        ),
        suite("generate")(
            test("writes extensions xml and yaml starter config into the repo root") {
                val repoRoot = tempPath(s"maven-setup-${JSystem.nanoTime()}")
                val nested   = Path(repoRoot, "modules", "app")
                val xmlPath  = fsPath(repoRoot).resolve(".mvn").resolve("extensions.xml")
                val yamlPath = fsPath(repoRoot).resolve("mill-interceptor.yaml")

                for
                    _ <- repoRoot.removeAll
                    _ <- nested.mkDir
                    _ <- Path(repoRoot, ".git").mkDir
                    generated <- generateSuccess(
                        nested,
                        MavenSetupOptions()
                    )
                    xmlExists <- Sync.defer(Files.exists(xmlPath))
                    yamlExists <- Sync.defer(Files.exists(yamlPath))
                    xml <- Sync.defer(Files.readString(xmlPath))
                    yaml <- Sync.defer(Files.readString(yamlPath))
                    _ <- repoRoot.removeAll
                yield
                    assertTrue(generated.map(_.path).toSet == Set(Path(".mvn", "extensions.xml"), Path("mill-interceptor.yaml"))) &&
                    assertTrue(xmlExists) &&
                    assertTrue(yamlExists) &&
                    assertTrue(xml.contains("1.2.3")) &&
                    assertTrue(yaml.contains("scalafmtEnabled: true"))
            },
            test("dry run reports planned files without writing them") {
                val repoRoot = tempPath(s"maven-setup-dry-run-${JSystem.nanoTime()}")
                val nested   = Path(repoRoot, "modules", "app")
                val xmlPath  = fsPath(repoRoot).resolve(".mvn").resolve("extensions.xml")
                val yamlPath = fsPath(repoRoot).resolve("mill-interceptor.yaml")

                for
                    _ <- repoRoot.removeAll
                    _ <- nested.mkDir
                    _ <- Path(repoRoot, ".git").mkDir
                    generated <- generateSuccess(
                        nested,
                        MavenSetupOptions(dryRun = true)
                    )
                    xmlExists <- Sync.defer(Files.exists(xmlPath))
                    yamlExists <- Sync.defer(Files.exists(yamlPath))
                    _ <- repoRoot.removeAll
                yield
                    assertTrue(generated.map(_.path).toSet == Set(Path(".mvn", "extensions.xml"), Path("mill-interceptor.yaml"))) &&
                    assertTrue(!xmlExists) &&
                    assertTrue(!yamlExists)
            },
            test("existing files fail without force") {
                val repoRoot = tempPath(s"maven-setup-existing-${JSystem.nanoTime()}")
                val nested   = Path(repoRoot, "modules", "app")
                val xmlPath  = fsPath(repoRoot).resolve(".mvn").resolve("extensions.xml")

                for
                    _ <- repoRoot.removeAll
                    _ <- nested.mkDir
                    _ <- Path(repoRoot, ".git").mkDir
                    _ <- Sync.defer(Files.createDirectories(xmlPath.getParent))
                    _ <- Sync.defer(Files.writeString(xmlPath, "existing"))
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
                val xmlPath  = fsPath(repoRoot).resolve(".mvn").resolve("extensions.xml")

                for
                    _ <- repoRoot.removeAll
                    _ <- nested.mkDir
                    _ <- Path(repoRoot, ".git").mkDir
                    _ <- Sync.defer(Files.createDirectories(xmlPath.getParent))
                    _ <- Sync.defer(Files.writeString(xmlPath, "existing"))
                    _ <- generateSuccess(
                        nested,
                        MavenSetupOptions(force = true)
                    )
                    xml <- Sync.defer(Files.readString(xmlPath))
                    _ <- repoRoot.removeAll
                yield assertTrue(xml.contains("1.2.3"))
            },
            test("pkl format writes mill-interceptor.pkl instead of yaml") {
                val repoRoot = tempPath(s"maven-setup-pkl-${JSystem.nanoTime()}")
                val nested   = Path(repoRoot, "modules", "app")
                val pklPath  = fsPath(repoRoot).resolve("mill-interceptor.pkl")
                val yamlPath = fsPath(repoRoot).resolve("mill-interceptor.yaml")

                for
                    _ <- repoRoot.removeAll
                    _ <- nested.mkDir
                    _ <- Path(repoRoot, ".git").mkDir
                    generated <- generateSuccess(
                        nested,
                        MavenSetupOptions(format = MavenSetupFormat.Pkl)
                    )
                    pklExists  <- Sync.defer(Files.exists(pklPath))
                    yamlExists <- Sync.defer(Files.exists(yamlPath))
                    pkl <- Sync.defer(Files.readString(pklPath))
                    _ <- repoRoot.removeAll
                yield
                    assertTrue(generated.map(_.path).toSet == Set(Path(".mvn", "extensions.xml"), Path("mill-interceptor.pkl"))) &&
                    assertTrue(pklExists) &&
                    assertTrue(!yamlExists) &&
                    assertTrue(pkl.contains("scalafmtEnabled = true"))
            }
        )
    )
