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

import jakarta.mail.{Message, Session, Transport}
import jakarta.mail.internet.{InternetAddress, MimeMessage}
import za.co.absa.statusboard.config.providers.EMailProviderConfig
import zio.{Task, ULayer, ZIO, ZLayer}

import java.util.Properties

class EmailProviderImpl extends EmailProvider {
  override def sendEmail(to: Seq[String], subject: String, body: String): Task[Unit] = {
    for {
      config <- ZIO.config[EMailProviderConfig](EMailProviderConfig.config)
      _ <- ZIO.attemptBlocking {
        val properties = new Properties()
        properties.put("mail.smtp.host", config.smtpHost)

        val session = Session.getDefaultInstance(properties)
        val message = new MimeMessage(session)

        message.setFrom(new InternetAddress(config.senderAddress))
        to.foreach(email => message.addRecipient(Message.RecipientType.TO, new InternetAddress(email)))
        message.setSubject(subject)
        message.setText(body)

        Transport.send(message)
      }
    } yield ()
  }.tapError { error =>
    ZIO.logError(s"An error occurred while sending email: ${error.getMessage}")
  }
}

object EmailProviderImpl {
  val layer: ULayer[EmailProvider] = ZLayer.succeed(new EmailProviderImpl)
}
