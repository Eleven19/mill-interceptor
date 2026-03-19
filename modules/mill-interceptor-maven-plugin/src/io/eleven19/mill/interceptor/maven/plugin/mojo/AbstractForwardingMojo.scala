package io.eleven19.mill.interceptor.maven.plugin.mojo

import io.eleven19.mill.interceptor.maven.plugin.model.ExecutionRequest
import org.apache.maven.plugin.AbstractMojo

/** Shared Mojo base for lifecycle-forwarding Maven goals.
  *
  * Later lifecycle Mojos should supply a Maven execution context here and use the derived neutral request for
  * resolution and execution.
  */
abstract class AbstractForwardingMojo extends AbstractMojo:

    protected def executionContext: MavenExecutionContext

    final protected def executionRequest: ExecutionRequest =
        executionContext.toExecutionRequest
