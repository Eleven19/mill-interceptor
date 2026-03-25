package io.eleven19.mill.interceptor.maven.plugin.mojo

import io.eleven19.mill.interceptor.model.ExecutionRequestKind
import org.apache.maven.plugins.annotations.Mojo

@Mojo(name = "inspect-plan", threadSafe = true)
class InspectPlanMojo extends AbstractForwardingMojo:

    override protected def executionKind: ExecutionRequestKind =
        ExecutionRequestKind.ExplicitGoal

    override protected def requestedName: String = "inspect-plan"

    override protected def inspectOnly: Boolean = true
