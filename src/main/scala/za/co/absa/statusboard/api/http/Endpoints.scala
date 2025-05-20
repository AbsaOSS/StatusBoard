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

import sttp.model.StatusCode
import sttp.tapir.generic.auto.schemaForCaseClass
import sttp.tapir.json.circe.{jsonBody, jsonQuery}
import sttp.tapir.{Endpoint, PublicEndpoint}
import sttp.tapir.ztapir._
import za.co.absa.statusboard.api.http.Constants.Endpoints._
import za.co.absa.statusboard.api.http.Constants.Headers.Authorization
import za.co.absa.statusboard.api.http.Constants.Params.{Environment, ServiceName}
import za.co.absa.statusboard.api.http.EndpointErrors._
import za.co.absa.statusboard.model.{ErrorResponse, RawStatus, RefinedStatus, ServiceConfiguration, ServiceConfigurationReference}
import za.co.absa.statusboard.model.ApiResponse.{MultiApiResponse, SingleApiResponse}

object Endpoints {
  val zioMetrics: PublicEndpoint[Unit, Unit, String, Any] = endpoint
    .get
    .in(ZioMetrics)
    .out(stringBody)
    .out(statusCode(StatusCode.Ok))

  val health: PublicEndpoint[Unit, Unit, String, Any] = endpoint
    .get
    .in(Health)
    .out(stringBody)
    .out(statusCode(StatusCode.Ok))

  val getServiceConfigurations: PublicEndpoint[Option[Boolean], ErrorResponse, MultiApiResponse[ServiceConfiguration], Any] = endpoint
    .get
    .in(Api / V1 / Configurations).in(query[Option[Boolean]](IncludeHidden))
    .out(jsonBody[MultiApiResponse[ServiceConfiguration]])
    .out(statusCode(StatusCode.Ok))
    .errorOut(
      oneOf[ErrorResponse](
        badRequest,
        internalServerError
      )
    )

  val getServiceConfigurationByName: PublicEndpoint[(String, String), ErrorResponse, SingleApiResponse[ServiceConfiguration], Any] = endpoint
    .get
    .in(Api / V1 / Configurations / path[String](Environment) / path[String](ServiceName))
    .out(jsonBody[SingleApiResponse[ServiceConfiguration]])
    .out(statusCode(StatusCode.Ok))
    .errorOut(
      oneOf[ErrorResponse](
        badRequest,
        recordNotFound,
        internalServerError
      )
    )

  val getServiceConfigurationDependencies: PublicEndpoint[(String, String), ErrorResponse, MultiApiResponse[ServiceConfigurationReference], Any] = endpoint
    .get
    .in(Api / V1 / Configurations / path[String](Environment) / path[String](ServiceName) / Dependencies)
    .out(jsonBody[MultiApiResponse[ServiceConfigurationReference]])
    .out(statusCode(StatusCode.Ok))
    .errorOut(
      oneOf[ErrorResponse](
        badRequest,
        recordNotFound,
        internalServerError
      )
    )

  val getServiceConfigurationDependents: PublicEndpoint[(String, String), ErrorResponse, MultiApiResponse[ServiceConfigurationReference], Any] = endpoint
    .get
    .in(Api / V1 / Configurations / path[String](Environment) / path[String](ServiceName) / Dependents)
    .out(jsonBody[MultiApiResponse[ServiceConfigurationReference]])
    .out(statusCode(StatusCode.Ok))
    .errorOut(
      oneOf[ErrorResponse](
        badRequest,
        internalServerError
      )
    )

  val postServiceConfiguration: Endpoint[String, ServiceConfiguration, ErrorResponse, Unit, Any] = endpoint
    .post
    .securityIn(auth.apiKey(header[String](Authorization)))
    .in(Api / V1 / Configurations)
    .in(jsonBody[ServiceConfiguration])
    .out(statusCode(StatusCode.Created))
    .errorOut(
      oneOf[ErrorResponse](
        badRequest,
        unauthorized,
        dataConflict,
        internalServerError
      )
    )

  val putServiceConfiguration: Endpoint[String, (String, String, ServiceConfiguration), ErrorResponse, Unit, Any] = endpoint
    .put
    .securityIn(auth.apiKey(header[String](Authorization)))
    .in(Api / V1 / Configurations / path[String](Environment) / path[String](ServiceName))
    .in(jsonBody[ServiceConfiguration])
    .out(statusCode(StatusCode.Ok))
    .errorOut(
      oneOf[ErrorResponse](
        badRequest,
        unauthorized,
        recordNotFound,
        dataConflict,
        internalServerError
      )
    )

  val postServiceConfigurationWithRename: Endpoint[String, (String, String, ServiceConfiguration), ErrorResponse, Unit, Any] = endpoint
    .post
    .securityIn(auth.apiKey(header[String](Authorization)))
    .in(Api / V1 / Configurations / path[String](Environment) / path[String](ServiceName) / NewName)
    .in(jsonBody[ServiceConfiguration])
    .out(statusCode(StatusCode.Ok))
    .errorOut(
      oneOf[ErrorResponse](
        badRequest,
        unauthorized,
        recordNotFound,
        dataConflict,
        internalServerError
      )
    )

