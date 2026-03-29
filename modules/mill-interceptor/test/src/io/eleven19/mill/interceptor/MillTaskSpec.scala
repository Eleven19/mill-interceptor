package io.eleven19.mill.interceptor

import zio.test.*

object MillTaskSpec extends ZIOSpecDefault:

    def spec: Spec[Any, Any] = suite("MillTask")(
        suite("toArgs")(
            test("task with no args returns single-element list") {
                val task = MillTask("compile")
                assertTrue(task.toArgs == List("compile"))
            },
            test("task with args returns name followed by args") {
                val task = MillTask("run", List("--main-class", "com.example.Main"))
                assertTrue(task.toArgs == List("run", "--main-class", "com.example.Main"))
            },
            test("task with single arg returns name and arg") {
                val task = MillTask("test", List("-t"))
                assertTrue(task.toArgs == List("test", "-t"))
            },
            test("task with empty args list returns single-element list") {
                val task = MillTask("clean", Nil)
                assertTrue(task.toArgs == List("clean"))
            }
        ),
        suite("withModule")(
            test("prefixes task name with module") {
                val task = MillTask("compile")
                assertTrue(task.withModule("core").name == "core.compile")
            },
            test("preserves args when prefixing") {
                val task = MillTask("run", List("--port", "8080"))
                val prefixed = task.withModule("server")
                assertTrue(prefixed.name == "server.run") &&
                    assertTrue(prefixed.args == List("--port", "8080"))
            },
            test("supports nested module paths") {
                val task = MillTask("test")
                assertTrue(task.withModule("sub.core").name == "sub.core.test")
            }
        )
    )
