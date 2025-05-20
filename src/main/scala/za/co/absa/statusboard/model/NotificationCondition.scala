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

package za.co.absa.statusboard.model

import com.github.dwickern.macros.NameOf._
import io.circe.{Decoder, Encoder}
import io.circe.generic.JsonCodec
import io.circe.syntax.EncoderOps
import sttp.tapir.Schema
import sttp.tapir.generic.auto._ // must be imported to support OpenAPI auto-derivation

sealed trait NotificationCondition {
  val conditionType: String
}

object NotificationCondition {
  @JsonCodec
  case class DurationBased(secondsInState: Int) extends NotificationCondition {
    val conditionType: String = nameOf(DurationBased)
  }

  implicit val notificationConditionEncoder: Encoder[NotificationCondition] = Encoder.instance {
    case durationBased@DurationBased(_) => Map(nameOf(DurationBased) -> durationBased).asJson
  }

  implicit val notificationConditionDecoder: Decoder[NotificationCondition] = Decoder.instance(
    cursor => {
      val determinant = cursor.keys.flatMap(_.headOption).toSeq.head
      determinant match {
        case "DurationBased" => cursor.downField(determinant).as[DurationBased]
      }
    }
  )

  implicit val schemaForNotificationCondition: Schema[NotificationCondition] = Schema.oneOfWrapped[NotificationCondition]
}
