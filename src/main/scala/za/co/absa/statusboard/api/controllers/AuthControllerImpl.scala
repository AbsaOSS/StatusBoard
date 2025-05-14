package za.co.absa.statusboard.api.controllers

import za.co.absa.statusboard.config.ServerConfig
import za.co.absa.statusboard.model.ErrorResponse
import za.co.absa.statusboard.model.ErrorResponse.UnauthorizedErrorResponse
import zio.{IO, TaskLayer, ZIO, ZLayer}

class AuthControllerImpl(validKey: String) extends AuthController {
  def authorize(apiKey: String): IO[ErrorResponse, Unit] =
    if (apiKey == validKey) ZIO.unit else ZIO.fail(UnauthorizedErrorResponse("Unauthorized"))
}

object AuthControllerImpl {
  val layer: TaskLayer[AuthController] = ZLayer {
    for {
      config <- ZIO.config[ServerConfig](ServerConfig.config)
    } yield new AuthControllerImpl(config.apiKey)
  }
}
