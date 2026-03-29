package io.eleven19.mill.interceptor.maven.plugin

/** Parameter metadata for a Maven mojo field, used to generate plugin.xml descriptors.
  *
  * This is the single source of truth for the parameter declarations that Maven needs to inject values into mojo
  * fields. The Mill build loads this class after compilation to generate `META-INF/maven/plugin.xml`. A companion unit
  * test validates that every declared parameter corresponds to an actual field on the mojo class.
  */
final case class MojoParameterMeta(
    name: String,
    javaType: String,
    defaultValue: String,
    required: Boolean,
    description: String
)

/** Descriptor metadata for Maven plugin mojos, used by the Mill build to generate plugin.xml.
  *
  * When invoked as a main class, writes the forwarding parameters XML to stdout. The Mill build uses this via
  * subprocess invocation to avoid classloader-based reflection (keeping the build GraalVM native-image friendly).
  */
object MojoDescriptorMeta:

    /** Entry point for the Mill build to generate parameters XML without reflection. */
    def main(args: Array[String]): Unit =
        print(forwardingParametersXml())

    /** All `@Parameter`-annotated fields declared on `AbstractForwardingMojo`. */
    val forwardingParameters: Array[MojoParameterMeta] = Array(
        MojoParameterMeta(
            name = "repoRootDirectory",
            javaType = "java.io.File",
            defaultValue = "${session.executionRootDirectory}",
            required = true,
            description = "Root directory of the repository"
        ),
        MojoParameterMeta(
            name = "moduleRootDirectory",
            javaType = "java.io.File",
            defaultValue = "${project.basedir}",
            required = true,
            description = "Base directory of the current Maven module"
        ),
        MojoParameterMeta(
            name = "mavenSession",
            javaType = "org.apache.maven.execution.MavenSession",
            defaultValue = "${session}",
            required = false,
            description = "The current Maven session"
        ),
        MojoParameterMeta(
            name = "mavenProject",
            javaType = "org.apache.maven.project.MavenProject",
            defaultValue = "${project}",
            required = false,
            description = "The current Maven project"
        ),
        MojoParameterMeta(
            name = "artifactId",
            javaType = "java.lang.String",
            defaultValue = "${project.artifactId}",
            required = true,
            description = "The project artifact ID"
        ),
        MojoParameterMeta(
            name = "packaging",
            javaType = "java.lang.String",
            defaultValue = "${project.packaging}",
            required = true,
            description = "The project packaging type"
        ),
        MojoParameterMeta(
            name = "groupId",
            javaType = "java.lang.String",
            defaultValue = "${project.groupId}",
            required = true,
            description = "The project group ID"
        ),
        MojoParameterMeta(
            name = "sessionUserProperties",
            javaType = "java.util.Properties",
            defaultValue = "${session.userProperties}",
            required = false,
            description = "User properties from the Maven session"
        )
    )

    /** Generate the `<parameters>` and `<configuration>` XML block for forwarding mojos. */
    def forwardingParametersXml(): String =
        val params = forwardingParameters
            .map { p =>
                s"""|        <parameter>
                    |          <name>${p.name}</name>
                    |          <type>${p.javaType}</type>
                    |          <required>${p.required}</required>
                    |          <editable>false</editable>
                    |          <description>${p.description}</description>
                    |        </parameter>""".stripMargin
            }
            .mkString("\n")

        val configs = forwardingParameters
            .map { p =>
                s"""        <${p.name} implementation="${p.javaType}" default-value="${p.defaultValue}"/>"""
            }
            .mkString("\n")

        s"""|      <parameters>
            |$params
            |      </parameters>
            |      <configuration>
            |$configs
            |      </configuration>""".stripMargin
