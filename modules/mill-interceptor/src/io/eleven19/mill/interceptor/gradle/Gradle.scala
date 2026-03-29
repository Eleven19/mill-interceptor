package io.eleven19.mill.interceptor.gradle

import os.{proc, Inherit}

object Gradle:

    def run(args: Seq[String]): Unit =
        val cmd       = GradleArgParser.parse(args.toList)
        val millTasks = GradleCommandMapper.toMillTasks(cmd)

        if millTasks.isEmpty then
            scribe.warn("No Mill tasks mapped from the provided Gradle arguments")
            throw new IllegalArgumentException(
                s"No actionable Gradle tasks found in: ${args.toList.mkString(" ")}"
            )
        else
            scribe.info(s"Gradle tasks: ${cmd.tasks.mkString(", ")}")
            scribe.info(s"Mapped Mill tasks: ${millTasks.map(_.name).mkString(" ")}")

            if cmd.excludedTasks.nonEmpty then scribe.info(s"Excluded tasks: ${cmd.excludedTasks.mkString(", ")}")

            if cmd.dryRun then scribe.info("Dry run mode — Mill will not be executed")
            else
                cmd.projectProperties.get("version") match
                    case Some(v) => scribe.info(s"Project version: $v")
                    case None    => ()

                val millArgs = millTasks.flatMap(_.toArgs)
                scribe.info(s"Executing: mill ${millArgs.mkString(" ")}")
                val exitCode = os.proc("mill" +: millArgs).call(
                    stdin = os.Inherit,
                    stdout = os.Inherit,
                    stderr = os.Inherit,
                    check = false
                ).exitCode
                if exitCode != 0 then throw new RuntimeException(s"Mill exited with code $exitCode")
