package io.eleven19.mill.interceptor.maven.plugin.model

/** Execution policy for Maven phases or goals that do not resolve cleanly. */
enum ExecutionMode derives CanEqual:
    case Strict
    case Hybrid

object ExecutionMode:

    /** Parse persisted config into a supported execution mode. */
    def fromString(value: String): ExecutionMode =
        value.trim.toLowerCase match
            case "hybrid" => Hybrid
            case _        => Strict
