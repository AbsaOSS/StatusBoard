package za.co.absa.statusboard.checker

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.rds.RdsClientBuilder
import software.amazon.awssdk.services.rds.model.DescribeDbClustersRequest
import za.co.absa.statusboard.config.checkers.AwsRdsClusterCheckerConfig
import za.co.absa.statusboard.model.{RawStatus, StatusCheckAction}
import zio._

class AwsRdsClusterCheckerImpl(config: AwsRdsClusterCheckerConfig, rdsClientBuilder: RdsClientBuilder) extends AwsRdsClusterChecker {
  override def checkRawStatus(action: StatusCheckAction.AwsRdsCluster): UIO[RawStatus] = {
    for {
      rdsClient <- ZIO.attempt(rdsClientBuilder.region(Region.of(action.region)).build())
      request <- ZIO.attempt(DescribeDbClustersRequest.builder().dbClusterIdentifier(action.name).build())
      response <- ZIO.attempt(rdsClient.describeDBClusters(request))
      _ <- ZIO.unless(response.dbClusters().size() == 1)(
        ZIO.fail(new Exception(s"Response contains ${response.dbClusters().size()} items"))
      )
      awsStatus <- ZIO.attempt(response.dbClusters().getFirst.status())
    } yield if (config.red.contains(awsStatus)) RawStatus.Red(awsStatus, intermittent = false)
    else if (config.amber.contains(awsStatus)) RawStatus.Amber(awsStatus, intermittent = false)
    else if (config.green.contains(awsStatus)) RawStatus.Green(awsStatus)
    else throw new Exception(s"unknown status: $awsStatus")
  }.catchAll { error =>
    ZIO.logError(s"An error occurred while performing AWS RDS cluster status check for ${action.region} ${action.name} Details: ${error.toString}")
      .as(RawStatus.Red(s"FAIL: ${error.getMessage}", intermittent = true))
  }
}

object AwsRdsClusterCheckerImpl {
  val layer: ZLayer[RdsClientBuilder, Throwable, AwsRdsClusterChecker] = ZLayer {
    for {
      config <- ZIO.config[AwsRdsClusterCheckerConfig](AwsRdsClusterCheckerConfig.config)
      rdsClientBuilder <- ZIO.service[RdsClientBuilder]
    } yield new AwsRdsClusterCheckerImpl(config, rdsClientBuilder)
  }
}
