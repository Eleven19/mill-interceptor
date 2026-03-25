package io.eleven19.mill.interceptor.maven.plugin

import kyo.*
import kyo.test.KyoSpecDefault
import zio.test.*

import java.io.File
import java.lang.System as JSystem
import java.nio.file.{FileVisitResult, Files, Paths, SimpleFileVisitor, StandardCopyOption}
import java.nio.file.attribute.BasicFileAttributes

object MavenPluginIntegrationSpec extends KyoSpecDefault:

    private val mavenVersion = "3.9.13"

    private def tempPath(name: String): Path =
        Path("out", "maven-plugin-itest", name)

    private def absolute(path: Path): String =
        path.toJava.toAbsolutePath.normalize.toString

    def spec: Spec[Any, Any] = suite("MavenPluginIntegrationSpec")(
        test("executes the common lifecycle through extension-only activation with no config") {
            val tempDir    = tempPath(s"minimal-${JSystem.nanoTime()}")
            val localRepo  = Path(tempDir, "m2-repository")
            val fixtureDir = Path(tempDir, "fixture")

            for
                _ <- tempDir.removeAll
                _ <- tempDir.mkDir
                mavenCmd <- resolveMavenExecutable(tempDir)
                _ <- copyFixtureDirectory("fixtures/minimal-lifecycle", fixtureDir)
                _ <- installRepoMillLauncher(fixtureDir)
                install <- installRequiredArtifacts(mavenCmd, tempDir, localRepo)
                validate <- runCommand(
                    Seq(
                        mavenCmd,
                        s"-Dmaven.repo.local=${absolute(localRepo)}",
                        "validate"
                    ),
                    fixtureDir
                )
                compile <- runCommand(
                    Seq(
                        mavenCmd,
                        s"-Dmaven.repo.local=${absolute(localRepo)}",
                        "compile"
                    ),
                    fixtureDir
                )
                testPhase <- runCommand(
                    Seq(
                        mavenCmd,
                        s"-Dmaven.repo.local=${absolute(localRepo)}",
                        "test"
                    ),
                    fixtureDir
                )
                packagePhase <- runCommand(
                    Seq(
                        mavenCmd,
                        s"-Dmaven.repo.local=${absolute(localRepo)}",
                        "package"
                    ),
                    fixtureDir
                )
                verifyPhase <- runCommand(
                    Seq(
                        mavenCmd,
                        s"-Dmaven.repo.local=${absolute(localRepo)}",
                        "verify"
                    ),
                    fixtureDir
                )
                clean <- runCommand(
                    Seq(
                        mavenCmd,
                        s"-Dmaven.repo.local=${absolute(localRepo)}",
                        "clean"
                    ),
                    fixtureDir
                )
                inspectPlan <- runCommand(
                    Seq(
                        mavenCmd,
                        s"-Dmaven.repo.local=${absolute(localRepo)}",
                        "mill-interceptor:inspect-plan"
                    ),
                    fixtureDir
                )
                _ <- tempDir.removeAll
            yield
                assertTrue(install.exitCode == 0) &&
                assertTrue(validate.exitCode == 0) &&
                assertTrue(compile.exitCode == 0) &&
                assertTrue(testPhase.exitCode == 0) &&
                assertTrue(packagePhase.exitCode == 0) &&
                assertTrue(verifyPhase.exitCode == 0) &&
                assertTrue(clean.exitCode == 0) &&
                assertTrue(inspectPlan.exitCode == 0) &&
                assertTrue(inspectPlan.output.contains("Resolved Mill execution plan"))
        },
        test("publishes install and deploy through an extension-only publish-capable fixture") {
            val tempDir    = tempPath(s"publish-${JSystem.nanoTime()}")
            val localRepo  = Path(tempDir, "m2-repository")
            val fixtureDir = Path(tempDir, "fixture")
            val installedPom = Path(
                localRepo,
                "io",
                "eleven19",
                "mill",
                "interceptor",
                "fixture",
                "publish-lifecycle-fixture",
                "0.0.1",
                "publish-lifecycle-fixture-0.0.1.pom"
            )
            val deployedPom = Path(
                fixtureDir,
                "out",
                "maven-repo",
                "io",
                "eleven19",
                "mill",
                "interceptor",
                "fixture",
                "publish-lifecycle-fixture",
                "0.0.1",
                "publish-lifecycle-fixture-0.0.1.pom"
            )

            for
                _ <- tempDir.removeAll
                _ <- tempDir.mkDir
                mavenCmd <- resolveMavenExecutable(tempDir)
                _ <- copyFixtureDirectory("fixtures/publish-lifecycle", fixtureDir)
                _ <- installRepoMillLauncher(fixtureDir)
                install <- installRequiredArtifacts(mavenCmd, tempDir, localRepo)
                installPhase <- runCommand(
                    Seq(
                        mavenCmd,
                        s"-Dmaven.repo.local=${absolute(localRepo)}",
                        "install"
                    ),
                    fixtureDir
                )
                deployPhase <- runCommand(
                    Seq(
                        mavenCmd,
                        s"-Dmaven.repo.local=${absolute(localRepo)}",
                        s"-DaltDeploymentRepository=fixture::default::file:${absolute(Path(fixtureDir, "out", "maven-repo"))}",
                        "deploy"
                    ),
                    fixtureDir
                )
                installedExists <- pathExists(installedPom)
                deployedExists <- pathExists(deployedPom)
                _ <- tempDir.removeAll
            yield
                assertTrue(install.exitCode == 0) &&
                assertTrue(installPhase.exitCode == 0) &&
                assertTrue(deployPhase.exitCode == 0) &&
                assertTrue(installedExists) &&
                assertTrue(deployedExists) &&
                assertTrue(installPhase.output.contains("publishM2Local")) &&
                assertTrue(deployPhase.output.contains("mill-interceptor:0.0.0-SNAPSHOT:deploy")) &&
                assertTrue(deployPhase.output.contains("fixture/mill compile test jar publish"))
        },
        test("applies module-local overrides over repository defaults in a multi-module reactor") {
            val tempDir    = tempPath(s"override-${JSystem.nanoTime()}")
            val localRepo  = Path(tempDir, "m2-repository")
            val fixtureDir = Path(tempDir, "fixture")

            for
                _ <- tempDir.removeAll
                _ <- tempDir.mkDir
                mavenCmd <- resolveMavenExecutable(tempDir)
                _ <- copyFixtureDirectory("fixtures/multi-module-overrides", fixtureDir)
                _ <- installRepoMillLauncher(fixtureDir)
                install <- installRequiredArtifacts(mavenCmd, tempDir, localRepo)
                compileApp <- runCommand(
                    Seq(
                        mavenCmd,
                        s"-Dmaven.repo.local=${absolute(localRepo)}",
                        "-pl",
                        "app",
                        "compile"
                    ),
                    fixtureDir
                )
                _ <- tempDir.removeAll
            yield
                assertTrue(install.exitCode == 0) &&
                assertTrue(compileApp.exitCode == 0) &&
                assertTrue(!compileApp.output.contains("missing.compile")) &&
                assertTrue(compileApp.output.contains("BUILD SUCCESS"))
        },
        test("fails clearly in strict mode when a configured lifecycle target is unavailable") {
            val tempDir    = tempPath(s"strict-${JSystem.nanoTime()}")
            val localRepo  = Path(tempDir, "m2-repository")
            val fixtureDir = Path(tempDir, "fixture")

            for
                _ <- tempDir.removeAll
                _ <- tempDir.mkDir
                mavenCmd <- resolveMavenExecutable(tempDir)
                _ <- copyFixtureDirectory("fixtures/strict-failure", fixtureDir)
                _ <- installRepoMillLauncher(fixtureDir)
                install <- installRequiredArtifacts(mavenCmd, tempDir, localRepo)
                compilePhase <- runCommand(
                    Seq(
                        mavenCmd,
                        s"-Dmaven.repo.local=${absolute(localRepo)}",
                        "compile"
                    ),
                    fixtureDir
                )
                _ <- tempDir.removeAll
            yield
                assertTrue(install.exitCode == 0) &&
                assertTrue(compilePhase.exitCode != 0) &&
                assertTrue(compilePhase.output.contains("BUILD FAILURE"))
        },
        test("publishes locally and executes the placeholder goal through Maven") {
            val tempDir    = tempPath(s"run-${JSystem.nanoTime()}")
            val localRepo  = Path(tempDir, "m2-repository")
            val fixtureDir = Path(tempDir, "fixture")

            for
                _ <- tempDir.removeAll
                _ <- tempDir.mkDir
                mavenCmd <- resolveMavenExecutable(tempDir)
                _ <- copyFixtureDirectory("fixtures/placeholder-goal", fixtureDir)
                install <- installRequiredArtifacts(mavenCmd, tempDir, localRepo)
                validate <- runCommand(
                    Seq(
                        mavenCmd,
                        s"-Dmaven.repo.local=${absolute(localRepo)}",
                        "validate"
                    ),
                    fixtureDir
                )
                _ <- tempDir.removeAll
            yield
                assertTrue(install.exitCode == 0) &&
                assertTrue(validate.exitCode == 0) &&
                assertTrue(validate.output.contains(MavenPluginModule.placeholderMessage))
        },
    )

    private case class CommandResult(exitCode: Int, output: String)

    private def resolveMavenExecutable(tempDir: Path): String < Sync =
        sys.env.get("MVN_CMD") match
            case Some(cmd) => Sync.defer(cmd)
            case None =>
                findOnPath("mvn").map {
                    case Some(cmd) => Sync.defer(cmd)
                    case None      => downloadMaven(tempDir)
                }

    private def requiredPathEnv(name: String): String =
        sys.env.getOrElse(name, throw IllegalStateException(s"Missing required env var: $name"))

    private def installRequiredArtifacts(
        mavenCmd: String,
        tempDir: Path,
        localRepo: Path
    ): CommandResult < Sync =
        val pluginJar = requiredPathEnv("MILL_INTERCEPTOR_MAVEN_PLUGIN_JAR")
        val pluginPom = requiredPathEnv("MILL_INTERCEPTOR_MAVEN_PLUGIN_POM")
        val sharedJar = requiredPathEnv("MILL_INTERCEPTOR_SHARED_JAR")
        val sharedPom = requiredPathEnv("MILL_INTERCEPTOR_SHARED_POM")

        for
            installShared <- runCommand(
                Seq(
                    mavenCmd,
                    s"-Dmaven.repo.local=${absolute(localRepo)}",
                    "org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file",
                    s"-Dfile=$sharedJar",
                    s"-DpomFile=$sharedPom"
                ),
                tempDir
            )
            installPlugin <- runCommand(
                Seq(
                    mavenCmd,
                    s"-Dmaven.repo.local=${absolute(localRepo)}",
                    "org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file",
                    s"-Dfile=$pluginJar",
                    s"-DpomFile=$pluginPom"
                ),
                tempDir
            )
        yield
            CommandResult(
                installShared.exitCode match
                    case 0 => installPlugin.exitCode
                    case code => code,
                s"${installShared.output}\n${installPlugin.output}".trim
            )

    private def findOnPath(command: String): Option[String] < Sync =
        Sync.defer {
            sys.env
                .get("PATH")
                .toSeq
                .flatMap(_.split(File.pathSeparator))
                .map(path => Paths.get(path).resolve(command))
                .find(Files.isExecutable(_))
                .map(_.toString)
        }

    private def downloadMaven(tempDir: Path): String < Sync =
        val archive   = Path(tempDir, s"apache-maven-$mavenVersion-bin.tar.gz")
        val installTo = Path(tempDir, s"apache-maven-$mavenVersion")
        val url =
            s"https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.tar.gz"

        for
            download <- runCommand(
                Seq("curl", "-fsSL", url, "-o", absolute(archive)),
                tempDir
            )
            extract <- runCommand(
                Seq("tar", "-xzf", absolute(archive), "-C", absolute(tempDir)),
                tempDir
            )
            _ <- Sync.defer {
                if download.exitCode != 0 then
                    throw new IllegalStateException(s"Failed to download Maven:\n${download.output}")
            }
            _ <- Sync.defer {
                if extract.exitCode != 0 then
                    throw new IllegalStateException(s"Failed to extract Maven:\n${extract.output}")
            }
        yield absolute(Path(installTo, "bin", "mvn"))

    private def runCommand(command: Seq[String], cwd: Path): CommandResult < Sync =
        val processCommand = Process.Command(command*).cwd(cwd.toJava).redirectErrorStream(true)
        for
            process <- processCommand.spawn
            output <- Sync.defer(new String(process.stdout.readAllBytes()))
            exitCode <- process.waitFor
        yield CommandResult(exitCode, output)

    private def installRepoMillLauncher(targetDir: Path): Unit < Sync =
        Sync.defer {
            val pluginJar = Paths.get(requiredPathEnv("MILL_INTERCEPTOR_MAVEN_PLUGIN_JAR"))
            val source = pluginJar.getParent.getParent.getParent.getParent.getParent.resolve("mill")
            val target = targetDir.toJava.resolve("mill")
            val _ = Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
            val _ = target.toFile.setExecutable(true, false)
            ()
        }

    private def copyFixtureDirectory(resourceDir: String, targetDir: Path): Unit < Sync =
        Sync.defer {
            val resourceUri = MavenPluginIntegrationSpec.getClass.getClassLoader.getResource(resourceDir).toURI
            val sourceDir   = Paths.get(resourceUri)
            val targetRoot  = targetDir.toJava

            val _ = Files.walkFileTree(
                sourceDir,
                new SimpleFileVisitor[java.nio.file.Path]:
                    override def preVisitDirectory(
                        dir: java.nio.file.Path,
                        attrs: BasicFileAttributes
                    ): FileVisitResult =
                        Files.createDirectories(targetRoot.resolve(sourceDir.relativize(dir)))
                        FileVisitResult.CONTINUE

                    override def visitFile(
                        file: java.nio.file.Path,
                        attrs: BasicFileAttributes
                    ): FileVisitResult =
                        Files.copy(
                            file,
                            targetRoot.resolve(sourceDir.relativize(file)),
                            StandardCopyOption.REPLACE_EXISTING
                        )
                        FileVisitResult.CONTINUE
            )
            ()
        }

    private def pathExists(path: Path): Boolean < Sync =
        Sync.defer(path.toJava.toFile.exists())
