package io.eleven19.mill.interceptor.maven.plugin.config

import kyo.*

/** Whether a discovered config file came from the repository root or a module root. */
enum ConfigScope derives CanEqual:
    case Repository
    case Module

/** Supported source formats for interceptor configuration discovery. */
enum ConfigFormat derives CanEqual:
    case Yaml
    case Pkl

/** One config file discovered during precedence-ordered config lookup. */
final case class DiscoveredConfigSource(
    scope: ConfigScope,
    format: ConfigFormat,
    path: Path
) derives CanEqual

/** Deterministic config discovery for repository-level and module-level interceptor inputs. */
object ConfigDiscovery:

    /** Resolve a child path under a parent, preserving absolute path semantics. */
    private def childPath(parent: Path, children: String*): Path =
        Path(children.foldLeft(parent.toJava)((p, c) => p.resolve(c)).toString)

    /** Discover existing config files in the order they should be merged. */
    def discover(repoRoot: Path, moduleRoot: Path): Seq[DiscoveredConfigSource] < Sync =
        val candidates = Seq(
            DiscoveredConfigSource(
                ConfigScope.Repository,
                ConfigFormat.Yaml,
                childPath(repoRoot, "mill-interceptor.yaml")
            ),
            DiscoveredConfigSource(
                ConfigScope.Repository,
                ConfigFormat.Pkl,
                childPath(repoRoot, "mill-interceptor.pkl")
            ),
            DiscoveredConfigSource(
                ConfigScope.Repository,
                ConfigFormat.Yaml,
                childPath(repoRoot, ".config", "mill-interceptor", "config.yaml")
            ),
            DiscoveredConfigSource(
                ConfigScope.Repository,
                ConfigFormat.Pkl,
                childPath(repoRoot, ".config", "mill-interceptor", "config.pkl")
            ),
            DiscoveredConfigSource(
                ConfigScope.Module,
                ConfigFormat.Yaml,
                childPath(moduleRoot, "mill-interceptor.yaml")
            ),
            DiscoveredConfigSource(
                ConfigScope.Module,
                ConfigFormat.Pkl,
                childPath(moduleRoot, "mill-interceptor.pkl")
            ),
            DiscoveredConfigSource(
                ConfigScope.Module,
                ConfigFormat.Yaml,
                childPath(moduleRoot, ".config", "mill-interceptor", "config.yaml")
            ),
            DiscoveredConfigSource(
                ConfigScope.Module,
                ConfigFormat.Pkl,
                childPath(moduleRoot, ".config", "mill-interceptor", "config.pkl")
            )
        )

        for discovered <- Kyo.foreach(candidates) { candidate =>
                candidate.path.exists.map {
                    case true  => Some(candidate)
                    case false => None
                }
            }
        yield discovered.flatten
