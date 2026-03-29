package io.eleven19.mill.interceptor.maven.plugin

import io.eleven19.mill.interceptor.maven.plugin.mojo.AbstractForwardingMojo
import zio.test.*

object MojoDescriptorMetaSpec extends ZIOSpecDefault:

    def spec: Spec[Any, Any] = suite("MojoDescriptorMeta")(
        test("forwardingParameters declares exactly the fields on AbstractForwardingMojo") {
            val declaredNames = MojoDescriptorMeta.forwardingParameters.map(_.name).toSet
            val mojoFields = allFieldNames(classOf[AbstractForwardingMojo])

            val missing = declaredNames -- mojoFields
            val extra = declaredNames.filter(name => !mojoFields.contains(name))

            assertTrue(missing.isEmpty) &&
            assertTrue(extra.isEmpty)
        },
        test("forwardingParameters declares correct Java types for each field") {
            val mojoFieldTypes = allFieldTypes(classOf[AbstractForwardingMojo])

            val mismatches = MojoDescriptorMeta.forwardingParameters.flatMap { param =>
                mojoFieldTypes.get(param.name) match
                    case Some(actualType) if actualType != param.javaType =>
                        Some(s"${param.name}: declared=${param.javaType}, actual=$actualType")
                    case None =>
                        Some(s"${param.name}: field not found on AbstractForwardingMojo")
                    case _ => None
            }

            assertTrue(mismatches.isEmpty)
        },
        test("forwardingParametersXml generates valid XML with all parameters") {
            val xml = MojoDescriptorMeta.forwardingParametersXml()

            assertTrue(xml.contains("<parameters>")) &&
            assertTrue(xml.contains("</parameters>")) &&
            assertTrue(xml.contains("<configuration>")) &&
            assertTrue(xml.contains("</configuration>")) &&
            assertTrue(
                MojoDescriptorMeta.forwardingParameters.forall(p => xml.contains(s"<name>${p.name}</name>"))
            ) &&
            assertTrue(
                MojoDescriptorMeta.forwardingParameters.forall(p =>
                    xml.contains(s"""default-value="${p.defaultValue}"""")
                )
            )
        }
    )

    private def allFieldNames(clazz: Class[?]): Set[String] =
        allFields(clazz).map(_.getName).toSet

    private def allFieldTypes(clazz: Class[?]): Map[String, String] =
        allFields(clazz).map(f => f.getName -> f.getType.getName).toMap

    private def allFields(clazz: Class[?]): Array[java.lang.reflect.Field] =
        var current: Class[?] = clazz
        val fields = scala.collection.mutable.ArrayBuffer.empty[java.lang.reflect.Field]
        while current != null do
            fields.appendAll(current.getDeclaredFields)
            current = current.getSuperclass
        fields.toArray
