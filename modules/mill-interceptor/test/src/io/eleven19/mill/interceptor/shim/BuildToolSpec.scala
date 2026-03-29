package io.eleven19.mill.interceptor.shim

import zio.test.*

object BuildToolSpec extends ZIOSpecDefault:

    def spec: Spec[Any, Any] = suite("BuildTool")(
        suite("fromString")(
            test("maven resolves to Maven") {
                assertTrue(BuildTool.fromString("maven") == Some(BuildTool.Maven))
            },
            test("mvn resolves to Maven") {
                assertTrue(BuildTool.fromString("mvn") == Some(BuildTool.Maven))
            },
            test("gradle resolves to Gradle") {
                assertTrue(BuildTool.fromString("gradle") == Some(BuildTool.Gradle))
            },
            test("sbt resolves to Sbt") {
                assertTrue(BuildTool.fromString("sbt") == Some(BuildTool.Sbt))
            },
            test("all returns None (handled by caller)") {
                assertTrue(BuildTool.fromString("all") == None)
            },
            test("unknown returns None") {
                assertTrue(BuildTool.fromString("ant") == None)
            },
            test("case insensitive") {
                assertTrue(BuildTool.fromString("MAVEN") == Some(BuildTool.Maven))
            }
        ),
        suite("scriptName")(
            test("Maven tool name") {
                assertTrue(BuildTool.Maven.scriptName(false) == "mvn")
            },
            test("Maven wrapper name") {
                assertTrue(BuildTool.Maven.scriptName(true) == "mvnw")
            },
            test("Gradle tool name") {
                assertTrue(BuildTool.Gradle.scriptName(false) == "gradle")
            },
            test("Gradle wrapper name") {
                assertTrue(BuildTool.Gradle.scriptName(true) == "gradlew")
            },
            test("Sbt tool name") {
                assertTrue(BuildTool.Sbt.scriptName(false) == "sbt")
            },
            test("Sbt wrapper name") {
                assertTrue(BuildTool.Sbt.scriptName(true) == "sbtw")
            }
        ),
        suite("all")(
            test("contains all three tools") {
                assertTrue(BuildTool.all.size == 3)
            }
        )
    )
