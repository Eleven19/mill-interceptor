package io.eleven19.mill.interceptor.maven.plugin

import zio.test.*

import java.io.File
import java.lang.System as JSystem

object MavenPluginIntegrationSpec extends ZIOSpecDefault:

    private val mavenVersion = "3.9.13"

    private def tempPath(name: String): os.Path =
        os.Path(java.nio.file.Paths.get("out", "maven-plugin-itest", name).toAbsolutePath)

    private def absolute(path: os.Path): String =
        path.toNIO.toAbsolutePath.normalize.toString

    def spec: Spec[Any, Any] = suite("MavenPluginIntegrationSpec")(
        test("executes the common lifecycle through extension-only activation with no config") {
            val tempDir    = tempPath(s"minimal-${JSystem.nanoTime()}")
            val localRepo  = tempDir / "m2-repository"
            val fixtureDir = tempDir / "fixture"

            try os.remove.all(tempDir) catch case _: Exception => ()
            os.makeDir.all(tempDir)
            val mavenCmd = resolveMavenExecutable(tempDir)
            copyFixtureDirectory("fixtures/minimal-lifecycle", fixtureDir)
            installRepoMillLauncher(fixtureDir)
            val install = installRequiredArtifacts(mavenCmd, tempDir, localRepo)
            val validate = runCommand(
                Seq(
                    mavenCmd,
                    s"-Dmaven.repo.local=${absolute(localRepo)}",
                    "validate"
                ),
                fixtureDir
            )
            val compile = runCommand(
                Seq(
                    mavenCmd,
                    s"-Dmaven.repo.local=${absolute(localRepo)}",
                    "compile"
                ),
                fixtureDir
            )
            val testPhase = runCommand(
                Seq(
                    mavenCmd,
                    s"-Dmaven.repo.local=${absolute(localRepo)}",
                    "test"
                ),
                fixtureDir
            )
            val packagePhase = runCommand(
                Seq(
                    mavenCmd,
                    s"-Dmaven.repo.local=${absolute(localRepo)}",
                    "package"
                ),
                fixtureDir
            )
            val verifyPhase = runCommand(
                Seq(
                    mavenCmd,
                    s"-Dmaven.repo.local=${absolute(localRepo)}",
                    "verify"
                ),
                fixtureDir
            )
            val clean = runCommand(
                Seq(
                    mavenCmd,
                    s"-Dmaven.repo.local=${absolute(localRepo)}",
                    "clean"
                ),
                fixtureDir
            )
            val inspectPlan = runCommand(
                Seq(
                    mavenCmd,
                    s"-Dmaven.repo.local=${absolute(localRepo)}",
                    "mill-interceptor:inspect-plan"
                ),
                fixtureDir
            )
            try os.remove.all(tempDir) catch case _: Exception => ()

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
            val localRepo  = tempDir / "m2-repository"
            val fixtureDir = tempDir / "fixture"
            val installedPom = localRepo /
                "io" / "eleven19" / "mill" / "interceptor" / "fixture" /
                "publish-lifecycle-fixture" / "0.0.1" /
                "publish-lifecycle-fixture-0.0.1.pom"
            val deployedPom = fixtureDir /
                "out" / "maven-repo" /
                "io" / "eleven19" / "mill" / "interceptor" / "fixture" /
                "publish-lifecycle-fixture" / "0.0.1" /
                "publish-lifecycle-fixture-0.0.1.pom"

            try os.remove.all(tempDir) catch case _: Exception => ()
            os.makeDir.all(tempDir)
            val mavenCmd = resolveMavenExecutable(tempDir)
            copyFixtureDirectory("fixtures/publish-lifecycle", fixtureDir)
            installRepoMillLauncher(fixtureDir)
            val install = installRequiredArtifacts(mavenCmd, tempDir, localRepo)
            val installPhase = runCommand(
                Seq(
                    mavenCmd,
                    s"-Dmaven.repo.local=${absolute(localRepo)}",
                    "install"
                ),
                fixtureDir
            )
            val deployPhase = runCommand(
                Seq(
                    mavenCmd,
                    s"-Dmaven.repo.local=${absolute(localRepo)}",
                    s"-DaltDeploymentRepository=fixture::default::file:${absolute(fixtureDir / "out" / "maven-repo")}",
                    "deploy"
                ),
                fixtureDir
            )
            val installedExists = os.exists(installedPom)
            val deployedExists = os.exists(deployedPom)
            try os.remove.all(tempDir) catch case _: Exception => ()

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
            val localRepo  = tempDir / "m2-repository"
            val fixtureDir = tempDir / "fixture"

            try os.remove.all(tempDir) catch case _: Exception => ()
            os.makeDir.all(tempDir)
            val mavenCmd = resolveMavenExecutable(tempDir)
            copyFixtureDirectory("fixtures/multi-module-overrides", fixtureDir)
            installRepoMillLauncher(fixtureDir)
            val install = installRequiredArtifacts(mavenCmd, tempDir, localRepo)
            val compileApp = runCommand(
                Seq(
                    mavenCmd,
                    s"-Dmaven.repo.local=${absolute(localRepo)}",
                    "-pl",
                    "app",
                    "compile"
                ),
                fixtureDir
            )
            try os.remove.all(tempDir) catch case _: Exception => ()

            assertTrue(install.exitCode == 0) &&
            assertTrue(compileApp.exitCode == 0) &&
            assertTrue(!compileApp.output.contains("missing.compile")) &&
            assertTrue(compileApp.output.contains("BUILD SUCCESS"))
        },
        test("applies module-level PKL overrides over repository YAML in a multi-module reactor") {
            val tempDir    = tempPath(s"pkl-override-${JSystem.nanoTime()}")
            val localRepo  = tempDir / "m2-repository"
            val fixtureDir = tempDir / "fixture"

            try os.remove.all(tempDir) catch case _: Exception => ()
            os.makeDir.all(tempDir)
            val mavenCmd = resolveMavenExecutable(tempDir)
            copyFixtureDirectory("fixtures/pkl-module-override", fixtureDir)
            installRepoMillLauncher(fixtureDir)
            val install = installRequiredArtifacts(mavenCmd, tempDir, localRepo)
            val compileLib = runCommand(
                Seq(
                    mavenCmd,
                    s"-Dmaven.repo.local=${absolute(localRepo)}",
                    "-pl",
                    "lib",
                    "compile"
                ),
                fixtureDir
            )
            val compileApp = runCommand(
                Seq(
                    mavenCmd,
                    s"-Dmaven.repo.local=${absolute(localRepo)}",
                    "-pl",
                    "app",
                    "compile"
                ),
                fixtureDir
            )
            try os.remove.all(tempDir) catch case _: Exception => ()

            assertTrue(install.exitCode == 0) &&
            assertTrue(compileLib.exitCode != 0) &&
            assertTrue(compileLib.output.contains("missing.compile")) &&
            assertTrue(compileApp.exitCode == 0) &&
            assertTrue(!compileApp.output.contains("missing.compile")) &&
            assertTrue(compileApp.output.contains("BUILD SUCCESS"))
        },
        test("fails clearly in strict mode when a configured lifecycle target is unavailable") {
            val tempDir    = tempPath(s"strict-${JSystem.nanoTime()}")
            val localRepo  = tempDir / "m2-repository"
            val fixtureDir = tempDir / "fixture"

            try os.remove.all(tempDir) catch case _: Exception => ()
            os.makeDir.all(tempDir)
            val mavenCmd = resolveMavenExecutable(tempDir)
            copyFixtureDirectory("fixtures/strict-failure", fixtureDir)
            installRepoMillLauncher(fixtureDir)
            val install = installRequiredArtifacts(mavenCmd, tempDir, localRepo)
            val compilePhase = runCommand(
                Seq(
                    mavenCmd,
                    s"-Dmaven.repo.local=${absolute(localRepo)}",
                    "compile"
                ),
                fixtureDir
            )
            try os.remove.all(tempDir) catch case _: Exception => ()

            assertTrue(install.exitCode == 0) &&
            assertTrue(compilePhase.exitCode != 0) &&
            assertTrue(compilePhase.output.contains("BUILD FAILURE"))
        },
        test("publishes locally and executes the placeholder goal through Maven") {
            val tempDir    = tempPath(s"run-${JSystem.nanoTime()}")
            val localRepo  = tempDir / "m2-repository"
            val fixtureDir = tempDir / "fixture"

            try os.remove.all(tempDir) catch case _: Exception => ()
            os.makeDir.all(tempDir)
            val mavenCmd = resolveMavenExecutable(tempDir)
            copyFixtureDirectory("fixtures/placeholder-goal", fixtureDir)
            val install = installRequiredArtifacts(mavenCmd, tempDir, localRepo)
            val validate = runCommand(
                Seq(
                    mavenCmd,
                    s"-Dmaven.repo.local=${absolute(localRepo)}",
                    "validate"
                ),
                fixtureDir
            )
            try os.remove.all(tempDir) catch case _: Exception => ()

            assertTrue(install.exitCode == 0) &&
            assertTrue(validate.exitCode == 0) &&
            assertTrue(validate.output.contains(MavenPluginModule.placeholderMessage))
        },
    )

    private case class CommandResult(exitCode: Int, output: String)

    private def resolveMavenExecutable(tempDir: os.Path): String =
        sys.env.get("MVN_CMD") match
            case Some(cmd) => cmd
            case None =>
                findOnPath("mvn") match
                    case Some(cmd) => cmd
                    case None      => downloadMaven(tempDir)

    private def requiredPathEnv(name: String): String =
        sys.env.getOrElse(name, throw IllegalStateException(s"Missing required env var: $name"))

    private def installRequiredArtifacts(
        mavenCmd: String,
        tempDir: os.Path,
        localRepo: os.Path
    ): CommandResult =
        val pluginJar = requiredPathEnv("MILL_INTERCEPTOR_MAVEN_PLUGIN_JAR")
        val pluginPom = requiredPathEnv("MILL_INTERCEPTOR_MAVEN_PLUGIN_POM")
        val sharedJar = requiredPathEnv("MILL_INTERCEPTOR_SHARED_JAR")
        val sharedPom = requiredPathEnv("MILL_INTERCEPTOR_SHARED_POM")

        val installShared = runCommand(
            Seq(
                mavenCmd,
                s"-Dmaven.repo.local=${absolute(localRepo)}",
                "org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file",
                s"-Dfile=$sharedJar",
                s"-DpomFile=$sharedPom"
            ),
            tempDir
        )
        val installPlugin = runCommand(
            Seq(
                mavenCmd,
                s"-Dmaven.repo.local=${absolute(localRepo)}",
                "org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file",
                s"-Dfile=$pluginJar",
                s"-DpomFile=$pluginPom"
            ),
            tempDir
        )
        CommandResult(
            installShared.exitCode match
                case 0 => installPlugin.exitCode
                case code => code,
            s"${installShared.output}\n${installPlugin.output}".trim
        )

    private def findOnPath(command: String): Option[String] =
        sys.env
            .get("PATH")
            .toSeq
            .flatMap(_.split(File.pathSeparator))
            .map(path => java.nio.file.Paths.get(path).resolve(command))
            .find(java.nio.file.Files.isExecutable(_))
            .map(_.toString)

    private def downloadMaven(tempDir: os.Path): String =
        val archive   = tempDir / s"apache-maven-$mavenVersion-bin.tar.gz"
        val installTo = tempDir / s"apache-maven-$mavenVersion"
        val url =
            s"https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.tar.gz"

        val download = runCommand(
            Seq("curl", "-fsSL", url, "-o", absolute(archive)),
            tempDir
        )
        val extract = runCommand(
            Seq("tar", "-xzf", absolute(archive), "-C", absolute(tempDir)),
            tempDir
        )
        if download.exitCode != 0 then
            throw new IllegalStateException(s"Failed to download Maven:\n${download.output}")
        if extract.exitCode != 0 then
            throw new IllegalStateException(s"Failed to extract Maven:\n${extract.output}")
        absolute(installTo / "bin" / "mvn")

    private def runCommand(command: Seq[String], cwd: os.Path): CommandResult =
        val result = os.proc(command).call(cwd = cwd, mergeErrIntoOut = true, check = false)
        CommandResult(result.exitCode, result.out.text())

    private def installRepoMillLauncher(targetDir: os.Path): Unit =
        val pluginJar = java.nio.file.Paths.get(requiredPathEnv("MILL_INTERCEPTOR_MAVEN_PLUGIN_JAR"))
        val source = pluginJar.getParent.getParent.getParent.getParent.getParent.resolve("mill")
        val target = targetDir / "mill"
        os.copy(os.Path(source), target, replaceExisting = true)
        val _ = target.toIO.setExecutable(true, false)

    private def copyFixtureDirectory(resourceDir: String, targetDir: os.Path): Unit =
        val resourceUri = MavenPluginIntegrationSpec.getClass.getClassLoader.getResource(resourceDir).toURI
        val sourceDir   = os.Path(java.nio.file.Paths.get(resourceUri))
        os.copy(sourceDir, targetDir, replaceExisting = true, createFolders = true)
