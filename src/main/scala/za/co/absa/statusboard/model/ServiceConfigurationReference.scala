package za.co.absa.statusboard.model

import io.circe.generic.JsonCodec
import sttp.tapir.generic.auto._ // must be imported to support OpenAPI auto-derivation

@JsonCodec
case class ServiceConfigurationReference(environment: String, service: String)
