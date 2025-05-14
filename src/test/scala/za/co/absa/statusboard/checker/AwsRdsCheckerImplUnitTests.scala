package za.co.absa.statusboard.checker

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.rds.{RdsClient, RdsClientBuilder}
import za.co.absa.statusboard.model.RawStatus.{Amber, Green, Red}
import za.co.absa.statusboard.model.StatusCheckAction.AwsRds
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio._
import software.amazon.awssdk.services.rds.model.{DBInstance, DescribeDbInstancesRequest, DescribeDbInstancesResponse, RdsException}
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestAspect, TestEnvironment, assert}

import scala.jdk.CollectionConverters.SeqHasAsJava

object AwsRdsCheckerImplUnitTests extends ConfigProviderSpec {
  private val rdsClientBuilderMock = mock(classOf[RdsClientBuilder])
  private val rdsClientMock = mock(classOf[RdsClient])
  private val checkRequest = AwsRds(region = Region.AWS_GLOBAL.toString, name = "TestInstance")
  private val rdsDescribeDbInstancesResponseMock = mock(classOf[DescribeDbInstancesResponse])
  private val dbInstanceMock = mock(classOf[DBInstance])

  when(rdsClientBuilderMock.region(Region.AWS_GLOBAL))
    .thenReturn(rdsClientBuilderMock)
  when(rdsClientBuilderMock.build())
    .thenReturn(rdsClientMock)
  when(rdsClientMock.describeDBInstances(any[DescribeDbInstancesRequest]))
    .thenReturn(rdsDescribeDbInstancesResponseMock)
  when(rdsDescribeDbInstancesResponseMock.dbInstances())
    .thenReturn(Seq(dbInstanceMock).asJava)

  private def resetMock(desiredResponse: String): Task[Unit] = ZIO.attempt {
    reset(rdsClientMock)
    reset(dbInstanceMock)
    when(rdsClientMock.describeDBInstances(any[DescribeDbInstancesRequest]))
      .thenReturn(rdsDescribeDbInstancesResponseMock)
    when(dbInstanceMock.dbInstanceStatus())
      .thenReturn(desiredResponse)
  }

  private def resetMockToFail(message: String): Task[Unit] = ZIO.attempt {
    reset(rdsClientMock)
    reset(dbInstanceMock)
    when(rdsClientMock.describeDBInstances(any[DescribeDbInstancesRequest]))
      .thenThrow(RdsException.builder.message(message).build())
  }

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("HttpGetRequestStatusCodeOnlyCheckerImplSuite")(
      test("GREEN on success") {
        for {
          _ <- resetMock("Available")
          result <- AwsRdsChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Green("Available")))
      },
      test("AMBER on maybe") {
        for {
          _ <- resetMock("Configuring-enhanced-monitoring")
          result <- AwsRdsChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Amber("Configuring-enhanced-monitoring", intermittent = false)))
      },
      test("RED on not success") {
        for {
          _ <- resetMock("Deleting")
          result <- AwsRdsChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Red("Deleting", intermittent = false)))
      },
      test("RED on fail") {
        for {
          _ <- resetMockToFail("BAKA")
          result <- AwsRdsChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Red("FAIL: BAKA", intermittent = true)))
      }
    ) @@ TestAspect.sequential
  }.provide(
    AwsRdsCheckerImpl.layer,
    ZLayer.succeed(rdsClientBuilderMock),
  )
}
