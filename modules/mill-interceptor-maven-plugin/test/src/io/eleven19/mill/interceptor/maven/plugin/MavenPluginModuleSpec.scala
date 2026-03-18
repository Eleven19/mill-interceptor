package io.eleven19.mill.interceptor.maven.plugin

import io.eleven19.mill.interceptor.maven.plugin.mojo.DescribeMojo
import kyo.test.KyoSpecDefault
import org.apache.maven.plugin.AbstractMojo
import zio.test.*

object MavenPluginModuleSpec extends KyoSpecDefault:

    private val expectedGoals = Seq(
      "describe",
      "inspect-plan",
      "clean",
      "validate",
      "compile",
      "test",
      "package",
      "verify",
      "install",
      "deploy"
    )

    def spec: Spec[Any, Any] = suite("MavenPluginModule")(
        test("exposes the published artifact id") {
            assertTrue(MavenPluginModule.artifactId == "mill-interceptor-maven-plugin")
        },
        test("exposes the full supported goal registry") {
            assertTrue(MavenPluginModule.supportedGoals.map(_.goal) == expectedGoals)
        },
        test("exposes a concrete placeholder mojo implementation") {
            assertTrue(classOf[AbstractMojo].isAssignableFrom(classOf[DescribeMojo]))
        },
        test("packages a Maven plugin descriptor resource") {
            val descriptor = Option(getClass.getResourceAsStream("/META-INF/maven/plugin.xml"))
                .map(stream => scala.io.Source.fromInputStream(stream).mkString)

            assertTrue(descriptor.nonEmpty) &&
            assertTrue(expectedGoals.forall(goal => descriptor.exists(_.contains(s"<goal>$goal</goal>")))) &&
            assertTrue(
                descriptor.exists(
                    _.contains(
                        "io.eleven19.mill.interceptor.maven.plugin.mojo.DescribeMojo"
                    )
                )
            )
        }
    )
