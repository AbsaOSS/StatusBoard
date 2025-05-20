/*
 * Copyright 2024 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
