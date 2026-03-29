package io.eleven19.mill.interceptor.maven.plugin.config

import zio.test.*

import java.lang.System as JSystem

given canEqualOsPath: CanEqual[os.Path, os.Path] = CanEqual.derived

object ConfigDiscoverySpec extends ZIOSpecDefault:

    private def tempPath(name: String): os.Path =
        os.Path(java.nio.file.Paths.get("out", "maven-plugin-config-discovery-tests", name).toAbsolutePath)

    def spec: Spec[Any, Any] = suite("ConfigDiscovery")(
        test("discovers repository and module config files in deterministic precedence order") {
            val root       = tempPath(s"discovery-order-${JSystem.nanoTime()}")
            val moduleRoot = root / "module-a"

            val expected = Seq(
              root / "mill-interceptor.yaml",
              root / "mill-interceptor.pkl",
              root / ".config" / "mill-interceptor" / "config.yaml",
              root / ".config" / "mill-interceptor" / "config.pkl",
              moduleRoot / "mill-interceptor.yaml",
              moduleRoot / "mill-interceptor.pkl",
              moduleRoot / ".config" / "mill-interceptor" / "config.yaml",
              moduleRoot / ".config" / "mill-interceptor" / "config.pkl"
            )

            os.remove.all(root)
            createFiles(expected*)
            val discovered = ConfigDiscovery.discover(root, moduleRoot)
            os.remove.all(root)

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
            val moduleRoot = root / "module-b"
            val repoYaml   = root / "mill-interceptor.yaml"
            val modulePkl  = moduleRoot / ".config" / "mill-interceptor" / "config.pkl"

            os.remove.all(root)
            createFiles(repoYaml, modulePkl)
            val discovered = ConfigDiscovery.discover(root, moduleRoot)
            os.remove.all(root)

            assertTrue(discovered.map(_.path) == Seq(repoYaml, modulePkl)) &&
            assertTrue(discovered.map(_.scope) == Seq(ConfigScope.Repository, ConfigScope.Module)) &&
            assertTrue(discovered.map(_.format) == Seq(ConfigFormat.Yaml, ConfigFormat.Pkl))
        }
    )

    private def createFiles(paths: os.Path*): Unit =
        paths.foreach { path =>
            os.makeDir.all(path / os.up)
            os.write(path, "test: true")
        }
