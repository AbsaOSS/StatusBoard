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
