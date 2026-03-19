package io.eleven19.mill.interceptor.maven.plugin.exec

import kyo.test.KyoSpecDefault
import zio.test.*

object MillRunnerResultSpec extends KyoSpecDefault:

    def spec: Spec[Any, Any] = suite("MillRunnerResult")(
        test("models step results with rendered command vectors and exit codes") {
            val stepResult = StepResult(
                kind = RunnerStepKind.ProbeTarget,
                command = Some(Seq("mill", "resolve", "checkFormat")),
                exitCode = Some(0)
            )

            assertTrue(stepResult.kind == RunnerStepKind.ProbeTarget) &&
            assertTrue(stepResult.command.contains(Seq("mill", "resolve", "checkFormat"))) &&
            assertTrue(stepResult.exitCode.contains(0))
        },
        test("models typed failures with guidance and process metadata") {
            val failure = RunnerFailure(
                kind = RunnerStepKind.Fail,
                message = "No mapping found for explicit goal 'deploy-site' in strict mode",
                guidance = Seq("Add a goal mapping in mill-interceptor.yaml or mill-interceptor.pkl")
            )

            assertTrue(
                failure.kind == RunnerStepKind.Fail
            ) &&
            assertTrue(failure.message.contains("deploy-site")) &&
            assertTrue(failure.guidance.size == 1)
        },
        test("wraps completed steps and terminal status in a runner result") {
            val result = RunnerResult(
                status = RunnerStatus.Success,
                stepResults = Seq(
                    StepResult(
                        kind = RunnerStepKind.InvokeMill,
                        command = Some(Seq("mill", "compile")),
                        exitCode = Some(0)
                    )
                )
            )

            assertTrue(result.status == RunnerStatus.Success) &&
            assertTrue(result.stepResults.head.command.contains(Seq("mill", "compile"))) &&
            assertTrue(result.failure.isEmpty)
        }
    )
