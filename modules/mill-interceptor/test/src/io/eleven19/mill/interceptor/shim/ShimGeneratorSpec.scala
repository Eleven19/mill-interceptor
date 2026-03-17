package io.eleven19.mill.interceptor.shim

import kyo.*
import kyo.test.KyoSpecDefault
import zio.test.*
import java.lang.System as JSystem

object ShimGeneratorSpec extends KyoSpecDefault:

    private def tempPath(name: String): Path =
        Path("out", "mill-interceptor-tests", name)

    def spec: Spec[Any, Any] = suite("ShimGenerator")(
        suite("unixContent")(
            test("returns unix script for maven") {
                val content = ShimGenerator.unixContent(BuildTool.Maven, "0.1.0")
                Sync.defer(
                    assertTrue(content.startsWith("#!/usr/bin/env bash")) &&
                    assertTrue(content.contains("intercept maven"))
                )
            },
            test("returns unix script for gradle") {
                val content = ShimGenerator.unixContent(BuildTool.Gradle, "0.1.0")
                Sync.defer(
                    assertTrue(content.contains("intercept gradle"))
                )
            },
            test("returns unix script for sbt") {
                val content = ShimGenerator.unixContent(BuildTool.Sbt, "0.1.0")
                Sync.defer(
                    assertTrue(content.contains("intercept sbt"))
                )
            },
            test("embeds the specified version") {
                val content = ShimGenerator.unixContent(BuildTool.Maven, "2.0.0")
                Sync.defer(assertTrue(content.contains("2.0.0")))
            },
            test("contains all required environment variables") {
                val content = ShimGenerator.unixContent(BuildTool.Maven, "0.1.0")
                Sync.defer(
                    assertTrue(content.contains("MILL_INTERCEPTOR_HOME")) &&
                    assertTrue(content.contains("MILL_INTERCEPTOR_VERSION")) &&
                    assertTrue(content.contains("MILL_INTERCEPTOR_DOWNLOAD_URL")) &&
                    assertTrue(content.contains("MILL_INTERCEPTOR_OPTS")) &&
                    assertTrue(content.contains("MILL_INTERCEPTOR_DEBUG"))
                )
            },
            test("forwards args with exec") {
                val content = ShimGenerator.unixContent(BuildTool.Maven, "0.1.0")
                Sync.defer(assertTrue(content.contains("exec")))
            }
        ),
        suite("windowsContent")(
            test("returns windows script for maven") {
                val content = ShimGenerator.windowsContent(BuildTool.Maven, "0.1.0")
                Sync.defer(
                    assertTrue(content.startsWith("@echo off")) &&
                    assertTrue(content.contains("intercept maven"))
                )
            },
            test("returns windows script for gradle") {
                val content = ShimGenerator.windowsContent(BuildTool.Gradle, "0.1.0")
                Sync.defer(
                    assertTrue(content.contains("intercept gradle"))
                )
            },
            test("returns windows script for sbt") {
                val content = ShimGenerator.windowsContent(BuildTool.Sbt, "0.1.0")
                Sync.defer(
                    assertTrue(content.contains("intercept sbt"))
                )
            },
            test("embeds the specified version") {
                val content = ShimGenerator.windowsContent(BuildTool.Maven, "1.2.3")
                Sync.defer(assertTrue(content.contains("1.2.3")))
            },
            test("contains all required environment variables") {
                val content = ShimGenerator.windowsContent(BuildTool.Maven, "0.1.0")
                Sync.defer(
                    assertTrue(content.contains("MILL_INTERCEPTOR_HOME")) &&
                    assertTrue(content.contains("MILL_INTERCEPTOR_VERSION")) &&
                    assertTrue(content.contains("MILL_INTERCEPTOR_DOWNLOAD_URL")) &&
                    assertTrue(content.contains("MILL_INTERCEPTOR_OPTS")) &&
                    assertTrue(content.contains("MILL_INTERCEPTOR_DEBUG"))
                )
            },
            test("uses powershell for download") {
                val content = ShimGenerator.windowsContent(BuildTool.Maven, "0.1.0")
                Sync.defer(assertTrue(content.contains("powershell")))
            }
        ),
        suite("generate")(
            test("writes shim files to the requested directory") {
                val outputDir = tempPath(s"shim-generator-${JSystem.nanoTime()}")
                val options = ShimGenerateOptions(
                    tools = List(BuildTool.Maven),
                    wrapper = false,
                    outputDir = outputDir,
                    version = "9.9.9"
                )
                val unixPath = Path(outputDir, "mvn")
                val cmdPath  = Path(outputDir, "mvn.cmd")

                for
                    _ <- outputDir.removeAll
                    generated <- ShimGenerator.generate(options)
                    unixExists <- unixPath.exists
                    windowsExists <- cmdPath.exists
                    unixContent <- unixPath.read
                    windowsContent <- cmdPath.read
                    _ <- outputDir.removeAll
                yield
                    assertTrue(generated.map(_.path).toSet == Set(unixPath, cmdPath)) &&
                    assertTrue(unixExists) &&
                    assertTrue(windowsExists) &&
                    assertTrue(unixContent.contains("9.9.9")) &&
                    assertTrue(windowsContent.contains("9.9.9"))
            }
        )
    )
