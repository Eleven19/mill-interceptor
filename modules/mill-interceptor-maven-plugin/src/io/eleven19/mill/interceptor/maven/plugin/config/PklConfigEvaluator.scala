package io.eleven19.mill.interceptor.maven.plugin.config

import os.Path
import org.pkl.core.{Evaluator, ModuleSource, PModule, PObject}

import scala.jdk.CollectionConverters.*

/** Evaluates PKL config modules and converts them into the overlay model used by the plugin. */
object PklConfigEvaluator:

    /** Load one PKL file and normalize its module properties into a `ConfigOverlay`. */
    def load(path: os.Path): Either[ConfigLoadException, ConfigOverlay] =
        try
            val evaluator = Evaluator.preconfigured()
            try
                val module = evaluator.evaluate(ModuleSource.path(path.toNIO))
                Right(ConfigOverlay.fromRawMap(toRawMap(module), path))
            finally evaluator.close()
        catch
            case error: ConfigLoadException => Left(error)
            case error: Throwable           => Left(ConfigLoadException(path, error.getMessage.nn, error))

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
