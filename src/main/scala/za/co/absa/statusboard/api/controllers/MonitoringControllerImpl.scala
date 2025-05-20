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

import za.co.absa.statusboard.model.ErrorResponse
import za.co.absa.statusboard.model.ErrorResponse.InternalServerErrorResponse
import za.co.absa.statusboard.monitoring.MonitoringService
import zio._

class MonitoringControllerImpl(monitoringService: MonitoringService) extends MonitoringController {
  override def restart: IO[ErrorResponse, Unit] =
    monitoringService.restart.mapError(error => InternalServerErrorResponse(error.getMessage))

  override def restartForService(environment: String, serviceName: String): IO[ErrorResponse, Unit] =
    monitoringService.restartForService(environment, serviceName).mapError(error => InternalServerErrorResponse(error.getMessage))
}

object MonitoringControllerImpl {
  val layer: RLayer[MonitoringService, MonitoringController] = ZLayer {
    for {
      monitoringService <- ZIO.service[MonitoringService]
    } yield new MonitoringControllerImpl(monitoringService)
  }
}
