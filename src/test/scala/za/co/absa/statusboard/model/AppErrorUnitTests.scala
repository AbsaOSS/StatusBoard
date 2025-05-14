package za.co.absa.statusboard.model

import za.co.absa.statusboard.model.AppError.DatabaseError
import za.co.absa.statusboard.model.AppError.DatabaseError._
import zio._
import zio.test.Assertion.isUnit
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue, assertZIO}

object AppErrorUnitTests extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("AppErrorSuite")(
      test("Exception message for AppError::DatabaseError::DataConflictDatabaseError is properly populated") {
        assertZIO(for {
          error <- ZIO.succeed(DataConflictDatabaseError("BAKA"))
          errorAsDatabaseError <- ZIO.succeed(error: DatabaseError)
          errorAsAppError <- ZIO.succeed(error: AppError)
          errorAsException <- ZIO.succeed(error: Exception)
          _ <- assertTrue(error.message == "BAKA")
          _ <- assertTrue(errorAsDatabaseError.message == "BAKA")
          _ <- assertTrue(errorAsAppError.message == "BAKA")
          _ <- assertTrue(errorAsException.getMessage == "BAKA")
          _ <- assertTrue(errorAsException.toString.contains("BAKA"))
        } yield ())(isUnit)
      },
      test("Exception message for AppError::DatabaseError::DataConflictDatabaseError is properly populated") {
        assertZIO(for {
          error <- ZIO.succeed(GeneralDatabaseError("BAKA"))
          errorAsDatabaseError <- ZIO.succeed(error: DatabaseError)
          errorAsAppError <- ZIO.succeed(error: AppError)
          errorAsException <- ZIO.succeed(error: Exception)
          _ <- assertTrue(error.message == "BAKA")
          _ <- assertTrue(errorAsDatabaseError.message == "BAKA")
          _ <- assertTrue(errorAsAppError.message == "BAKA")
          _ <- assertTrue(errorAsException.getMessage == "BAKA")
          _ <- assertTrue(errorAsException.toString.contains("BAKA"))
        } yield ())(isUnit)
      },
      test("Exception message for AppError::DatabaseError::DataConflictDatabaseError is properly populated") {
        assertZIO(for {
          error <- ZIO.succeed(RecordNotFoundDatabaseError("BAKA"))
          errorAsDatabaseError <- ZIO.succeed(error: DatabaseError)
          errorAsAppError <- ZIO.succeed(error: AppError)
          errorAsException <- ZIO.succeed(error: Exception)
          _ <- assertTrue(error.message == "BAKA")
          _ <- assertTrue(errorAsDatabaseError.message == "BAKA")
          _ <- assertTrue(errorAsAppError.message == "BAKA")
          _ <- assertTrue(errorAsException.getMessage == "BAKA")
          _ <- assertTrue(errorAsException.toString.contains("BAKA"))
        } yield ())(isUnit)
      }
    )
  }
}
