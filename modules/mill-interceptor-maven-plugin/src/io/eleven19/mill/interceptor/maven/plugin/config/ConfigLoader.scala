package io.eleven19.mill.interceptor.maven.plugin.config

import os.Path
import org.virtuslab.yaml.*

/** Loads effective interceptor configuration from discovered YAML and PKL sources. */
object ConfigLoader:

    /** Discover, merge, and finalize repo and module config into one effective view. */
    def load(repoRoot: os.Path, moduleRoot: os.Path): Either[ConfigLoadException, EffectiveConfig] =
        val discovered = ConfigDiscovery.discover(repoRoot, moduleRoot)
        discovered.foldLeft[Either[ConfigLoadException, ConfigOverlay]](Right(ConfigOverlay())) {
            case (Right(current), source) => loadOverlay(source).map(current.merge)
            case (left, _)               => left
        }.map(_.toEffectiveConfig)

    private def loadOverlay(
        source: DiscoveredConfigSource
    ): Either[ConfigLoadException, ConfigOverlay] =
        source.format match
            case ConfigFormat.Yaml => loadYaml(source.path)
            case ConfigFormat.Pkl  => PklConfigEvaluator.load(source.path)

    private def loadYaml(path: os.Path): Either[ConfigLoadException, ConfigOverlay] =
        try
            val content = os.read(path)
            content.as[ConfigOverlay] match
                case Right(decoded) => Right(decoded)
                case Left(error)    => Left(ConfigLoadException(path, error.toString))
        catch
            case error: Throwable => Left(ConfigLoadException(path, error.getMessage.nn, error))
