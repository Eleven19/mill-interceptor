package io.eleven19.mill.interceptor.maven.plugin.exec

import zio.test.*

object MillRunnerResultSpec extends ZIOSpecDefault:

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
        test("models typed fail-step failures with guidance") {
            val failure = RunnerFailure.FailStep(
                message = "No mapping found for explicit goal 'deploy-site'",
                guidance = Seq("Add a goal mapping in mill-interceptor.yaml or mill-interceptor.pkl")
            )

            assertTrue(
                failure.message.contains("deploy-site")
            ) &&
            assertTrue(failure.guidance.size == 1)
        },
        test("models probe and invocation failures with command metadata") {
            val probeFailure = RunnerFailure.ProbeFailure(
                target = "checkFormat",
                command = Seq("mill", "resolve", "checkFormat"),
                exitCode = Some(1),
                message = "Mill target checkFormat was not available"
            )
            val invocationFailure = RunnerFailure.InvocationFailure(
                command = Seq("mill", "compile"),
                exitCode = Some(2),
                message = "Mill compile failed"
            )

            assertTrue(probeFailure.target == "checkFormat") &&
            assertTrue(probeFailure.command == Seq("mill", "resolve", "checkFormat")) &&
            assertTrue(probeFailure.exitCode.contains(1)) &&
            assertTrue(invocationFailure.command == Seq("mill", "compile")) &&
            assertTrue(invocationFailure.exitCode.contains(2))
        },
        test("wraps success and failure states in a runner result ADT") {
            val success = RunnerResult.Success(
                stepResults = Seq(
                    StepResult(
                        kind = RunnerStepKind.InvokeMill,
                        command = Some(Seq("mill", "compile")),
                        exitCode = Some(0)
                    )
                )
            )
            val failure = RunnerResult.Failure(
                stepResults = Seq(
                    StepResult(
                        kind = RunnerStepKind.ProbeTarget,
                        command = Some(Seq("mill", "resolve", "checkFormat")),
                        exitCode = Some(1)
                    )
                ),
                failure = RunnerFailure.ProbeFailure(
                    target = "checkFormat",
                    command = Seq("mill", "resolve", "checkFormat"),
                    exitCode = Some(1),
                    message = "Mill target checkFormat was not available"
                )
            )

            assertTrue(success.stepResults.head.command.contains(Seq("mill", "compile"))) &&
            assertTrue(failure.stepResults.head.command.contains(Seq("mill", "resolve", "checkFormat"))) &&
            assertTrue(failure.failure.message.contains("checkFormat"))
        }
    )
