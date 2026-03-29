package io.eleven19.mill.interceptor.sbt

import os.{proc, Inherit}

object Sbt:

    def run(args: Seq[String]): Unit =
        val cmd       = SbtArgParser.parse(args.toList)
        val millTasks = SbtCommandMapper.toMillTasks(cmd)

        if millTasks.isEmpty then
            scribe.warn("No Mill tasks mapped from the provided sbt arguments")
            throw new IllegalArgumentException(
                s"No actionable sbt tasks found in: ${args.toList.mkString(" ")}"
            )
        else
            scribe.info(s"sbt tasks: ${cmd.tasks.mkString(", ")}")
            scribe.info(s"Mapped Mill tasks: ${millTasks.map(_.name).mkString(" ")}")

            cmd.projects match
                case Nil  => ()
                case mods => scribe.info(s"Target projects: ${mods.mkString(", ")}")

            val millArgs = millTasks.flatMap(_.toArgs)
            scribe.info(s"Executing: mill ${millArgs.mkString(" ")}")
            val exitCode = os
                .proc("mill" +: millArgs)
                .call(
                    stdin = os.Inherit,
                    stdout = os.Inherit,
                    stderr = os.Inherit,
                    check = false
                )
                .exitCode
            if exitCode != 0 then throw new RuntimeException(s"Mill exited with code $exitCode")
