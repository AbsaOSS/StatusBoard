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

package za.co.absa.statusboard.api.http

import io.circe.generic.auto._
import org.slf4j.LoggerFactory
import sttp.model.StatusCode
import sttp.monad.MonadError
import sttp.tapir.DecodeResult
import sttp.tapir.generic.auto.schemaForCaseClass
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.interceptor.DecodeFailureContext
import sttp.tapir.server.interceptor.decodefailure.DecodeFailureHandler
import sttp.tapir.server.interceptor.decodefailure.DefaultDecodeFailureHandler.respond
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.ztapir.{headers, statusCode}
import za.co.absa.statusboard.model.ErrorResponse.BadRequestResponse
import zio.Task

object DecodeFailureHandlerImpl extends DecodeFailureHandler[Task] {
  private val logger = LoggerFactory.getLogger(getClass)

  override def apply(ctx: DecodeFailureContext)(implicit monad: MonadError[Task]): Task[Option[ValuedEndpointOutput[_]]] = {
    monad.unit(
      respond(ctx).map { case (sc, hs) =>
        val message = if (sc == StatusCode.Unauthorized) "Unauthorized" else ctx.failure match {
          case DecodeResult.Missing => s"Decoding error - missing value."
          case DecodeResult.Multiple(vs) => s"Decoding error - $vs."
          case DecodeResult.Error(original, message) => s"Decoding error '$message' for an input value '$original'."
          case DecodeResult.Mismatch(_, actual) => s"Unexpected value '$actual'."
          case DecodeResult.InvalidValue(errors) => s"Validation error - $errors."
        }

        logger.error(s"Request decoding failed: $message")

        val errorResponse = BadRequestResponse(message)
        ValuedEndpointOutput(statusCode.and(headers).and(jsonBody[BadRequestResponse]), (sc, hs, errorResponse))
      }
    )
  }
}
