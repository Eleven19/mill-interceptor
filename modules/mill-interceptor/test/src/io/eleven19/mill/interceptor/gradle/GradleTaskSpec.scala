package io.eleven19.mill.interceptor.gradle

import io.eleven19.mill.interceptor.MillTask
import zio.test.*

object GradleTaskSpec extends ZIOSpecDefault:

    def spec: Spec[Any, Any] = suite("GradleTask")(
        suite("fromString")(
            test("parses clean") {
                assertTrue(GradleTask.fromString("clean") == Some(GradleTask.Clean))
            },
            test("parses classes") {
                assertTrue(GradleTask.fromString("classes") == Some(GradleTask.Classes))
            },
            test("parses compile") {
                assertTrue(GradleTask.fromString("compile") == Some(GradleTask.Compile))
            },
            test("parses compileJava") {
                assertTrue(GradleTask.fromString("compileJava") == Some(GradleTask.CompileJava))
            },
            test("parses test") {
                assertTrue(GradleTask.fromString("test") == Some(GradleTask.Test))
            },
            test("parses check") {
                assertTrue(GradleTask.fromString("check") == Some(GradleTask.Check))
            },
            test("parses jar") {
                assertTrue(GradleTask.fromString("jar") == Some(GradleTask.Jar))
            },
            test("parses assemble") {
                assertTrue(GradleTask.fromString("assemble") == Some(GradleTask.Assemble))
            },
            test("parses build") {
                assertTrue(GradleTask.fromString("build") == Some(GradleTask.Build))
            },
            test("parses publish") {
                assertTrue(GradleTask.fromString("publish") == Some(GradleTask.Publish))
            },
            test("parses publishToMavenLocal") {
                assertTrue(GradleTask.fromString("publishToMavenLocal") == Some(GradleTask.PublishToMavenLocal))
            },
            test("returns None for unknown task") {
                assertTrue(GradleTask.fromString("unknownTask").isEmpty)
            },
            test("is case-sensitive unlike Maven") {
                assertTrue(GradleTask.fromString("CLEAN").isEmpty)
            }
        ),
        suite("millTasks")(
            test("Clean maps to clean") {
                assertTrue(GradleTask.Clean.millTasks == List(MillTask("clean")))
            },
            test("Build maps to compile, test, jar") {
                assertTrue(
                    GradleTask.Build.millTasks == List(MillTask("compile"), MillTask("test"), MillTask("jar"))
                )
            },
            test("Publish maps to compile, test, jar, publish") {
                assertTrue(
                    GradleTask.Publish.millTasks == List(
                        MillTask("compile"),
                        MillTask("test"),
                        MillTask("jar"),
                        MillTask("publish")
                    )
                )
            },
            test("PublishToMavenLocal maps to compile, test, jar, publishLocal") {
                assertTrue(
                    GradleTask.PublishToMavenLocal.millTasks == List(
                        MillTask("compile"),
                        MillTask("test"),
                        MillTask("jar"),
                        MillTask("publishLocal")
                    )
                )
            }
        )
    )
