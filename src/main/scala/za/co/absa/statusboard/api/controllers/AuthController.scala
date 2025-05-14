package za.co.absa.statusboard.api.controllers

import za.co.absa.statusboard.model.ErrorResponse
import zio.IO
import zio.macros.accessible

/**
 *  Interface for auth
 */
@accessible
trait AuthController {
  /**
   *  Authorizes execution of the method (or maybe not)
   */
  def authorize(apiKey: String): IO[ErrorResponse, Unit]
}
