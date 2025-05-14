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

import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, jawn}
import org.http4s.{Header, Method}
import org.http4s.blaze.client.BlazeClientBuilder
import org.mockito.Mockito.{mock, reset, when}
import org.typelevel.ci.CIString
import sttp.model.StatusCode
import za.co.absa.statusboard.api.controllers.{AuthController, MonitoringController, ServiceConfigurationController, StatusController}
import za.co.absa.statusboard.api.http.Constants.Endpoints._
import za.co.absa.statusboard.api.http.Constants.Headers.Authorization
import za.co.absa.statusboard.config.ServerConfig
import za.co.absa.statusboard.model.ApiResponse.{MultiApiResponse, SingleApiResponse}
import za.co.absa.statusboard.model.ErrorResponse.{DataConflictErrorResponse, InternalServerErrorResponse, RecordNotFoundErrorResponse}
import za.co.absa.statusboard.model.{RawStatus, RefinedStatus, ServiceConfigurationReference}
import za.co.absa.statusboard.providers.BlazeHttpClientProvider
import za.co.absa.statusboard.providers.Response.{ResponseStatusOnly, ResponseStatusWithMessage}
import za.co.absa.statusboard.testUtils.TestData.rawStatusRed
import za.co.absa.statusboard.testUtils.{ConfigProviderSpec, TestData}
import zio.Runtime.defaultBlockingExecutor
import zio._
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.test.Assertion.equalTo
import zio.test._
import zio.interop.catz._

import java.time.Instant

object ServerUnitTests extends ConfigProviderSpec {
  private val serviceConfigurationControllerMock = mock(classOf[ServiceConfigurationController])
  private val monitoringControllerMock = mock(classOf[MonitoringController])
  private val statusControllerMock = mock(classOf[StatusController])
  private val prometheusPublisherMock = mock(classOf[PrometheusPublisher])
  private val authControllerMock = mock(classOf[AuthController])
  private val apiKey = "VALID_FOR_TEST"
  when(authControllerMock.authorize(apiKey)).thenReturn(ZIO.unit)

  private val configuration = TestData.serviceConfiguration
  private val configurationDifferentService = configuration.copy(name = "Different service")

  private val refinedStatus = TestData.refinedStatus
  private val refinedStatusAnother: RefinedStatus = refinedStatus.copy(
    status = rawStatusRed,
    firstSeen = Instant.parse("2024-05-17T15:00:00Z"),
    lastSeen = Instant.parse("2024-05-17T16:00:00Z"))

