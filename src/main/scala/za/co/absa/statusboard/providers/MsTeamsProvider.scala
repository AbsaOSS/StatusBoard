package za.co.absa.statusboard.providers

import zio.Task

/**
 *  Trait representing an MS Teams provider.
 */
trait MsTeamsProvider {

  /**
   *  Sends an email to the specified recipients.
   *
   *  @param message A message to be sent
   *  @return A [[zio.IO]] producing a [[Unit]] representing the asynchronous operation
   *         or a [[Throwable]] if an error occurs.
   */
  def sendMessage(message: String): Task[Unit]
}
