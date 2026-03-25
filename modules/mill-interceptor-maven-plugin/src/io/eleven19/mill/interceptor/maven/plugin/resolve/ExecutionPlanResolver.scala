package io.eleven19.mill.interceptor.maven.plugin.resolve

import io.eleven19.mill.interceptor.model.*
import io.eleven19.mill.interceptor.maven.plugin.config.EffectiveConfig
import io.eleven19.mill.interceptor.maven.plugin.model.LifecycleBaseline

object ExecutionPlanResolver:

    def resolve(
        request: ExecutionRequest,
        config: EffectiveConfig
    ): MillExecutionPlan =
        val baseline = LifecycleBaseline.resolve(config, properties = request.properties)
        val steps = request.kind match
            case ExecutionRequestKind.LifecyclePhase =>
                resolveLifecyclePhase(request, baseline)
            case ExecutionRequestKind.ExplicitGoal =>
                resolveExplicitGoal(request, config)

        MillExecutionPlan(
            request = request,
            executionMode = baseline.executionMode,
            steps = steps
        )

    private def resolveLifecyclePhase(
        request: ExecutionRequest,
        baseline: LifecycleBaseline
    ): Seq[PlanStep] =
        val lifecycleSteps = baseline.lifecycleMappings.get(request.requestedName) match
            case Some(targets) if targets.nonEmpty =>
                Seq(PlanStep.InvokeMill(targets))
            case Some(_) =>
                Seq.empty
            case None =>
                Seq(
                    PlanStep.Fail(
                        message = s"No mapping found for lifecycle phase '${request.requestedName}'",
                        guidance = Seq(
                            "Add a lifecycle mapping in mill-interceptor.yaml or mill-interceptor.pkl"
                        )
                    )
                )

        request.requestedName match
            case "validate" => resolveValidateHookSteps(baseline) ++ lifecycleSteps
            case _          => lifecycleSteps

    private def resolveExplicitGoal(
        request: ExecutionRequest,
        config: EffectiveConfig
    ): Seq[PlanStep] =
        config.goals.get(request.requestedName) match
            case Some(targets) =>
                Seq(PlanStep.InvokeMill(targets))
            case None =>
                Seq(
                    PlanStep.Fail(
                        message = s"No mapping found for explicit goal '${request.requestedName}'",
                        guidance = Seq(
                            "Add a goal mapping in mill-interceptor.yaml or mill-interceptor.pkl"
                        )
                    )
                )

    private def resolveValidateHookSteps(
        baseline: LifecycleBaseline
    ): Seq[PlanStep] =
        baseline.validate.scalafmt.toSeq.flatMap { hook =>
            Seq(
                PlanStep.ProbeTarget(hook.target),
                PlanStep.InvokeMill(Seq(hook.target))
            )
        }
