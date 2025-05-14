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

import org.http4s.Method
import za.co.absa.statusboard.model.{RawStatus, StatusCheckAction}
import za.co.absa.statusboard.providers.HttpClientProvider
import zio._

class HttpGetRequestWithMessageCheckerImpl(httpClientProvider: HttpClientProvider) extends HttpGetRequestWithMessageChecker {
  override def checkRawStatus(action: StatusCheckAction.HttpGetRequestWithMessage): UIO[RawStatus] = {
    for {
      response <- httpClientProvider.retrieveMessage(
        method = Method.GET,
        uriString = s"${action.protocol}://${action.host}:${action.port}/${action.path}")
      status <- ZIO.succeed {
        if (response.status.isSuccess)
          RawStatus.Green(s"${response.status.code}: ${response.message}")
        else {
          RawStatus.Red(s"${response.status.code}: ${response.message}", intermittent = true)
        }
      }
    } yield status
  }.catchAll {error =>
    ZIO.logError(s"An error occurred while performing HTTP GET status check for ${action.protocol}://${action.host}:${action.port}/${action.path}: ${error.getMessage} Details: ${error.toString}")
      .as(RawStatus.Red(s"FAIL: ${error.getMessage}", intermittent = true))
  }
}

object HttpGetRequestWithMessageCheckerImpl {
  val layer: ZLayer[Any with HttpClientProvider, Throwable, HttpGetRequestWithMessageChecker] = ZLayer {
    ZIO.serviceWith[HttpClientProvider](httpClientProvider => new HttpGetRequestWithMessageCheckerImpl(httpClientProvider))
  }
}
