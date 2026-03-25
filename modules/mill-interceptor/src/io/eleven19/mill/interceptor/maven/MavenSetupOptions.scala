package io.eleven19.mill.interceptor.maven

/** Starter config format for `mill-interceptor maven setup`. */
enum MavenSetupFormat derives CanEqual:
    case Yaml, Pkl

object MavenSetupFormat:

    /** Parse a setup format from CLI input. */
    def fromString(value: String): Option[MavenSetupFormat] =
        value.toLowerCase match
            case "yaml" => Some(Yaml)
            case "pkl"  => Some(Pkl)
            case _      => None

/** Options for generating Maven plugin setup files. */
final case class MavenSetupOptions(
    dryRun: Boolean = false,
    format: MavenSetupFormat = MavenSetupFormat.Yaml,
    force: Boolean = false
) derives CanEqual
