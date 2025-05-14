package za.co.absa.statusboard.providers

import zio.Task

/**
 *  Trait representing an email provider.
 */
trait EmailProvider {

  /**
   *  Sends an email to the specified recipients.
   *
   *  @param to sequence of email addresses to send the email to
   *  @param subject mail title
   *  @param body the body of the email
   */
  def sendEmail(to: Seq[String], subject: String, body: String): Task[Unit]
}
