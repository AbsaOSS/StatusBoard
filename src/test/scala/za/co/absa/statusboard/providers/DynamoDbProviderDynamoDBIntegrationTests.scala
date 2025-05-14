package za.co.absa.statusboard.providers

import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model._
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio._
import zio.test.Assertion.isUnit
import zio.test._

object DynamoDbProviderDynamoDBIntegrationTests extends ConfigProviderSpec {
  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("DynamoDbProviderSuite")(
      test("listing tables should not crash") {
        assertZIO(
          for {
            request <- ZIO.attempt {
              ListTablesRequest.builder().build()
            }
            _ <- ZIO.serviceWith[DynamoDbClient](_.listTables(request))
          } yield ()
        )(isUnit)
      }
    )
  }.provide(DynamoDbProvider.layer)
}
