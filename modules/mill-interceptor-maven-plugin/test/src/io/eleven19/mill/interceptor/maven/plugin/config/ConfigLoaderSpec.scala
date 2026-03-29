package io.eleven19.mill.interceptor.maven.plugin.config

import zio.test.*

import java.lang.System as JSystem

object ConfigLoaderSpec extends ZIOSpecDefault:

    private def tempPath(name: String): os.Path =
        os.Path(java.nio.file.Paths.get("out", "maven-plugin-config-loader-tests", name).toAbsolutePath)

    def spec: Spec[Any, Any] = suite("ConfigLoader")(
        test("loads YAML-only config") {
            val root       = tempPath(s"yaml-only-${JSystem.nanoTime()}")
            val moduleRoot = root / "module-a"
            val repoYaml   = root / "mill-interceptor.yaml"

            val yaml =
                """mode: strict
                  |mill:
                  |  executable: ./millw
                  |  environment:
                  |    JAVA_HOME: /opt/java
                  |lifecycle:
                  |  compile:
                  |    - app.compile
                  |validate:
                  |  scalafmtEnabled: true
                  |  scalafmtTarget: __.checkFormat
                  |""".stripMargin

            os.remove.all(root)
            writeFile(repoYaml, yaml)
            val loaded = ConfigLoader.load(repoRoot = root, moduleRoot = moduleRoot)
            os.remove.all(root)

            assertTrue(loaded.isRight) &&
            assertTrue(loaded.exists(_.mode == "strict")) &&
            assertTrue(loaded.exists(_.mill.executable == "./millw")) &&
            assertTrue(loaded.exists(_.mill.environment == Map("JAVA_HOME" -> "/opt/java"))) &&
            assertTrue(loaded.exists(_.lifecycle == Map("compile" -> Seq("app.compile")))) &&
            assertTrue(loaded.exists(_.validate.scalafmtEnabled)) &&
            assertTrue(loaded.exists(_.validate.scalafmtTarget.contains("__.checkFormat")))
        },
        test("allows PKL to override YAML-derived values") {
            val root       = tempPath(s"pkl-override-${JSystem.nanoTime()}")
            val moduleRoot = root / "module-b"
            val repoYaml   = root / "mill-interceptor.yaml"
            val repoPkl    = root / "mill-interceptor.pkl"

            val yaml =
                """mill:
                  |  executable: ./millw
                  |lifecycle:
                  |  compile:
                  |    - app.compile
                  |""".stripMargin

            val pkl =
                """mill {
                  |  executable = "./mill"
                  |}
                  |
                  |lifecycle {
                  |  compile = List("core.compile")
                  |}
                  |""".stripMargin

            os.remove.all(root)
            writeFile(repoYaml, yaml)
            writeFile(repoPkl, pkl)
            val loaded = ConfigLoader.load(repoRoot = root, moduleRoot = moduleRoot)
            os.remove.all(root)

            assertTrue(loaded.isRight) &&
            assertTrue(loaded.exists(_.mill.executable == "./mill")) &&
            assertTrue(loaded.exists(_.lifecycle == Map("compile" -> Seq("core.compile"))))
        },
        test("applies module overrides after repository defaults") {
            val root         = tempPath(s"module-override-${JSystem.nanoTime()}")
            val moduleRoot   = root / "module-c"
            val repoYaml     = root / "mill-interceptor.yaml"
            val moduleConfig = moduleRoot / "mill-interceptor.yaml"

            val repoYamlText =
                """lifecycle:
                  |  test:
                  |    - root.test
                  |""".stripMargin

            val moduleYamlText =
                """lifecycle:
                  |  test:
                  |    - module.test
                  |goals:
                  |  inspect-plan:
                  |    - interceptor.inspect
                  |""".stripMargin

            os.remove.all(root)
            writeFile(repoYaml, repoYamlText)
            writeFile(moduleConfig, moduleYamlText)
            val loaded = ConfigLoader.load(repoRoot = root, moduleRoot = moduleRoot)
            os.remove.all(root)

            assertTrue(loaded.isRight) &&
            assertTrue(loaded.exists(_.lifecycle == Map("test" -> Seq("module.test")))) &&
            assertTrue(loaded.exists(_.goals == Map("inspect-plan" -> Seq("interceptor.inspect"))))
        },
        test("reports malformed config with the source path") {
            val root       = tempPath(s"malformed-${JSystem.nanoTime()}")
            val moduleRoot = root / "module-d"
            val repoYaml   = root / "mill-interceptor.yaml"

            os.remove.all(root)
            writeFile(repoYaml, "mill: [")
            val result = ConfigLoader.load(root, moduleRoot)
            os.remove.all(root)

            assertTrue(
                result match
                    case Left(error: ConfigLoadException) =>
                        error.getMessage.contains(repoYaml.toString)
                    case _ => false
            )
        }
    )

    private def writeFile(path: os.Path, contents: String): Unit =
        os.makeDir.all(path / os.up)
        os.write(path, contents)
