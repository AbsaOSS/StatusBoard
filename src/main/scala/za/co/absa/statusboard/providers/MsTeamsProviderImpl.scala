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
