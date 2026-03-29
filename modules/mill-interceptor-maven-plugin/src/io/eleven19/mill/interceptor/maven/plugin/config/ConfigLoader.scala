package io.eleven19.mill.interceptor.maven.plugin.config

import os.Path
import org.virtuslab.yaml.*
import purelogic.Abort

/** Loads effective interceptor configuration from discovered YAML and PKL sources. */
object ConfigLoader:

    /** Discover, merge, and finalize repo and module config into one effective view. */
    def load(repoRoot: os.Path, moduleRoot: os.Path): Either[ConfigLoadException, EffectiveConfig] =
        Abort[ConfigLoadException]:
            val discovered = ConfigDiscovery.discover(repoRoot, moduleRoot)
            discovered
                .foldLeft(ConfigOverlay()) { (current, source) =>
                    current.merge(loadOverlay(source))
                }
                .toEffectiveConfig

    private def loadOverlay(
        source: DiscoveredConfigSource
    )(using abort: Abort[ConfigLoadException]): ConfigOverlay =
        val result = source.format match
            case ConfigFormat.Yaml => loadYaml(source.path)
            case ConfigFormat.Pkl  => PklConfigEvaluator.load(source.path)
        abort.extractEither(result)

    private def loadYaml(path: os.Path): Either[ConfigLoadException, ConfigOverlay] =
        try
            val content = os.read(path)
            content.as[ConfigOverlay] match
                case Right(decoded) => Right(decoded)
                case Left(error)    => Left(ConfigLoadException(path, error.toString))
        catch case error: Throwable => Left(ConfigLoadException(path, error.getMessage.nn, error))
