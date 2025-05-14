package za.co.absa.statusboard.model

import com.github.dwickern.macros.NameOf.nameOf
import io.circe.{Decoder, Encoder, Json}
import sttp.tapir.Schema.annotations.format

sealed trait RawStatus {
  val color: String
  val statusMessage: String
  val intermittent: Boolean
}

object RawStatus {
  @format("RED({statusMessage})")
  case class Red(statusMessage: String, intermittent: Boolean) extends RawStatus {
    val color = nameOf(Red)
    override def toString: String = if (intermittent) s"RED[$statusMessage]" else s"RED($statusMessage)"
  }

  @format("AMBER({statusMessage})")
  case class Amber(statusMessage: String, intermittent: Boolean) extends RawStatus {
    val color = nameOf(Amber)
    override def toString: String = if (intermittent) s"AMBER[$statusMessage]" else s"AMBER($statusMessage)"
  }

  @format("GREEN({statusMessage})")
  case class Green(statusMessage: String) extends RawStatus {
    val color = nameOf(Green)
    val intermittent = false
    override def toString: String = s"GREEN($statusMessage)"
  }

  @format("BLACK(Not Monitored)")
  case class Black() extends RawStatus {
    val color = nameOf(Black)
    val statusMessage = "Not Monitored"
    val intermittent = false
    override def toString: String = "BLACK(Not Monitored)"
  }

  implicit val rawStatusEncoder: Encoder[RawStatus] = new Encoder[RawStatus] {
    final def apply(status: RawStatus): Json = Json.fromString(status.toString)
  }

  implicit val rawStatusDecoder: Decoder[RawStatus] = Decoder.decodeString.emap {
    case s"RED($statusMessage)" => Right(Red(statusMessage, false))
    case s"RED[$statusMessage]" => Right(Red(statusMessage, true))
    case s"AMBER($statusMessage)" => Right(Amber(statusMessage, false))
    case s"AMBER[$statusMessage]" => Right(Amber(statusMessage, true))
    case s"GREEN($statusMessage)" => Right(Green(statusMessage))
    case s"BLACK(Not Monitored)" => Right(Black())
  }
}
