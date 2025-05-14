package za.co.absa.statusboard.checker

import org.http4s.{Method, Status}
import org.mockito.Mockito.{mock, reset, when}
import za.co.absa.statusboard.model.RawStatus.{Amber, Green, Red}
import za.co.absa.statusboard.model.StatusCheckAction.HttpGetRequestWithJsonStatus
import za.co.absa.statusboard.providers.Response.ResponseStatusWithMessage
import za.co.absa.statusboard.providers.HttpClientProvider
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio._
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestAspect, TestEnvironment, assert}

object HttpGetRequestWithJsonStatusCheckerImplUnitTests extends ConfigProviderSpec {
  private val httpClientProviderMock = mock(classOf[HttpClientProvider])
  private val checkRequest = HttpGetRequestWithJsonStatus(
    protocol = "TEST",
    host = "test",
    port = 0,
    path = "testing",
    jsonStatusPath = Seq("status"),
    greenRegex = "GREEN",
    amberRegex = "AMBER")

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("HttpGetRequestWithJsonStatusCheckerImplSuite")(
      test("GREEN on success × JSON_GREEN") {
        for {
          _ <- ZIO.attempt {
            reset(httpClientProviderMock)
            when(httpClientProviderMock.retrieveMessage(Method.GET, "TEST://test:0/testing"))
              .thenReturn(ZIO.succeed(ResponseStatusWithMessage(Status.Ok, "{\"status\": \"GREEN\"}")))
          }
          result <- HttpGetRequestWithJsonStatusChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Green(s"${Status.Ok.code}: GREEN")))
      },
      test("AMBER on success × JSON_AMBER") {
        for {
          _ <- ZIO.attempt {
            reset(httpClientProviderMock)
            when(httpClientProviderMock.retrieveMessage(Method.GET, "TEST://test:0/testing"))
              .thenReturn(ZIO.succeed(ResponseStatusWithMessage(Status.Ok, "{\"status\": \"AMBER\"}")))
          }
          result <- HttpGetRequestWithJsonStatusChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Amber(s"${Status.Ok.code}: AMBER", intermittent = false)))
      },
      test("RED on success × JSON_RED") {
        for {
          _ <- ZIO.attempt {
            reset(httpClientProviderMock)
            when(httpClientProviderMock.retrieveMessage(Method.GET, "TEST://test:0/testing"))
              .thenReturn(ZIO.succeed(ResponseStatusWithMessage(Status.Ok, "{\"status\": \"RED\"}")))
          }
          result <- HttpGetRequestWithJsonStatusChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Red(s"${Status.Ok.code}: RED", intermittent = false)))
      },
      test("GREEN on not success × JSON_GREEN - ignoring HttpStatusCode #175") {
        for {
          _ <- ZIO.attempt {
            reset(httpClientProviderMock)
            when(httpClientProviderMock.retrieveMessage(Method.GET, "TEST://test:0/testing"))
              .thenReturn(ZIO.succeed(ResponseStatusWithMessage(Status.BadRequest, "{\"status\": \"GREEN\"}")))
          }
          result <- HttpGetRequestWithJsonStatusChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Green(s"${Status.BadRequest.code}: GREEN")))
      },
      test("AMBER on not success × JSON_AMBER") {
        for {
          _ <- ZIO.attempt {
            reset(httpClientProviderMock)
            when(httpClientProviderMock.retrieveMessage(Method.GET, "TEST://test:0/testing"))
              .thenReturn(ZIO.succeed(ResponseStatusWithMessage(Status.BadRequest, "{\"status\": \"AMBER\"}")))
          }
          result <- HttpGetRequestWithJsonStatusChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Amber(s"${Status.BadRequest.code}: AMBER", intermittent = false)))
      },
      test("RED on not success × JSON_RED") {
        for {
          _ <- ZIO.attempt {
            reset(httpClientProviderMock)
            when(httpClientProviderMock.retrieveMessage(Method.GET, "TEST://test:0/testing"))
              .thenReturn(ZIO.succeed(ResponseStatusWithMessage(Status.BadRequest, "{\"status\": \"RED\"}")))
          }
          result <- HttpGetRequestWithJsonStatusChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Red(s"${Status.BadRequest.code}: RED", intermittent = false)))
      },
      test("RED on not success × NO_JSON") {
        for {
          _ <- ZIO.attempt {
            reset(httpClientProviderMock)
            when(httpClientProviderMock.retrieveMessage(Method.GET, "TEST://test:0/testing"))
              .thenReturn(ZIO.succeed(ResponseStatusWithMessage(Status.BadRequest, "BAKA")))
          }
          result <- HttpGetRequestWithJsonStatusChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Red(s"${Status.BadRequest.code}: BAKA", intermittent = true)))
      },
      test("RED on failure") {
        for {
          _ <- ZIO.attempt {
            reset(httpClientProviderMock)
            when(httpClientProviderMock.retrieveMessage(Method.GET, "TEST://test:0/testing"))
              .thenReturn(ZIO.fail(new Exception("FUBAR")))
          }
          result <- HttpGetRequestWithJsonStatusChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Red(s"FAIL: FUBAR", intermittent = true)))
      }
    ) @@ TestAspect.sequential
  }.provide(
    HttpGetRequestWithJsonStatusCheckerImpl.layer,
    ZLayer.succeed(httpClientProviderMock),
  )
}
