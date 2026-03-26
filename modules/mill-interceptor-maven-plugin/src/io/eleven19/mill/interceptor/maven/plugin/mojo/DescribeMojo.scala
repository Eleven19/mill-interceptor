package io.eleven19.mill.interceptor.maven.plugin.mojo

import io.eleven19.mill.interceptor.maven.plugin.MavenPluginModule
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Mojo

/** Emits a human-readable description of the Maven plugin surface. */
@Mojo(name = "describe", threadSafe = true)
class DescribeMojo extends AbstractMojo:

    override def execute(): Unit =
        getLog.info(MavenPluginModule.placeholderMessage)
