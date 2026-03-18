package io.eleven19.mill.interceptor.maven.plugin.config

import kyo.*
import kyo.test.KyoSpecDefault
import zio.test.*

import java.lang.System as JSystem
import java.nio.file.Files

object ConfigDiscoverySpec extends KyoSpecDefault:

    private def tempPath(name: String): Path =
        Path("out", "maven-plugin-config-discovery-tests", name)

    def spec: Spec[Any, Any] = suite("ConfigDiscovery")(
        test("discovers repository and module config files in deterministic precedence order") {
            val root       = tempPath(s"discovery-order-${JSystem.nanoTime()}")
            val moduleRoot = Path(root, "module-a")

            val expected = Seq(
              Path(root, "mill-interceptor.yaml"),
              Path(root, "mill-interceptor.pkl"),
              Path(root, ".config", "mill-interceptor", "config.yaml"),
              Path(root, ".config", "mill-interceptor", "config.pkl"),
              Path(moduleRoot, "mill-interceptor.yaml"),
              Path(moduleRoot, "mill-interceptor.pkl"),
              Path(moduleRoot, ".config", "mill-interceptor", "config.yaml"),
              Path(moduleRoot, ".config", "mill-interceptor", "config.pkl")
            )

            for
                _ <- root.removeAll
                _ <- createFiles(expected*)
                discovered <- ConfigDiscovery.discover(root, moduleRoot)
                _ <- root.removeAll
            yield
                assertTrue(discovered.map(_.path) == expected) &&
                assertTrue(
                    discovered.map(_.scope) ==
                        Seq(
                          ConfigScope.Repository,
                          ConfigScope.Repository,
                          ConfigScope.Repository,
                          ConfigScope.Repository,
                          ConfigScope.Module,
                          ConfigScope.Module,
                          ConfigScope.Module,
                          ConfigScope.Module
                        )
                ) &&
                assertTrue(
                    discovered.map(_.format) ==
                        Seq(
                          ConfigFormat.Yaml,
                          ConfigFormat.Pkl,
                          ConfigFormat.Yaml,
                          ConfigFormat.Pkl,
                          ConfigFormat.Yaml,
                          ConfigFormat.Pkl,
                          ConfigFormat.Yaml,
                          ConfigFormat.Pkl
                        )
                )
        },
        test("returns only config files that exist") {
            val root       = tempPath(s"discovery-existing-${JSystem.nanoTime()}")
            val moduleRoot = Path(root, "module-b")
            val repoYaml   = Path(root, "mill-interceptor.yaml")
            val modulePkl  = Path(moduleRoot, ".config", "mill-interceptor", "config.pkl")

            for
                _ <- root.removeAll
                _ <- createFiles(repoYaml, modulePkl)
                discovered <- ConfigDiscovery.discover(root, moduleRoot)
                _ <- root.removeAll
            yield
                assertTrue(discovered.map(_.path) == Seq(repoYaml, modulePkl)) &&
                assertTrue(discovered.map(_.scope) == Seq(ConfigScope.Repository, ConfigScope.Module)) &&
                assertTrue(discovered.map(_.format) == Seq(ConfigFormat.Yaml, ConfigFormat.Pkl))
        }
    )

    private def createFiles(paths: Path*): Unit < Sync =
        Kyo.foreachDiscard(paths) { path =>
            for
                _ <- ensureParentDirectory(path)
                _ <- path.write("test: true")
            yield ()
        }

    private def ensureParentDirectory(path: Path): Unit < Sync =
        Sync.defer {
            val parent = path.toJava.getParent
            if parent != null then
                val _ = Files.createDirectories(parent)
            ()
        }
