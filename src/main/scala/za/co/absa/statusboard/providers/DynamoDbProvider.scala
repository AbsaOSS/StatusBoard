package za.co.absa.statusboard.providers

import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import za.co.absa.statusboard.config.providers.DynamoDBProviderConfig
import zio._

import java.net.URI

/**
 *  Provides access to AWS DynamoDB
 */
object DynamoDbProvider {
  val layer: ZLayer[Any, Throwable, DynamoDbClient] = ZLayer {
    for {
      config <- ZIO.config[DynamoDBProviderConfig](DynamoDBProviderConfig.config)
      baseBuilder <- ZIO.attempt {
        DynamoDbClient.builder()
      }
      targetedBuilder <- config.endpointOverride match {
        case Some(endpointOverride: String) => ZIO.attempt(baseBuilder.endpointOverride(URI.create(endpointOverride)))
        case None => ZIO.succeed(baseBuilder)
      }
    } yield targetedBuilder.build()
  }
}
