package io.eleven19.mill.interceptor.shim

import kyo.*
import kyo.test.KyoSpecDefault
import zio.test.*

object ShimTemplateSpec extends KyoSpecDefault:

    def spec: Spec[Any, Any] = suite("ShimTemplate")(
        suite("unix")(
            test("starts with shebang") {
                val content = ShimTemplate.unix(BuildTool.Maven, "0.1.0")
                Sync.defer(assertTrue(content.startsWith("#!/usr/bin/env bash")))
            },
            test("contains intercept command for maven") {
                val content = ShimTemplate.unix(BuildTool.Maven, "0.1.0")
                Sync.defer(assertTrue(content.contains("intercept maven")))
            },
            test("contains intercept command for gradle") {
                val content = ShimTemplate.unix(BuildTool.Gradle, "0.1.0")
                Sync.defer(assertTrue(content.contains("intercept gradle")))
            },
            test("contains intercept command for sbt") {
                val content = ShimTemplate.unix(BuildTool.Sbt, "0.1.0")
                Sync.defer(assertTrue(content.contains("intercept sbt")))
            },
            test("contains MILL_INTERCEPTOR_HOME env var") {
                val content = ShimTemplate.unix(BuildTool.Maven, "0.1.0")
                Sync.defer(assertTrue(content.contains("MILL_INTERCEPTOR_HOME")))
            },
            test("contains MILL_INTERCEPTOR_VERSION env var") {
                val content = ShimTemplate.unix(BuildTool.Maven, "0.1.0")
                Sync.defer(assertTrue(content.contains("MILL_INTERCEPTOR_VERSION")))
            },
            test("contains MILL_INTERCEPTOR_DOWNLOAD_URL env var") {
                val content = ShimTemplate.unix(BuildTool.Maven, "0.1.0")
                Sync.defer(assertTrue(content.contains("MILL_INTERCEPTOR_DOWNLOAD_URL")))
            },
            test("contains MILL_INTERCEPTOR_OPTS env var") {
                val content = ShimTemplate.unix(BuildTool.Maven, "0.1.0")
                Sync.defer(assertTrue(content.contains("MILL_INTERCEPTOR_OPTS")))
            },
            test("contains MILL_INTERCEPTOR_DEBUG env var") {
                val content = ShimTemplate.unix(BuildTool.Maven, "0.1.0")
                Sync.defer(assertTrue(content.contains("MILL_INTERCEPTOR_DEBUG")))
            },
            test("embeds the specified version") {
                val content = ShimTemplate.unix(BuildTool.Maven, "1.2.3")
                Sync.defer(assertTrue(content.contains("1.2.3")))
            },
            test("forwards args with exec") {
                val content = ShimTemplate.unix(BuildTool.Maven, "0.1.0")
                Sync.defer(assertTrue(content.contains("exec")))
            }
        ),
        suite("windows")(
            test("starts with @echo off") {
                val content = ShimTemplate.windows(BuildTool.Maven, "0.1.0")
                Sync.defer(assertTrue(content.startsWith("@echo off")))
            },
            test("contains intercept command for maven") {
                val content = ShimTemplate.windows(BuildTool.Maven, "0.1.0")
                Sync.defer(assertTrue(content.contains("intercept maven")))
            },
            test("contains intercept command for gradle") {
                val content = ShimTemplate.windows(BuildTool.Gradle, "0.1.0")
                Sync.defer(assertTrue(content.contains("intercept gradle")))
            },
            test("contains intercept command for sbt") {
                val content = ShimTemplate.windows(BuildTool.Sbt, "0.1.0")
                Sync.defer(assertTrue(content.contains("intercept sbt")))
            },
            test("contains MILL_INTERCEPTOR_HOME env var") {
                val content = ShimTemplate.windows(BuildTool.Maven, "0.1.0")
                Sync.defer(assertTrue(content.contains("MILL_INTERCEPTOR_HOME")))
            },
            test("contains MILL_INTERCEPTOR_VERSION env var") {
                val content = ShimTemplate.windows(BuildTool.Maven, "0.1.0")
                Sync.defer(assertTrue(content.contains("MILL_INTERCEPTOR_VERSION")))
            },
            test("contains MILL_INTERCEPTOR_DOWNLOAD_URL env var") {
                val content = ShimTemplate.windows(BuildTool.Maven, "0.1.0")
                Sync.defer(assertTrue(content.contains("MILL_INTERCEPTOR_DOWNLOAD_URL")))
            },
            test("contains MILL_INTERCEPTOR_OPTS env var") {
                val content = ShimTemplate.windows(BuildTool.Maven, "0.1.0")
                Sync.defer(assertTrue(content.contains("MILL_INTERCEPTOR_OPTS")))
            },
            test("contains MILL_INTERCEPTOR_DEBUG env var") {
                val content = ShimTemplate.windows(BuildTool.Maven, "0.1.0")
                Sync.defer(assertTrue(content.contains("MILL_INTERCEPTOR_DEBUG")))
            },
            test("embeds the specified version") {
                val content = ShimTemplate.windows(BuildTool.Maven, "1.2.3")
                Sync.defer(assertTrue(content.contains("1.2.3")))
            },
            test("uses powershell for download") {
                val content = ShimTemplate.windows(BuildTool.Maven, "0.1.0")
                Sync.defer(assertTrue(content.contains("powershell")))
            }
        )
    )
