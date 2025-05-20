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

import org.http4s.{Header, Method, Status}
import za.co.absa.statusboard.providers.Response.{ResponseStatusOnly, ResponseStatusWithMessage}
import zio.Task
import zio.macros.accessible

trait Response

object Response {
  case class ResponseStatusOnly(status: Status) extends Response
  case class ResponseStatusWithMessage(status: Status, message: String) extends Response
}

@accessible
trait HttpClientProvider {
  def performRequest(method: Method, uriString: String, includeResponsePayload: Boolean, maybeRequestPayload: Option[String] = None, maybeRequestHeaders: Option[List[Header.Raw]] = None) : Task[Response]

  def retrieveStatus(method: Method, uriString: String, maybeRequestPayload: Option[String] = None, maybeRequestHeaders: Option[List[Header.Raw]] = None): Task[ResponseStatusOnly] =
    performRequest(method, uriString, includeResponsePayload = false, maybeRequestPayload, maybeRequestHeaders).map(_.asInstanceOf[ResponseStatusOnly])

  def retrieveMessage(method: Method, uriString: String, maybeRequestPayload: Option[String] = None, maybeRequestHeaders: Option[List[Header.Raw]] = None): Task[ResponseStatusWithMessage] =
    performRequest(method, uriString, includeResponsePayload = true, maybeRequestPayload, maybeRequestHeaders).map(_.asInstanceOf[ResponseStatusWithMessage])
}
