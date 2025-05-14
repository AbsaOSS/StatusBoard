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

package za.co.absa.statusboard.checker

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.rds.RdsClientBuilder
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest
import za.co.absa.statusboard.config.checkers.AwsRdsCheckerConfig
import za.co.absa.statusboard.model.{RawStatus, StatusCheckAction}
import zio._

class AwsRdsCheckerImpl(config: AwsRdsCheckerConfig, rdsClientBuilder: RdsClientBuilder) extends AwsRdsChecker {
  override def checkRawStatus(action: StatusCheckAction.AwsRds): UIO[RawStatus] = {
    for {
      rdsClient <- ZIO.attempt(rdsClientBuilder.region(Region.of(action.region)).build())
      request <- ZIO.attempt(DescribeDbInstancesRequest.builder().dbInstanceIdentifier(action.name).build())
      response <- ZIO.attempt(rdsClient.describeDBInstances(request))
      _ <- ZIO.unless(response.dbInstances().size() == 1)(
        ZIO.fail(new Exception(s"Response contains ${response.dbInstances().size()} items"))
      )
      awsStatus <- ZIO.attempt(response.dbInstances().getFirst.dbInstanceStatus())
    } yield if (config.red.contains(awsStatus)) RawStatus.Red(awsStatus, intermittent = false)
    else if (config.amber.contains(awsStatus)) RawStatus.Amber(awsStatus, intermittent = false)
    else if (config.green.contains(awsStatus)) RawStatus.Green(awsStatus)
    else throw new Exception(s"unknown status: $awsStatus")
  }.catchAll { error =>
    ZIO.logError(s"An error occurred while performing AWS RDS status check for ${action.region} ${action.name} Details: ${error.toString}")
      .as(RawStatus.Red(s"FAIL: ${error.getMessage}", intermittent = true))
  }
}

object AwsRdsCheckerImpl {
  val layer: ZLayer[RdsClientBuilder, Throwable, AwsRdsChecker] = ZLayer {
    for {
      config <- ZIO.config[AwsRdsCheckerConfig](AwsRdsCheckerConfig.config)
      rdsClientBuilder <- ZIO.service[RdsClientBuilder]
    } yield new AwsRdsCheckerImpl(config, rdsClientBuilder)
  }
}
