package io.eleven19.mill.interceptor.maven.plugin

import kyo.test.KyoSpecDefault
import zio.test.*

object MavenPluginModuleSpec extends KyoSpecDefault:

    def spec: Spec[Any, Any] = suite("MavenPluginModule")(
        test("exposes the module name") {
            assertTrue(MavenPluginModule.moduleName == "mill-interceptor-maven-plugin")
        }
    )
