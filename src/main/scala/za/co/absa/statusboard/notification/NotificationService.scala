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

package za.co.absa.statusboard.notification

import za.co.absa.statusboard.model.{NotificationAction, NotificationCondition, RefinedStatus}
import zio._
import zio.macros.accessible

/**
 *  Interface for managing notifications.
 */
@accessible
trait NotificationService {
  /**
   *  Performs notification action when condition is met
   *
   *  @param condition Condition for when notification needs to happen
   *  @param actions Notification actions to happen
   *  @return A [[RefinedStatus]]
   *          when notification happens and is success, amended status (notified=true)
   *          when no notification is required, original status
   *          when partial failure happens
   *            proceeds with as much notifications as it can
   *            logs relevant errors
   *            returns original status (notified=false)
   */
  def notifyIfApplicable(condition: NotificationCondition, actions: Seq[NotificationAction], status: RefinedStatus): UIO[RefinedStatus]
}
