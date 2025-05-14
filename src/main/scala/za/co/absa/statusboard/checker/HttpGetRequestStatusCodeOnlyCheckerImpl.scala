package za.co.absa.statusboard.checker

import org.http4s.Method
import za.co.absa.statusboard.model.{RawStatus, StatusCheckAction}
import za.co.absa.statusboard.providers.HttpClientProvider
import zio._

class HttpGetRequestStatusCodeOnlyCheckerImpl(httpClientProvider: HttpClientProvider) extends HttpGetRequestStatusCodeOnlyChecker {
  override def checkRawStatus(action: StatusCheckAction.HttpGetRequestStatusCodeOnly): UIO[RawStatus] = {
    for {
      response <- httpClientProvider.retrieveStatus(
        method = Method.GET,
        uriString = s"${action.protocol}://${action.host}:${action.port}/${action.path}")
      status <- ZIO.succeed {
        if (response.status.isSuccess)
          RawStatus.Green(s"${response.status.code}: ${response.status.reason}")
        else {
          RawStatus.Red(s"${response.status.code}: ${response.status.reason}", intermittent = true)
        }
      }
    } yield status
  }.catchAll { error =>
    ZIO.logError(s"An error occurred while performing HTTP GET status check for ${action.protocol}://${action.host}:${action.port}/${action.path}: ${error.getMessage} Details: ${error.toString}")
      .as(RawStatus.Red(s"FAIL: ${error.getMessage}", intermittent = true))
  }
}

object HttpGetRequestStatusCodeOnlyCheckerImpl {
  val layer: ZLayer[Any with HttpClientProvider, Throwable, HttpGetRequestStatusCodeOnlyChecker] = ZLayer {
    ZIO.serviceWith[HttpClientProvider](httpClientProvider => new HttpGetRequestStatusCodeOnlyCheckerImpl(httpClientProvider))
  }
}
