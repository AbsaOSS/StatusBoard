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

import za.co.absa.statusboard.model.{RawStatus, StatusCheckAction}
import zio.{UIO, ZIO, ZLayer}

class PolymorphicChecker(
    httpGetRequestStatusCodeOnlyChecker: HttpGetRequestStatusCodeOnlyChecker,
    httpGetRequestWithMessageChecker: HttpGetRequestWithMessageChecker,
    httpGetRequestWithJsonStatusChecker: HttpGetRequestWithJsonStatusChecker,
    fixedStatusChecker: FixedStatusChecker,
    temporaryFixedStatusChecker: TemporaryFixedStatusChecker,
    compositionChecker: CompositionChecker,
    awsRdsChecker: AwsRdsChecker,
    awsRdsClusterChecker: AwsRdsClusterChecker,
    awsEmrChecker: AwsEmrChecker,
    awsEc2AmiComplianceChecker: AwsEc2AmiComplianceChecker
  ) extends Checker {
  override def checkRawStatus(action: StatusCheckAction): UIO[RawStatus] = {
    action match {
      case getRequest: StatusCheckAction.HttpGetRequestStatusCodeOnly  => httpGetRequestStatusCodeOnlyChecker.checkRawStatus(getRequest)
      case getRequest: StatusCheckAction.HttpGetRequestWithMessage  => httpGetRequestWithMessageChecker.checkRawStatus(getRequest)
      case getRequest: StatusCheckAction.HttpGetRequestWithJsonStatus  => httpGetRequestWithJsonStatusChecker.checkRawStatus(getRequest)
      case fixed: StatusCheckAction.FixedStatus => fixedStatusChecker.checkRawStatus(fixed)
      case temporaryFixed: StatusCheckAction.TemporaryFixedStatus => temporaryFixedStatusChecker.checkRawStatus(temporaryFixed)
      case composition: StatusCheckAction.Composition => compositionChecker.checkRawStatus(composition)
      case awsRds: StatusCheckAction.AwsRds => awsRdsChecker.checkRawStatus(awsRds)
      case awsRdsCluster: StatusCheckAction.AwsRdsCluster => awsRdsClusterChecker.checkRawStatus(awsRdsCluster)
      case awsEmr: StatusCheckAction.AwsEmr => awsEmrChecker.checkRawStatus(awsEmr)
      case awsEc2AmiCompliance: StatusCheckAction.AwsEc2AmiCompliance => awsEc2AmiComplianceChecker.checkRawStatus(awsEc2AmiCompliance)
      case _ => ZIO.die(new Exception(s"FATAL: Status check action ${action.actionType} not supported"))
    }
  }
}

object PolymorphicChecker {
  val layer: ZLayer[
    HttpGetRequestStatusCodeOnlyChecker with
    HttpGetRequestWithMessageChecker with
    HttpGetRequestWithJsonStatusChecker with
    FixedStatusChecker with
    TemporaryFixedStatusChecker with
    CompositionChecker with
    AwsRdsChecker with
    AwsRdsClusterChecker with
    AwsEmrChecker with
    AwsEc2AmiComplianceChecker,
    Throwable,
    Checker
  ] = ZLayer {
    for {
      httpGetRequestStatusCodeOnlyChecker <- ZIO.service[HttpGetRequestStatusCodeOnlyChecker]
      httpGetRequestWithMessageChecker <- ZIO.service[HttpGetRequestWithMessageChecker]
      httpGetRequestWithJsonStatusChecker <- ZIO.service[HttpGetRequestWithJsonStatusChecker]
      fixedStatusChecker <- ZIO.service[FixedStatusChecker]
      temporaryFixedStatusChecker <- ZIO.service[TemporaryFixedStatusChecker]
      compositionChecker <- ZIO.service[CompositionChecker]
      awsRdsChecker <- ZIO.service[AwsRdsChecker]
      awsRdsClusterChecker <- ZIO.service[AwsRdsClusterChecker]
      awsEmrChecker <- ZIO.service[AwsEmrChecker]
      awsEc2AmiComplianceChecker <- ZIO.service[AwsEc2AmiComplianceChecker]
    } yield new PolymorphicChecker(
      httpGetRequestStatusCodeOnlyChecker,
      httpGetRequestWithMessageChecker,
      httpGetRequestWithJsonStatusChecker,
      fixedStatusChecker,
      temporaryFixedStatusChecker,
      compositionChecker,
      awsRdsChecker,
      awsRdsClusterChecker,
      awsEmrChecker,
      awsEc2AmiComplianceChecker
    )
  }
}
