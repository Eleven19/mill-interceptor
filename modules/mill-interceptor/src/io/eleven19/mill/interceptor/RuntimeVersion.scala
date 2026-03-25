package io.eleven19.mill.interceptor

object RuntimeVersion:

    def current: Option[String] =
        Option(getClass.getPackage)
            .flatMap(pkg => Option(pkg.getImplementationVersion))
