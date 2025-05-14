package za.co.absa.statusboard.api.http

import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import zio.Task

object HttpMetrics {
  val prometheusMetrics: PrometheusMetrics[Task] = PrometheusMetrics[Task]("http")
    .addRequestsTotal()
    .addRequestsActive()
    .addRequestsDuration()
}
