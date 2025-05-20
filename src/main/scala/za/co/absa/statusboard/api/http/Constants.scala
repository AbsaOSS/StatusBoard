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
