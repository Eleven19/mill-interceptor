package io.github.eleven19.mill.interceptor.shim

import kyo.*
import kyo.test.KyoSpecDefault
import zio.test.*

object BuildToolSpec extends KyoSpecDefault:

    def spec: Spec[Any, Any] = suite("BuildTool")(
        suite("fromString")(
            test("maven resolves to Maven") {
                Sync.defer(assertTrue(BuildTool.fromString("maven") == Some(BuildTool.Maven)))
            },
            test("mvn resolves to Maven") {
                Sync.defer(assertTrue(BuildTool.fromString("mvn") == Some(BuildTool.Maven)))
            },
            test("gradle resolves to Gradle") {
                Sync.defer(assertTrue(BuildTool.fromString("gradle") == Some(BuildTool.Gradle)))
            },
            test("sbt resolves to Sbt") {
                Sync.defer(assertTrue(BuildTool.fromString("sbt") == Some(BuildTool.Sbt)))
            },
            test("all returns None (handled by caller)") {
                Sync.defer(assertTrue(BuildTool.fromString("all") == None))
            },
            test("unknown returns None") {
                Sync.defer(assertTrue(BuildTool.fromString("ant") == None))
            },
            test("case insensitive") {
                Sync.defer(assertTrue(BuildTool.fromString("MAVEN") == Some(BuildTool.Maven)))
            }
        ),
        suite("scriptName")(
            test("Maven tool name") {
                Sync.defer(assertTrue(BuildTool.Maven.scriptName(false) == "mvn"))
            },
            test("Maven wrapper name") {
                Sync.defer(assertTrue(BuildTool.Maven.scriptName(true) == "mvnw"))
            },
            test("Gradle tool name") {
                Sync.defer(assertTrue(BuildTool.Gradle.scriptName(false) == "gradle"))
            },
            test("Gradle wrapper name") {
                Sync.defer(assertTrue(BuildTool.Gradle.scriptName(true) == "gradlew"))
            },
            test("Sbt tool name") {
                Sync.defer(assertTrue(BuildTool.Sbt.scriptName(false) == "sbt"))
            },
            test("Sbt wrapper name") {
                Sync.defer(assertTrue(BuildTool.Sbt.scriptName(true) == "sbtw"))
            }
        ),
        suite("all")(
            test("contains all three tools") {
                Sync.defer(assertTrue(BuildTool.all.size == 3))
            }
        )
    )
