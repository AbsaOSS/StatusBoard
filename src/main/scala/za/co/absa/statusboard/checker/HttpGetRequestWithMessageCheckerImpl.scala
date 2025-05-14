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
