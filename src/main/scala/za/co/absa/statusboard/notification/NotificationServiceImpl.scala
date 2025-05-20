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
import za.co.absa.statusboard.notification.actioners.NotificationActioner
import za.co.absa.statusboard.notification.deciders.NotificationDecider
import zio.{UIO, ZIO, ZLayer}

class NotificationServiceImpl(notificationDecider: NotificationDecider, notificationActioner: NotificationActioner) extends NotificationService {
  override def notifyIfApplicable(condition: NotificationCondition, actions: Seq[NotificationAction], status: RefinedStatus): UIO[RefinedStatus] = {
    for {
      shouldNotify <- notificationDecider.shouldNotify(condition, status)
      afterPotentialNotify <- if (!shouldNotify) ZIO.succeed(status) else for {
        notificationSuccesses <- ZIO.foreach(actions)(action =>
          notificationActioner.notify(action, status).foldZIO(
            err => ZIO.logError(s"Failed to send notification [$action] for $status with ${err.getMessage}").as(false),
            _ => ZIO.succeed(true)))
      } yield if (notificationSuccesses.forall(item => item)) status.copy(notificationSent = true) else status
    } yield afterPotentialNotify
  }
}

object NotificationServiceImpl {
  val layer: ZLayer[NotificationDecider with NotificationActioner, Throwable, NotificationService] = ZLayer {
    for {
      notificationDecider <- ZIO.service[NotificationDecider]
      notificationActioner <- ZIO.service[NotificationActioner]
    } yield new NotificationServiceImpl(notificationDecider, notificationActioner)
  }
}
