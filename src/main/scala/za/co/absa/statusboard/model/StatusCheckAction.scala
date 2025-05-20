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
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder}
import sttp.tapir.generic.auto._ // must be imported to support OpenAPI auto-derivation

sealed trait StatusCheckAction {
  val actionType: String
}

object StatusCheckAction {
  @JsonCodec
  case class HttpGetRequestStatusCodeOnly(
    protocol: String,
    host: String,
    port: Int,
    path: String
  ) extends StatusCheckAction {
    val actionType: String = nameOf(HttpGetRequestStatusCodeOnly)
  }

  @JsonCodec
  case class HttpGetRequestWithMessage(
    protocol: String,
    host: String,
    port: Int,
    path: String
  ) extends StatusCheckAction {
    val actionType: String = nameOf(HttpGetRequestWithMessage)
  }

  @JsonCodec
  case class HttpGetRequestWithJsonStatus(
    protocol: String,
    host: String,
    port: Int,
    path: String,
    jsonStatusPath: Seq[String],
    greenRegex: String,
    amberRegex: String
  ) extends StatusCheckAction {
    val actionType: String = nameOf(HttpGetRequestWithJsonStatus)
  }

  @JsonCodec
  case class FixedStatus(status: RawStatus) extends StatusCheckAction {
    val actionType: String = nameOf(FixedStatus)
  }

  case class TemporaryFixedStatus(status: RawStatus, originalCheck: StatusCheckAction) extends StatusCheckAction {
    val actionType: String = nameOf(TemporaryFixedStatus)
  }

  sealed trait CompositionItem {}

  object CompositionItem {
    @JsonCodec
    case class Direct(reference: ServiceConfigurationReference) extends CompositionItem
    @JsonCodec
    case class Partial(items: Seq[CompositionItem], min: Int) extends CompositionItem
  }

  @JsonCodec
  case class Composition(
    greenRequiredForGreen: Seq[CompositionItem], // if dependency not green, this status cannot be green (can be amber or red)
    amberRequiredForGreen: Seq[CompositionItem], // if dependency not even amber, this status cannot be green (can be amber or red)
    greenRequiredForAmber: Seq[CompositionItem], // if dependency not green, this status cannot be even amber (red)
    amberRequiredForAmber: Seq[CompositionItem], // if dependency not even amber, this status cannot be even amber (red)
  ) extends StatusCheckAction {
    val actionType: String = nameOf(FixedStatus)
  }

  @JsonCodec
  case class AwsRds(
    region: String,
    name: String
  ) extends StatusCheckAction {
    val actionType: String = nameOf(AwsRds)
  }

  @JsonCodec
  case class AwsRdsCluster(
    region: String,
    name: String
  ) extends StatusCheckAction {
    val actionType: String = nameOf(AwsRds)
  }

  @JsonCodec
  case class AwsEmr(
    region: String,
    clusterId: String,
    checkNodes: Boolean,
  ) extends StatusCheckAction {
    val actionType: String = nameOf(AwsEmr)
  }

  @JsonCodec
  case class AwsEc2AmiCompliance(
                     region: String,
                     id: String
                   ) extends StatusCheckAction {
    val actionType: String = nameOf(AwsEc2AmiCompliance)
  }

  // Flatten direct reference for backwards compatibility
  implicit val compositionItemEncoder: Encoder[CompositionItem] = Encoder.instance {
    case CompositionItem.Direct(reference) => reference.asJson
    case partial @ CompositionItem.Partial(_, _) => partial.asJson
  }

  // Flatten direct reference for backwards compatibility
  implicit val compositionItemDecoder: Decoder[CompositionItem] = Decoder.instance(
    cursor => {
      Seq(
        cursor.as[ServiceConfigurationReference].map(x => CompositionItem.Direct(x).asInstanceOf[CompositionItem]),
        cursor.as[CompositionItem.Partial]
      ).collectFirst { case Right(alpha) => alpha }
        .toRight(DecodingFailure("CompositionItem", cursor.history))
    }
  )

  implicit val temporaryFixedStatusEncoder: Encoder[TemporaryFixedStatus] = deriveEncoder[TemporaryFixedStatus]

  implicit val temporaryFixedStatusDecoder: Decoder[TemporaryFixedStatus] = deriveDecoder[TemporaryFixedStatus]

  implicit val statusCheckEncoder: Encoder[StatusCheckAction] = Encoder.instance {
    case httpGetRequestStatusCodeOnly @ HttpGetRequestStatusCodeOnly(_, _, _, _) => Map(nameOf(HttpGetRequestStatusCodeOnly) -> httpGetRequestStatusCodeOnly).asJson
    case httpGetRequestWithMessage @ HttpGetRequestWithMessage(_, _, _, _) => Map(nameOf(HttpGetRequestWithMessage) -> httpGetRequestWithMessage).asJson
    case httpGetRequestWithJsonStatus @ HttpGetRequestWithJsonStatus(_, _, _, _, _, _, _) => Map(nameOf(HttpGetRequestWithJsonStatus) -> httpGetRequestWithJsonStatus).asJson
    case fixedStatus @ FixedStatus(_) => Map(nameOf(FixedStatus) -> fixedStatus).asJson
    case temporaryFixedStatus @ TemporaryFixedStatus(_, _) => Map(nameOf(TemporaryFixedStatus) -> temporaryFixedStatus).asJson
    case composition @ Composition(_, _, _, _) => Map(nameOf(Composition) -> composition).asJson
    case awsRds @ AwsRds(_, _) => Map(nameOf(AwsRds) -> awsRds).asJson
    case awsRdsCluster @ AwsRdsCluster(_, _) => Map(nameOf(AwsRdsCluster) -> awsRdsCluster).asJson
    case awsEmr @ AwsEmr(_, _, _) => Map(nameOf(AwsEmr) -> awsEmr).asJson
    case awsEc2AmiCompliance @ AwsEc2AmiCompliance(_, _) => Map(nameOf(AwsEc2AmiCompliance) -> awsEc2AmiCompliance).asJson
  }

  implicit val statusCheckDecoder: Decoder[StatusCheckAction] = Decoder.instance(
    cursor => {
      val determinant = cursor.keys.flatMap(_.headOption).toSeq.head
      determinant match {
        case "HttpGetRequestStatusCodeOnly" => cursor.downField(determinant).as[HttpGetRequestStatusCodeOnly]
        case "HttpGetRequestWithMessage" => cursor.downField(determinant).as[HttpGetRequestWithMessage]
        case "HttpGetRequestWithJsonStatus" => cursor.downField(determinant).as[HttpGetRequestWithJsonStatus]
        case "FixedStatus" => cursor.downField(determinant).as[FixedStatus]
        case "TemporaryFixedStatus" => cursor.downField(determinant).as[TemporaryFixedStatus]
        case "Composition" => cursor.downField(determinant).as[Composition]
        case "AwsRds" => cursor.downField(determinant).as[AwsRds]
        case "AwsRdsCluster" => cursor.downField(determinant).as[AwsRdsCluster]
        case "AwsEmr" => cursor.downField(determinant).as[AwsEmr]
        case "AwsEc2AmiCompliance" => cursor.downField(determinant).as[AwsEc2AmiCompliance]
      }
    }
  )
}
