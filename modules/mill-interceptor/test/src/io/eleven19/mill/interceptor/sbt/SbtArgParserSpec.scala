package io.eleven19.mill.interceptor.sbt

import zio.test.*

object SbtArgParserSpec extends ZIOSpecDefault:

    def spec: Spec[Any, Any] = suite("SbtArgParser")(
        suite("parse - tasks")(
            test("empty args produce empty command") {
                val cmd = SbtArgParser.parse(Nil)
                assertTrue(cmd.tasks.isEmpty) && assertTrue(cmd.projects.isEmpty)
            },
            test("single task is parsed") {
                val cmd = SbtArgParser.parse(List("compile"))
                assertTrue(cmd.tasks == List("compile"))
            },
            test("multiple tasks are parsed in order") {
                val cmd = SbtArgParser.parse(List("clean", "compile"))
                assertTrue(cmd.tasks == List("clean", "compile"))
            }
        ),
        suite("parse - flags")(
            test("-batch sets batch mode") {
                val cmd = SbtArgParser.parse(List("-batch", "compile"))
                assertTrue(cmd.batch)
            },
            test("-offline sets offline mode") {
                val cmd = SbtArgParser.parse(List("-offline", "compile"))
                assertTrue(cmd.offline)
            },
            test("-no-colors sets noColor") {
                val cmd = SbtArgParser.parse(List("-no-colors", "compile"))
                assertTrue(cmd.noColor)
            }
        ),
        suite("parse - project selection")(
            test("project keyword selects a project") {
                val cmd = SbtArgParser.parse(List("project", "core", "compile"))
                assertTrue(cmd.projects == List("core"))
            },
            test("multiple project selections are captured") {
                val cmd = SbtArgParser.parse(List("project", "api", "test", "project", "core", "compile"))
                assertTrue(cmd.projects == List("api", "core"))
            },
            test("project at end of args without value is treated as task") {
                val cmd = SbtArgParser.parse(List("compile", "project"))
                assertTrue(cmd.tasks.contains("project"))
            }
        )
    )
