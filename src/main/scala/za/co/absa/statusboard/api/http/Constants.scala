package za.co.absa.statusboard.api.http

object Constants {
  object Headers {
    final val Authorization = "Authorization"
  }

  object Endpoints {
    final val Api = "api"
    final val V1 = "v1"

    final val Health = "health"
    final val ZioMetrics = "zio-metrics"

    final val Configurations = "configurations"
    final val IncludeHidden = "include-hidden"
    final val Dependencies = "dependencies"
    final val Dependents = "dependents"
    final val NewName = "new-name"
    final val MaintenanceMessage = "message"
    final val Status = "status"
    final val TemporaryState = "temporary-state"
    final val Restore = "restore"

    final val Statuses = "statuses"
    final val Latest = "latest"

    final val Monitoring = "monitoring"
    final val Restart = "restart"
  }

  object Params {
    final val Environment = "env"
    final val ServiceName = "service-name"
  }

  final val SwaggerApiName = "Status Board API"
  final val SwaggerApiVersion = "1.0"
}
