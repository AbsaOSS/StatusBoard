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

import org.mockito.Mockito._
import za.co.absa.statusboard.model.StatusCheckAction
import za.co.absa.statusboard.testUtils.{ConfigProviderSpec, TestData}
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestAspect, TestEnvironment, assert}
import zio.{Scope, Task, ZIO, ZLayer}

object PolymorphicCheckerUnitTests extends ConfigProviderSpec {
  private val httpGetRequestStatusCodeOnlyMock = mock(classOf[StatusCheckAction.HttpGetRequestStatusCodeOnly])
  private val httpGetRequestStatusCodeOnlyCheckerMock = mock(classOf[HttpGetRequestStatusCodeOnlyChecker])
  private val httpGetRequestWithMessageMock = mock(classOf[StatusCheckAction.HttpGetRequestWithMessage])
  private val httpGetRequestWithMessageCheckerMock = mock(classOf[HttpGetRequestWithMessageChecker])
  private val httpGetRequestWithJsonStatusMock = mock(classOf[StatusCheckAction.HttpGetRequestWithJsonStatus])
  private val httpGetRequestWithJsonStatusCheckerMock = mock(classOf[HttpGetRequestWithJsonStatusChecker])
  private val fixedStatusMock = mock(classOf[StatusCheckAction.FixedStatus])
  private val fixedStatusCheckerMock = mock(classOf[FixedStatusChecker])
  private val temporaryFixedStatusMock = mock(classOf[StatusCheckAction.TemporaryFixedStatus])
  private val temporaryFixedStatusCheckerMock = mock(classOf[TemporaryFixedStatusChecker])
  private val compositionMock = mock(classOf[StatusCheckAction.Composition])
  private val compositionCheckerMock = mock(classOf[CompositionChecker])
  private val awsRdsMock = mock(classOf[StatusCheckAction.AwsRds])
  private val awsRdsCheckerMock = mock(classOf[AwsRdsChecker])
  private val awsRdsClusterMock = mock(classOf[StatusCheckAction.AwsRdsCluster])
  private val awsRdsClusterCheckerMock = mock(classOf[AwsRdsClusterChecker])
  private val awsEmrMock = mock(classOf[StatusCheckAction.AwsEmr])
  private val awsEmrCheckerMock = mock(classOf[AwsEmrChecker])
  private val awsEc2AmiComplianceMock = mock(classOf[StatusCheckAction.AwsEc2AmiCompliance])
  private val awsEc2AmiComplianceCheckerMock = mock(classOf[AwsEc2AmiComplianceChecker])

