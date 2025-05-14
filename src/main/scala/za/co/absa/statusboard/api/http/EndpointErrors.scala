package za.co.absa.statusboard.api.http

import sttp.model.StatusCode
import sttp.tapir.EndpointOutput
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.ztapir.oneOfVariantFromMatchType
import sttp.tapir.generic.auto.schemaForCaseClass
import za.co.absa.statusboard.model.ErrorResponse.{BadRequestResponse, DataConflictErrorResponse, InternalServerErrorResponse, RecordNotFoundErrorResponse, UnauthorizedErrorResponse}

object EndpointErrors {
  val badRequest: EndpointOutput.OneOfVariant[BadRequestResponse] = {
    oneOfVariantFromMatchType(
      StatusCode.BadRequest,
      jsonBody[BadRequestResponse]
    )
  }

  val unauthorized: EndpointOutput.OneOfVariant[UnauthorizedErrorResponse] = {
    oneOfVariantFromMatchType(
      StatusCode.Unauthorized,
      jsonBody[UnauthorizedErrorResponse]
    )
  }

  val recordNotFound: EndpointOutput.OneOfVariant[RecordNotFoundErrorResponse] = {
    oneOfVariantFromMatchType(
      StatusCode.NotFound,
      jsonBody[RecordNotFoundErrorResponse]
    )
  }

  val dataConflict: EndpointOutput.OneOfVariant[DataConflictErrorResponse] = {
    oneOfVariantFromMatchType(
      StatusCode.Conflict,
      jsonBody[DataConflictErrorResponse]
    )
  }

  val internalServerError: EndpointOutput.OneOfVariant[InternalServerErrorResponse] = {
    oneOfVariantFromMatchType(
      StatusCode.InternalServerError,
      jsonBody[InternalServerErrorResponse]
    )
  }
}
