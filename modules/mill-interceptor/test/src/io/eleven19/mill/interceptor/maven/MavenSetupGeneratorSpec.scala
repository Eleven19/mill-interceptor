package io.eleven19.mill.interceptor.maven

import zio.test.*
import java.lang.System as JSystem
import java.io.StringReader
import java.nio.file.{Files, Paths}
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

given CanEqual[os.Path, os.Path] = CanEqual.derived

object MavenSetupGeneratorSpec extends ZIOSpecDefault:

    private def tempPath(name: String): os.Path =
        os.Path(Paths.get("out", "mill-interceptor-tests", name).toAbsolutePath)

    private def generateSuccess(
        startPath: os.Path,
        options: MavenSetupOptions,
        extensionVersion: String = "1.2.3"
    ): List[GeneratedSetupFile] =
        MavenSetupGenerator.generate(startPath, options, extensionVersion) match
            case Right(value) => value
            case Left(ex)     => throw new AssertionError(s"Expected success but got: $ex")

    def spec: Spec[Any, Any] = suite("MavenSetupGenerator")(
        suite("render")(
            test("extensions xml uses published coordinates") {
                val xml = MavenSetupGenerator.renderExtensionsXml("1.2.3")
                val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                builder.parse(InputSource(StringReader(xml)))
                assertTrue(xml.contains("io.eleven19.mill-interceptor")) &&
                assertTrue(xml.contains("mill-interceptor-maven-plugin")) &&
                assertTrue(xml.contains("1.2.3")) &&
                assertTrue(!xml.contains("\\\""))
            },
            test("extensions xml escapes XML-special characters in the version") {
                val xml = MavenSetupGenerator.renderExtensionsXml("1.0&beta<2>")
                val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                builder.parse(InputSource(StringReader(xml)))
                assertTrue(xml.contains("1.0&amp;beta&lt;2&gt;")) &&
                assertTrue(!xml.contains("1.0&beta<2>"))
            },
            test("yaml starter is the default file content") {
                val yaml = MavenSetupGenerator.renderStarterConfig(MavenSetupFormat.Yaml)
                assertTrue(yaml.contains("# Optional overrides for the Maven extension baseline")) &&
                assertTrue(yaml.contains("validate:")) &&
                assertTrue(yaml.contains("scalafmtEnabled: true")) &&
                assertTrue(yaml.contains("#   compile:")) &&
                assertTrue(yaml.contains("#     - __.compile"))
            },
            test("pkl starter uses the plugin config schema") {
                val pkl = MavenSetupGenerator.renderStarterConfig(MavenSetupFormat.Pkl)
                assertTrue(pkl.contains("validate {")) &&
                assertTrue(pkl.contains("scalafmtEnabled = true")) &&
                assertTrue(pkl.contains("""//   compile = List("__.compile")"""))
            }
        ),
        suite("generate")(
            test("writes extensions xml and yaml starter config into the repo root") {
                val repoRoot = tempPath(s"maven-setup-${JSystem.nanoTime()}")
                val nested   = repoRoot / "modules" / "app"
                val xmlPath  = repoRoot / ".mvn" / "extensions.xml"
                val yamlPath = repoRoot / "mill-interceptor.yaml"

                os.remove.all(repoRoot)
                os.makeDir.all(nested)
                os.makeDir.all(repoRoot / ".git")
                val generated = generateSuccess(nested, MavenSetupOptions())
                val xmlExists  = os.exists(xmlPath)
                val yamlExists = os.exists(yamlPath)
                val xml        = os.read(xmlPath)
                val yaml       = os.read(yamlPath)
                os.remove.all(repoRoot)

                assertTrue(generated.map(_.path).toSet == Set(xmlPath, yamlPath)) &&
                assertTrue(xmlExists) &&
                assertTrue(yamlExists) &&
                assertTrue(xml.contains("1.2.3")) &&
                assertTrue(yaml.contains("scalafmtEnabled: true"))
            },
            test("dry run reports planned files without writing them") {
                val repoRoot = tempPath(s"maven-setup-dry-run-${JSystem.nanoTime()}")
                val nested   = repoRoot / "modules" / "app"
                val xmlPath  = repoRoot / ".mvn" / "extensions.xml"
                val yamlPath = repoRoot / "mill-interceptor.yaml"

                os.remove.all(repoRoot)
                os.makeDir.all(nested)
                os.makeDir.all(repoRoot / ".git")
                val generated = generateSuccess(nested, MavenSetupOptions(dryRun = true))
                val xmlExists  = os.exists(xmlPath)
                val yamlExists = os.exists(yamlPath)
                os.remove.all(repoRoot)

                assertTrue(generated.map(_.path).toSet == Set(xmlPath, yamlPath)) &&
                assertTrue(!xmlExists) &&
                assertTrue(!yamlExists)
            },
            test("existing files fail without force") {
                val repoRoot = tempPath(s"maven-setup-existing-${JSystem.nanoTime()}")
                val nested   = repoRoot / "modules" / "app"
                val xmlPath  = repoRoot / ".mvn" / "extensions.xml"

                os.remove.all(repoRoot)
                os.makeDir.all(nested)
                os.makeDir.all(repoRoot / ".git")
                os.makeDir.all(xmlPath / os.up)
                os.write(xmlPath, "existing")
                val result = MavenSetupGenerator.generate(nested, MavenSetupOptions(), extensionVersion = "1.2.3")
                os.remove.all(repoRoot)

                result match
                    case Left(ex: IllegalArgumentException) =>
                        assertTrue(ex.getMessage.contains("overwrite existing file"))
                    case _ => assertTrue(false)
            },
            test("force overwrites existing files") {
                val repoRoot = tempPath(s"maven-setup-force-${JSystem.nanoTime()}")
                val nested   = repoRoot / "modules" / "app"
                val xmlPath  = repoRoot / ".mvn" / "extensions.xml"

                os.remove.all(repoRoot)
                os.makeDir.all(nested)
                os.makeDir.all(repoRoot / ".git")
                os.makeDir.all(xmlPath / os.up)
                os.write(xmlPath, "existing")
                val _ = generateSuccess(nested, MavenSetupOptions(force = true))
                val xml = os.read(xmlPath)
                os.remove.all(repoRoot)

                assertTrue(xml.contains("1.2.3"))
            },
            test("pkl format writes mill-interceptor.pkl instead of yaml") {
                val repoRoot = tempPath(s"maven-setup-pkl-${JSystem.nanoTime()}")
                val nested   = repoRoot / "modules" / "app"
                val pklPath  = repoRoot / "mill-interceptor.pkl"
                val yamlPath = repoRoot / "mill-interceptor.yaml"

                os.remove.all(repoRoot)
                os.makeDir.all(nested)
                os.makeDir.all(repoRoot / ".git")
                val generated = generateSuccess(nested, MavenSetupOptions(format = MavenSetupFormat.Pkl))
                val pklExists  = os.exists(pklPath)
                val yamlExists = os.exists(yamlPath)
                val pkl        = os.read(pklPath)
                os.remove.all(repoRoot)

                assertTrue(generated.map(_.path).toSet == Set(xmlPath(repoRoot), pklPath)) &&
                assertTrue(pklExists) &&
                assertTrue(!yamlExists) &&
                assertTrue(pkl.contains("scalafmtEnabled = true"))
            }
        )
    )

    private def xmlPath(repoRoot: os.Path): os.Path = repoRoot / ".mvn" / "extensions.xml"
