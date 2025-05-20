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

import za.co.absa.statusboard.model.ApiResponse.{MultiApiResponse, SingleApiResponse}
import za.co.absa.statusboard.model.AppError.DatabaseError.RecordNotFoundDatabaseError
import za.co.absa.statusboard.model.{ErrorResponse, RefinedStatus}
import za.co.absa.statusboard.model.ErrorResponse.{InternalServerErrorResponse, RecordNotFoundErrorResponse}
import za.co.absa.statusboard.repository.StatusRepository
import zio.{IO, RLayer, ZIO, ZLayer}

class StatusControllerImpl(statusRepository: StatusRepository) extends StatusController {
  override def getLatestStatus(environment: String, serviceName: String): IO[ErrorResponse, SingleApiResponse[RefinedStatus]] =
    statusRepository.getLatestStatus(environment, serviceName)
      .mapBoth(
        {
          case RecordNotFoundDatabaseError(message) => RecordNotFoundErrorResponse(message)
          case otherError => InternalServerErrorResponse(otherError.getMessage)
        },
        SingleApiResponse(_)
      )

  override def getAllStatuses(environment: String, serviceName: String): IO[ErrorResponse, MultiApiResponse[RefinedStatus]] =
    statusRepository.getAllStatuses(environment, serviceName)
      .mapBoth(error => InternalServerErrorResponse(error.getMessage), MultiApiResponse(_))

  override def getLatestStatusOfAllActiveConfigurations(): IO[ErrorResponse, MultiApiResponse[RefinedStatus]] =
    statusRepository.getLatestStatusOfAllActiveConfigurations().mapBoth(error => InternalServerErrorResponse(error.getMessage), data => MultiApiResponse(data.toSeq))
}

object StatusControllerImpl {
  val layer: RLayer[StatusRepository, StatusController] = ZLayer {
    for {
      statusRepository <- ZIO.service[StatusRepository]
    } yield new StatusControllerImpl(statusRepository)
  }
}
