package io.eleven19.mill.interceptor.maven.plugin.config

import kyo.*
import kyo.test.KyoSpecDefault
import zio.test.*

import java.lang.System as JSystem
import java.nio.file.Files

object ConfigLoaderSpec extends KyoSpecDefault:

    private def tempPath(name: String): Path =
        Path("out", "maven-plugin-config-loader-tests", name)

    def spec: Spec[Any, Any] = suite("ConfigLoader")(
        test("loads YAML-only config") {
            val root       = tempPath(s"yaml-only-${JSystem.nanoTime()}")
            val moduleRoot = Path(root, "module-a")
            val repoYaml   = Path(root, "mill-interceptor.yaml")

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

            for
                _ <- root.removeAll
                _ <- writeFile(repoYaml, yaml)
                loaded <- ConfigLoader.load(repoRoot = root, moduleRoot = moduleRoot)
                _ <- root.removeAll
            yield
                assertTrue(loaded.mode == "strict") &&
                assertTrue(loaded.mill.executable == "./millw") &&
                assertTrue(loaded.mill.environment == Map("JAVA_HOME" -> "/opt/java")) &&
                assertTrue(loaded.lifecycle == Map("compile" -> Seq("app.compile"))) &&
                assertTrue(loaded.validate.scalafmtEnabled) &&
                assertTrue(loaded.validate.scalafmtTarget.contains("__.checkFormat"))
        },
        test("allows PKL to override YAML-derived values") {
            val root       = tempPath(s"pkl-override-${JSystem.nanoTime()}")
            val moduleRoot = Path(root, "module-b")
            val repoYaml   = Path(root, "mill-interceptor.yaml")
            val repoPkl    = Path(root, "mill-interceptor.pkl")

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

            for
                _ <- root.removeAll
                _ <- writeFile(repoYaml, yaml)
                _ <- writeFile(repoPkl, pkl)
                loaded <- ConfigLoader.load(repoRoot = root, moduleRoot = moduleRoot)
                _ <- root.removeAll
            yield
                assertTrue(loaded.mill.executable == "./mill") &&
                assertTrue(loaded.lifecycle == Map("compile" -> Seq("core.compile")))
        },
        test("applies module overrides after repository defaults") {
            val root         = tempPath(s"module-override-${JSystem.nanoTime()}")
            val moduleRoot   = Path(root, "module-c")
            val repoYaml     = Path(root, "mill-interceptor.yaml")
            val moduleConfig = Path(moduleRoot, "mill-interceptor.yaml")

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

            for
                _ <- root.removeAll
                _ <- writeFile(repoYaml, repoYamlText)
                _ <- writeFile(moduleConfig, moduleYamlText)
                loaded <- ConfigLoader.load(repoRoot = root, moduleRoot = moduleRoot)
                _ <- root.removeAll
            yield
                assertTrue(loaded.lifecycle == Map("test" -> Seq("module.test"))) &&
                assertTrue(loaded.goals == Map("inspect-plan" -> Seq("interceptor.inspect")))
        },
        test("reports malformed config with the source path") {
            val root       = tempPath(s"malformed-${JSystem.nanoTime()}")
            val moduleRoot = Path(root, "module-d")
            val repoYaml   = Path(root, "mill-interceptor.yaml")

            for
                _ <- root.removeAll
                _ <- writeFile(repoYaml, "mill: [")
                exit <- Abort.run[ConfigLoadException](ConfigLoader.load(root, moduleRoot))
                _ <- root.removeAll
            yield
                assertTrue(
                    exit match
                        case kyo.Result.Error(error: ConfigLoadException) =>
                            error.getMessage.contains(repoYaml.toString)
                        case _ => false
                )
        }
    )

    private def writeFile(path: Path, contents: String): Unit < Sync =
        for
            _ <- ensureParentDirectory(path)
            _ <- path.write(contents)
        yield ()

    private def ensureParentDirectory(path: Path): Unit < Sync =
        Sync.defer {
            val parent = path.toJava.getParent
            if parent != null then
                val _ = Files.createDirectories(parent)
            ()
        }
