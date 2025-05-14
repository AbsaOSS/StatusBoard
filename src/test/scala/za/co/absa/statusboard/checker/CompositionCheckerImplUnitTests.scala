package za.co.absa.statusboard.checker

import org.mockito.Mockito.{mock, when}
import za.co.absa.statusboard.model.AppError.DatabaseError.RecordNotFoundDatabaseError
import za.co.absa.statusboard.model.ServiceConfigurationReference
import za.co.absa.statusboard.model.RawStatus.{Amber, Green, Red}
import za.co.absa.statusboard.model.StatusCheckAction.CompositionItem.{Direct, Partial}
import za.co.absa.statusboard.model.StatusCheckAction.{Composition, CompositionItem}
import za.co.absa.statusboard.repository.StatusRepository
import za.co.absa.statusboard.testUtils.{ConfigProviderSpec, TestData}
import zio._
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestEnvironment, assert}

object CompositionCheckerImplUnitTests extends ConfigProviderSpec {
  private val statusRepositoryMock = mock(classOf[StatusRepository])
  when(statusRepositoryMock.getLatestStatus("TEST", "GREEN"))
    .thenReturn(ZIO.succeed(TestData.refinedStatus.copy(status = TestData.rawStatusGreen)))
  when(statusRepositoryMock.getLatestStatus("TEST", "AMBER_INT"))
    .thenReturn(ZIO.succeed(TestData.refinedStatus.copy(status = TestData.rawStatusAmberIntermittent)))
  when(statusRepositoryMock.getLatestStatus("TEST", "AMBER"))
    .thenReturn(ZIO.succeed(TestData.refinedStatus.copy(status = TestData.rawStatusAmber)))
  when(statusRepositoryMock.getLatestStatus("TEST", "RED_INT"))
    .thenReturn(ZIO.succeed(TestData.refinedStatus.copy(status = TestData.rawStatusRedIntermittent)))
  when(statusRepositoryMock.getLatestStatus("TEST", "RED"))
    .thenReturn(ZIO.succeed(TestData.refinedStatus.copy(status = TestData.rawStatusRed)))
  when(statusRepositoryMock.getLatestStatus("TEST", "404"))
    .thenReturn(ZIO.fail(RecordNotFoundDatabaseError("BAKA")))
  private val expectedGreen = Green("OK")
  private val expectedAmberIntermittent = Amber("PARTIAL", intermittent = true)
  private val expectedAmber = Amber("PARTIAL", intermittent = false)
  private val expectedRedIntermittent = Red("NOT OK", intermittent = true)
  private val expectedRed = Red("NOT OK", intermittent = false)

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("CompositionCheckerImplSuite")(
      suite("General")(
        test("GREEN on no requirements") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(Green("OK")))
        }
      ),
      suite("Partial composition items")(
        test("GREEN on partially fulfilled") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq(
                Partial(
                  Seq(
                    Direct(ServiceConfigurationReference("TEST", "GREEN")),
                    Direct(ServiceConfigurationReference("TEST", "GREEN")),
                    Direct(ServiceConfigurationReference("TEST", "RED_INT")),
                    Direct(ServiceConfigurationReference("TEST", "RED"))
                  ),
                  2
                )
              )
            ))
          } yield assert(result)(equalTo(expectedGreen))
        },
        test("RED_INT on not partially fulfilled with intermittent failure") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq(
                Partial(
                  Seq(
                    Direct(ServiceConfigurationReference("TEST", "GREEN")),
                    Direct(ServiceConfigurationReference("TEST", "RED")),
                    Direct(ServiceConfigurationReference("TEST", "RED_INT")),
                    Direct(ServiceConfigurationReference("TEST", "RED"))
                  ),
                  2
                )
              )
            ))
          } yield assert(result)(equalTo(expectedRedIntermittent))
        },
        test("RED on not partially fulfilled without intermittent failure") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq(
                Partial(
                  Seq(
                    Direct(ServiceConfigurationReference("TEST", "GREEN")),
                    Direct(ServiceConfigurationReference("TEST", "RED")),
                    Direct(ServiceConfigurationReference("TEST", "RED"))
                  ),
                  2
                )
              )
            ))
          } yield assert(result)(equalTo(expectedRed))
        }
      ),
      suite("greenRequiredForGreen")(
        test("GREEN on GREEN") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN"))),
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedGreen))
        },
        test("AMBER_INT on AMBER_INT") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "AMBER_INT"))),
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedAmberIntermittent))
        },
        test("AMBER_INT on AMBER_INT with better") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN")), Direct(ServiceConfigurationReference("TEST", "AMBER_INT"))),
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedAmberIntermittent))
        },
        test("AMBER_INT on RED_INT") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "RED_INT"))),
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedAmberIntermittent))
        },
        test("AMBER_INT on RED_INT with better") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN")), Direct(ServiceConfigurationReference("TEST", "AMBER_INT")), Direct(ServiceConfigurationReference("TEST", "RED_INT"))),
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedAmberIntermittent))
        },
        test("AMBER on AMBER") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "AMBER"))),
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedAmber))
        },
        test("AMBER on RED") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "RED"))),
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedAmber))
        },
        test("AMBER on RED with better") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN")), Direct(ServiceConfigurationReference("TEST", "AMBER_INT")), Direct(ServiceConfigurationReference("TEST", "AMBER")), Direct(ServiceConfigurationReference("TEST", "RED_INT")), Direct(ServiceConfigurationReference("TEST", "RED"))),
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedAmber))
        },
        test("AMBER_INT on FAILING") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "404"))),
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedAmberIntermittent))
        },
        test("AMBER_INT on FAILING with better") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN")), Direct(ServiceConfigurationReference("TEST", "404"))),
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedAmberIntermittent))
        }
      ),
      suite("amberRequiredForGreen")(
        test("GREEN on GREEN") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN"))),
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedGreen))
        },
        test("GREEN on AMBER_INT") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "AMBER_INT"))),
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedGreen))
        },
        test("GREEN on AMBER") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "AMBER"))),
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedGreen))
        },
        test("GREEN on AMBER with better") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN")), Direct(ServiceConfigurationReference("TEST", "AMBER_INT")), Direct(ServiceConfigurationReference("TEST", "AMBER"))),
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedGreen))
        },
        test("AMBER_INT on RED_INT") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "RED_INT"))),
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedAmberIntermittent))
        },
        test("AMBER_INT on RED_INT with better") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN")), Direct(ServiceConfigurationReference("TEST", "AMBER_INT")), Direct(ServiceConfigurationReference("TEST", "AMBER")), Direct(ServiceConfigurationReference("TEST", "RED_INT"))),
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedAmberIntermittent))
        },
        test("AMBER on RED") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "RED"))),
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedAmber))
        },
        test("AMBER on RED with better") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN")), Direct(ServiceConfigurationReference("TEST", "AMBER_INT")), Direct(ServiceConfigurationReference("TEST", "AMBER")), Direct(ServiceConfigurationReference("TEST", "RED_INT")), Direct(ServiceConfigurationReference("TEST", "RED"))),
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedAmber))
        },
        test("AMBER on any failure present") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "RED"))),
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedAmber))
        },
        test("AMBER_INT on FAILING") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "404"))),
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedAmberIntermittent))
        },
        test("AMBER_INT on FAILING with better") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN")), Direct(ServiceConfigurationReference("TEST", "404"))),
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedAmberIntermittent))
        }
      ),
      suite("greenRequiredForAmber")(
        test("GREEN on GREEN") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN"))),
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedGreen))
        },
        test("RED_INT on AMBER_INT") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "AMBER_INT"))),
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedRedIntermittent))
        },
        test("RED_INT on AMBER_INT with better") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN")), Direct(ServiceConfigurationReference("TEST", "AMBER_INT"))),
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedRedIntermittent))
        },
        test("RED_INT on RED_INT") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "RED_INT"))),
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedRedIntermittent))
        },
        test("RED_INT on RED_INT with better") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN")), Direct(ServiceConfigurationReference("TEST", "AMBER_INT")), Direct(ServiceConfigurationReference("TEST", "RED_INT"))),
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedRedIntermittent))
        },
        test("RED on AMBER") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "AMBER"))),
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedRed))
        },
        test("RED on AMBER with better") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN")), Direct(ServiceConfigurationReference("TEST", "AMBER_INT")), Direct(ServiceConfigurationReference("TEST", "AMBER"))),
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedRed))
        },
        test("RED on RED") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "RED"))),
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedRed))
        },
        test("RED on RED with better") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN")), Direct(ServiceConfigurationReference("TEST", "AMBER_INT")), Direct(ServiceConfigurationReference("TEST", "AMBER")), Direct(ServiceConfigurationReference("TEST", "RED_INT")), Direct(ServiceConfigurationReference("TEST", "RED"))),
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedRed))
        },
        test("RED_INT on FAILING") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "404"))),
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedRedIntermittent))
        },
        test("RED_INT on FAILING with better") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN")), Direct(ServiceConfigurationReference("TEST", "404"))),
              amberRequiredForAmber = Seq.empty[CompositionItem]
            ))
          } yield assert(result)(equalTo(expectedRedIntermittent))
        }
      ),
      suite("amberRequiredForAmber")(
        test("GREEN on GREEN") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN")))
            ))
          } yield assert(result)(equalTo(expectedGreen))
        },
        test("GREEN on AMBER_INT") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "AMBER_INT")))
            ))
          } yield assert(result)(equalTo(expectedGreen))
        },
        test("GREEN on AMBER") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "AMBER")))
            ))
          } yield assert(result)(equalTo(expectedGreen))
        },
        test("GREEN on AMBER with better") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN")), Direct(ServiceConfigurationReference("TEST", "AMBER_INT")), Direct(ServiceConfigurationReference("TEST", "AMBER")))
            ))
          } yield assert(result)(equalTo(expectedGreen))
        },
        test("RED_INT on RED_INT") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "RED_INT")))
            ))
          } yield assert(result)(equalTo(expectedRedIntermittent))
        },
        test("RED_INT on RED_INT with better") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN")), Direct(ServiceConfigurationReference("TEST", "AMBER_INT")), Direct(ServiceConfigurationReference("TEST", "AMBER")), Direct(ServiceConfigurationReference("TEST", "RED_INT")))
            ))
          } yield assert(result)(equalTo(expectedRedIntermittent))
        },
        test("RED on RED") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "RED")))
            ))
          } yield assert(result)(equalTo(expectedRed))
        },
        test("RED on RED with better") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN")), Direct(ServiceConfigurationReference("TEST", "AMBER_INT")), Direct(ServiceConfigurationReference("TEST", "AMBER")), Direct(ServiceConfigurationReference("TEST", "RED_INT")), Direct(ServiceConfigurationReference("TEST", "RED")))
            ))
          } yield assert(result)(equalTo(expectedRed))
        },
        test("RED_INT on FAILING") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "404")))
            ))
          } yield assert(result)(equalTo(expectedRedIntermittent))
        },
        test("RED_INT on FAILING with better") {
          for {
            result <- CompositionChecker.checkRawStatus(Composition(
              greenRequiredForGreen = Seq.empty[CompositionItem],
              amberRequiredForGreen = Seq.empty[CompositionItem],
              greenRequiredForAmber = Seq.empty[CompositionItem],
              amberRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TEST", "GREEN")), Direct(ServiceConfigurationReference("TEST", "404")))
            ))
          } yield assert(result)(equalTo(expectedRedIntermittent))
        }
      )
    )
  }.provide(
    CompositionCheckerImpl.layer,
    ZLayer.succeed(statusRepositoryMock),
  )
}
