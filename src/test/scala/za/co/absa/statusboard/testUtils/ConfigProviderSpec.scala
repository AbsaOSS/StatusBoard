package za.co.absa.statusboard.testUtils

import zio.config.typesafe.TypesafeConfigProvider
import zio.test.ZIOSpec
import zio.{Runtime, ZLayer}

abstract class ConfigProviderSpec extends ZIOSpec[Unit] {
  override def bootstrap: ZLayer[Any, Any, Unit] =
    Runtime.setConfigProvider(TypesafeConfigProvider.fromResourcePath())
}
