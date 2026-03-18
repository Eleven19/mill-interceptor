package io.eleven19.mill.interceptor.maven.plugin.config

import kyo.Path
import org.virtuslab.yaml.*

final case class EffectiveConfig(
    mode: String = "strict",
    mill: MillConfig = MillConfig(),
    lifecycle: Map[String, Seq[String]] = Map.empty,
    goals: Map[String, Seq[String]] = Map.empty,
    validate: ValidateConfig = ValidateConfig()
) derives CanEqual

final case class MillConfig(
    executable: String = "mill",
    environment: Map[String, String] = Map.empty,
    workingDirectory: Option[String] = None
) derives CanEqual, YamlCodec

final case class ValidateConfig(
    scalafmtEnabled: Boolean = false,
    scalafmtTarget: Option[String] = None
) derives CanEqual, YamlCodec

final case class ConfigOverlay(
    mode: Option[String] = None,
    mill: Option[MillConfigOverlay] = None,
    lifecycle: Map[String, Seq[String]] = Map.empty,
    goals: Map[String, Seq[String]] = Map.empty,
    validate: Option[ValidateConfigOverlay] = None
) derives CanEqual, YamlCodec:

    def merge(other: ConfigOverlay): ConfigOverlay =
        ConfigOverlay(
          mode = other.mode.orElse(mode),
          mill = (mill, other.mill) match
              case (Some(existing), Some(incoming)) => Some(existing.merge(incoming))
              case (_, Some(incoming))              => Some(incoming)
              case (Some(existing), None)           => Some(existing)
              case (None, None)                     => None,
          lifecycle = lifecycle ++ other.lifecycle,
          goals = goals ++ other.goals,
          validate = (validate, other.validate) match
              case (Some(existing), Some(incoming)) => Some(existing.merge(incoming))
              case (_, Some(incoming))              => Some(incoming)
              case (Some(existing), None)           => Some(existing)
              case (None, None)                     => None
        )

    def toEffectiveConfig: EffectiveConfig =
        EffectiveConfig(
          mode = mode.getOrElse("strict"),
          mill = mill.map(_.toEffectiveConfig).getOrElse(MillConfig()),
          lifecycle = lifecycle,
          goals = goals,
          validate = validate.map(_.toEffectiveConfig).getOrElse(ValidateConfig())
        )

object ConfigOverlay:

    def fromRawMap(raw: Map[String, Any], source: Path): ConfigOverlay =
        ConfigOverlay(
          mode = optionalString(raw, "mode", source),
          mill = optionalObject(raw, "mill", source).map(MillConfigOverlay.fromRawMap(_, source)),
          lifecycle = optionalCommandMap(raw, "lifecycle", source).getOrElse(Map.empty),
          goals = optionalCommandMap(raw, "goals", source).getOrElse(Map.empty),
          validate = optionalObject(raw, "validate", source).map(ValidateConfigOverlay.fromRawMap(_, source))
        )

    private def optionalString(raw: Map[String, Any], field: String, source: Path): Option[String] =
        raw.get(field).map {
            case value: String => value
            case other =>
                throw ConfigLoadException(source, s"Expected '$field' to be a string but found ${other.getClass.getSimpleName}")
        }

    private def optionalObject(
        raw: Map[String, Any],
        field: String,
        source: Path
    ): Option[Map[String, Any]] =
        raw.get(field).map {
            case value: Map[?, ?] =>
                value.toSeq.collect { case (key: String, nested) => key -> nested }.toMap
            case other =>
                throw ConfigLoadException(source, s"Expected '$field' to be an object but found ${other.getClass.getSimpleName}")
        }

    private def optionalCommandMap(
        raw: Map[String, Any],
        field: String,
        source: Path
    ): Option[Map[String, Seq[String]]] =
        optionalObject(raw, field, source).map(_.map { case (key, value) =>
            key -> decodeStringSeq(field, key, value, source)
        })

    private def decodeStringSeq(
        parentField: String,
        field: String,
        value: Any,
        source: Path
    ): Seq[String] =
        value match
            case values: Seq[?] =>
                values.map {
                    case item: String => item
                    case other =>
                        throw ConfigLoadException(
                          source,
                          s"Expected '$parentField.$field' to contain only strings but found ${other.getClass.getSimpleName}"
                        )
                }
            case other =>
                throw ConfigLoadException(
                  source,
                  s"Expected '$parentField.$field' to be a sequence of strings but found ${other.getClass.getSimpleName}"
                )

