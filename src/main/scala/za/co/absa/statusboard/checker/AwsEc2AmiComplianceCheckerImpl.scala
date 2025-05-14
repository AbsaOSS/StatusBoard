package za.co.absa.statusboard.checker

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ec2.{Ec2Client, Ec2ClientBuilder}
import software.amazon.awssdk.services.ec2.model.{DescribeImagesRequest, DescribeInstancesRequest, Filter}
import za.co.absa.statusboard.checker.AwsEc2AmiComplianceCheckerImpl.{mapWithAllCanonNamesToLatestNumber, nameToCanonAndNumber}
import za.co.absa.statusboard.model.{RawStatus, StatusCheckAction}
import zio._

import scala.jdk.CollectionConverters.ListHasAsScala

class AwsEc2AmiComplianceCheckerImpl(ec2ClientBuilder: Ec2ClientBuilder) extends AwsEc2AmiComplianceChecker {
  override def checkRawStatus(action: StatusCheckAction.AwsEc2AmiCompliance): UIO[RawStatus] = {
    for {
      ec2Client <- ZIO.attempt(ec2ClientBuilder.region(Region.of(action.region)).build())
      ec2Request <- ZIO.attempt(DescribeInstancesRequest.builder().instanceIds(action.id).build())
      ec2Response <- ZIO.attempt(ec2Client.describeInstances(ec2Request))
      usedAmiId <- ZIO.attempt(ec2Response.reservations().getFirst.instances().getFirst.imageId())
      usedAmiRequest <- ZIO.attempt(DescribeImagesRequest.builder().imageIds(usedAmiId).build())
      usedAmiResponse <- ZIO.attempt(ec2Client.describeImages(usedAmiRequest))
      usedAmiName <- ZIO.attempt(usedAmiResponse.images().getFirst.name())
      usedAmiCanonNameAndNumber <- nameToCanonAndNumber(usedAmiName)
      allAmiCanonNamesToValidNumber <- mapWithAllCanonNamesToLatestNumber(ec2Client)
    } yield if (!allAmiCanonNamesToValidNumber.contains(usedAmiCanonNameAndNumber.canonName))
      RawStatus.Red(usedAmiName, intermittent = false)
    else if (allAmiCanonNamesToValidNumber(usedAmiCanonNameAndNumber.canonName) == usedAmiCanonNameAndNumber.number)
      RawStatus.Green(usedAmiName)
    else
      RawStatus.Amber(usedAmiName, intermittent = false)
  }.catchAll {
    error =>
      ZIO.logError(s"An error occurred while performing AWS EC2 AMI Compliance status check for ${action.region} ${action.id} Details: ${error.toString}")
        .as(RawStatus.Red(s"FAIL: ${error.getMessage}", intermittent = true))
  }
}

object AwsEc2AmiComplianceCheckerImpl {
  private case class CanonNameWithNumber(canonName: String, number: Long)
  private val formatRegex = "\\d{8}T\\d{6}".r

  private def mapWithAllCanonNamesToLatestNumber(ec2Client: Ec2Client): Task[Map[String, Long]] = for {
    allAmiRequestFilter <- ZIO.attempt(Filter.builder().name("is-public").values("false").build())
    allAmiRequest <- ZIO.attempt(DescribeImagesRequest.builder().filters(allAmiRequestFilter).build())
    allAmiResponse <- ZIO.attempt(ec2Client.describeImages(allAmiRequest))
    allAmiNames <- ZIO.attempt(allAmiResponse.images().asScala.toSeq.map(x => x.name()))
    allAmiCanonNamesAndNumbers <- ZIO.foreach(allAmiNames)(nameToCanonAndNumber)
  } yield allAmiCanonNamesAndNumbers
    .groupBy(_.canonName)
    .view
    .mapValues(_.maxBy(_.number).number).toMap

  private def nameToCanonAndNumber(name: String): Task[CanonNameWithNumber] = ZIO.attempt{
    formatRegex.findFirstIn(name) match {
      case Some(timestampPortion) => CanonNameWithNumber(
        name.replace(timestampPortion, ""),
        timestampPortion.substring(0, 8).toInt) // Ignore time part, YYYYMMDD should suffice
      case None => throw new Exception(f"Failed to decompose AMI name for $name")
    }
  }

  val layer: ZLayer[Ec2ClientBuilder, Throwable, AwsEc2AmiComplianceChecker] = ZLayer {
    for {
      ec2ClientBuilder <- ZIO.service[Ec2ClientBuilder]
    } yield new AwsEc2AmiComplianceCheckerImpl(ec2ClientBuilder)
  }
}
