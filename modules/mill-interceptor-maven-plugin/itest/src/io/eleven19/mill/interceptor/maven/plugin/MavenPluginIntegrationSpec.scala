package io.eleven19.mill.interceptor.maven.plugin

import kyo.test.KyoSpecDefault
import zio.test.*

import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import scala.sys.process.*

object MavenPluginIntegrationSpec extends KyoSpecDefault:

    private val mavenVersion = "3.9.13"

    def spec: Spec[Any, Any] = suite("MavenPluginIntegrationSpec")(
        test("publishes locally and executes the placeholder goal through Maven") {
            val tempDir    = Files.createTempDirectory("mill-interceptor-maven-plugin-itest")
            val localRepo  = tempDir.resolve("m2-repository")
            val fixtureDir = tempDir.resolve("fixture")
            val mavenCmd   = resolveMavenExecutable(tempDir)
            val pluginJar  = requiredPathEnv("MILL_INTERCEPTOR_MAVEN_PLUGIN_JAR")
            val pluginPom  = requiredPathEnv("MILL_INTERCEPTOR_MAVEN_PLUGIN_POM")

            copyFixtureDirectory("fixtures/placeholder-goal", fixtureDir)

            val install = runCommand(
                Seq(
                    mavenCmd.toString,
                    s"-Dmaven.repo.local=$localRepo",
                    "org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file",
                    s"-Dfile=$pluginJar",
                    s"-DpomFile=$pluginPom"
                ),
                tempDir
            )
            val validate = runCommand(
                Seq(
                    mavenCmd.toString,
                    s"-Dmaven.repo.local=$localRepo",
                    "validate"
                ),
                fixtureDir
            )

            deleteRecursively(tempDir)

            assertTrue(install.exitCode == 0) &&
            assertTrue(validate.exitCode == 0) &&
            assertTrue(validate.output.contains(MavenPluginModule.placeholderMessage))
        }
    )

    private case class CommandResult(exitCode: Int, output: String)

    private def resolveMavenExecutable(tempDir: Path): Path =
        sys.env.get("MVN_CMD").map(Paths.get(_)).filter(Files.isExecutable(_)).getOrElse {
            findOnPath("mvn").getOrElse(downloadMaven(tempDir))
        }

    private def requiredPathEnv(name: String): Path =
        Paths.get(
            sys.env.getOrElse(name, throw IllegalStateException(s"Missing required env var: $name"))
        )

    private def findOnPath(command: String): Option[Path] =
        sys.env
            .get("PATH")
            .toSeq
            .flatMap(_.split(java.io.File.pathSeparator))
            .map(path => Paths.get(path).resolve(command))
            .find(Files.isExecutable(_))

    private def downloadMaven(tempDir: Path): Path =
        val archive   = tempDir.resolve(s"apache-maven-$mavenVersion-bin.tar.gz")
        val installTo = tempDir.resolve(s"apache-maven-$mavenVersion")
        val url =
            s"https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.tar.gz"

        val download =
            runCommand(Seq("curl", "-fsSL", url, "-o", archive.toString), tempDir)
        val extract =
            runCommand(Seq("tar", "-xzf", archive.toString, "-C", tempDir.toString), tempDir)

        require(download.exitCode == 0, s"Failed to download Maven:\n${download.output}")
        require(extract.exitCode == 0, s"Failed to extract Maven:\n${extract.output}")

        installTo.resolve("bin").resolve("mvn")

    private def runCommand(command: Seq[String], cwd: Path): CommandResult =
        val output = new StringBuilder
        val logger = ProcessLogger(
            line =>
                output.append(line)
                output.append('\n')
                (),
            line =>
                output.append(line)
                output.append('\n')
                ()
        )
        val exitCode = Process(command, cwd.toFile).!(logger)

        CommandResult(exitCode, output.toString)

    private def copyFixtureDirectory(resourceDir: String, targetDir: Path): Unit =
        val resourceUri = getClass.getClassLoader.getResource(resourceDir).toURI
        val sourceDir   = Paths.get(resourceUri)

        val _ = Files.walkFileTree(
            sourceDir,
            new SimpleFileVisitor[Path]:
                override def preVisitDirectory(
                    dir: Path,
                    attrs: BasicFileAttributes
                ): FileVisitResult =
                    Files.createDirectories(targetDir.resolve(sourceDir.relativize(dir)))
                    FileVisitResult.CONTINUE

                override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult =
                    Files.copy(
                        file,
                        targetDir.resolve(sourceDir.relativize(file)),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                    FileVisitResult.CONTINUE
        )

    private def deleteRecursively(path: Path): Unit =
        if Files.exists(path) then
            val _ = Files.walkFileTree(
                path,
                new SimpleFileVisitor[Path]:
                    override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult =
                        Files.deleteIfExists(file)
                        FileVisitResult.CONTINUE

                    override def postVisitDirectory(dir: Path, exc: IOException | Null): FileVisitResult =
                        Files.deleteIfExists(dir)
                        FileVisitResult.CONTINUE
            )
