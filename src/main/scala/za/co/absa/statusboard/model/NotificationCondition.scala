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
