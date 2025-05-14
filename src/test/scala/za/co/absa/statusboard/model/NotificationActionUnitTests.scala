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

import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import za.co.absa.statusboard.model.NotificationAction.ColorLabels
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestEnvironment, assert}
import zio.{Scope, ZIO}

object NotificationActionUnitTests extends ConfigProviderSpec {
  private val emailDefault: NotificationAction = NotificationAction.EMail(Seq("mail.no-one.test", "also.no-one.test"))
  private val emailDefaultJson =
    """{
      |  "EMail" : {
      |    "addresses" : [
      |      "mail.no-one.test",
      |      "also.no-one.test"
      |    ]
      |  }
      |}""".stripMargin
  private val emailCustomSubjectMessage: NotificationAction = NotificationAction.EMail(
    Seq("mail.no-one.test", "also.no-one.test"),
    Some("CustomTitle"),
    Some(Seq("CustomBody1", "CustomBody2"))
  )
  private val emailCustomSubjectMessageJson =
    """{
      |  "EMail" : {
      |    "addresses" : [
      |      "mail.no-one.test",
      |      "also.no-one.test"
      |    ],
      |    "subject" : "CustomTitle",
      |    "body" : [
      |      "CustomBody1",
      |      "CustomBody2"
      |    ]
      |  }
      |}""".stripMargin
  private val emailCustomColorLabels: NotificationAction = NotificationAction.EMail(
    Seq("mail.no-one.test", "also.no-one.test"),
    colorLabels = Some(ColorLabels("RedLbl", "AmberLbl", "GreenLbl", "BlackLbl"))
  )
  private val emailCustomColorLabelsJson =
    """{
      |  "EMail" : {
      |    "addresses" : [
      |      "mail.no-one.test",
      |      "also.no-one.test"
      |    ],
      |    "colorLabels" : {
      |      "red" : "RedLbl",
      |      "amber" : "AmberLbl",
      |      "green" : "GreenLbl",
      |      "black" : "BlackLbl"
      |    }
      |  }
      |}""".stripMargin
  private val msTeams: NotificationAction = NotificationAction.MSTeams()
  private val msTeamsJson =
    """{
      |  "MSTeams" : {
      |    💩
      |  }
      |}""".stripMargin.replace("💩", "") // IntelliJ strips trailing spaces even in verbatim string
  private val repository: NotificationAction = NotificationAction.Repository()
  private val repositoryJson =
    """{
      |  "Repository" : {
      |    💩
      |  }
      |}""".stripMargin.replace("💩", "") // IntelliJ strips trailing spaces even in verbatim string

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("ServiceConfigurationSuite")(
      suite("E-Mail")(
        test("JSON serialization default") {
          for {
            serialized <- ZIO.succeed(emailDefault.asJson)
          } yield assert(serialized.toString)(equalTo(emailDefaultJson))
        },
        test("JSON deserialization default") {
          for {
            jsonObject <- ZIO.fromEither(parse(emailDefaultJson))
            deserialized <- ZIO.fromEither(jsonObject.as[NotificationAction])
          } yield assert(deserialized)(equalTo(emailDefault))
        },
        test("JSON serialization custom subject/message") {
          for {
            serialized <- ZIO.succeed(emailCustomSubjectMessage.asJson)
          } yield assert(serialized.toString)(equalTo(emailCustomSubjectMessageJson))
        },
        test("JSON deserialization custom subject/message") {
          for {
            jsonObject <- ZIO.fromEither(parse(emailCustomSubjectMessageJson))
            deserialized <- ZIO.fromEither(jsonObject.as[NotificationAction])
          } yield assert(deserialized)(equalTo(emailCustomSubjectMessage))
        },
        test("JSON serialization custom color labels") {
          for {
            serialized <- ZIO.succeed(emailCustomColorLabels.asJson)
          } yield assert(serialized.toString)(equalTo(emailCustomColorLabelsJson))
        },
        test("JSON deserialization custom color labels") {
          for {
            jsonObject <- ZIO.fromEither(parse(emailCustomColorLabelsJson))
            deserialized <- ZIO.fromEither(jsonObject.as[NotificationAction])
          } yield assert(deserialized)(equalTo(emailCustomColorLabels))
        }
      ),
      suite("MS Teams")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(msTeams.asJson)
          } yield assert(serialized.toString)(equalTo(msTeamsJson))
        },
        test("JSON deserialization") {
          for {
            jsonObject <- ZIO.fromEither(parse(msTeamsJson))
            deserialized <- ZIO.fromEither(jsonObject.as[NotificationAction])
          } yield assert(deserialized)(equalTo(msTeams))
        }
      ),
      suite("Repository")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(repository.asJson)
          } yield assert(serialized.toString)(equalTo(repositoryJson))
        },
        test("JSON deserialization") {
          for {
            jsonObject <- ZIO.fromEither(parse(repositoryJson))
            deserialized <- ZIO.fromEither(jsonObject.as[NotificationAction])
          } yield assert(deserialized)(equalTo(repository))
        }
      )
    )
  }
}
