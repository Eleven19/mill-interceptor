package io.github.eleven19.mill.interceptor.maven

/** A Mill task with optional arguments. */
final case class MillTask(name: String, args: List[String] = Nil):
    /** Render this task as the CLI tokens Mill expects. */
    def toArgs: List[String] =
        if args.isEmpty then List(name)
        else name :: args

    /** Prefix this task with a module selector, e.g. `core.compile`. */
    def withModule(module: String): MillTask =
        copy(name = s"$module.$name")
