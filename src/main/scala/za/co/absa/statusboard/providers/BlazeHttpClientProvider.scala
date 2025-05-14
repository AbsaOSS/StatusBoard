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
