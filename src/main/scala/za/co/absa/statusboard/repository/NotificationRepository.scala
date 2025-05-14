package za.co.absa.statusboard.repository

import za.co.absa.statusboard.model.AppError.DatabaseError
import za.co.absa.statusboard.model.RefinedStatus
import zio._
import zio.macros.accessible

/**
 *  Interface for a repository with status notifications
 */
@accessible
trait NotificationRepository {
  /**
   *  Persists a status notification
   *
   *  @param status The status notification to be persisted
   *  @return A [[zio.IO]] that will either produce Unit if the status notification is successfully persisted,
   *         or a [[za.co.absa.statusboard.model.AppError.DatabaseError]] if an error occurs.
   */
  def persistNotification(status: RefinedStatus): IO[DatabaseError, Unit]

  /**
   *  Renames service in existing records
   *
   *  @param environmentOld Old environment
   *  @param serviceNameOld Old service name
   *  @param environmentNew New environment
   *  @param serviceNameNew New service name
   *  @return A [[zio.IO]] that will either produce Unit if the rename operation is successfull persisted,
   *         or a [[za.co.absa.statusboard.model.AppError.DatabaseError]] if an error occurs.
   */
  def renameService(environmentOld: String, serviceNameOld: String, environmentNew: String, serviceNameNew: String): IO[DatabaseError, Unit]
}
