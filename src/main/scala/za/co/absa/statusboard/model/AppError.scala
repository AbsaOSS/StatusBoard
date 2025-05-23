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

sealed trait AppError extends Exception {
  val message: String
}

object AppError {
  sealed trait DatabaseError extends AppError

  object DatabaseError {
    final case class DataConflictDatabaseError(message: String) extends Exception(message) with DatabaseError
    final case class RecordNotFoundDatabaseError(message: String) extends Exception(message) with DatabaseError
    final case class GeneralDatabaseError(message: String) extends Exception(message) with DatabaseError
  }
}
