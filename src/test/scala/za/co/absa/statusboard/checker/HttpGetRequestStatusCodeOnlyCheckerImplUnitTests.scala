package za.co.absa.statusboard.checker

import org.http4s.{Method, Status}
import org.mockito.Mockito.{mock, reset, when}
import za.co.absa.statusboard.model.RawStatus.{Green, Red}
import za.co.absa.statusboard.model.StatusCheckAction.HttpGetRequestStatusCodeOnly
import za.co.absa.statusboard.providers.HttpClientProvider
import za.co.absa.statusboard.providers.Response.ResponseStatusOnly
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio._
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestAspect, TestEnvironment, assert}

object HttpGetRequestStatusCodeOnlyCheckerImplUnitTests extends ConfigProviderSpec {
  private val httpClientProviderMock = mock(classOf[HttpClientProvider])
  private val checkRequest = HttpGetRequestStatusCodeOnly(protocol = "TEST", host = "test", port = 0, path = "testing")

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("HttpGetRequestStatusCodeOnlyCheckerImplSuite")(
      test("GREEN on success") {
        for {
          _ <- ZIO.attempt {
            reset(httpClientProviderMock)
            when(httpClientProviderMock.retrieveStatus(Method.GET, "TEST://test:0/testing"))
              .thenReturn(ZIO.succeed(ResponseStatusOnly(Status.Ok)))
          }
          result <- HttpGetRequestStatusCodeOnlyChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Green(s"${Status.Ok.code}: ${Status.Ok.reason}")))
      },
      test("RED on not success") {
        for {
          _ <- ZIO.attempt {
            reset(httpClientProviderMock)
            when(httpClientProviderMock.retrieveStatus(Method.GET, "TEST://test:0/testing"))
              .thenReturn(ZIO.succeed(ResponseStatusOnly(Status.BadRequest)))
          }
          result <- HttpGetRequestStatusCodeOnlyChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Red(s"${Status.BadRequest.code}: ${Status.BadRequest.reason}", intermittent = true)))
      },
      test("RED on fail") {
        for {
          _ <- ZIO.attempt {
            reset(httpClientProviderMock)
            when(httpClientProviderMock.retrieveStatus(Method.GET, "TEST://test:0/testing"))
              .thenReturn(ZIO.fail(new Exception("BAKA")))
          }
          result <- HttpGetRequestStatusCodeOnlyChecker.checkRawStatus(checkRequest)
        } yield assert(result)(equalTo(Red("FAIL: BAKA", intermittent = true)))
      }
    ) @@ TestAspect.sequential
  }.provide(
    HttpGetRequestStatusCodeOnlyCheckerImpl.layer,
    ZLayer.succeed(httpClientProviderMock),
  )
}
