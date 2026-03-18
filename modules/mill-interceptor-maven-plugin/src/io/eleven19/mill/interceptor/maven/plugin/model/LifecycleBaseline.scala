package io.eleven19.mill.interceptor.maven.plugin.model

import io.eleven19.mill.interceptor.maven.plugin.config.EffectiveConfig

/** Optional Scalafmt validation target and its disable switch. */
final case class ValidateScalafmtHook(
    target: String,
    disableProperty: String
) derives CanEqual

/** Validate-phase behaviors derived from the baseline and config. */
final case class ValidateBaseline(
    scalafmt: Option[ValidateScalafmtHook]
) derives CanEqual

/** Conventional Maven lifecycle mapping resolved for the current build. */
final case class LifecycleBaseline(
    executionMode: ExecutionMode,
    lifecycleMappings: Map[String, Seq[String]],
    validate: ValidateBaseline
) derives CanEqual

object LifecycleBaseline:

    private val scalafmtDisableProperty = "mill.interceptor.scalafmt"

    private val defaultLifecycleMappings = Map(
        "clean"    -> Seq("clean"),
        "validate" -> Seq.empty,
        "compile"  -> Seq("compile"),
        "test"     -> Seq("compile", "test"),
        "package"  -> Seq("compile", "test", "jar"),
        "verify"   -> Seq("compile", "test"),
        "install"  -> Seq("compile", "test", "jar", "publishLocal"),
        "deploy"   -> Seq("compile", "test", "jar", "publish")
    )

    /** Resolve the conventional lifecycle baseline for the supplied configuration.
      *
      * Explicit lifecycle mappings override built-in defaults by phase name.
      */
    def resolve(
        config: EffectiveConfig,
        properties: Map[String, String] = Map.empty
    ): LifecycleBaseline =
        LifecycleBaseline(
            executionMode = ExecutionMode.fromString(config.mode),
            lifecycleMappings = defaultLifecycleMappings ++ config.lifecycle,
            validate = ValidateBaseline(
                scalafmt = resolveScalafmtHook(config, properties)
            )
        )

    private def resolveScalafmtHook(
        config: EffectiveConfig,
        properties: Map[String, String]
    ): Option[ValidateScalafmtHook] =
        val enabledByConfig = config.validate.scalafmtEnabled
        val enabled =
            properties.get(scalafmtDisableProperty).map(parseBoolean) match
                case Some(false) => false
                case Some(true)  => true
                case None        => enabledByConfig

        if enabled then
            Some(
                ValidateScalafmtHook(
                    target = config.validate.scalafmtTarget.getOrElse("checkFormat"),
                    disableProperty = scalafmtDisableProperty
                )
            )
        else None

    private def parseBoolean(value: String): Boolean =
        value.trim.toLowerCase match
            case "true" | "1" | "yes" | "on"  => true
            case "false" | "0" | "no" | "off" => false
            case _                            => false
