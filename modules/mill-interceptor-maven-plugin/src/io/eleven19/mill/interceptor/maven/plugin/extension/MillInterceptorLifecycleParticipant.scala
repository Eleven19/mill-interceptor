package io.eleven19.mill.interceptor.maven.plugin.extension

import javax.inject.Named
import javax.inject.Singleton
import org.apache.maven.AbstractMavenLifecycleParticipant

/** Core-extension bootstrap surface for the Maven interceptor artifact.
  *
  * The first blocker for extension-only activation is making the artifact discoverable by Maven's container when it is
  * loaded from `.mvn/extensions.xml`. The actual lifecycle interception behavior is added in later tasks.
  */
@Named
@Singleton
final class MillInterceptorLifecycleParticipant extends AbstractMavenLifecycleParticipant
