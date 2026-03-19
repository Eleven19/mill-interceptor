package io.eleven19.mill.interceptor.maven.plugin.mojo

import org.apache.maven.plugins.annotations.Mojo

@Mojo(name = "test", threadSafe = true)
class TestMojo extends AbstractForwardingMojo:

    override protected def executionContext: MavenExecutionContext =
        throw new UnsupportedOperationException("Lifecycle forwarding is not implemented yet.")

    override def execute(): Unit =
        placeholderExecute()
