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
import zio.IO
import zio.macros.accessible

/**
 *  Interface for managing monitoring
 */
@accessible
trait MonitoringController {
  /**
   *  (Re)Starts monitoring for all service configurations
   */
  def restart: IO[ErrorResponse, Unit]

  /**
   *  Restarts monitoring for single service configuration
   *
   *  This method can be used to
   *    start a monitoring task that didn't exist before
   *    update an existing one with changed configuration (including configuration of no monitoring)
   *    restart monitoring task that misbehaves / crashed
   *
   *  @param environment Environment for which the monitoring task is to be restarted.
   *  @param serviceName name of the service for which the monitoring task is to be restarted.
   */
  def restartForService(environment: String, serviceName: String): IO[ErrorResponse, Unit]
}
