package za.co.absa.statusboard.providers

import software.amazon.awssdk.services.emr.{EmrClient, EmrClientBuilder}
import zio._

/**
 *  Provides access to AWS EMR
 */
object EmrClientBuilderProvider {
  val layer: ZLayer[Any, Throwable, EmrClientBuilder] = ZLayer {
        ZIO.attempt(EmrClient.builder())
  }
}
