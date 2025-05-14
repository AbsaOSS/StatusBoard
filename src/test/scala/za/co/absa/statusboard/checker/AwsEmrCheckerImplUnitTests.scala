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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.emr.{EmrClient, EmrClientBuilder}
import za.co.absa.statusboard.model.RawStatus.{Amber, Green, Red}
import za.co.absa.statusboard.model.StatusCheckAction.AwsEmr
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio._
import software.amazon.awssdk.services.emr.model.{Cluster, ClusterState, ClusterStatus, DescribeClusterRequest, DescribeClusterResponse, EmrException, Instance, InstanceState, InstanceStatus, ListInstancesRequest, ListInstancesResponse}
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestAspect, TestEnvironment, assert}

import scala.jdk.CollectionConverters.SeqHasAsJava

object AwsEmrCheckerImplUnitTests extends ConfigProviderSpec {
  private val emrClientBuilderMock = mock(classOf[EmrClientBuilder])
  private val emrClientMock = mock(classOf[EmrClient])
  private val checkRequestClusterOnly = AwsEmr(region = Region.AWS_GLOBAL.toString, clusterId = "TestInstance", checkNodes = false)
  private val checkRequestWithNodes = AwsEmr(region = Region.AWS_GLOBAL.toString, clusterId = "TestInstance", checkNodes = true)

  when(emrClientBuilderMock.region(Region.AWS_GLOBAL))
    .thenReturn(emrClientBuilderMock)
  when(emrClientBuilderMock.build())
    .thenReturn(emrClientMock)

  private def resetMocks(): Task[Unit] = ZIO.attempt {
    reset(emrClientMock)
  }

  private def setClusterStateMock(clusterState: ClusterState): Task[Unit] = ZIO.attempt {
    val describeClusterResponseMock = mock(classOf[DescribeClusterResponse])
    val clusterMock = mock(classOf[Cluster])
    val clusterStatusMock = mock(classOf[ClusterStatus])
    when(emrClientMock.describeCluster(any[DescribeClusterRequest]))
      .thenReturn(describeClusterResponseMock)
    when(describeClusterResponseMock.cluster())
      .thenReturn(clusterMock)
    when(clusterMock.status())
      .thenReturn(clusterStatusMock)
    when(clusterStatusMock.state())
      .thenReturn(clusterState)
  }

  private def setClusterStateMockToFail(message: String): Task[Unit] = ZIO.attempt {
    when(emrClientMock.describeCluster(any[DescribeClusterRequest]))
      .thenThrow(EmrException.builder.message(message).build())
  }

  private def setNodesMock(nodeStates: Seq[InstanceState]): Task[Unit] = ZIO.attempt {
    val listInstancesResponseMock = mock(classOf[ListInstancesResponse])
    val instanceNodesMock = nodeStates.map(_ => mock(classOf[Instance]))
    val instanceNodeStatusesMock = nodeStates.map(_ => mock(classOf[InstanceStatus]))

    when(emrClientMock.listInstances(any[ListInstancesRequest]))
      .thenReturn(listInstancesResponseMock)
    when(listInstancesResponseMock.instances())
      .thenReturn(instanceNodesMock.asJava)
    instanceNodesMock.zip(instanceNodeStatusesMock).foreach { case (node, status) => when(node.status()).thenReturn(status) }
    instanceNodeStatusesMock.zip(nodeStates).foreach { case (status, state) => when(status.state()).thenReturn(state) }
  }

