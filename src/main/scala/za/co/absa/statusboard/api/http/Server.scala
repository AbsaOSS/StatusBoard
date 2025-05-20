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

import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.server.Router
import za.co.absa.statusboard.config.ServerConfig
import zio._
import zio.interop.catz._

object Server {
  def start: ZIO[Routes, Throwable, Fiber.Runtime[Throwable, Unit]] = for {
    config <- ZIO.config[ServerConfig](ServerConfig.config)
    executor <- ZIO.executor
    routes <- ZIO.serviceWith[Routes](_.routes)
    corsRoutes <- ZIO.attempt {
      CORS.policy
        .withAllowOriginAll
        .withAllowCredentials(false)
        .apply(routes)
    }
    baseBuilder <- ZIO.attempt {
      BlazeServerBuilder[Task]
        .bindHttp(config.port, "0.0.0.0")
        .withExecutionContext(executor.asExecutionContext)
        .withHttpApp(Router("/" -> corsRoutes).orNotFound)
    }
    liveFiber <- baseBuilder.serve.compile.drain.fork
  } yield liveFiber
}
