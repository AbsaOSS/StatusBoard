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

package za.co.absa.statusboard.providers

import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.http.HeaderNames.ContentType
import org.http4s.client.Client
import org.http4s.{Header, Method, Request, Uri}
import org.typelevel.ci.CIString
import za.co.absa.statusboard.providers.Response.{ResponseStatusOnly, ResponseStatusWithMessage}
import zio.Runtime.defaultBlockingExecutor
import zio._
import zio.interop.catz._

class BlazeHttpClientProvider(httpClient: Client[Task]) extends HttpClientProvider {
  override def performRequest(method: Method, uriString: String, includeResponsePayload: Boolean, maybeRequestPayload: Option[String] = None, maybeRequestHeaders: Option[List[Header.Raw]] = None) : Task[Response] = for {
    requestURI <- ZIO.fromEither(Uri.fromString(uriString))
      .tapError(error => ZIO.fail(new Exception(s"Error creating URI from $uriString with ${error.getMessage}")))
    requestPhase0 <- ZIO.succeed(Request[Task](
      method = method,
      uri = requestURI))
    requestPhase1 <- maybeRequestPayload match {
      case Some(payload) => ZIO.attempt {
        requestPhase0
          .withHeaders(Header.Raw(CIString(ContentType), "application/json"))
          .withEntity(payload)
      }
      case None => ZIO.succeed(requestPhase0)
    }
    requestPhase2 <- maybeRequestHeaders match {
      case Some(headers) => ZIO.attempt(requestPhase1.putHeaders(headers))
      case None => ZIO.succeed(requestPhase1)
    }
    response <- httpClient.run(requestPhase2).use { rawResponse =>
      for {
        status <- ZIO.succeed(rawResponse.status)
        body <- if (includeResponsePayload) rawResponse.bodyText.compile.toList.map(_.mkString) else ZIO.succeed("")
      } yield if (includeResponsePayload) ResponseStatusWithMessage(status, body) else ResponseStatusOnly(status)
    }
  } yield response
}

object BlazeHttpClientProvider {
  val layer: ZLayer[Any with Scope, Throwable, HttpClientProvider] = ZLayer {
    BlazeClientBuilder[Task]
      .withExecutionContext(defaultBlockingExecutor.asExecutionContext)
      .resource
      .map(client => new BlazeHttpClientProvider(client))
      .toScopedZIO
  }
}
