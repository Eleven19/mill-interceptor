package io.github.eleven19.mill.interceptor.maven

/** Parses raw Maven CLI arguments into a [[MvnCommand]]. */
object MvnArgParser:

    /** Parse a list of Maven CLI arguments into a structured [[MvnCommand]]. */
    def parse(args: List[String]): MvnCommand =
        parseLoop(args, MvnCommand.empty)

    @annotation.tailrec
    private def parseLoop(args: List[String], cmd: MvnCommand): MvnCommand =
        args match
            case Nil => cmd

            // Projects
            case ("-pl" | "--projects") :: value :: rest =>
                parseLoop(rest, cmd.copy(projects = cmd.projects ++ value.split(",").toList.map(_.trim)))

            // Also make
            case ("-am" | "--also-make") :: rest =>
                parseLoop(rest, cmd.copy(alsoMake = true))

            // Also make dependents
            case ("-amd" | "--also-make-dependents") :: rest =>
                parseLoop(rest, cmd.copy(alsoMakeDependents = true))

            // Offline
            case ("-o" | "--offline") :: rest =>
                parseLoop(rest, cmd.copy(offline = true))

            // Debug
            case ("-X" | "--debug") :: rest =>
                parseLoop(rest, cmd.copy(debug = true))

            // Quiet
            case ("-q" | "--quiet") :: rest =>
                parseLoop(rest, cmd.copy(quiet = true))

            // Non-recursive
            case ("-N" | "--non-recursive") :: rest =>
                parseLoop(rest, cmd.copy(nonRecursive = true))

            // Threads
            case ("-T" | "--threads") :: value :: rest =>
                parseLoop(rest, cmd.copy(threads = value.toIntOption))

            // Profiles
            case ("-P" | "--activate-profiles") :: value :: rest =>
                parseLoop(rest, cmd.copy(profiles = cmd.profiles ++ value.split(",").toList.map(_.trim)))

            // File (POM) - skip, not relevant for Mill
            case ("-f" | "--file") :: _ :: rest =>
                parseLoop(rest, cmd)

            // System properties -Dkey=value
            case arg :: rest if arg.startsWith("-D") =>
                val prop = arg.stripPrefix("-D")
                prop match
                    case "skipTests" =>
                        parseLoop(rest, cmd.copy(skipTests = true))
                    case s"maven.test.skip=$v" if v.equalsIgnoreCase("true") =>
                        parseLoop(rest, cmd.copy(skipTests = true))
                    case s"$key=$value" =>
                        parseLoop(rest, cmd.copy(properties = cmd.properties + (key -> value)))
                    case key =>
                        parseLoop(rest, cmd.copy(properties = cmd.properties + (key -> "true")))

            // Flags we skip (errors, update-snapshots, etc.)
            case ("-e" | "--errors" | "-U" | "--update-snapshots" | "-B" | "--batch-mode") :: rest =>
                parseLoop(rest, cmd)

            // Anything else is a phase or goal
            case arg :: rest =>
                MavenPhase.fromString(arg) match
                    case Some(phase) =>
                        parseLoop(rest, cmd.copy(phases = cmd.phases :+ phase))
                    case None =>
                        parseLoop(rest, cmd.copy(goals = cmd.goals :+ arg))
