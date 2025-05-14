package za.co.absa.statusboard.checker

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.rds.model.{DBCluster, DescribeDbClustersRequest, DescribeDbClustersResponse, RdsException}
import software.amazon.awssdk.services.rds.{RdsClient, RdsClientBuilder}
import za.co.absa.statusboard.model.RawStatus.{Amber, Green, Red}
import za.co.absa.statusboard.model.StatusCheckAction.AwsRdsCluster
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio._
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestAspect, TestEnvironment, assert}

import scala.jdk.CollectionConverters.SeqHasAsJava

object AwsRdsClusterCheckerImplUnitTests extends ConfigProviderSpec {
  private val rdsClientBuilderMock = mock(classOf[RdsClientBuilder])
  private val rdsClientMock = mock(classOf[RdsClient])
  private val checkRequest = AwsRdsCluster(region = Region.AWS_GLOBAL.toString, name = "TestInstance")
  private val rdsDescribeDbClustersResponseMock = mock(classOf[DescribeDbClustersResponse])
  private val dbClusterMock = mock(classOf[DBCluster])

  when(rdsClientBuilderMock.region(Region.AWS_GLOBAL))
    .thenReturn(rdsClientBuilderMock)
  when(rdsClientBuilderMock.build())
    .thenReturn(rdsClientMock)
  when(rdsClientMock.describeDBClusters(any[DescribeDbClustersRequest]))
    .thenReturn(rdsDescribeDbClustersResponseMock)
  when(rdsDescribeDbClustersResponseMock.dbClusters())
    .thenReturn(Seq(dbClusterMock).asJava)

  private def resetMock(desiredResponse: String): Task[Unit] = ZIO.attempt {
    reset(rdsClientMock)
    reset(dbClusterMock)
    when(rdsClientMock.describeDBClusters(any[DescribeDbClustersRequest]))
      .thenReturn(rdsDescribeDbClustersResponseMock)
    when(dbClusterMock.status())
      .thenReturn(desiredResponse)
  }

  private def resetMockToFail(message: String): Task[Unit] = ZIO.attempt {
    reset(rdsClientMock)
    reset(dbClusterMock)
    when(rdsClientMock.describeDBClusters(any[DescribeDbClustersRequest]))
      .thenThrow(RdsException.builder.message(message).build())
  }

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("HttpGetRequestStatusCodeOnlyCheckerImplSuite")(
      test("GREEN on success") {
        for {
          _ <- resetMock("Available")
          result <- AwsRdsClusterChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Green("Available")))
      },
      test("AMBER on maybe") {
        for {
          _ <- resetMock("Backing-up")
          result <- AwsRdsClusterChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Amber("Backing-up", intermittent = false)))
      },
      test("RED on not success") {
        for {
          _ <- resetMock("Deleting")
          result <- AwsRdsClusterChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Red("Deleting", intermittent = false)))
      },
      test("RED on fail") {
        for {
          _ <- resetMockToFail("BAKA")
          result <- AwsRdsClusterChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Red("FAIL: BAKA", intermittent = true)))
      }
    ) @@ TestAspect.sequential
  }.provide(
    AwsRdsClusterCheckerImpl.layer,
    ZLayer.succeed(rdsClientBuilderMock),
  )
}
