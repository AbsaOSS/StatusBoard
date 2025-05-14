package za.co.absa.statusboard.api.http

import org.http4s.HttpRoutes
import zio.Task
import zio.macros.accessible

@accessible
trait Routes {
  val routes: HttpRoutes[Task]
}
