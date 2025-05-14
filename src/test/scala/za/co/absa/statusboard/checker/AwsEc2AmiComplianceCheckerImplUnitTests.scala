package za.co.absa.statusboard.checker

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ec2.model.{DescribeImagesRequest, DescribeImagesResponse, DescribeInstancesRequest, DescribeInstancesResponse, Ec2Exception, Image, Instance, Reservation}
import software.amazon.awssdk.services.ec2.{Ec2Client, Ec2ClientBuilder}
import za.co.absa.statusboard.model.RawStatus.{Amber, Green, Red}
import za.co.absa.statusboard.model.StatusCheckAction.AwsEc2AmiCompliance
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio._
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestAspect, TestEnvironment, assert}

import scala.jdk.CollectionConverters.SeqHasAsJava

object AwsEc2AmiComplianceCheckerImplUnitTests extends ConfigProviderSpec {
  private val ec2ClientBuilderMock = mock(classOf[Ec2ClientBuilder])
  private val ec2ClientMock = mock(classOf[Ec2Client])
  private val checkRequest = AwsEc2AmiCompliance(region = Region.AWS_GLOBAL.toString, id = "i-ec2-12345")
  private val describeInstancesResponseMock = mock(classOf[DescribeInstancesResponse])
  private val reservationMock = mock(classOf[Reservation])
  private val instanceMock = mock(classOf[Instance])
  private val describeImagesResponseServiceMock = mock(classOf[DescribeImagesResponse])
  private val imageServiceMock = mock(classOf[Image])
  private val describeImagesResponseAllMock = mock(classOf[DescribeImagesResponse])
  private val imageAllFirstMock = mock(classOf[Image])
  private val imageAllSecondMock = mock(classOf[Image])

  when(ec2ClientBuilderMock.region(Region.AWS_GLOBAL))
    .thenReturn(ec2ClientBuilderMock)
  when(ec2ClientBuilderMock.build())
    .thenReturn(ec2ClientMock)
  when(describeInstancesResponseMock.reservations())
    .thenReturn(Seq(reservationMock).asJava)
  when(reservationMock.instances())
    .thenReturn(Seq(instanceMock).asJava)
  when(instanceMock.imageId())
    .thenReturn("i-ami-12345")
  when(describeImagesResponseServiceMock.images())
    .thenReturn(Seq(imageServiceMock).asJava)
  when(describeImagesResponseAllMock.images())
    .thenReturn(Seq(imageAllFirstMock, imageAllSecondMock).asJava)

  private def resetMock(amiNameService: String, amiNameAllFirst: String, amiNameAllSecond: String): Task[Unit] = ZIO.attempt {
    reset(ec2ClientMock)
    reset(imageServiceMock)
    reset(imageAllFirstMock)
    reset(imageAllSecondMock)
    when(ec2ClientMock.describeInstances(any[DescribeInstancesRequest]))
      .thenReturn(describeInstancesResponseMock)
    when(ec2ClientMock.describeImages(any[DescribeImagesRequest]))
      .thenAnswer(invocation => {
        val request = invocation.getArgument[DescribeImagesRequest](0)
        val specificAmiRequested = !request.imageIds().isEmpty
        if (specificAmiRequested) describeImagesResponseServiceMock else describeImagesResponseAllMock
      })
    when(imageServiceMock.name())
      .thenReturn(amiNameService)
    when(imageAllFirstMock.name())
      .thenReturn(amiNameAllFirst)
    when(imageAllSecondMock.name())
      .thenReturn(amiNameAllSecond)
  }

  private def resetMockToFail(message: String): Task[Unit] = ZIO.attempt {
    reset(ec2ClientMock)
    reset(imageServiceMock)
    reset(imageAllFirstMock)
    reset(imageAllSecondMock)
    when(ec2ClientMock.describeInstances(any[DescribeInstancesRequest]))
      .thenThrow(Ec2Exception.builder.message(message).build())
  }

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("AwsEc2AmiComplianceCheckerImplSuite")(
      test("GREEN on up-to-date") {
        for {
          _ <- resetMock("testami-20240711T154645", "testami-20240711T154645", "testami-20230711T154645")
          result <- AwsEc2AmiComplianceChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Green("testami-20240711T154645")))
      },
      test("AMBER on stale") {
        for {
          _ <- resetMock("testami-20230711T154645", "testami-20240711T154645", "testami-20230711T154645")
          result <- AwsEc2AmiComplianceChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Amber("testami-20230711T154645", intermittent = false)))
      },
      test("RED on not present") {
        for {
          _ <- resetMock("badtestami-20230711T154645", "testami-20240711T154645", "testami-20230711T154645")
          result <- AwsEc2AmiComplianceChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Red("badtestami-20230711T154645", intermittent = false)))
      },
      test("RED on fail") {
        for {
          _ <- resetMockToFail("BAKA")
          result <- AwsEc2AmiComplianceChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Red("FAIL: BAKA", intermittent = true)))
      }
    ) @@ TestAspect.sequential
  }.provide(
    AwsEc2AmiComplianceCheckerImpl.layer,
    ZLayer.succeed(ec2ClientBuilderMock),
  )
}
