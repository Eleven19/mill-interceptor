package io.eleven19.mill.interceptor.maven.plugin.mojo

import io.eleven19.mill.interceptor.model.*
import os.Path

/** Maven-facing input that can be translated into a neutral execution request.
  *
  * The context stays intentionally small and Maven-shaped:
  *   - execution kind
  *   - requested goal or lifecycle phase
  *   - repo and module roots
  *   - module identity
  *   - user-supplied properties
  */
final case class MavenExecutionContext(
    kind: ExecutionRequestKind,
    requestedName: String,
    repoRoot: Path,
    moduleRoot: Path,
    module: ModuleRef,
    userProperties: Map[String, String] = Map.empty
) derives CanEqual:

    def isLifecyclePhase: Boolean =
        kind == ExecutionRequestKind.LifecyclePhase

    def isExplicitGoal: Boolean =
        kind == ExecutionRequestKind.ExplicitGoal

    def toExecutionRequest: ExecutionRequest =
        ExecutionRequest(
            kind = kind,
            requestedName = requestedName,
            repoRoot = repoRoot,
            moduleRoot = moduleRoot,
            module = module,
            properties = userProperties
        )
