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

package za.co.absa.statusboard.checker

import io.circe.{ACursor, jawn}
import org.http4s.Method
import za.co.absa.statusboard.model.{RawStatus, StatusCheckAction}
import za.co.absa.statusboard.providers.HttpClientProvider
import za.co.absa.statusboard.utils.RegexMatcher
import zio._

class HttpGetRequestWithJsonStatusCheckerImpl(httpClientProvider: HttpClientProvider) extends HttpGetRequestWithJsonStatusChecker {
  override def checkRawStatus(action: StatusCheckAction.HttpGetRequestWithJsonStatus): UIO[RawStatus] = {
    for {
      response <- httpClientProvider.retrieveMessage(
        method = Method.GET,
        uriString = s"${action.protocol}://${action.host}:${action.port}/${action.path}")
      maybeJson <- ZIO.succeed(jawn.parse(response.message).toOption)
      maybeStatusMessage <- maybeJson match {
        case None => ZIO.none
        case Some(json) => ZIO.attempt {
          action.jsonStatusPath.foldLeft(json.hcursor: ACursor) {
            (cursor, key) => cursor.downField(key)
          }.as[String].toOption
        }
      }
      status <- ZIO attempt {
        val greenMatcher = RegexMatcher(action.greenRegex)
        val amberMatcher = RegexMatcher(action.amberRegex)
        (response.message, maybeStatusMessage) match {
          // Status defined by JSON (completely ignore HTTPStatusCode #175)
          case (_, Some(jsonStatus@greenMatcher())) => RawStatus.Green(s"${response.status.code}: $jsonStatus")
          case (_, Some(jsonStatus@amberMatcher())) => RawStatus.Amber(s"${response.status.code}: $jsonStatus", intermittent = false)
          case (_, Some(jsonStatus)) => RawStatus.Red(s"${response.status.code}: $jsonStatus", intermittent = false)
          // JSON failed => RED, at least extract message
          case (rawMessage, None) => RawStatus.Red(s"${response.status.code}: $rawMessage", intermittent = true) // connection errors mostly go here
        }
      }
    } yield status
  }.catchAll { error =>
    ZIO.logError(s"An error occurred while performing HTTP GET status check for ${action.protocol}://${action.host}:${action.port}/${action.path}: ${error.getMessage} Details: ${error.toString}")
      .as(RawStatus.Red(s"FAIL: ${error.getMessage}", intermittent = true))
  }
}

object HttpGetRequestWithJsonStatusCheckerImpl {
  val layer: ZLayer[Any with HttpClientProvider, Throwable, HttpGetRequestWithJsonStatusChecker] = ZLayer {
    ZIO.serviceWith[HttpClientProvider](httpClientProvider => new HttpGetRequestWithJsonStatusCheckerImpl(httpClientProvider))
  }
}
