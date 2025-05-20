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
