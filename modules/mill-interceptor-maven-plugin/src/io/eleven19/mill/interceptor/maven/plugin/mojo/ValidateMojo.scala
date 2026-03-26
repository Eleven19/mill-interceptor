package io.eleven19.mill.interceptor.maven.plugin.mojo

import io.eleven19.mill.interceptor.model.ExecutionRequestKind
import org.apache.maven.plugins.annotations.Mojo

@Mojo(name = "validate", threadSafe = true)
class ValidateMojo extends AbstractForwardingMojo:

    override protected def executionKind: ExecutionRequestKind =
        ExecutionRequestKind.LifecyclePhase

    override protected def requestedName: String = "validate"
