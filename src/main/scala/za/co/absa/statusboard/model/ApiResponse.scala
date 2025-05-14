package za.co.absa.statusboard.model

import io.circe.generic.JsonCodec

sealed trait ApiResponse

object ApiResponse {
  @JsonCodec
  case class SingleApiResponse[T](record: T) extends ApiResponse
  @JsonCodec
  case class MultiApiResponse[T](records: Seq[T]) extends ApiResponse
}
