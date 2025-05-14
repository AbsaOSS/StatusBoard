package za.co.absa.statusboard.monitoring

import za.co.absa.statusboard.model.ServiceConfiguration
import zio.UIO
import zio.macros.accessible

@accessible
trait MonitoringWorker {
  def performMonitoringWork(serviceConfiguration: ServiceConfiguration): UIO[Unit]
}
