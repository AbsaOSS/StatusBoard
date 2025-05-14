package za.co.absa.statusboard.checker

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.emr.{EmrClient, EmrClientBuilder}
import software.amazon.awssdk.services.emr.model.{DescribeClusterRequest, ListInstancesRequest}
import za.co.absa.statusboard.config.checkers.AwsEmrCheckerConfig
import za.co.absa.statusboard.model.{RawStatus, StatusCheckAction}
import zio._

import scala.jdk.CollectionConverters.CollectionHasAsScala

class AwsEmrCheckerImpl(config: AwsEmrCheckerConfig, emrClientBuilder: EmrClientBuilder) extends AwsEmrChecker {
  override def checkRawStatus(action: StatusCheckAction.AwsEmr): UIO[RawStatus] = {
    for {
      emrClient <- ZIO.attempt(emrClientBuilder.region(Region.of(action.region)).build())
      clusterStatus <- clusterStatus(emrClient, action.clusterId)
      nodesStatus <- if (action.checkNodes) nodesStatus(emrClient, action.clusterId) else ZIO.succeed(RawStatus.Green("NO CHECK"))
    } yield (clusterStatus, nodesStatus) match {
      case (cluster: RawStatus.Red, _) => cluster
      case (_, nodes: RawStatus.Red) => nodes
      case (cluster: RawStatus.Amber, _) => cluster
      case (_, nodes: RawStatus.Amber) => nodes
      case (cluster: RawStatus.Green, _: RawStatus.Green) => cluster
      case _ => throw new Exception(s"Unexpected status combination cluster: $clusterStatus node: $nodesStatus")
    }
  }.catchAll { error =>
    ZIO.logError(s"An error occurred while performing AWS EMR status check for ${action.region} ${action.clusterId} Details: ${error.toString}")
      .as(RawStatus.Red(s"FAIL: ${error.getMessage}", intermittent = true))
  }

  private def clusterStatus(emrClient: EmrClient, clusterId: String): Task[RawStatus] = for {
    request <- ZIO.attempt(DescribeClusterRequest.builder().clusterId(clusterId).build())
    response <- ZIO.attempt(emrClient.describeCluster(request))
    awsStatus <- ZIO.attempt(response.cluster().status().state().toString)
  } yield if (config.red.contains(awsStatus)) RawStatus.Red(awsStatus, intermittent = false)
  else if (config.amber.contains(awsStatus)) RawStatus.Amber(awsStatus, intermittent = false)
  else if (config.green.contains(awsStatus)) RawStatus.Green(awsStatus)
  else throw new Exception(s"unknown status: $awsStatus")

  private def nodesStatus(emrClient: EmrClient, clusterId: String): Task[RawStatus] = for {
    request <- ZIO.attempt(ListInstancesRequest.builder().clusterId(clusterId).build())
    response <- ZIO.attempt(emrClient.listInstances(request))
    nodeStatuses <- ZIO.attempt(response.instances().asScala.map(_.status().state().toString).toSeq)
    nodesUp <- ZIO.attempt(nodeStatuses.count(x => config.nodeUp.contains(x)))
    nodesIgnored <- ZIO.attempt(nodeStatuses.count(x => config.nodeIgnored.contains(x)))
    nodesFailed <- ZIO.attempt(nodeStatuses.count(x => config.nodeFailed.contains(x)))
    unknownNodeStatuses <- ZIO.attempt(nodeStatuses.size - nodesUp - nodesIgnored - nodesFailed)
    _ <- ZIO.attempt(if (unknownNodeStatuses > 0) throw new Exception(s"$unknownNodeStatuses nodes with unknown status") else ())
    statusMessage <- ZIO.succeed(s"Nodes: Up: $nodesUp Ignored: $nodesIgnored Failed: $nodesFailed")
    _ <- ZIO.logInfo(s"$clusterId $statusMessage")
  } yield if (nodesUp == 0) RawStatus.Red(statusMessage, false) // No UP node => RED
  else if (nodesFailed > 0) RawStatus.Amber(statusMessage, false) // UP node but also FAILURE => AMBER
  else RawStatus.Green(statusMessage) // UP with no FAILURE => GREEN
}

object AwsEmrCheckerImpl {
  val layer: ZLayer[EmrClientBuilder, Throwable, AwsEmrChecker] = ZLayer {
    for {
      config <- ZIO.config[AwsEmrCheckerConfig](AwsEmrCheckerConfig.config)
      emrClientBuilder <- ZIO.service[EmrClientBuilder]
    } yield new AwsEmrCheckerImpl(config, emrClientBuilder)
  }
}