  private def setNodesMockToFail(message: String): Task[Unit] = ZIO.attempt {
    when(emrClientMock.listInstances(any[ListInstancesRequest]))
      .thenThrow(EmrException.builder.message(message).build())
  }

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("HttpGetRequestStatusCodeOnlyCheckerImplSuite")(
      suite("ClusterCheckOnly")(
        test("GREEN on green") {
          for {
            _ <- resetMocks()
            _ <- setClusterStateMock(ClusterState.RUNNING)
            _ <- setNodesMockToFail("not supposed to be invoked")
            result <- AwsEmrChecker.checkRawStatus(checkRequestClusterOnly)
          } yield assert(result)(equalTo(Green("RUNNING")))
        },
        // NO CLUSTER AMBER STATE
        test("RED on red") {
          for {
            _ <- resetMocks()
            _ <- setClusterStateMock(ClusterState.TERMINATED_WITH_ERRORS)
            _ <- setNodesMockToFail("not supposed to be invoked")
            result <- AwsEmrChecker.checkRawStatus(checkRequestClusterOnly)
          } yield assert(result)(equalTo(Red("TERMINATED_WITH_ERRORS", intermittent = false)))
        },
        test("RED on failure") {
          for {
            _ <- resetMocks()
            _ <- setClusterStateMockToFail("BAKA")
            _ <- setNodesMockToFail("not supposed to be invoked")
            result <- AwsEmrChecker.checkRawStatus(checkRequestClusterOnly)
          } yield assert(result)(equalTo(Red("FAIL: BAKA", intermittent = true)))
        }
      ),
      suite("CheckWithNodes")(
        test("GREEN on all green") {
          for {
            _ <- resetMocks()
            _ <- setClusterStateMock(ClusterState.RUNNING)
            _ <- setNodesMock(Seq(InstanceState.RUNNING))
            result <- AwsEmrChecker.checkRawStatus(checkRequestWithNodes)
          } yield assert(result)(equalTo(Green("RUNNING")))
        },
        test("GREEN on all green with extra ignored nodes") {
          for {
            _ <- resetMocks()
            _ <- setClusterStateMock(ClusterState.RUNNING)
            _ <- setNodesMock(Seq(InstanceState.RUNNING, InstanceState.PROVISIONING, InstanceState.TERMINATED))
            result <- AwsEmrChecker.checkRawStatus(checkRequestWithNodes)
          } yield assert(result)(equalTo(Green("RUNNING")))
        },
        // NO CLUSTER AMBER STATE
        // NO NODE AMBER STATE
        test("RED on no green node") {
          for {
            _ <- resetMocks()
            _ <- setClusterStateMock(ClusterState.RUNNING)
            _ <- setNodesMock(Seq(InstanceState.PROVISIONING))
            result <- AwsEmrChecker.checkRawStatus(checkRequestWithNodes)
          } yield assert(result)(equalTo(Red("Nodes: Up: 0 Ignored: 1 Failed: 0", intermittent = false)))
        },
        test("RED on Cluster RED") {
          for {
            _ <- resetMocks()
            _ <- setClusterStateMock(ClusterState.TERMINATED_WITH_ERRORS)
            _ <- setNodesMock(Seq(InstanceState.RUNNING))
            result <- AwsEmrChecker.checkRawStatus(checkRequestWithNodes)
          } yield assert(result)(equalTo(Red("TERMINATED_WITH_ERRORS", intermittent = false)))
        },
        test("RED on Cluster fail") {
          for {
            _ <- resetMocks()
            _ <- setClusterStateMockToFail("BAKA")
            _ <- setNodesMock(Seq(InstanceState.RUNNING))
            result <- AwsEmrChecker.checkRawStatus(checkRequestWithNodes)
          } yield assert(result)(equalTo(Red("FAIL: BAKA", intermittent = true)))
        },
        test("RED on Nodes failure") {
          for {
            _ <- resetMocks()
            _ <- setClusterStateMock(ClusterState.RUNNING)
            _ <- setNodesMockToFail("BAKA")
            result <- AwsEmrChecker.checkRawStatus(checkRequestWithNodes)
          } yield assert(result)(equalTo(Red("FAIL: BAKA", intermittent = true)))
        }
      )
    ) @@ TestAspect.sequential
  }.provide(
    AwsEmrCheckerImpl.layer,
    ZLayer.succeed(emrClientBuilderMock),
  )
}
