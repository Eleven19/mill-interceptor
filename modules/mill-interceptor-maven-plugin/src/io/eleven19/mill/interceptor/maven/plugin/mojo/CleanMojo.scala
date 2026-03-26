package io.eleven19.mill.interceptor.maven.plugin.mojo

import io.eleven19.mill.interceptor.model.ExecutionRequestKind
import org.apache.maven.plugins.annotations.Mojo

/** Forwards Maven `clean` requests into the resolved Mill execution plan. */
@Mojo(name = "clean", threadSafe = true)
class CleanMojo extends AbstractForwardingMojo:

    override protected def executionKind: ExecutionRequestKind =
        ExecutionRequestKind.LifecyclePhase

    override protected def requestedName: String = "clean"
