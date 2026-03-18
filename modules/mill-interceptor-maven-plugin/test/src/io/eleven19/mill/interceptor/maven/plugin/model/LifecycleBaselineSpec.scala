package io.eleven19.mill.interceptor.maven.plugin.model

import io.eleven19.mill.interceptor.maven.plugin.config.{EffectiveConfig, ValidateConfig}
import kyo.test.KyoSpecDefault
import zio.test.*

object LifecycleBaselineSpec extends KyoSpecDefault:

    def spec: Spec[Any, Any] = suite("LifecycleBaseline")(
        test("provides the default lifecycle mappings for clean through deploy") {
            val baseline = LifecycleBaseline.resolve(EffectiveConfig())

            assertTrue(baseline.executionMode == ExecutionMode.Strict) &&
            assertTrue(
                baseline.lifecycleMappings == Map(
                    "clean" -> Seq("clean"),
                    "validate" -> Seq.empty,
                    "compile" -> Seq("compile"),
                    "test" -> Seq("compile", "test"),
                    "package" -> Seq("compile", "test", "jar"),
                    "verify" -> Seq("compile", "test"),
                    "install" -> Seq("compile", "test", "jar", "publishLocal"),
                    "deploy" -> Seq("compile", "test", "jar", "publish")
                )
            ) &&
            assertTrue(
                baseline.validate.scalafmt == Some(
                    ValidateScalafmtHook(
                        target = "checkFormat",
                        disableProperty = "mill.interceptor.scalafmt"
                    )
                )
            )
        },
        test("replaces baseline phase mappings with explicit lifecycle overrides") {
            val config = EffectiveConfig(
                lifecycle = Map(
                    "compile" -> Seq("core.compile"),
                    "test" -> Seq("core.compile", "core.test")
                )
            )

            val baseline = LifecycleBaseline.resolve(config)

            assertTrue(baseline.lifecycleMappings("compile") == Seq("core.compile")) &&
            assertTrue(baseline.lifecycleMappings("test") == Seq("core.compile", "core.test")) &&
            assertTrue(baseline.lifecycleMappings("package") == Seq("compile", "test", "jar"))
        },
        test("disables the optional validate scalafmt hook via config or property override") {
            val disabledByConfig = LifecycleBaseline.resolve(
                EffectiveConfig(validate = ValidateConfig(scalafmtEnabled = false))
            )
            val disabledByProperty = LifecycleBaseline.resolve(
                EffectiveConfig(validate = ValidateConfig(scalafmtEnabled = true)),
                properties = Map("mill.interceptor.scalafmt" -> "false")
            )

            assertTrue(disabledByConfig.validate.scalafmt.isEmpty) &&
            assertTrue(disabledByProperty.validate.scalafmt.isEmpty)
        }
    )
