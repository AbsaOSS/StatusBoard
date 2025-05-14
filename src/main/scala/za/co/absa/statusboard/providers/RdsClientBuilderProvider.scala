package za.co.absa.statusboard.providers

import software.amazon.awssdk.services.rds.{RdsClient, RdsClientBuilder}
import zio._

/**
 *  Provides access to AWS RDS
 */
object RdsClientBuilderProvider {
  val layer: ZLayer[Any, Throwable, RdsClientBuilder] = ZLayer {
        ZIO.attempt(RdsClient.builder())
  }
}
