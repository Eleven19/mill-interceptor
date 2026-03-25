package io.eleven19.mill.interceptor.maven.plugin.mojo

import io.eleven19.mill.interceptor.maven.plugin.model.ExecutionRequestKind
import org.apache.maven.plugins.annotations.Mojo

@Mojo(name = "clean", threadSafe = true)
class CleanMojo extends AbstractForwardingMojo:

    override protected def executionKind: ExecutionRequestKind =
        ExecutionRequestKind.LifecyclePhase

    override protected def requestedName: String = "clean"
