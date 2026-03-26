package io.eleven19.mill.interceptor.maven.plugin.config

import kyo.*
import org.virtuslab.yaml.*

/** Loads effective interceptor configuration from discovered YAML and PKL sources. */
object ConfigLoader:

    /** Discover, merge, and finalize repo and module config into one effective view. */
    def load(repoRoot: Path, moduleRoot: Path): EffectiveConfig < (Abort[ConfigLoadException] & Sync) =
        for
            discovered <- ConfigDiscovery.discover(repoRoot, moduleRoot)
            merged <- Kyo.foldLeft(discovered)(ConfigOverlay()) { (current, source) =>
                loadOverlay(source).map(current.merge)
            }
        yield merged.toEffectiveConfig

    private def loadOverlay(
        source: DiscoveredConfigSource
    ): ConfigOverlay < (Abort[ConfigLoadException] & Sync) =
        source.format match
            case ConfigFormat.Yaml => loadYaml(source.path)
            case ConfigFormat.Pkl  => PklConfigEvaluator.load(source.path)

    private def loadYaml(path: Path): ConfigOverlay < (Abort[ConfigLoadException] & Sync) =
        for
            content <- path.read
            config <- content.as[ConfigOverlay] match
                case Right(decoded) => Sync.defer(decoded)
                case Left(error)    => Abort.fail(ConfigLoadException(path, error.toString))
        yield config
