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
import za.co.absa.statusboard.model.{ErrorResponse, RefinedStatus}
import zio.IO
import zio.macros.accessible

/**
 *  Interface for obtaining status reports
 */
@accessible
trait StatusController {
  /**
   *  Retrieves the latest status of a given service.
   *
   *  @param environment Environment
   *  @param serviceName Service name
   */
  def getLatestStatus(environment: String, serviceName: String): IO[ErrorResponse, SingleApiResponse[RefinedStatus]]

  /**
   *  Retrieves full status history for a given service.
   *
   *  @param environment Environment
   *  @param serviceName Service name
   */
  def getAllStatuses(environment: String, serviceName: String): IO[ErrorResponse, MultiApiResponse[RefinedStatus]]

  /**
   *  Retrieves the latest status of all active services.
   */
  def getLatestStatusOfAllActiveConfigurations(): IO[ErrorResponse, MultiApiResponse[RefinedStatus]]
}
