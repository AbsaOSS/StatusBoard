package za.co.absa.statusboard.utils

import zio._
import zio.test.Assertion.equalTo
import zio.test._

object JinjaLikeTemplatingUnitTests extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("JinjaLikeTemplatingSuite")(
      test("Match replaces properly") {
        for {
          template <- ZIO.succeed( """create table {{ns}}.{{table}}.{{ns}}
                                     |{{ns}}.{{foo}}""".stripMargin)
          vars <- ZIO.succeed(Map("ns" -> "lhd", "table" -> "core", "foo" -> "bar"))
          expected <- ZIO.succeed("""create table lhd.core.lhd
              |lhd.bar""".stripMargin)
          result <- ZIO.succeed(JinjaLikeTemplating.renderTemplate(template, vars))
        } yield assert(result)(equalTo(expected))
      }
    )
  }
}
