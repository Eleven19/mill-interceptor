package io.eleven19.mill.interceptor.maven

import os.{proc, Inherit}

object Mvn:

    def run(args: Seq[String]): Unit =
        val cmd       = MvnArgParser.parse(args.toList)
        val millTasks = MillCommandMapper.toMillTasks(cmd)

        if millTasks.isEmpty then
            scribe.warn("No Mill tasks mapped from the provided Maven arguments")
            throw new IllegalArgumentException(
                s"No actionable Maven phases found in: ${args.toList.mkString(" ")}"
            )
        else
            scribe.info(s"Maven phases: ${cmd.phases.mkString(", ")}")
            scribe.info(s"Mapped Mill tasks: ${millTasks.map(_.name).mkString(" ")}")

            if cmd.skipTests then scribe.info("Tests will be skipped")

            cmd.projects match
                case Nil  => ()
                case mods => scribe.info(s"Target modules: ${mods.mkString(", ")}")

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
