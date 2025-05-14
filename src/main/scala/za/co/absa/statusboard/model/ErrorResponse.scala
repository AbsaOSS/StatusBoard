package za.co.absa.statusboard.model

import io.circe.generic.JsonCodec

sealed trait ErrorResponse {
  def message: String
}

object ErrorResponse {
  @JsonCodec
  final case class BadRequestResponse(message: String) extends ErrorResponse
  @JsonCodec
  final case class UnauthorizedErrorResponse(message: String) extends ErrorResponse
  @JsonCodec
  final case class RecordNotFoundErrorResponse(message: String) extends ErrorResponse
  @JsonCodec
  final case class DataConflictErrorResponse(message: String) extends ErrorResponse
  @JsonCodec
  final case class InternalServerErrorResponse(message: String) extends ErrorResponse
}
