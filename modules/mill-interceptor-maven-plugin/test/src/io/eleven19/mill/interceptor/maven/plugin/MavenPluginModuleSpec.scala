package io.eleven19.mill.interceptor.maven.plugin

import io.eleven19.mill.interceptor.maven.plugin.mojo.CleanMojo
import io.eleven19.mill.interceptor.maven.plugin.mojo.DescribeMojo
import io.eleven19.mill.interceptor.maven.plugin.mojo.CompileMojo
import io.eleven19.mill.interceptor.maven.plugin.mojo.DeployMojo
import io.eleven19.mill.interceptor.maven.plugin.mojo.InspectPlanMojo
import io.eleven19.mill.interceptor.maven.plugin.mojo.InstallMojo
import io.eleven19.mill.interceptor.maven.plugin.mojo.PackageMojo
import io.eleven19.mill.interceptor.maven.plugin.mojo.TestMojo
import io.eleven19.mill.interceptor.maven.plugin.mojo.ValidateMojo
import io.eleven19.mill.interceptor.maven.plugin.mojo.VerifyMojo
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

    private val expectedLifecycleImplementations = Map(
      "clean"    -> classOf[CleanMojo].getName,
      "validate" -> classOf[ValidateMojo].getName,
      "compile"  -> classOf[CompileMojo].getName,
      "test"     -> classOf[TestMojo].getName,
      "package"  -> classOf[PackageMojo].getName,
      "verify"   -> classOf[VerifyMojo].getName,
      "install"  -> classOf[InstallMojo].getName,
      "deploy"   -> classOf[DeployMojo].getName
    )

    private val expectedOperationalImplementations = Map(
      "describe"     -> classOf[DescribeMojo].getName,
      "inspect-plan" -> classOf[InspectPlanMojo].getName
    )

    def spec: Spec[Any, Any] = suite("MavenPluginModule")(
        test("exposes the published artifact id") {
            assertTrue(MavenPluginModule.artifactId == "mill-interceptor-maven-plugin")
        },
        test("exposes the full supported goal registry") {
            assertTrue(MavenPluginModule.supportedGoals.map(_.goal) == expectedGoals)
        },
        test("maps lifecycle goals to distinct concrete mojo implementations") {
            assertTrue(
                expectedLifecycleImplementations.forall { case (goal, implementationClass) =>
                    MavenPluginModule.supportedGoals
                        .find(_.goal == goal)
                        .exists(_.implementationClass == implementationClass)
                }
            )
        },
        test("maps operational goals to their concrete mojo implementations") {
            assertTrue(
                expectedOperationalImplementations.forall { case (goal, implementationClass) =>
                    MavenPluginModule.supportedGoals
                        .find(_.goal == goal)
                        .exists(_.implementationClass == implementationClass)
                }
            )
        },
        test("packages a Maven plugin descriptor resource") {
            val descriptor = Option(getClass.getResourceAsStream("/META-INF/maven/plugin.xml"))
                .map(stream => scala.io.Source.fromInputStream(stream).mkString)

            assertTrue(descriptor.nonEmpty) &&
            assertTrue(expectedGoals.forall(goal => descriptor.exists(_.contains(s"<goal>$goal</goal>")))) &&
            assertTrue(descriptor.exists(_.contains(classOf[DescribeMojo].getName))) &&
            assertTrue(
                expectedLifecycleImplementations.values.forall(impl =>
                    descriptor.exists(_.contains(impl))
                )
            )
        }
    )
