package io.eleven19.mill.interceptor.maven.plugin.config

import kyo.*
import org.pkl.core.{Evaluator, ModuleSource, PModule, PObject}

import scala.jdk.CollectionConverters.*

object PklConfigEvaluator:

    def load(path: Path): ConfigOverlay < (Abort[ConfigLoadException] & Sync) =
        Abort.catching[ConfigLoadException] {
            Sync.defer {
                try
                    val evaluator = Evaluator.preconfigured()
                    try
                        val module = evaluator.evaluate(ModuleSource.path(path.toJava))
                        ConfigOverlay.fromRawMap(toRawMap(module), path)
                    finally evaluator.close()
                catch
                    case error: ConfigLoadException => throw error
                    case error: Throwable           => throw ConfigLoadException(path, error.getMessage.nn, error)
            }
        }

    private def toRawMap(module: PModule): Map[String, Any] =
        module.getProperties.asScala.toMap.view.mapValues(convertValue).toMap

    private def convertValue(value: Any): Any =
        value match
            case module: PModule =>
                module.getProperties.asScala.toMap.view.mapValues(convertValue).toMap
            case obj: PObject =>
                obj.getProperties.asScala.toMap.view.mapValues(convertValue).toMap
            case map: java.util.Map[?, ?] =>
                map.asScala.toMap.collect { case (key: String, nested) => key -> convertValue(nested) }
            case list: java.util.List[?] =>
                list.asScala.toSeq.map(convertValue)
            case other => other