  val putServiceConfigurationMaintenanceMessage: Endpoint[String, (String, String, String), ErrorResponse, Unit, Any] = endpoint
    .put
    .securityIn(auth.apiKey(header[String](Authorization)))
    .in(Api / V1 / Configurations / path[String](Environment) / path[String](ServiceName) / MaintenanceMessage)
    .in(query[String](MaintenanceMessage))
    .out(statusCode(StatusCode.Ok))
    .errorOut(
      oneOf[ErrorResponse](
        badRequest,
        unauthorized,
        recordNotFound,
        internalServerError
      )
    )

  val putServiceConfigurationTemporaryState: Endpoint[String, (String, String, RawStatus, String), ErrorResponse, Unit, Any] = endpoint
    .put
    .securityIn(auth.apiKey(header[String](Authorization)))
    .in(Api / V1 / Configurations / path[String](Environment) / path[String](ServiceName) / TemporaryState)
    .in(jsonQuery[RawStatus](Status)).in(query[String](MaintenanceMessage))
    .out(statusCode(StatusCode.Ok))
    .errorOut(
      oneOf[ErrorResponse](
        badRequest,
        unauthorized,
        recordNotFound,
        internalServerError
      )
    )

  val postServiceConfigurationRestoreFromTemporaryState: Endpoint[String, (String, String), ErrorResponse, Unit, Any] = endpoint
    .post
    .securityIn(auth.apiKey(header[String](Authorization)))
    .in(Api / V1 / Configurations / path[String](Environment) / path[String](ServiceName) / Restore)
    .out(statusCode(StatusCode.Ok))
    .errorOut(
      oneOf[ErrorResponse](
        badRequest,
        unauthorized,
        recordNotFound,
        internalServerError
      )
    )

  val deleteServiceConfiguration: Endpoint[String, (String, String), ErrorResponse, Unit, Any] = endpoint
    .delete
    .securityIn(auth.apiKey(header[String](Authorization)))
    .in(Api / V1 / Configurations / path[String](Environment) / path[String](ServiceName))
    .out(statusCode(StatusCode.NoContent))
    .errorOut(
      oneOf[ErrorResponse](
        badRequest,
        unauthorized,
        internalServerError
      )
    )

  val getLatestServiceStatusByName: PublicEndpoint[(String, String), ErrorResponse, SingleApiResponse[RefinedStatus], Any] = endpoint
    .get
    .in(Api / V1 / Statuses / path[String](Environment) / path[String](ServiceName) / Latest)
    .out(jsonBody[SingleApiResponse[RefinedStatus]])
    .out(statusCode(StatusCode.Ok))
    .errorOut(
      oneOf[ErrorResponse](
        badRequest,
        recordNotFound,
        internalServerError
      )
    )

  val getServiceStatusHistoryByName: PublicEndpoint[(String, String), ErrorResponse, MultiApiResponse[RefinedStatus], Any] = endpoint
    .get
    .in(Api / V1 / Statuses / path[String](Environment) / path[String](ServiceName))
    .out(jsonBody[MultiApiResponse[RefinedStatus]])
    .out(statusCode(StatusCode.Ok))
    .errorOut(
      oneOf[ErrorResponse](
        badRequest,
        internalServerError
      )
    )

  val getLatestStatusOfAllActiveServices: PublicEndpoint[Unit, ErrorResponse, MultiApiResponse[RefinedStatus], Any] = endpoint
    .get
    .in(Api / V1 / Statuses)
    .out(jsonBody[MultiApiResponse[RefinedStatus]])
    .out(statusCode(StatusCode.Ok))
    .errorOut(
      oneOf[ErrorResponse](
        badRequest,
        internalServerError
      )
    )

  val postRestartMonitoring: Endpoint[String, Unit, ErrorResponse, Unit, Any] = endpoint
    .post
    .securityIn(auth.apiKey(header[String](Authorization)))
    .in(Api / V1 / Monitoring / Restart)
    .out(statusCode(StatusCode.Accepted))
    .errorOut(
      oneOf[ErrorResponse](
        badRequest,
        unauthorized,
        internalServerError
      )
    )

  val postRestartMonitoringForServiceConfiguration: Endpoint[String, (String, String), ErrorResponse, Unit, Any] = endpoint
    .post
    .securityIn(auth.apiKey(header[String](Authorization)))
    .in(Api / V1 / Monitoring / Restart / path[String](Environment) / path[String](ServiceName))
    .out(statusCode(StatusCode.Accepted))
    .errorOut(
      oneOf[ErrorResponse](
        badRequest,
        unauthorized,
        internalServerError
      )
    )
}
