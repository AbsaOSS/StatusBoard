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
import org.http4s.Method
import za.co.absa.statusboard.config.providers.MSTeamsProviderConfig
import zio.{Task, ZIO, ZLayer}

import java.io.IOException

class MsTeamsProviderImpl(httpClientProvider: HttpClientProvider, webhookUrl: String) extends MsTeamsProvider {
  override def sendMessage(message: String): Task[Unit] = {
    for {
      response <- httpClientProvider.retrieveMessage(
        method = Method.POST,
        uriString = webhookUrl,
        maybeRequestPayload = Some(message))
      _ <- ZIO.unless(response.status.isSuccess)(ZIO.fail(new IOException(s"${response.status.code}: $message")))
    } yield ()
  }
}

object MsTeamsProviderImpl {
  val layer: ZLayer[Any with HttpClientProvider, Throwable, MsTeamsProvider] = ZLayer {
    for {
      config <- ZIO.config[MSTeamsProviderConfig](MSTeamsProviderConfig.config)
      httpClientProvider <- ZIO.service[HttpClientProvider]
    } yield new MsTeamsProviderImpl(httpClientProvider, config.webhookUrl)
  }
}
