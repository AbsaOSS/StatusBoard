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

package za.co.absa.statusboard.repository

import za.co.absa.statusboard.model.AppError.DatabaseError
import za.co.absa.statusboard.model.RefinedStatus
import zio._
import zio.macros.accessible

/**
 *  Interface for a repository with status notifications
 */
@accessible
trait NotificationRepository {
  /**
   *  Persists a status notification
   *
   *  @param status The status notification to be persisted
   *  @return A [[zio.IO]] that will either produce Unit if the status notification is successfully persisted,
   *         or a [[za.co.absa.statusboard.model.AppError.DatabaseError]] if an error occurs.
   */
  def persistNotification(status: RefinedStatus): IO[DatabaseError, Unit]

  /**
   *  Renames service in existing records
   *
   *  @param environmentOld Old environment
   *  @param serviceNameOld Old service name
   *  @param environmentNew New environment
   *  @param serviceNameNew New service name
   *  @return A [[zio.IO]] that will either produce Unit if the rename operation is successfull persisted,
   *         or a [[za.co.absa.statusboard.model.AppError.DatabaseError]] if an error occurs.
   */
  def renameService(environmentOld: String, serviceNameOld: String, environmentNew: String, serviceNameNew: String): IO[DatabaseError, Unit]
}
