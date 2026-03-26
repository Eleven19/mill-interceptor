package io.eleven19.mill.interceptor.maven.plugin.mojo

import io.eleven19.mill.interceptor.model.ExecutionRequestKind
import org.apache.maven.plugins.annotations.Mojo

/** Forwards Maven `deploy` requests into the resolved Mill execution plan. */
@Mojo(name = "deploy", threadSafe = true)
class DeployMojo extends AbstractForwardingMojo:

    override protected def executionKind: ExecutionRequestKind =
        ExecutionRequestKind.LifecyclePhase

    override protected def requestedName: String = "deploy"
