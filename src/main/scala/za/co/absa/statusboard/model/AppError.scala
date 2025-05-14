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
