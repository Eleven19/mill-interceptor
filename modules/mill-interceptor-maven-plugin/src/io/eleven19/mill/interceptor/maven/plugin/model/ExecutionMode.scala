package io.eleven19.mill.interceptor.maven.plugin.model

enum ExecutionMode derives CanEqual:
    case Strict
    case Hybrid

object ExecutionMode:

    def fromString(value: String): ExecutionMode =
        value.trim.toLowerCase match
            case "hybrid" => Hybrid
            case _        => Strict