  def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("ServerSuite")(
      suite("Maintenance")(
        suite("healthEndpoint")(
          test("is alive") {
            requestCheckMessage(
              ZIO.unit,
              Method.GET,
              makeStringUriRoot(Health),
              StatusCode.Ok.code,
              "I'm alive"
            )
          }
        )
      ),
      suite("ServiceConfigurations")(
        suite("getServiceConfigurationsEndpoint")(
          test("passes call to controller - default") {
            requestCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.getServiceConfigurations(None))
                  .thenReturn(ZIO.succeed(MultiApiResponse(List(configuration, configurationDifferentService))))
              },
              Method.GET,
              makeStringUriV1(Configurations),
              StatusCode.Ok.code,
              MultiApiResponse(List(configuration, configurationDifferentService))
            )
          },
          test("passes call to controller - hidden treatment selection") {
            requestCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.getServiceConfigurations(Some(false)))
                  .thenReturn(ZIO.succeed(MultiApiResponse(List(configuration, configurationDifferentService))))
              },
              Method.GET,
              makeStringUriV1(s"$Configurations?$IncludeHidden=false"),
              StatusCode.Ok.code,
              MultiApiResponse(List(configuration, configurationDifferentService))
            )
          },
          test("failure presents InternalServerError") {
            requestCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.getServiceConfigurations(None))
                  .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
              },
              Method.GET,
              makeStringUriV1(Configurations),
              StatusCode.InternalServerError.code,
              InternalServerErrorResponse("BAKA")
            )
          }
        ),
        suite("getServiceConfigurationByNameEndpoint")(
          test("passes call to controller") {
            requestCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.getServiceConfiguration("TestEnv", "TestService"))
                  .thenReturn(ZIO.succeed(SingleApiResponse(configuration)))
              },
              Method.GET,
              makeStringUriV1(s"$Configurations/TestEnv/TestService"),
              StatusCode.Ok.code,
              SingleApiResponse(configuration)
            )
          },
          test("failure presents InternalServerError") {
            requestCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.getServiceConfiguration("TestEnv", "TestService"))
                  .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
              },
              Method.GET,
              makeStringUriV1(s"$Configurations/TestEnv/TestService"),
              StatusCode.InternalServerError.code,
              InternalServerErrorResponse("BAKA")
            )
          },
          test("missing record presents RecordNotFound") {
            requestCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.getServiceConfiguration("TestEnv", "TestService"))
                  .thenReturn(ZIO.fail(RecordNotFoundErrorResponse("BAKA")))
              },
              Method.GET,
              makeStringUriV1(s"$Configurations/TestEnv/TestService"),
              StatusCode.NotFound.code,
              RecordNotFoundErrorResponse("BAKA")
            )
          }
        ),
        suite("getServiceConfigurationDependenciesEndpoint")(
          test("passes call to controller") {
            requestCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.getServiceConfigurationDependencies("TestEnv", "TestService"))
                  .thenReturn(ZIO.succeed(MultiApiResponse(Seq(ServiceConfigurationReference("TestEnv", "TestService2")))))
              },
              Method.GET,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$Dependencies"),
              StatusCode.Ok.code,
              MultiApiResponse(Seq(ServiceConfigurationReference("TestEnv", "TestService2")))
            )
          },
          test("failure presents InternalServerError") {
            requestCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.getServiceConfigurationDependencies("TestEnv", "TestService"))
                  .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
              },
              Method.GET,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$Dependencies"),
              StatusCode.InternalServerError.code,
              InternalServerErrorResponse("BAKA")
            )
          },
          test("missing record presents RecordNotFound") {
            requestCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.getServiceConfigurationDependencies("TestEnv", "TestService"))
                  .thenReturn(ZIO.fail(RecordNotFoundErrorResponse("BAKA")))
              },
              Method.GET,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$Dependencies"),
              StatusCode.NotFound.code,
              RecordNotFoundErrorResponse("BAKA")
            )
          }
        ),
        suite("getServiceConfigurationDependentsEndpoint")(
          test("passes call to controller") {
            requestCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.getServiceConfigurationDependents("TestEnv", "TestService"))
                  .thenReturn(ZIO.succeed(MultiApiResponse(Seq(ServiceConfigurationReference("TestEnv", "TestService2")))))
              },
              Method.GET,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$Dependents"),
              StatusCode.Ok.code,
              MultiApiResponse(Seq(ServiceConfigurationReference("TestEnv", "TestService2")))
            )
          },
          test("failure presents InternalServerError") {
            requestCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.getServiceConfigurationDependents("TestEnv", "TestService"))
                  .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
              },
              Method.GET,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$Dependents"),
              StatusCode.InternalServerError.code,
              InternalServerErrorResponse("BAKA")
            )
          }
          /* this endpoint implementation does not recognize 404, it considers service has no dependents */
        ),
        suite("postServiceConfigurationEndpoint")(
          test("auth protected") {
            requestWithPayloadCheckCode(
              ZIO.unit,
              Method.POST,
              makeStringUriV1(s"$Configurations"),
              configuration,
              StatusCode.Unauthorized.code
            )
          },
          test("passes call to controller") {
            requestWithPayloadCheckCode(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.createNewServiceConfiguration(configuration))
                  .thenReturn(ZIO.unit)
              },
              Method.POST,
              makeStringUriV1(s"$Configurations"),
              configuration,
              StatusCode.Created.code,
              Some(apiKey)
            )
          },
          test("failure presents InternalServerError") {
            requestWithPayloadCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.createNewServiceConfiguration(configuration))
                  .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
              },
              Method.POST,
              makeStringUriV1(s"$Configurations"),
              configuration,
              StatusCode.InternalServerError.code,
              InternalServerErrorResponse("BAKA"),
              Some(apiKey)
            )
          },
          test("data conflict presents DataConflictResponse") {
            requestWithPayloadCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.createNewServiceConfiguration(configuration))
                  .thenReturn(ZIO.fail(DataConflictErrorResponse("BAKA")))
              },
              Method.POST,
              makeStringUriV1(s"$Configurations"),
              configuration,
              StatusCode.Conflict.code,
              DataConflictErrorResponse("BAKA"),
              Some(apiKey)
            )
          }
        ),
        suite("putServiceConfigurationEndpoint")(
          test("auth protected") {
            requestWithPayloadCheckCode(
              ZIO.unit,
              Method.PUT,
              makeStringUriV1(s"$Configurations/TestEnv/TestService"),
              configuration,
              StatusCode.Unauthorized.code
            )
          },
          test("passes call to controller") {
            requestWithPayloadCheckCode(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.updateExistingServiceConfiguration("TestEnv", "TestService", configuration))
                  .thenReturn(ZIO.unit)
              },
              Method.PUT,
              makeStringUriV1(s"$Configurations/TestEnv/TestService"),
              configuration,
              StatusCode.Ok.code,
              Some(apiKey)
            )
          },
          test("failure presents InternalServerError") {
            requestWithPayloadCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.updateExistingServiceConfiguration("TestEnv", "TestService", configuration))
                  .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
              },
              Method.PUT,
              makeStringUriV1(s"$Configurations/TestEnv/TestService"),
              configuration,
              StatusCode.InternalServerError.code,
              InternalServerErrorResponse("BAKA"),
              Some(apiKey)
            )
          },
          test("data conflict presents DataConflictResponse") {
            requestWithPayloadCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.updateExistingServiceConfiguration("TestEnv", "TestService", configuration))
                  .thenReturn(ZIO.fail(DataConflictErrorResponse("BAKA")))
              },
              Method.PUT,
              makeStringUriV1(s"$Configurations/TestEnv/TestService"),
              configuration,
              StatusCode.Conflict.code,
              DataConflictErrorResponse("BAKA"),
              Some(apiKey)
            )
          },
          test("missing record presents RecordNotFound") {
            requestWithPayloadCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.updateExistingServiceConfiguration("TestEnv", "TestService", configuration))
                  .thenReturn(ZIO.fail(RecordNotFoundErrorResponse("BAKA")))
              },
              Method.PUT,
              makeStringUriV1(s"$Configurations/TestEnv/TestService"),
              configuration,
              StatusCode.NotFound.code,
              RecordNotFoundErrorResponse("BAKA"),
              Some(apiKey)
            )
          }
        ),
        suite("postServiceConfigurationWithRename")(
          test("auth protected") {
            requestWithPayloadCheckCode(
              ZIO.unit,
              Method.POST,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$NewName"),
              configuration,
              StatusCode.Unauthorized.code
            )
          },
          test("passes call to controller") {
            requestWithPayloadCheckCode(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.renameExistingServiceConfiguration("TestEnv", "TestService", configuration))
                  .thenReturn(ZIO.unit)
              },
              Method.POST,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$NewName"),
              configuration,
              StatusCode.Ok.code,
              Some(apiKey)
            )
          },
          test("failure presents InternalServerError") {
            requestWithPayloadCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.renameExistingServiceConfiguration("TestEnv", "TestService", configuration))
                  .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
              },
              Method.POST,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$NewName"),
              configuration,
              StatusCode.InternalServerError.code,
              InternalServerErrorResponse("BAKA"),
              Some(apiKey)
            )
          },
          test("data conflict presents DataConflictResponse") {
            requestWithPayloadCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.renameExistingServiceConfiguration("TestEnv", "TestService", configuration))
                  .thenReturn(ZIO.fail(DataConflictErrorResponse("BAKA")))
              },
              Method.POST,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$NewName"),
              configuration,
              StatusCode.Conflict.code,
              DataConflictErrorResponse("BAKA"),
              Some(apiKey)
            )
          },
          test("missing record presents RecordNotFound") {
            requestWithPayloadCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.renameExistingServiceConfiguration("TestEnv", "TestService", configuration))
                  .thenReturn(ZIO.fail(RecordNotFoundErrorResponse("BAKA")))
              },
              Method.POST,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$NewName"),
              configuration,
              StatusCode.NotFound.code,
              RecordNotFoundErrorResponse("BAKA"),
              Some(apiKey)
            )
          }
        ),
        suite("putServiceConfigurationMaintenanceMessage")(
          test("auth protected") {
            requestCheckCode(
              ZIO.unit,
              Method.PUT,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$MaintenanceMessage?$MaintenanceMessage=Testing"),
              StatusCode.Unauthorized.code
            )
          },
          test("passes call to controller") {
            requestCheckCode(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.setMaintenanceMessage("TestEnv", "TestService", "Testing"))
                  .thenReturn(ZIO.unit)
              },
              Method.PUT,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$MaintenanceMessage?$MaintenanceMessage=Testing"),
              StatusCode.Ok.code,
              maybeApiCode = Some(apiKey)
            )
          },
          test("failure presents InternalServerError") {
            requestCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.setMaintenanceMessage("TestEnv", "TestService", "Testing"))
                  .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
              },
              Method.PUT,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$MaintenanceMessage?$MaintenanceMessage=Testing"),
              StatusCode.InternalServerError.code,
              InternalServerErrorResponse("BAKA"),
              maybeApiCode = Some(apiKey)
            )
          },
          test("missing record presents RecordNotFound") {
            requestCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.setMaintenanceMessage("TestEnv", "TestService", "Testing"))
                  .thenReturn(ZIO.fail(RecordNotFoundErrorResponse("BAKA")))
              },
              Method.PUT,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$MaintenanceMessage?$MaintenanceMessage=Testing"),
              StatusCode.NotFound.code,
              RecordNotFoundErrorResponse("BAKA"),
              maybeApiCode = Some(apiKey)
            )
          }
        ),
        suite("putServiceConfigurationTemporaryState")(
          test("auth protected") {
            requestCheckCode(
              ZIO.unit,
              Method.PUT,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$TemporaryState?$Status=%22RED(BAKA)%22&$MaintenanceMessage=Testing"),
              StatusCode.Unauthorized.code
            )
          },
          test("passes call to controller") {
            requestCheckCode(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.setTemporaryStatus("TestEnv", "TestService", RawStatus.Red("BAKA", false), "Testing"))
                  .thenReturn(ZIO.unit)
              },
              Method.PUT,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$TemporaryState?$Status=%22RED(BAKA)%22&$MaintenanceMessage=Testing"),
              StatusCode.Ok.code,
              maybeApiCode = Some(apiKey)
            )
          },
          test("failure presents InternalServerError") {
            requestCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.setTemporaryStatus("TestEnv", "TestService", RawStatus.Red("BAKA", false), "Testing"))
                  .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
              },
              Method.PUT,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$TemporaryState?$Status=%22RED(BAKA)%22&$MaintenanceMessage=Testing"),
              StatusCode.InternalServerError.code,
              InternalServerErrorResponse("BAKA"),
              maybeApiCode = Some(apiKey)
            )
          },
          test("missing record presents RecordNotFound") {
            requestCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.setTemporaryStatus("TestEnv", "TestService", RawStatus.Red("BAKA", false), "Testing"))
                  .thenReturn(ZIO.fail(RecordNotFoundErrorResponse("BAKA")))
              },
              Method.PUT,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$TemporaryState?$Status=%22RED(BAKA)%22&$MaintenanceMessage=Testing"),
              StatusCode.NotFound.code,
              RecordNotFoundErrorResponse("BAKA"),
              maybeApiCode = Some(apiKey)
            )
          }
        ),
        suite("postServiceConfigurationRestoreFromTemporaryState")(
          test("auth protected") {
            requestCheckCode(
              ZIO.unit,
              Method.POST,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$Restore"),
              StatusCode.Unauthorized.code
            )
          },
          test("passes call to controller") {
            requestCheckCode(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.restoreFromTemporaryStatus("TestEnv", "TestService"))
                  .thenReturn(ZIO.unit)
              },
              Method.POST,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$Restore"),
              StatusCode.Ok.code,
              maybeApiCode = Some(apiKey)
            )
          },
          test("failure presents InternalServerError") {
            requestCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.restoreFromTemporaryStatus("TestEnv", "TestService"))
                  .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
              },
              Method.POST,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$Restore"),
              StatusCode.InternalServerError.code,
              InternalServerErrorResponse("BAKA"),
              maybeApiCode = Some(apiKey)
            )
          },
          test("missing record presents RecordNotFound") {
            requestCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.restoreFromTemporaryStatus("TestEnv", "TestService"))
                  .thenReturn(ZIO.fail(RecordNotFoundErrorResponse("BAKA")))
              },
              Method.POST,
              makeStringUriV1(s"$Configurations/TestEnv/TestService/$Restore"),
              StatusCode.NotFound.code,
              RecordNotFoundErrorResponse("BAKA"),
              maybeApiCode = Some(apiKey)
            )
          }
        ),
        suite("deleteServiceConfigurationEndpoint")(
          test("auth protected") {
            requestCheckCode(
              ZIO.unit,
              Method.DELETE,
              makeStringUriV1(s"$Configurations/TestEnv/TestService"),
              StatusCode.Unauthorized.code
            )
          },
          test("passes call to controller") {
            requestCheckCode(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.deleteServiceConfiguration("TestEnv", "TestService"))
                  .thenReturn(ZIO.unit)
              },
              Method.DELETE,
              makeStringUriV1(s"$Configurations/TestEnv/TestService"),
              StatusCode.NoContent.code,
              maybeApiCode = Some(apiKey)
            )
          },
          test("failure presents InternalServerError") {
            requestCheckPayload(
              ZIO.attempt {
                when(serviceConfigurationControllerMock.deleteServiceConfiguration("TestEnv", "TestService"))
                  .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
              },
              Method.DELETE,
              makeStringUriV1(s"$Configurations/TestEnv/TestService"),
              StatusCode.InternalServerError.code,
              InternalServerErrorResponse("BAKA"),
              maybeApiCode = Some(apiKey)
            )
          },
        ),
      ),
      suite("Status")(
        suite("getLatestServiceStatusByNameEndpoint")(
          test("passes call to controller") {
            requestCheckPayload(
              ZIO.attempt {
                when(statusControllerMock.getLatestStatus("TestEnv", "TestService"))
                  .thenReturn(ZIO.succeed(SingleApiResponse(refinedStatus)))
              },
              Method.GET,
              makeStringUriV1(s"$Statuses/TestEnv/TestService/latest"),
              StatusCode.Ok.code,
              SingleApiResponse(refinedStatus)
            )
          },
          test("failure presents InternalServerError") {
            requestCheckPayload(
              ZIO.attempt {
                when(statusControllerMock.getLatestStatus("TestEnv", "TestService"))
                  .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
              },
              Method.GET,
              makeStringUriV1(s"$Statuses/TestEnv/TestService/latest"),
              StatusCode.InternalServerError.code,
              InternalServerErrorResponse("BAKA")
            )
          },
          test("missing record presents RecordNotFound") {
            requestCheckPayload(
              ZIO.attempt {
                when(statusControllerMock.getLatestStatus("TestEnv", "TestService"))
                  .thenReturn(ZIO.fail(RecordNotFoundErrorResponse("BAKA")))
              },
              Method.GET,
              makeStringUriV1(s"$Statuses/TestEnv/TestService/latest"),
              StatusCode.NotFound.code,
              RecordNotFoundErrorResponse("BAKA")
            )
          }
        ),
        suite("getServiceStatusHistoryByNameEndpoint")(
          test("passes call to controller") {
            requestCheckPayload(
              ZIO.attempt {
                when(statusControllerMock.getAllStatuses("TestEnv", "TestService"))
                  .thenReturn(ZIO.succeed(MultiApiResponse(List(refinedStatus, refinedStatusAnother))))
              },
              Method.GET,
              makeStringUriV1(s"$Statuses/TestEnv/TestService"),
              StatusCode.Ok.code,
              MultiApiResponse(List(refinedStatus, refinedStatusAnother))
            )
          },
          test("failure presents InternalServerError") {
            requestCheckPayload(
              ZIO.attempt {
                when(statusControllerMock.getAllStatuses("TestEnv", "TestService"))
                  .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
              },
              Method.GET,
              makeStringUriV1(s"$Statuses/TestEnv/TestService"),
              StatusCode.InternalServerError.code,
              InternalServerErrorResponse("BAKA")
            )
          }
        ),
        suite("getLatestStatusOfAllActiveConfigurations")(
          test("passes call to controller") {
            requestCheckPayload(
              ZIO.attempt {
                when(statusControllerMock.getLatestStatusOfAllActiveConfigurations())
                  .thenReturn(ZIO.succeed(MultiApiResponse(List(refinedStatus))))
              },
              Method.GET,
              makeStringUriV1(s"$Statuses"),
              StatusCode.Ok.code,
              MultiApiResponse(List(refinedStatus))
            )
          },
          test("failure presents InternalServerError") {
            requestCheckPayload(
              ZIO.attempt {
                when(statusControllerMock.getLatestStatusOfAllActiveConfigurations())
                  .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
              },
              Method.GET,
              makeStringUriV1(s"$Statuses"),
              StatusCode.InternalServerError.code,
              InternalServerErrorResponse("BAKA")
            )
          }
        ),
      ),
      suite("Monitoring")(
        suite("postRestartMonitoringEndpoint")(
          test("auth protected") {
            requestCheckCode(
              ZIO.unit,
              Method.POST,
              makeStringUriV1(s"$Monitoring/$Restart"),
              StatusCode.Unauthorized.code
            )
          },
          test("passes call to controller") {
            requestCheckCode(
              ZIO.attempt {
                when(monitoringControllerMock.restart)
                  .thenReturn(ZIO.unit)
              },
              Method.POST,
              makeStringUriV1(s"$Monitoring/$Restart"),
              StatusCode.Accepted.code,
              maybeApiCode = Some(apiKey)
            )
          },
          test("failure presents InternalServerError") {
            requestCheckPayload(
              ZIO.attempt {
                when(monitoringControllerMock.restart)
                  .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
              },
              Method.POST,
              makeStringUriV1(s"$Monitoring/$Restart"),
              StatusCode.InternalServerError.code,
              InternalServerErrorResponse("BAKA"),
              maybeApiCode = Some(apiKey)
            )
          }
        ),
        suite("postRestartMonitoringForServiceConfigurationEndpoint")(
          test("auth protected") {
            requestCheckCode(
              ZIO.unit,
              Method.POST,
              makeStringUriV1(s"$Monitoring/$Restart/TestEnv/TestService"),
              StatusCode.Unauthorized.code
            )
          },
          test("passes call to controller") {
            requestCheckCode(
              ZIO.attempt {
                when(monitoringControllerMock.restartForService("TestEnv", "TestService"))
                  .thenReturn(ZIO.unit)
              },
              Method.POST,
              makeStringUriV1(s"$Monitoring/$Restart/TestEnv/TestService"),
              StatusCode.Accepted.code,
              maybeApiCode = Some(apiKey)
            )
          },
          test("failure presents InternalServerError") {
            requestCheckPayload(
              ZIO.attempt {
                when(monitoringControllerMock.restartForService("TestEnv", "TestService"))
                  .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
              },
              Method.POST,
              makeStringUriV1(s"$Monitoring/$Restart/TestEnv/TestService"),
              StatusCode.InternalServerError.code,
              InternalServerErrorResponse("BAKA"),
              maybeApiCode = Some(apiKey)
            )
          }
        )
      )
    ) @@ TestAspect.before(resetMocks) @@ TestAspect.withLiveClock @@ TestAspect.sequential
  }.provide(
    Scope.default,
    RoutesImpl.layer,
    ZLayer.succeed(serviceConfigurationControllerMock),
    ZLayer.succeed(monitoringControllerMock),
    ZLayer.succeed(statusControllerMock),
    ZLayer.succeed(authControllerMock),
    ZLayer.succeed(prometheusPublisherMock)
  )

  private def resetMocks: Task[Unit] = ZIO.attempt {
    reset(serviceConfigurationControllerMock)
    reset(monitoringControllerMock)
    reset(statusControllerMock)
    reset(prometheusPublisherMock)
  }

  private def makeStringUriRoot(path: String): Task[String] = for {
    port <- ZIO.config[ServerConfig](ServerConfig.config).map(_.port)
  } yield s"http://127.0.0.1:$port/$path"

  private def makeStringUriV1(path: String): Task[String] = makeStringUriRoot(s"$Api/$V1/$path")

  private def obtainClientProviderAfterMockedServiceSetup(setupMocks: Task[Unit]) = for {
    _ <- setupMocks
    _ <- Server.start
    httpClient <- BlazeClientBuilder[Task]
      .withExecutionContext(defaultBlockingExecutor.asExecutionContext)
      .resource
      .toScopedZIO
    httpClientProvider <- ZIO.succeed(new BlazeHttpClientProvider(httpClient))
    // Note: server is not always ready after "start" returns, so postpone other tests until healthcheck succeeds
    healthUri <- makeStringUriRoot(Health)
    _ <- httpClientProvider.retrieveStatus(Method.GET,healthUri)
      .map(_.status == org.http4s.Status.Ok)
      .retry(Schedule.recurs(5) && Schedule.exponential(1.second))
  } yield httpClientProvider

  private def performRequest(setupMocks: Task[Unit], method: Method, uriStringZIO: Task[String], includeResponsePayload: Boolean, maybePayload: Option[String], maybeApiCode: Option[String] = None) = for {
    clientProvider <- obtainClientProviderAfterMockedServiceSetup(setupMocks)
    uriString <- uriStringZIO
    maybeHeaders <- maybeApiCode match {
      case Some(apiCode) => ZIO.some(List(Header.Raw(CIString(Authorization), apiCode)))
      case None => ZIO.none
    }
    response <- clientProvider.performRequest(method, uriString, includeResponsePayload, maybePayload, maybeHeaders)
  } yield response

  private def requestCheckCode(setupMocks: Task[Unit], method: Method, uriStringZIO: Task[String], expectedStatusCode: Int, maybePayload: Option[String] = None, maybeApiCode: Option[String] = None) = for {
    response <- performRequest(setupMocks, method, uriStringZIO, includeResponsePayload = false, maybePayload, maybeApiCode).map(_.asInstanceOf[ResponseStatusOnly])
    _ <- assert(response.status.code)(equalTo(expectedStatusCode))
  } yield assertCompletes

  private def requestCheckMessage(setupMocks: Task[Unit], method: Method, uriStringZIO: Task[String], expectedStatusCode: Int, expectedMessage: String, maybePayload: Option[String] = None, maybeApiCode: Option[String] = None) = for {
    response <- performRequest(setupMocks, method, uriStringZIO, includeResponsePayload = true, maybePayload, maybeApiCode).map(_.asInstanceOf[ResponseStatusWithMessage])
    _ <- assert(response.status.code)(equalTo(expectedStatusCode))
    _ <- assert(response.message)(equalTo(expectedMessage))
  } yield assertCompletes

  private def requestCheckPayload[TPayload](setupMocks: Task[Unit], method: Method, uriStringZIO: Task[String], expectedStatusCode: Int, expectedPayload: TPayload, maybePayload: Option[String] = None, maybeApiCode: Option[String] = None)(implicit d: Decoder[TPayload]) = for {
    response <- performRequest(setupMocks, method, uriStringZIO, includeResponsePayload = true, maybePayload, maybeApiCode).map(_.asInstanceOf[ResponseStatusWithMessage])
    _ <- assert(response.status.code)(equalTo(expectedStatusCode))
    messageJson <- ZIO.fromEither(jawn.parse(response.message))
    messagePojo <- ZIO.fromEither(messageJson.as[TPayload])
    _ <- assert(messagePojo)(equalTo(expectedPayload))
  } yield assertCompletes

  private def requestWithPayloadCheckCode[TRequestPayload](setupMocks: Task[Unit], method: Method, uriStringZIO: Task[String], requestPayload: TRequestPayload, expectedStatusCode: Int, maybeApiCode: Option[String] = None)(implicit e: Encoder[TRequestPayload]) =
    requestCheckCode(setupMocks, method, uriStringZIO, expectedStatusCode, Some(requestPayload.asJson.toString), maybeApiCode)

  private def requestWithPayloadCheckPayload[TRequestPayload, TResponsePayload](setupMocks: Task[Unit], method: Method, uriStringZIO: Task[String], requestPayload: TRequestPayload, expectedStatusCode: Int, expectedPayload: TResponsePayload, maybeApiCode: Option[String] = None)(implicit e: Encoder[TRequestPayload], d: Decoder[TResponsePayload]) =
    requestCheckPayload[TResponsePayload](setupMocks, method, uriStringZIO, expectedStatusCode, expectedPayload, Some(requestPayload.asJson.toString), maybeApiCode)
}
