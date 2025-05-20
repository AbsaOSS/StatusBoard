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

package za.co.absa.statusboard.checker

import org.http4s.{Method, Status}
import org.mockito.Mockito.{mock, reset, when}
import za.co.absa.statusboard.model.RawStatus.{Green, Red}
import za.co.absa.statusboard.model.StatusCheckAction.HttpGetRequestWithMessage
import za.co.absa.statusboard.providers.Response.ResponseStatusWithMessage
import za.co.absa.statusboard.providers.HttpClientProvider
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio._
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestAspect, TestEnvironment, assert}

object HttpGetRequestWithMessageCheckerImplUnitTests extends ConfigProviderSpec {
  private val httpClientProviderMock = mock(classOf[HttpClientProvider])
  private val checkRequest = HttpGetRequestWithMessage(protocol = "TEST", host = "test", port = 0, path = "testing")

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("HttpGetRequestWithMessageCheckerImplSuite")(
      test("GREEN on success") {
        for {
          _ <- ZIO.attempt {
            reset(httpClientProviderMock)
            when(httpClientProviderMock.retrieveMessage(Method.GET, "TEST://test:0/testing"))
              .thenReturn(ZIO.succeed(ResponseStatusWithMessage(Status.Ok, "GOOD")))
          }
          result <- HttpGetRequestWithMessageChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Green(s"${Status.Ok.code}: GOOD")))
      },
      test("RED on not success") {
        for {
          _ <- ZIO.attempt {
            reset(httpClientProviderMock)
            when(httpClientProviderMock.retrieveMessage(Method.GET, "TEST://test:0/testing"))
              .thenReturn(ZIO.succeed(ResponseStatusWithMessage(Status.BadRequest, "NOT OK")))
          }
          result <- HttpGetRequestWithMessageChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Red(s"${Status.BadRequest.code}: NOT OK", intermittent = true)))
      },
      test("RED on fail") {
        for {
          _ <- ZIO.attempt {
            reset(httpClientProviderMock)
            when(httpClientProviderMock.retrieveMessage(Method.GET, "TEST://test:0/testing"))
              .thenReturn(ZIO.fail(new Exception("BAKA")))
          }
          result <- HttpGetRequestWithMessageChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Red("FAIL: BAKA", intermittent = true)))
      }
    ) @@ TestAspect.sequential
  }.provide(
    HttpGetRequestWithMessageCheckerImpl.layer,
    ZLayer.succeed(httpClientProviderMock),
  )
}
