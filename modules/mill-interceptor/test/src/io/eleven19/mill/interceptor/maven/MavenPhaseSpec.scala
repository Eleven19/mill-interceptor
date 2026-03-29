package io.eleven19.mill.interceptor.maven

import io.eleven19.mill.interceptor.MillTask
import zio.test.*

object MavenPhaseSpec extends ZIOSpecDefault:

    def spec: Spec[Any, Any] = suite("MavenPhase")(
        suite("fromString")(
            test("parses clean") {
                assertTrue(MavenPhase.fromString("clean") == Some(MavenPhase.Clean))
            },
            test("parses validate") {
                assertTrue(MavenPhase.fromString("validate") == Some(MavenPhase.Validate))
            },
            test("parses compile") {
                assertTrue(MavenPhase.fromString("compile") == Some(MavenPhase.Compile))
            },
            test("parses test") {
                assertTrue(MavenPhase.fromString("test") == Some(MavenPhase.Test))
            },
            test("parses package") {
                assertTrue(MavenPhase.fromString("package") == Some(MavenPhase.Package))
            },
            test("parses verify") {
                assertTrue(MavenPhase.fromString("verify") == Some(MavenPhase.Verify))
            },
            test("parses install") {
                assertTrue(MavenPhase.fromString("install") == Some(MavenPhase.Install))
            },
            test("parses deploy") {
                assertTrue(MavenPhase.fromString("deploy") == Some(MavenPhase.Deploy))
            },
            test("is case-insensitive") {
                assertTrue(MavenPhase.fromString("COMPILE") == Some(MavenPhase.Compile))
            },
            test("returns None for unknown phase") {
                assertTrue(MavenPhase.fromString("unknown").isEmpty)
            },
            test("returns None for empty string") {
                assertTrue(MavenPhase.fromString("").isEmpty)
            }
        ),
        suite("millTasks")(
            test("Clean maps to clean") {
                assertTrue(MavenPhase.Clean.millTasks == List(MillTask("clean")))
            },
            test("Validate maps to empty") {
                assertTrue(MavenPhase.Validate.millTasks.isEmpty)
            },
            test("Compile maps to compile") {
                assertTrue(MavenPhase.Compile.millTasks == List(MillTask("compile")))
            },
            test("Test maps to compile and test") {
                assertTrue(MavenPhase.Test.millTasks == List(MillTask("compile"), MillTask("test")))
            },
            test("Package maps to compile, test, jar") {
                assertTrue(
                    MavenPhase.Package.millTasks == List(MillTask("compile"), MillTask("test"), MillTask("jar"))
                )
            },
            test("Install maps to compile, test, jar, publishLocal") {
                assertTrue(
                    MavenPhase.Install.millTasks == List(
                        MillTask("compile"),
                        MillTask("test"),
                        MillTask("jar"),
                        MillTask("publishLocal")
                    )
                )
            },
            test("Deploy maps to compile, test, jar, publish") {
                assertTrue(
                    MavenPhase.Deploy.millTasks == List(
                        MillTask("compile"),
                        MillTask("test"),
                        MillTask("jar"),
                        MillTask("publish")
                    )
                )
            }
        )
    )
