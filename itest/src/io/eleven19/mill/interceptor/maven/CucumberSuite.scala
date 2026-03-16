package io.eleven19.mill.interceptor.maven

import org.junit.platform.suite.api.*

@Suite
@IncludeEngines(Array("cucumber"))
@SelectClasspathResource("features")
class CucumberSuite
