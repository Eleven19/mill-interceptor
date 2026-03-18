package io.eleven19.mill.interceptor.maven.plugin.config

import kyo.*

enum ConfigScope derives CanEqual:
    case Repository
    case Module

enum ConfigFormat derives CanEqual:
    case Yaml
    case Pkl

final case class DiscoveredConfigSource(
    scope: ConfigScope,
    format: ConfigFormat,
    path: Path
) derives CanEqual

object ConfigDiscovery:

    def discover(repoRoot: Path, moduleRoot: Path): Seq[DiscoveredConfigSource] < Sync =
        val candidates = Seq(
            DiscoveredConfigSource(
                ConfigScope.Repository,
                ConfigFormat.Yaml,
                Path(repoRoot, "mill-interceptor.yaml")
            ),
            DiscoveredConfigSource(
                ConfigScope.Repository,
                ConfigFormat.Pkl,
                Path(repoRoot, "mill-interceptor.pkl")
            ),
            DiscoveredConfigSource(
                ConfigScope.Repository,
                ConfigFormat.Yaml,
                Path(repoRoot, ".config", "mill-interceptor", "config.yaml")
            ),
            DiscoveredConfigSource(
                ConfigScope.Repository,
                ConfigFormat.Pkl,
                Path(repoRoot, ".config", "mill-interceptor", "config.pkl")
            ),
            DiscoveredConfigSource(
                ConfigScope.Module,
                ConfigFormat.Yaml,
                Path(moduleRoot, "mill-interceptor.yaml")
            ),
            DiscoveredConfigSource(
                ConfigScope.Module,
                ConfigFormat.Pkl,
                Path(moduleRoot, "mill-interceptor.pkl")
            ),
            DiscoveredConfigSource(
                ConfigScope.Module,
                ConfigFormat.Yaml,
                Path(moduleRoot, ".config", "mill-interceptor", "config.yaml")
            ),
            DiscoveredConfigSource(
                ConfigScope.Module,
                ConfigFormat.Pkl,
                Path(moduleRoot, ".config", "mill-interceptor", "config.pkl")
            )
        )

        for discovered <- Kyo.foreach(candidates) { candidate =>
                candidate.path.exists.map {
                    case true  => Some(candidate)
                    case false => None
                }
            }
        yield discovered.flatten
