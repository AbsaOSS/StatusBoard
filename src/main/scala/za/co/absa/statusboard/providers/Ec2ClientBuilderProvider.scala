package za.co.absa.statusboard.providers

import software.amazon.awssdk.services.ec2.{Ec2Client, Ec2ClientBuilder}
import zio._

/**
 *  Provides access to AWS EC2
 */
object Ec2ClientBuilderProvider {
  val layer: ZLayer[Any, Throwable, Ec2ClientBuilder] = ZLayer {
        ZIO.attempt(Ec2Client.builder())
  }
}
