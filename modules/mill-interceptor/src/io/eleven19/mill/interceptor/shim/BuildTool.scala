package io.eleven19.mill.interceptor.shim

/** Supported build tools for which shim scripts can be generated. */
enum BuildTool(val toolName: String, val wrapperName: String, val interceptName: String) derives CanEqual:
    case Maven  extends BuildTool("mvn", "mvnw", "maven")
    case Gradle extends BuildTool("gradle", "gradlew", "gradle")
    case Sbt    extends BuildTool("sbt", "sbtw", "sbt")

    /** Return the script base name depending on whether wrapper mode is requested. */
    def scriptName(wrapper: Boolean): String =
        if wrapper then wrapperName else toolName
end BuildTool

object BuildTool:

    /** All supported build tools. */
    val all: List[BuildTool] = List(Maven, Gradle, Sbt)

    /** Parse a tool name from a CLI argument. Returns `None` for unrecognised names. */
    def fromString(name: String): Option[BuildTool] =
        name.toLowerCase match
            case "maven" | "mvn" => Some(Maven)
            case "gradle"        => Some(Gradle)
            case "sbt"           => Some(Sbt)
            case "all"           => None // handled specially by caller
            case _               => None
end BuildTool