  private def resetMocks: Task[Unit] = ZIO.attempt {
    reset(httpGetRequestStatusCodeOnlyCheckerMock)
    reset(httpGetRequestWithMessageCheckerMock)
    reset(httpGetRequestWithJsonStatusCheckerMock)
    reset(awsRdsCheckerMock)
    reset(awsRdsClusterCheckerMock)
    reset(awsEmrCheckerMock)
    reset(awsEc2AmiComplianceCheckerMock)
  }

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("PolymorphicCheckerSuite")(
      test("Pass HttpGetRequestStatusCodeOnly action to HttpGetRequestStatusCodeOnlyChecker") {
          for {
            _ <- ZIO.attempt {
              when(httpGetRequestStatusCodeOnlyCheckerMock.checkRawStatus(httpGetRequestStatusCodeOnlyMock))
                .thenReturn(ZIO.succeed(TestData.rawStatusGreen))
            }
            result <- Checker.checkRawStatus(httpGetRequestStatusCodeOnlyMock)
            _ <- ZIO.attempt(verify(httpGetRequestStatusCodeOnlyCheckerMock, times(1)).checkRawStatus(httpGetRequestStatusCodeOnlyMock))
          } yield assert(result)(equalTo(TestData.rawStatusGreen))
      },
      test("Pass HttpGetRequestWithMessage action to HttpGetRequestWithMessageChecker") {
          for {
            _ <- ZIO.attempt {
              when(httpGetRequestWithMessageCheckerMock.checkRawStatus(httpGetRequestWithMessageMock))
                .thenReturn(ZIO.succeed(TestData.rawStatusGreen))
            }
            result <- Checker.checkRawStatus(httpGetRequestWithMessageMock)
            _ <- ZIO.attempt(verify(httpGetRequestWithMessageCheckerMock, times(1)).checkRawStatus(httpGetRequestWithMessageMock))
          } yield assert(result)(equalTo(TestData.rawStatusGreen))
      },
      test("Pass HttpGetRequestWithJsonStatus action to HttpGetRequestWithJsonStatusChecker") {
          for {
            _ <- ZIO.attempt {
              when(httpGetRequestWithJsonStatusCheckerMock.checkRawStatus(httpGetRequestWithJsonStatusMock))
                .thenReturn(ZIO.succeed(TestData.rawStatusGreen))
            }
            result <- Checker.checkRawStatus(httpGetRequestWithJsonStatusMock)
            _ <- ZIO.attempt(verify(httpGetRequestWithJsonStatusCheckerMock, times(1)).checkRawStatus(httpGetRequestWithJsonStatusMock))
          } yield assert(result)(equalTo(TestData.rawStatusGreen))
      },
      test("Pass FixedStatus action to FixedStatusChecker") {
          for {
            _ <- ZIO.attempt {
              when(fixedStatusCheckerMock.checkRawStatus(fixedStatusMock))
                .thenReturn(ZIO.succeed(TestData.rawStatusGreen))
            }
            result <- Checker.checkRawStatus(fixedStatusMock)
            _ <- ZIO.attempt(verify(fixedStatusCheckerMock, times(1)).checkRawStatus(fixedStatusMock))
          } yield assert(result)(equalTo(TestData.rawStatusGreen))
      },
      test("Pass TemporaryFixedStatus action to TemporaryFixedStatusChecker") {
          for {
            _ <- ZIO.attempt {
              when(temporaryFixedStatusCheckerMock.checkRawStatus(temporaryFixedStatusMock))
                .thenReturn(ZIO.succeed(TestData.rawStatusGreen))
            }
            result <- Checker.checkRawStatus(temporaryFixedStatusMock)
            _ <- ZIO.attempt(verify(temporaryFixedStatusCheckerMock, times(1)).checkRawStatus(temporaryFixedStatusMock))
          } yield assert(result)(equalTo(TestData.rawStatusGreen))
      },
      test("Pass Composition action to CompositionChecker") {
          for {
            _ <- ZIO.attempt {
              when(compositionCheckerMock.checkRawStatus(compositionMock))
                .thenReturn(ZIO.succeed(TestData.rawStatusGreen))
            }
            result <- Checker.checkRawStatus(compositionMock)
            _ <- ZIO.attempt(verify(compositionCheckerMock, times(1)).checkRawStatus(compositionMock))
          } yield assert(result)(equalTo(TestData.rawStatusGreen))
      },
      test("Pass AwsRds action to AwsRdsChecker") {
          for {
            _ <- ZIO.attempt {
              when(awsRdsCheckerMock.checkRawStatus(awsRdsMock))
                .thenReturn(ZIO.succeed(TestData.rawStatusGreen))
            }
            result <- Checker.checkRawStatus(awsRdsMock)
            _ <- ZIO.attempt(verify(awsRdsCheckerMock, times(1)).checkRawStatus(awsRdsMock))
          } yield assert(result)(equalTo(TestData.rawStatusGreen))
      },
      test("Pass AwsRds action to AwsRdsClusterChecker") {
          for {
            _ <- ZIO.attempt {
              when(awsRdsClusterCheckerMock.checkRawStatus(awsRdsClusterMock))
                .thenReturn(ZIO.succeed(TestData.rawStatusGreen))
            }
            result <- Checker.checkRawStatus(awsRdsClusterMock)
            _ <- ZIO.attempt(verify(awsRdsClusterCheckerMock, times(1)).checkRawStatus(awsRdsClusterMock))
          } yield assert(result)(equalTo(TestData.rawStatusGreen))
      },
      test("Pass AwsRds action to AwsRdsChecker") {
          for {
            _ <- ZIO.attempt {
              when(awsEmrCheckerMock.checkRawStatus(awsEmrMock))
                .thenReturn(ZIO.succeed(TestData.rawStatusGreen))
            }
            result <- Checker.checkRawStatus(awsEmrMock)
            _ <- ZIO.attempt(verify(awsEmrCheckerMock, times(1)).checkRawStatus(awsEmrMock))
          } yield assert(result)(equalTo(TestData.rawStatusGreen))
      },
      test("Pass AwsEc2AmiCompliance action to AwsEc2AmiComplianceChecker") {
          for {
            _ <- ZIO.attempt {
              when(awsEc2AmiComplianceCheckerMock.checkRawStatus(awsEc2AmiComplianceMock))
                .thenReturn(ZIO.succeed(TestData.rawStatusGreen))
            }
            result <- Checker.checkRawStatus(awsEc2AmiComplianceMock)
            _ <- ZIO.attempt(verify(awsEc2AmiComplianceCheckerMock, times(1)).checkRawStatus(awsEc2AmiComplianceMock))
          } yield assert(result)(equalTo(TestData.rawStatusGreen))
      }
    ) @@ TestAspect.before(resetMocks) @@ TestAspect.sequential
  }.provide(
    PolymorphicChecker.layer,
    ZLayer.succeed(httpGetRequestStatusCodeOnlyCheckerMock),
    ZLayer.succeed(httpGetRequestWithMessageCheckerMock),
    ZLayer.succeed(httpGetRequestWithJsonStatusCheckerMock),
    ZLayer.succeed(fixedStatusCheckerMock),
    ZLayer.succeed(temporaryFixedStatusCheckerMock),
    ZLayer.succeed(compositionCheckerMock),
    ZLayer.succeed(awsRdsCheckerMock),
    ZLayer.succeed(awsRdsClusterCheckerMock),
    ZLayer.succeed(awsEmrCheckerMock),
    ZLayer.succeed(awsEc2AmiComplianceCheckerMock)
  )
}
