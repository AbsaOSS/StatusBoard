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
