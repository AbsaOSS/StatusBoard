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
import io.circe.generic.JsonCodec
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}
import sttp.tapir.Schema
import sttp.tapir.generic.auto._ // must be imported to support OpenAPI auto-derivation

sealed trait NotificationAction {
  val actionType: String
}

object NotificationAction {
  @JsonCodec
  case class EMail(
    addresses: Seq[String],
    subject: Option[String] = None,
    body: Option[Seq[String]] = None,
    colorLabels: Option[ColorLabels] = None
  ) extends NotificationAction {
    val actionType: String = nameOf(EMail)
  }

  @JsonCodec
  case class ColorLabels(red: String, amber: String, green: String, black: String)

  @JsonCodec
  case class Repository() extends NotificationAction {
    val actionType: String = nameOf(Repository)
  }

  @JsonCodec
  case class MSTeams() extends NotificationAction {
    val actionType: String = nameOf(MSTeams)
  }

  // null values are not dropped by default, we need to to keep it backwards compatible where possible
  implicit val emailEncoder: Encoder[EMail] = Encoder.instance { email =>
    Json.obj(
      "addresses" -> email.addresses.asJson,
      "subject" -> email.subject.asJson,
      "body" -> email.body.asJson,
      "colorLabels" -> email.colorLabels.asJson
    ).dropNullValues
  }

  implicit val notificationActionEncoder: Encoder[NotificationAction] = Encoder.instance {
    case email@EMail(_, _, _, _) => Map(nameOf(EMail) -> email).asJson
    case repository@Repository() => Map(nameOf(Repository) -> repository).asJson
    case msTeams@MSTeams() => Map(nameOf(MSTeams) -> msTeams).asJson
  }

  implicit val notificationActionDecoder: Decoder[NotificationAction] = Decoder.instance(
    cursor => {
      val determinant = cursor.keys.flatMap(_.headOption).toSeq.head
      determinant match {
        case "EMail" => cursor.downField(determinant).as[EMail]
        case "Repository" => cursor.downField(determinant).as[Repository]
        case "MSTeams" => cursor.downField(determinant).as[MSTeams]
      }
    }
  )

  implicit val schemaForNotificationAction: Schema[NotificationAction] = Schema.oneOfWrapped[NotificationAction]
}