final case class MillConfigOverlay(
    executable: Option[String] = None,
    environment: Map[String, String] = Map.empty,
    workingDirectory: Option[String] = None
) derives CanEqual, YamlCodec:

    def merge(other: MillConfigOverlay): MillConfigOverlay =
        MillConfigOverlay(
          executable = other.executable.orElse(executable),
          environment = environment ++ other.environment,
          workingDirectory = other.workingDirectory.orElse(workingDirectory)
        )

    def toEffectiveConfig: MillConfig =
        MillConfig(
          executable = executable.getOrElse("mill"),
          environment = environment,
          workingDirectory = workingDirectory
        )

object MillConfigOverlay:

    def fromRawMap(raw: Map[String, Any], source: Path): MillConfigOverlay =
        MillConfigOverlay(
          executable = raw.get("executable").map {
              case value: String => value
              case other =>
                  throw ConfigLoadException(
                    source,
                    s"Expected 'mill.executable' to be a string but found ${other.getClass.getSimpleName}"
                  )
          },
          environment = raw.get("environment") match
              case None => Map.empty
              case Some(values: Map[?, ?]) =>
                  values.map {
                      case (key: String, value: String) => key -> value
                      case (_, other) =>
                          throw ConfigLoadException(
                            source,
                            s"Expected 'mill.environment' values to be strings but found ${other.getClass.getSimpleName}"
                          )
                  }.toMap
              case Some(other) =>
                  throw ConfigLoadException(
                    source,
                    s"Expected 'mill.environment' to be an object but found ${other.getClass.getSimpleName}"
                  ),
          workingDirectory = raw.get("workingDirectory").map {
              case value: String => value
              case other =>
                  throw ConfigLoadException(
                    source,
                    s"Expected 'mill.workingDirectory' to be a string but found ${other.getClass.getSimpleName}"
                  )
          }
        )

final case class ValidateConfigOverlay(
    scalafmtEnabled: Option[Boolean] = None,
    scalafmtTarget: Option[String] = None
) derives CanEqual, YamlCodec:

    def merge(other: ValidateConfigOverlay): ValidateConfigOverlay =
        ValidateConfigOverlay(
          scalafmtEnabled = other.scalafmtEnabled.orElse(scalafmtEnabled),
          scalafmtTarget = other.scalafmtTarget.orElse(scalafmtTarget)
        )

    def toEffectiveConfig: ValidateConfig =
        ValidateConfig(
          scalafmtEnabled = scalafmtEnabled.getOrElse(false),
          scalafmtTarget = scalafmtTarget
        )

object ValidateConfigOverlay:

    def fromRawMap(raw: Map[String, Any], source: Path): ValidateConfigOverlay =
        ValidateConfigOverlay(
          scalafmtEnabled = raw.get("scalafmtEnabled").map {
              case value: Boolean => value
              case other =>
                  throw ConfigLoadException(
                    source,
                    s"Expected 'validate.scalafmtEnabled' to be a boolean but found ${other.getClass.getSimpleName}"
                  )
          },
          scalafmtTarget = raw.get("scalafmtTarget").map {
              case value: String => value
              case other =>
                  throw ConfigLoadException(
                    source,
                    s"Expected 'validate.scalafmtTarget' to be a string but found ${other.getClass.getSimpleName}"
                  )
          }
        )

final case class ConfigLoadException(path: Path, detail: String, cause0: Throwable | Null = null)
    extends RuntimeException(s"Failed to load config from $path: $detail", cause0)
