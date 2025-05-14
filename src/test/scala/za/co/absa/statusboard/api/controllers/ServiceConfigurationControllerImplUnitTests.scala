package za.co.absa.statusboard.api.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, _}
import za.co.absa.statusboard.model.ApiResponse.{MultiApiResponse, SingleApiResponse}
import za.co.absa.statusboard.model.AppError.DatabaseError.{DataConflictDatabaseError, GeneralDatabaseError, RecordNotFoundDatabaseError}
import za.co.absa.statusboard.model.ErrorResponse.{DataConflictErrorResponse, InternalServerErrorResponse, RecordNotFoundErrorResponse}
import za.co.absa.statusboard.model.RawStatus.Black
import za.co.absa.statusboard.model.StatusCheckAction.{Composition, CompositionItem, TemporaryFixedStatus}
import za.co.absa.statusboard.model.StatusCheckAction.CompositionItem.{Direct, Partial}
import za.co.absa.statusboard.model.{NotificationAction, RefinedStatus, ServiceConfiguration, ServiceConfigurationReference}
import za.co.absa.statusboard.repository.{NotificationRepository, ServiceConfigurationRepository, StatusRepository}
import za.co.absa.statusboard.testUtils.{ConfigProviderSpec, TestData}
import zio._
import zio.test.Assertion.{equalTo, failsWithA, hasSameElements}
import zio.test._

import scala.collection.mutable.ListBuffer

object ServiceConfigurationControllerImplUnitTests extends ConfigProviderSpec {
  private val serviceConfigurationsRepositoryMock = mock(classOf[ServiceConfigurationRepository])
  private val statusRepositoryMock = mock(classOf[StatusRepository])
  private val notificationRepositoryMock = mock(classOf[NotificationRepository])
  private val monitoringControllerMock = mock(classOf[MonitoringController])

  private def resetMocks: Task[Unit] = ZIO.attempt {
    reset(serviceConfigurationsRepositoryMock)
    reset(statusRepositoryMock)
    reset(monitoringControllerMock)
  }

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("ServiceConfigurationControllerImplSuite")(
      suite("getServiceConfigurations")(
        test("return repository configurations - default") {
          for {
            // Arrange
            visibleConfiguration <- ZIO.succeed(TestData.serviceConfiguration.copy(env="Test", name="TestVisible", hidden=false))
            hiddenConfiguration <- ZIO.succeed(TestData.serviceConfiguration.copy(env="Test", name="TestHidden", hidden=true))
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfigurations)
                .thenReturn(ZIO.succeed(Seq(visibleConfiguration, hiddenConfiguration)))
            }
            // Act
            result <- ServiceConfigurationController.getServiceConfigurations(None)
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(1)).getServiceConfigurations)
          } yield assert(result.records)(hasSameElements(Seq(visibleConfiguration, hiddenConfiguration)))
        },
        test("return repository configurations - explicit all") {
          for {
            // Arrange
            visibleConfiguration <- ZIO.succeed(TestData.serviceConfiguration.copy(env="Test", name="TestVisible", hidden=false))
            hiddenConfiguration <- ZIO.succeed(TestData.serviceConfiguration.copy(env="Test", name="TestHidden", hidden=true))
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfigurations)
                .thenReturn(ZIO.succeed(Seq(visibleConfiguration, hiddenConfiguration)))
            }
            // Act
            result <- ServiceConfigurationController.getServiceConfigurations(Some(true))
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(1)).getServiceConfigurations)
          } yield assert(result.records)(hasSameElements(Seq(visibleConfiguration, hiddenConfiguration)))
        },
        test("return repository configurations - explicit only visible") {
          for {
            // Arrange
            visibleConfiguration <- ZIO.succeed(TestData.serviceConfiguration.copy(env="Test", name="TestVisible", hidden=false))
            hiddenConfiguration <- ZIO.succeed(TestData.serviceConfiguration.copy(env="Test", name="TestHidden", hidden=true))
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfigurations)
                .thenReturn(ZIO.succeed(Seq(visibleConfiguration, hiddenConfiguration)))
            }
            // Act
            result <- ServiceConfigurationController.getServiceConfigurations(Some(false))
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(1)).getServiceConfigurations)
          } yield assert(result.records)(hasSameElements(Seq(visibleConfiguration)))
        },
        test("failure presents InternalServerError") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfigurations)
                .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.getServiceConfigurations(None).exit
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(1)).getServiceConfigurations)
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        }
      ),
      suite("getServiceConfiguration")(
        test("passes call to repository") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration("TestEnv", "TestService"))
                .thenReturn(ZIO.succeed(TestData.serviceConfiguration))
            }
            // Act
            result <- ServiceConfigurationController.getServiceConfiguration("TestEnv", "TestService")
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(1)).getServiceConfiguration("TestEnv", "TestService"))
          } yield assert(result)(equalTo(SingleApiResponse(TestData.serviceConfiguration)))
        },
        test("failure presents InternalServerError") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration("TestEnv", "TestService"))
                .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.getServiceConfiguration("TestEnv", "TestService").exit
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(1)).getServiceConfiguration("TestEnv", "TestService"))
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        },
        test("not found presents NotFoundResponse") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration("TestEnv", "TestService"))
                .thenReturn(ZIO.fail(RecordNotFoundDatabaseError("NotPresent")))
            }
            // Act
            failure <- ServiceConfigurationController.getServiceConfiguration("TestEnv", "TestService").exit
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(1)).getServiceConfiguration("TestEnv", "TestService"))
          } yield assert(failure)(failsWithA[RecordNotFoundErrorResponse])
        }
      ),
      suite("getServiceConfigurationDependencies")(
        test("flat configuration has no dependencies") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration("TestEnv", "TestService"))
                .thenReturn(ZIO.succeed(TestData.serviceConfiguration))
            }
            // Act
            result <- ServiceConfigurationController.getServiceConfigurationDependencies("TestEnv", "TestService")
            // Assert
          } yield assert(result)(equalTo(MultiApiResponse(Seq.empty[ServiceConfigurationReference])))
        },
        test("composition has dependencies") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration("TestEnv", "TestService"))
                .thenReturn(ZIO.succeed {
                  TestData.serviceConfiguration.copy(
                    statusCheckAction = Composition(
                      greenRequiredForGreen = Seq(
                        Direct(ServiceConfigurationReference("G4GEnv", "G4GService")),
                        Direct(ServiceConfigurationReference("CommonEnv", "CommonService")),
                        Partial(
                          Seq(
                            Direct(ServiceConfigurationReference("NestedG4GEnv", "NestedG4GService")),
                            Direct(ServiceConfigurationReference("NestedCommonEnv", "NestedCommonService")),
                          ),
                          1
                        )
                      ),
                      amberRequiredForGreen = Seq(
                        Direct(ServiceConfigurationReference("A4GEnv", "A4GService")),
                        Direct(ServiceConfigurationReference("CommonEnv", "CommonService")),
                        Partial(
                          Seq(
                            Direct(ServiceConfigurationReference("NestedA4GEnv", "NestedA4GService")),
                            Direct(ServiceConfigurationReference("NestedCommonEnv", "NestedCommonService")),
                          ),
                          1
                        )
                      ),
                      greenRequiredForAmber = Seq(
                        Direct(ServiceConfigurationReference("G4AEnv", "G4AService")),
                        Direct(ServiceConfigurationReference("CommonEnv", "CommonService")),
                        Partial(
                          Seq(
                            Direct(ServiceConfigurationReference("NestedG4AEnv", "NestedG4AService")),
                            Direct(ServiceConfigurationReference("NestedCommonEnv", "NestedCommonService")),
                          ),
                          1
                        )
                      ),
                      amberRequiredForAmber = Seq(
                        Direct(ServiceConfigurationReference("A4AEnv", "A4AService")),
                        Direct(ServiceConfigurationReference("CommonEnv", "CommonService")),
                        Partial(
                          Seq(
                            Direct(ServiceConfigurationReference("NestedA4AEnv", "NestedA4AService")),
                            Direct(ServiceConfigurationReference("NestedCommonEnv", "NestedCommonService")),
                          ),
                          1
                        )
                      )
                    )
                  )
                })
            }
            // Act
            result <- ServiceConfigurationController.getServiceConfigurationDependencies("TestEnv", "TestService")
            // Assert
          } yield assert(result.records)(hasSameElements(Seq(
            ServiceConfigurationReference("CommonEnv", "CommonService"),
            ServiceConfigurationReference("G4GEnv", "G4GService"),
            ServiceConfigurationReference("G4AEnv", "G4AService"),
            ServiceConfigurationReference("A4GEnv", "A4GService"),
            ServiceConfigurationReference("A4AEnv", "A4AService"),
            ServiceConfigurationReference("NestedCommonEnv", "NestedCommonService"),
            ServiceConfigurationReference("NestedG4GEnv", "NestedG4GService"),
            ServiceConfigurationReference("NestedG4AEnv", "NestedG4AService"),
            ServiceConfigurationReference("NestedA4GEnv", "NestedA4GService"),
            ServiceConfigurationReference("NestedA4AEnv", "NestedA4AService")
          )))
        },
        test("failure presents InternalServerError") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration("TestEnv", "TestService"))
                .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.getServiceConfigurationDependencies("TestEnv", "TestService").exit
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(1)).getServiceConfiguration("TestEnv", "TestService"))
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        },
        test("not found presents NotFoundResponse") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration("TestEnv", "TestService"))
                .thenReturn(ZIO.fail(RecordNotFoundDatabaseError("NotPresent")))
            }
            // Act
            failure <- ServiceConfigurationController.getServiceConfigurationDependencies("TestEnv", "TestService").exit
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(1)).getServiceConfiguration("TestEnv", "TestService"))
          } yield assert(failure)(failsWithA[RecordNotFoundErrorResponse])
        }
      ),
      suite("getServiceConfigurationDependents")(
        test("configuration with no dependents") {
          for {
            // Arrange
            testConf <- ZIO.succeed(TestData.serviceConfiguration.copy(env = "TestEnv", name = "TestService"))
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfigurations)
                .thenReturn(ZIO.succeed(Seq(testConf)))
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration("TestEnv", "TestService"))
                .thenReturn(ZIO.succeed(testConf))
            }
            // Act
            result <- ServiceConfigurationController.getServiceConfigurationDependents("TestEnv", "TestService")
            // Assert
          } yield assert(result)(equalTo(MultiApiResponse(Seq.empty[ServiceConfigurationReference])))
        },
        test("configuration with dependents") {
          for {
            // Arrange
            testConf <- ZIO.succeed(TestData.serviceConfiguration.copy(env = "TestEnv", name = "TestService"))
            dependentG4G <- ZIO.succeed {
              TestData.serviceConfiguration.copy(
                env = "TestEnv",
                name = "TestDependentServiceG4G",
                statusCheckAction = Composition(
                  greenRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TestEnv", "TestService"))),
                  amberRequiredForGreen = Seq.empty[CompositionItem],
                  greenRequiredForAmber = Seq.empty[CompositionItem],
                  amberRequiredForAmber = Seq.empty[CompositionItem]
                )
              )
            }
            dependentA4G <- ZIO.succeed {
              TestData.serviceConfiguration.copy(
                env = "TestEnv",
                name = "TestDependentServiceA4G",
                statusCheckAction = Composition(
                  greenRequiredForGreen = Seq.empty[CompositionItem],
                  amberRequiredForGreen = Seq(Direct(ServiceConfigurationReference("TestEnv", "TestService"))),
                  greenRequiredForAmber = Seq.empty[CompositionItem],
                  amberRequiredForAmber = Seq.empty[CompositionItem]
                )
              )
            }
            dependentG4A <- ZIO.succeed {
              TestData.serviceConfiguration.copy(
                env = "TestEnv",
                name = "TestDependentServiceG4A",
                statusCheckAction = Composition(
                  greenRequiredForGreen = Seq.empty[CompositionItem],
                  amberRequiredForGreen = Seq.empty[CompositionItem],
                  greenRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TestEnv", "TestService"))),
                  amberRequiredForAmber = Seq.empty[CompositionItem]
                )
              )
            }
            dependentA4A <- ZIO.succeed {
              TestData.serviceConfiguration.copy(
                env = "TestEnv",
                name = "TestDependentServiceA4A",
                statusCheckAction = Composition(
                  greenRequiredForGreen = Seq.empty[CompositionItem],
                  amberRequiredForGreen = Seq.empty[CompositionItem],
                  greenRequiredForAmber = Seq.empty[CompositionItem],
                  amberRequiredForAmber = Seq(Direct(ServiceConfigurationReference("TestEnv", "TestService")))
                )
              )
            }
            dependentA4ANested <- ZIO.succeed {
              TestData.serviceConfiguration.copy(
                env = "TestEnv",
                name = "TestDependentServiceA4ANested",
                statusCheckAction = Composition(
                  greenRequiredForGreen = Seq.empty[CompositionItem],
                  amberRequiredForGreen = Seq.empty[CompositionItem],
                  greenRequiredForAmber = Seq.empty[CompositionItem],
                  amberRequiredForAmber = Seq(Partial(Seq(Direct(ServiceConfigurationReference("TestEnv", "TestService"))), 2))
                )
              )
            }
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfigurations)
                .thenReturn(ZIO.succeed(Seq(testConf, dependentG4G, dependentG4A, dependentA4G, dependentA4A, dependentA4ANested)))
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration("TestEnv", "TestService"))
                .thenReturn(ZIO.succeed(testConf))
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration("TestEnv", "TestDependentServiceG4G"))
                .thenReturn(ZIO.succeed(dependentG4G))
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration("TestEnv", "TestDependentServiceG4A"))
                .thenReturn(ZIO.succeed(dependentG4A))
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration("TestEnv", "TestDependentServiceA4G"))
                .thenReturn(ZIO.succeed(dependentA4G))
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration("TestEnv", "TestDependentServiceA4A"))
                .thenReturn(ZIO.succeed(dependentA4A))
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration("TestEnv", "TestDependentServiceA4ANested"))
                .thenReturn(ZIO.succeed(dependentA4ANested))
            }
            // Act
            result <- ServiceConfigurationController.getServiceConfigurationDependents("TestEnv", "TestService")
            // Assert
          } yield assert(result.records)(hasSameElements(Seq(
            ServiceConfigurationReference("TestEnv", "TestDependentServiceG4G"),
            ServiceConfigurationReference("TestEnv", "TestDependentServiceG4A"),
            ServiceConfigurationReference("TestEnv", "TestDependentServiceA4G"),
            ServiceConfigurationReference("TestEnv", "TestDependentServiceA4A"),
            ServiceConfigurationReference("TestEnv", "TestDependentServiceA4ANested")
          )))
        },
        test("failure at getServiceConfigurations() presents InternalServerError") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfigurations)
                .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.getServiceConfigurationDependents("TestEnv", "TestService").exit
            // Assert
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        },
        test("failure at getServiceConfiguration() presents InternalServerError") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfigurations)
                .thenReturn(ZIO.succeed(Seq(TestData.serviceConfiguration.copy(env = "TestEnv", name = "TestService"))))
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration("TestEnv", "TestService"))
                .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.getServiceConfigurationDependents("TestEnv", "TestService").exit
            // Assert
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        }
      ),
      suite("createNewServiceConfiguration")(
        test("passes call to repository") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.createNewServiceConfiguration(TestData.serviceConfiguration))
                .thenReturn(ZIO.unit)
            }
            // Act
            _ <- ServiceConfigurationController.createNewServiceConfiguration(TestData.serviceConfiguration)
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(1)).createNewServiceConfiguration(TestData.serviceConfiguration))
          } yield assertCompletes
        },
        test("failure presents InternalServerError") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.createNewServiceConfiguration(TestData.serviceConfiguration))
                .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.createNewServiceConfiguration(TestData.serviceConfiguration).exit
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(1)).createNewServiceConfiguration(TestData.serviceConfiguration))
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        },
        test("data conflict presents DataConflictResponse") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.createNewServiceConfiguration(TestData.serviceConfiguration))
                .thenReturn(ZIO.fail(DataConflictDatabaseError("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.createNewServiceConfiguration(TestData.serviceConfiguration).exit
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(1)).createNewServiceConfiguration(TestData.serviceConfiguration))
          } yield assert(failure)(failsWithA[DataConflictErrorResponse])
        },
        test("repository notification is mandatory") {
          for {
            // Arrange
            serviceConfigurationNoRepositoryNotification <- ZIO.succeed(TestData.serviceConfiguration.copy(notificationAction = Seq.empty[NotificationAction]))
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.createNewServiceConfiguration(serviceConfigurationNoRepositoryNotification))
                .thenReturn(ZIO.unit)
            }
            // Act
            result <- ServiceConfigurationController.createNewServiceConfiguration(serviceConfigurationNoRepositoryNotification).exit
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(0)).createNewServiceConfiguration(any[ServiceConfiguration]))
          } yield assert(result)(failsWithA[DataConflictErrorResponse])
        }
      ),
      suite("updateExistingServiceConfiguration")(
        test("passes call to repository") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.updateExistingServiceConfiguration(TestData.serviceConfiguration))
                .thenReturn(ZIO.unit)
            }
            // Act
            _ <- ServiceConfigurationController.updateExistingServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, TestData.serviceConfiguration)
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(1)).updateExistingServiceConfiguration(TestData.serviceConfiguration))
          } yield assertCompletes
        },
        test("failure presents InternalServerError") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.updateExistingServiceConfiguration(TestData.serviceConfiguration))
                .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.updateExistingServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, TestData.serviceConfiguration).exit
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(1)).updateExistingServiceConfiguration(TestData.serviceConfiguration))
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        },
        test("data conflict presents DataConflictResponse") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.updateExistingServiceConfiguration(TestData.serviceConfiguration))
                .thenReturn(ZIO.fail(DataConflictDatabaseError("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.updateExistingServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, TestData.serviceConfiguration).exit
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(1)).updateExistingServiceConfiguration(TestData.serviceConfiguration))
          } yield assert(failure)(failsWithA[DataConflictErrorResponse])
        },
        test("not found presents NotFoundResponse") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.updateExistingServiceConfiguration(TestData.serviceConfiguration))
                .thenReturn(ZIO.fail(RecordNotFoundDatabaseError("NotPresent")))
            }
            // Act
            failure <- ServiceConfigurationController.updateExistingServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, TestData.serviceConfiguration).exit
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(1)).updateExistingServiceConfiguration(TestData.serviceConfiguration))
          } yield assert(failure)(failsWithA[RecordNotFoundErrorResponse])
        },
        test("rename should not be allowed") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.updateExistingServiceConfiguration(TestData.serviceConfiguration))
                .thenReturn(ZIO.unit)
            }
            // Act
            failure <- ServiceConfigurationController.updateExistingServiceConfiguration(TestData.serviceConfiguration.env, "BadName", TestData.serviceConfiguration).exit
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(0)).updateExistingServiceConfiguration(any[ServiceConfiguration]))
          } yield assert(failure)(failsWithA[DataConflictErrorResponse])
        },
        test("change of environment should not be allowed") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.updateExistingServiceConfiguration(TestData.serviceConfiguration))
                .thenReturn(ZIO.unit)
            }
            // Act
            failure <- ServiceConfigurationController.updateExistingServiceConfiguration("BadEnv", TestData.serviceConfiguration.name, TestData.serviceConfiguration).exit
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(0)).updateExistingServiceConfiguration(any[ServiceConfiguration]))
          } yield assert(failure)(failsWithA[DataConflictErrorResponse])
        },
        test("repository notification is mandatory") {
          for {
            // Arrange
            serviceConfigurationNoRepositoryNotification <- ZIO.succeed(TestData.serviceConfiguration.copy(notificationAction = Seq.empty[NotificationAction]))
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.updateExistingServiceConfiguration(serviceConfigurationNoRepositoryNotification))
                .thenReturn(ZIO.unit)
            }
            // Act
            failure <- ServiceConfigurationController.updateExistingServiceConfiguration(serviceConfigurationNoRepositoryNotification.env, serviceConfigurationNoRepositoryNotification.name, serviceConfigurationNoRepositoryNotification).exit
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(0)).updateExistingServiceConfiguration(any[ServiceConfiguration]))
          } yield assert(failure)(failsWithA[DataConflictErrorResponse])
        }
      ),
      suite("renameExistingServiceConfiguration")(
        test("performs all rename-relevant actions in defined order") {
          for {
            // Arrange
            list <- ZIO.succeed(ListBuffer.empty[String])
            serviceConfigurationRenamed <- ZIO.succeed(TestData.serviceConfiguration.copy(name = "newName", env = "newEnv"))
            _ <- ZIO.attempt {
              when(statusRepositoryMock.getLatestStatus(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(TestData.refinedStatus))
              when(statusRepositoryMock.getLatestStatus(serviceConfigurationRenamed.env, serviceConfigurationRenamed.name))
                .thenReturn(ZIO.fail(RecordNotFoundDatabaseError("NotPresent")))
              when(serviceConfigurationsRepositoryMock.deleteServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(list.addOne("REPO_DELETE")).unit)
              when(monitoringControllerMock.restartForService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(list.addOne("RESTART_OLD")).unit)
              when(statusRepositoryMock.renameService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationRenamed.env, serviceConfigurationRenamed.name))
                .thenReturn(ZIO.succeed(list.addOne("STATUS_RENAME")).unit)
              when(notificationRepositoryMock.renameService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationRenamed.env, serviceConfigurationRenamed.name))
                .thenReturn(ZIO.succeed(list.addOne("NOTIFICATION_RENAME")).unit)
              when(serviceConfigurationsRepositoryMock.createNewServiceConfiguration(serviceConfigurationRenamed))
                .thenReturn(ZIO.succeed(list.addOne("REPO_CREATE")).unit)
              when(monitoringControllerMock.restartForService(serviceConfigurationRenamed.env, serviceConfigurationRenamed.name))
                .thenReturn(ZIO.succeed(list.addOne("RESTART_NEW")).unit)
            }
            // Act
            _ <- ServiceConfigurationController.renameExistingServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationRenamed)
            // Assert
            _ <- assertTrue(list.toSeq == Seq("REPO_DELETE", "RESTART_OLD", "STATUS_RENAME", "NOTIFICATION_RENAME", "REPO_CREATE", "RESTART_NEW"))
          } yield assertCompletes
        },
        test("no active rename presents DataConflictResponse") {
          for {
            // Arrange
            // Act
            failure <- ServiceConfigurationController.renameExistingServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, TestData.serviceConfiguration).exit
            // Assert
          } yield assert(failure)(failsWithA[DataConflictErrorResponse])
        },
        test("rename to conflicting configuration presents DataConflictResponse") {
          for {
            // Arrange
            serviceConfigurationRenamed <- ZIO.succeed(TestData.serviceConfiguration.copy(name = "newName", env = "newEnv"))
            _ <- ZIO.attempt {
              when(statusRepositoryMock.getLatestStatus(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(TestData.refinedStatus))
              when(statusRepositoryMock.getLatestStatus(serviceConfigurationRenamed.env, serviceConfigurationRenamed.name))
                .thenReturn(ZIO.succeed(TestData.refinedStatus))
            }
            // Act
            failure <- ServiceConfigurationController.renameExistingServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationRenamed).exit
            // Assert
          } yield assert(failure)(failsWithA[DataConflictErrorResponse])
        },
        test("not found presents NotFoundResponse") {
          for {
            // Arrange
            serviceConfigurationRenamed <- ZIO.succeed(TestData.serviceConfiguration.copy(name = "newName", env = "newEnv"))
            _ <- ZIO.attempt {
              when(statusRepositoryMock.getLatestStatus(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.fail(RecordNotFoundDatabaseError("NotPresent")))
              when(statusRepositoryMock.getLatestStatus(serviceConfigurationRenamed.env, serviceConfigurationRenamed.name))
                .thenReturn(ZIO.fail(RecordNotFoundDatabaseError("NotPresent")))
            }
            // Act
            failure <- ServiceConfigurationController.renameExistingServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationRenamed).exit
            // Assert
          } yield assert(failure)(failsWithA[RecordNotFoundErrorResponse])
        },
        test("repository notification is mandatory") {
          for {
            // Arrange
            list <- ZIO.succeed(ListBuffer.empty[String])
            serviceConfigurationRenamedNoNotification <- ZIO.succeed(TestData.serviceConfiguration.copy(name = "newName", env = "newEnv", notificationAction = Seq.empty[NotificationAction]))
            _ <- ZIO.attempt {
              when(statusRepositoryMock.getLatestStatus(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(TestData.refinedStatus))
              when(statusRepositoryMock.getLatestStatus(serviceConfigurationRenamedNoNotification.env, serviceConfigurationRenamedNoNotification.name))
                .thenReturn(ZIO.fail(RecordNotFoundDatabaseError("NotPresent")))
            }
            // Act
            failure <- ServiceConfigurationController.renameExistingServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationRenamedNoNotification).exit
            // Assert
          } yield assert(failure)(failsWithA[DataConflictErrorResponse])
        },
        test("failure at controller fail to start renamed service") {
          for {
            // Arrange
            list <- ZIO.succeed(ListBuffer.empty[String])
            serviceConfigurationRenamed <- ZIO.succeed(TestData.serviceConfiguration.copy(name = "newName", env = "newEnv"))
            _ <- ZIO.attempt {
              when(statusRepositoryMock.getLatestStatus(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(TestData.refinedStatus))
              when(statusRepositoryMock.getLatestStatus(serviceConfigurationRenamed.env, serviceConfigurationRenamed.name))
                .thenReturn(ZIO.fail(RecordNotFoundDatabaseError("NotPresent")))
              when(serviceConfigurationsRepositoryMock.deleteServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(list.addOne("REPO_DELETE")).unit)
              when(monitoringControllerMock.restartForService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(list.addOne("RESTART_OLD")).unit)
              when(statusRepositoryMock.renameService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationRenamed.env, serviceConfigurationRenamed.name))
                .thenReturn(ZIO.succeed(list.addOne("STATUS_RENAME")).unit)
              when(notificationRepositoryMock.renameService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationRenamed.env, serviceConfigurationRenamed.name))
                .thenReturn(ZIO.succeed(list.addOne("NOTIFICATION_RENAME")).unit)
              when(serviceConfigurationsRepositoryMock.createNewServiceConfiguration(serviceConfigurationRenamed))
                .thenReturn(ZIO.succeed(list.addOne("REPO_CREATE")).unit)
              when(monitoringControllerMock.restartForService(serviceConfigurationRenamed.env, serviceConfigurationRenamed.name))
                .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.renameExistingServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationRenamed).exit
            // Assert
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        },
        test("failure at repository fail to create renamed configuration") {
          for {
            // Arrange
            list <- ZIO.succeed(ListBuffer.empty[String])
            serviceConfigurationRenamed <- ZIO.succeed(TestData.serviceConfiguration.copy(name = "newName", env = "newEnv"))
            _ <- ZIO.attempt {
              when(statusRepositoryMock.getLatestStatus(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(TestData.refinedStatus))
              when(statusRepositoryMock.getLatestStatus(serviceConfigurationRenamed.env, serviceConfigurationRenamed.name))
                .thenReturn(ZIO.fail(RecordNotFoundDatabaseError("NotPresent")))
              when(serviceConfigurationsRepositoryMock.deleteServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(list.addOne("REPO_DELETE")).unit)
              when(monitoringControllerMock.restartForService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(list.addOne("RESTART_OLD")).unit)
              when(statusRepositoryMock.renameService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationRenamed.env, serviceConfigurationRenamed.name))
                .thenReturn(ZIO.succeed(list.addOne("STATUS_RENAME")).unit)
              when(notificationRepositoryMock.renameService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationRenamed.env, serviceConfigurationRenamed.name))
                .thenReturn(ZIO.succeed(list.addOne("NOTIFICATION_RENAME")).unit)
              when(serviceConfigurationsRepositoryMock.createNewServiceConfiguration(serviceConfigurationRenamed))
                .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.renameExistingServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationRenamed).exit
            // Assert
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        },
        test("failure at repository fail to rename notifications") {
          for {
            // Arrange
            list <- ZIO.succeed(ListBuffer.empty[String])
            serviceConfigurationRenamed <- ZIO.succeed(TestData.serviceConfiguration.copy(name = "newName", env = "newEnv"))
            _ <- ZIO.attempt {
              when(statusRepositoryMock.getLatestStatus(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(TestData.refinedStatus))
              when(statusRepositoryMock.getLatestStatus(serviceConfigurationRenamed.env, serviceConfigurationRenamed.name))
                .thenReturn(ZIO.fail(RecordNotFoundDatabaseError("NotPresent")))
              when(serviceConfigurationsRepositoryMock.deleteServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(list.addOne("REPO_DELETE")).unit)
              when(monitoringControllerMock.restartForService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(list.addOne("RESTART_OLD")).unit)
              when(statusRepositoryMock.renameService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationRenamed.env, serviceConfigurationRenamed.name))
                .thenReturn(ZIO.succeed(list.addOne("STATUS_RENAME")).unit)
              when(notificationRepositoryMock.renameService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationRenamed.env, serviceConfigurationRenamed.name))
                .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.renameExistingServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationRenamed).exit
            // Assert
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        },
        test("failure at repository fail to rename status history") {
          for {
            // Arrange
            list <- ZIO.succeed(ListBuffer.empty[String])
            serviceConfigurationRenamed <- ZIO.succeed(TestData.serviceConfiguration.copy(name = "newName", env = "newEnv"))
            _ <- ZIO.attempt {
              when(statusRepositoryMock.getLatestStatus(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(TestData.refinedStatus))
              when(statusRepositoryMock.getLatestStatus(serviceConfigurationRenamed.env, serviceConfigurationRenamed.name))
                .thenReturn(ZIO.fail(RecordNotFoundDatabaseError("NotPresent")))
              when(serviceConfigurationsRepositoryMock.deleteServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(list.addOne("REPO_DELETE")).unit)
              when(monitoringControllerMock.restartForService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(list.addOne("RESTART_OLD")).unit)
              when(statusRepositoryMock.renameService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationRenamed.env, serviceConfigurationRenamed.name))
                .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.renameExistingServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationRenamed).exit
            // Assert
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        },
        test("failure at controller fail to stop old service") {
          for {
            // Arrange
            list <- ZIO.succeed(ListBuffer.empty[String])
            serviceConfigurationRenamed <- ZIO.succeed(TestData.serviceConfiguration.copy(name = "newName", env = "newEnv"))
            _ <- ZIO.attempt {
              when(statusRepositoryMock.getLatestStatus(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(TestData.refinedStatus))
              when(statusRepositoryMock.getLatestStatus(serviceConfigurationRenamed.env, serviceConfigurationRenamed.name))
                .thenReturn(ZIO.fail(RecordNotFoundDatabaseError("NotPresent")))
              when(serviceConfigurationsRepositoryMock.deleteServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(list.addOne("REPO_DELETE")).unit)
              when(monitoringControllerMock.restartForService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.renameExistingServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationRenamed).exit
            // Assert
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        },
        test("failure at repository fail to delete old configuration") {
          for {
            // Arrange
            list <- ZIO.succeed(ListBuffer.empty[String])
            serviceConfigurationRenamed <- ZIO.succeed(TestData.serviceConfiguration.copy(name = "newName", env = "newEnv"))
            _ <- ZIO.attempt {
              when(statusRepositoryMock.getLatestStatus(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(TestData.refinedStatus))
              when(statusRepositoryMock.getLatestStatus(serviceConfigurationRenamed.env, serviceConfigurationRenamed.name))
                .thenReturn(ZIO.fail(RecordNotFoundDatabaseError("NotPresent")))
              when(serviceConfigurationsRepositoryMock.deleteServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.renameExistingServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationRenamed).exit
            // Assert
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        },
        test("failure at repository fail to retrieve status") {
          for {
            // Arrange
            list <- ZIO.succeed(ListBuffer.empty[String])
            serviceConfigurationRenamed <- ZIO.succeed(TestData.serviceConfiguration.copy(name = "newName", env = "newEnv"))
            _ <- ZIO.attempt {
              when(statusRepositoryMock.getLatestStatus(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.renameExistingServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationRenamed).exit
            // Assert
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        }
      ),
      suite("setMaintenanceMessage")(
        test("change message and perform restart") {
          for {
            // Arrange
            list <- ZIO.succeed(ListBuffer.empty[String])
            serviceConfigurationNewMessage <- ZIO.succeed(TestData.serviceConfiguration.copy(maintenanceMessage = "AmRunningThisTest"))
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(TestData.serviceConfiguration))
              when(serviceConfigurationsRepositoryMock.updateExistingServiceConfiguration(serviceConfigurationNewMessage))
                .thenReturn(ZIO.succeed(list.addOne("UPDATE")).unit)
              when(monitoringControllerMock.restartForService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(list.addOne("RESET")).unit)
            }
            // Act
            _ <- ServiceConfigurationController.setMaintenanceMessage(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationNewMessage.maintenanceMessage)
            // Assert
            _ <- assertTrue(list.toSeq == Seq("UPDATE", "RESET"))
          } yield assertCompletes
        },
        test("not found presents NotFoundResponse") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration("NotExistEnv", "NotExistService"))
                .thenReturn(ZIO.fail(RecordNotFoundDatabaseError("NotPresent")))
            }
            // Act
            failure <- ServiceConfigurationController.setMaintenanceMessage("NotExistEnv", "NotExistService", "NewMessage").exit
            // Assert
          } yield assert(failure)(failsWithA[RecordNotFoundErrorResponse])
        },
        test("Failure at repository presents InternalServerError") {
          for {
            // Arrange
            serviceConfigurationNewMessage <- ZIO.succeed(TestData.serviceConfiguration.copy(maintenanceMessage = "AmRunningThisTest"))
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(TestData.serviceConfiguration))
              when(serviceConfigurationsRepositoryMock.updateExistingServiceConfiguration(serviceConfigurationNewMessage))
                .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.setMaintenanceMessage(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationNewMessage.maintenanceMessage).exit
            // Assert
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        },
        test("Failure at monitoring restart presents InternalServerError") {
          for {
            // Arrange
            serviceConfigurationNewMessage <- ZIO.succeed(TestData.serviceConfiguration.copy(maintenanceMessage = "AmRunningThisTest"))
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(TestData.serviceConfiguration))
              when(serviceConfigurationsRepositoryMock.updateExistingServiceConfiguration(serviceConfigurationNewMessage))
                .thenReturn(ZIO.unit)
              when(monitoringControllerMock.restartForService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.setMaintenanceMessage(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, serviceConfigurationNewMessage.maintenanceMessage).exit
            // Assert
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        }
      ),
      suite("setTemporaryStatus")(
        test("change check action and perform restart") {
          for {
            // Arrange
            list <- ZIO.succeed(ListBuffer.empty[String])
            serviceConfigurationTemporaryStatus <- ZIO.succeed {
              TestData.serviceConfiguration.copy(
                statusCheckAction = TemporaryFixedStatus(TestData.rawStatusAmber, TestData.serviceConfiguration.statusCheckAction),
                maintenanceMessage = "TestingSomething"
              )
            }
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(TestData.serviceConfiguration))
              when(serviceConfigurationsRepositoryMock.updateExistingServiceConfiguration(serviceConfigurationTemporaryStatus))
                .thenReturn(ZIO.succeed(list.addOne("UPDATE")).unit)
              when(monitoringControllerMock.restartForService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(list.addOne("RESET")).unit)
            }
            // Act
            _ <- ServiceConfigurationController.setTemporaryStatus(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, TestData.rawStatusAmber, serviceConfigurationTemporaryStatus.maintenanceMessage)
            // Assert
            _ <- assertTrue(list.toSeq == Seq("UPDATE", "RESET"))
          } yield assertCompletes
        },
        test("don't nest temporary status, perform replacement") {
          for {
            // Arrange
            serviceConfigurationTemporaryStatus <- ZIO.succeed {
              TestData.serviceConfiguration.copy(
                statusCheckAction = TemporaryFixedStatus(TestData.rawStatusAmber, TestData.serviceConfiguration.statusCheckAction),
                maintenanceMessage = "TestingSomething"
              )
            }
            serviceConfigurationTemporaryStatus2 <- ZIO.succeed {
              TestData.serviceConfiguration.copy(
                statusCheckAction = TemporaryFixedStatus(TestData.rawStatusRed, TestData.serviceConfiguration.statusCheckAction),
                maintenanceMessage = "TestingSomethingAgain"
              )
            }
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(serviceConfigurationTemporaryStatus))
              when(serviceConfigurationsRepositoryMock.updateExistingServiceConfiguration(serviceConfigurationTemporaryStatus2))
                .thenReturn(ZIO.unit)
              when(monitoringControllerMock.restartForService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.unit)
            }
            // Act
            _ <- ServiceConfigurationController.setTemporaryStatus(serviceConfigurationTemporaryStatus.env, serviceConfigurationTemporaryStatus.name, TestData.rawStatusRed, serviceConfigurationTemporaryStatus2.maintenanceMessage)
            // Assert
          } yield assertCompletes
        },
        test("not found presents NotFoundResponse") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration("NotExistEnv", "NotExistService"))
                .thenReturn(ZIO.fail(RecordNotFoundDatabaseError("NotPresent")))
            }
            // Act
            failure <- ServiceConfigurationController.setTemporaryStatus("NotExistEnv", "NotExistService", TestData.rawStatusAmber, "NewMessage").exit
            // Assert
          } yield assert(failure)(failsWithA[RecordNotFoundErrorResponse])
        },
        test("Failure at repository presents InternalServerError") {
          for {
            // Arrange
            serviceConfigurationTemporaryStatus <- ZIO.succeed {
              TestData.serviceConfiguration.copy(
                statusCheckAction = TemporaryFixedStatus(TestData.rawStatusAmber, TestData.serviceConfiguration.statusCheckAction),
                maintenanceMessage = "TestingSomething"
              )
            }
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(TestData.serviceConfiguration))
              when(serviceConfigurationsRepositoryMock.updateExistingServiceConfiguration(serviceConfigurationTemporaryStatus))
                .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.setTemporaryStatus(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, TestData.rawStatusAmber, serviceConfigurationTemporaryStatus.maintenanceMessage).exit
            // Assert
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        },
        test("Failure at monitoring restart presents InternalServerError") {
          for {
            // Arrange
            serviceConfigurationTemporaryStatus <- ZIO.succeed {
              TestData.serviceConfiguration.copy(
                statusCheckAction = TemporaryFixedStatus(TestData.rawStatusAmber, TestData.serviceConfiguration.statusCheckAction),
                maintenanceMessage = "TestingSomething"
              )
            }
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(TestData.serviceConfiguration))
              when(serviceConfigurationsRepositoryMock.updateExistingServiceConfiguration(serviceConfigurationTemporaryStatus))
                .thenReturn(ZIO.unit)
              when(monitoringControllerMock.restartForService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.setTemporaryStatus(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name, TestData.rawStatusAmber, serviceConfigurationTemporaryStatus.maintenanceMessage).exit
            // Assert
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        }
      ),
      suite("restoreFromTemporaryStatus")(
        test("change check action and perform restart") {
          for {
            // Arrange
            list <- ZIO.succeed(ListBuffer.empty[String])
            serviceConfigurationTemporaryStatus <- ZIO.succeed {
              TestData.serviceConfiguration.copy(
                statusCheckAction = TemporaryFixedStatus(TestData.rawStatusAmber, TestData.serviceConfiguration.statusCheckAction),
                maintenanceMessage = "TestingSomething"
              )
            }
            serviceConfigurationRestored <- ZIO.succeed {
              TestData.serviceConfiguration.copy(maintenanceMessage = "")
            }
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(serviceConfigurationTemporaryStatus))
              when(serviceConfigurationsRepositoryMock.updateExistingServiceConfiguration(serviceConfigurationRestored))
                .thenReturn(ZIO.succeed(list.addOne("UPDATE")).unit)
              when(monitoringControllerMock.restartForService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(list.addOne("RESET")).unit)
            }
            // Act
            _ <- ServiceConfigurationController.restoreFromTemporaryStatus(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name)
            // Assert
            _ <- assertTrue(list.toSeq == Seq("UPDATE", "RESET"))
          } yield assertCompletes
        },
        test("no change when nothing to restore") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(TestData.serviceConfiguration))
            }
            // Act
            _ <- ServiceConfigurationController.restoreFromTemporaryStatus(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name)
            // Assert
          } yield assertCompletes
        },
        test("not found presents NotFoundResponse") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration("NotExistEnv", "NotExistService"))
                .thenReturn(ZIO.fail(RecordNotFoundDatabaseError("NotPresent")))
            }
            // Act
            failure <- ServiceConfigurationController.restoreFromTemporaryStatus("NotExistEnv", "NotExistService").exit
            // Assert
          } yield assert(failure)(failsWithA[RecordNotFoundErrorResponse])
        },
        test("Failure at repository presents InternalServerError") {
          for {
            // Arrange
            serviceConfigurationTemporaryStatus <- ZIO.succeed {
              TestData.serviceConfiguration.copy(
                statusCheckAction = TemporaryFixedStatus(TestData.rawStatusAmber, TestData.serviceConfiguration.statusCheckAction),
                maintenanceMessage = "TestingSomething"
              )
            }
            serviceConfigurationRestored <- ZIO.succeed {
              TestData.serviceConfiguration.copy(maintenanceMessage = "")
            }
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(serviceConfigurationTemporaryStatus))
              when(serviceConfigurationsRepositoryMock.updateExistingServiceConfiguration(serviceConfigurationRestored))
                .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.restoreFromTemporaryStatus(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name).exit
            // Assert
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        },
        test("Failure at monitoring restart presents InternalServerError") {
          for {
            // Arrange
            serviceConfigurationTemporaryStatus <- ZIO.succeed {
              TestData.serviceConfiguration.copy(
                statusCheckAction = TemporaryFixedStatus(TestData.rawStatusAmber, TestData.serviceConfiguration.statusCheckAction),
                maintenanceMessage = "TestingSomething"
              )
            }
            serviceConfigurationRestored <- ZIO.succeed {
              TestData.serviceConfiguration.copy(maintenanceMessage = "")
            }
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.getServiceConfiguration(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.succeed(serviceConfigurationTemporaryStatus))
              when(serviceConfigurationsRepositoryMock.updateExistingServiceConfiguration(serviceConfigurationRestored))
                .thenReturn(ZIO.unit)
              when(monitoringControllerMock.restartForService(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name))
                .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.restoreFromTemporaryStatus(TestData.serviceConfiguration.env, TestData.serviceConfiguration.name).exit
            // Assert
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        }
      ),
      suite("deleteServiceConfiguration")(
        test("passes call to repository, stops monitoring and marks black status") {
          for {
            // Arrange
            list <- ZIO.succeed(ListBuffer.empty[String])
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.deleteServiceConfiguration("TestEnv", "TestService"))
                .thenReturn(ZIO.succeed(list.addOne("REPO_DELETE")).unit)
              when(monitoringControllerMock.restartForService("TestEnv", "TestService"))
                .thenReturn(ZIO.succeed(list.addOne("RESTART")).unit)
              when(statusRepositoryMock.getLatestStatus("TestEnv", "TestService"))
                .thenReturn(ZIO.succeed(list.addOne("REPO_GET")).as(TestData.refinedStatus))
              when(statusRepositoryMock.createOrUpdate(any[RefinedStatus]))
                .thenReturn(ZIO.succeed(list.addOne("REPO_WRITE")).unit)
            }
            // Act
            result <- ServiceConfigurationController.deleteServiceConfiguration("TestEnv", "TestService")
            // Assert
            _ <- assertTrue(list.toSeq == Seq("REPO_DELETE", "RESTART", "REPO_GET", "REPO_WRITE"))
          } yield assertCompletes
        },
        test("passes call to repository, stops monitoring, don't double marks black status when already black") {
          for {
            // Arrange
            list <- ZIO.succeed(ListBuffer.empty[String])
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.deleteServiceConfiguration("TestEnv", "TestService"))
                .thenReturn(ZIO.succeed(list.addOne("REPO_DELETE")).unit)
              when(monitoringControllerMock.restartForService("TestEnv", "TestService"))
                .thenReturn(ZIO.succeed(list.addOne("RESTART")).unit)
              when(statusRepositoryMock.getLatestStatus("TestEnv", "TestService"))
                .thenReturn(ZIO.succeed(list.addOne("REPO_GET")).as(TestData.refinedStatus.copy(status = Black())))
            }
            // Act
            result <- ServiceConfigurationController.deleteServiceConfiguration("TestEnv", "TestService")
            // Assert
            _ <- assertTrue(list.toSeq == Seq("REPO_DELETE", "RESTART", "REPO_GET"))
          } yield assertCompletes
        },
        test("passes call to repository, stops monitoring, don't double marks black status when not known") {
          for {
            // Arrange
            list <- ZIO.succeed(ListBuffer.empty[String])
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.deleteServiceConfiguration("TestEnv", "TestService"))
                .thenReturn(ZIO.succeed(list.addOne("REPO_DELETE")).unit)
              when(monitoringControllerMock.restartForService("TestEnv", "TestService"))
                .thenReturn(ZIO.succeed(list.addOne("RESTART")).unit)
              when(statusRepositoryMock.getLatestStatus("TestEnv", "TestService"))
                .thenReturn(ZIO.succeed(list.addOne("REPO_GET")) *> ZIO.fail(RecordNotFoundDatabaseError("BAKA")))
            }
            // Act
            result <- ServiceConfigurationController.deleteServiceConfiguration("TestEnv", "TestService")
            // Assert
            _ <- assertTrue(list.toSeq == Seq("REPO_DELETE", "RESTART", "REPO_GET"))
          } yield assertCompletes
        },
        test("failure at repo presents InternalServerError") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.deleteServiceConfiguration("TestEnv", "TestService"))
                .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.deleteServiceConfiguration("TestEnv", "TestService").exit
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(1)).deleteServiceConfiguration("TestEnv", "TestService"))
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        },
        test("failure at controller presents InternalServerError") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.deleteServiceConfiguration("TestEnv", "TestService"))
                .thenReturn(ZIO.unit)
              when(monitoringControllerMock.restartForService("TestEnv", "TestService"))
                .thenReturn(ZIO.fail(InternalServerErrorResponse("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.deleteServiceConfiguration("TestEnv", "TestService").exit
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(1)).deleteServiceConfiguration("TestEnv", "TestService"))
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        },
        test("failure at status-repo-read presents InternalServerError") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.deleteServiceConfiguration("TestEnv", "TestService"))
                .thenReturn(ZIO.unit)
              when(monitoringControllerMock.restartForService("TestEnv", "TestService"))
                .thenReturn(ZIO.unit)
              when(statusRepositoryMock.getLatestStatus("TestEnv", "TestService"))
                .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.deleteServiceConfiguration("TestEnv", "TestService").exit
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(1)).deleteServiceConfiguration("TestEnv", "TestService"))
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        },
        test("failure at status-repo-write presents InternalServerError") {
          for {
            // Arrange
            _ <- ZIO.attempt {
              when(serviceConfigurationsRepositoryMock.deleteServiceConfiguration("TestEnv", "TestService"))
                .thenReturn(ZIO.unit)
              when(monitoringControllerMock.restartForService("TestEnv", "TestService"))
                .thenReturn(ZIO.unit)
              when(statusRepositoryMock.getLatestStatus("TestEnv", "TestService"))
                .thenReturn(ZIO.succeed(TestData.refinedStatus))
              when(statusRepositoryMock.createOrUpdate(any[RefinedStatus]))
                .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
            }
            // Act
            failure <- ServiceConfigurationController.deleteServiceConfiguration("TestEnv", "TestService").exit
            // Assert
            _ <- ZIO.attempt(verify(serviceConfigurationsRepositoryMock, times(1)).deleteServiceConfiguration("TestEnv", "TestService"))
          } yield assert(failure)(failsWithA[InternalServerErrorResponse])
        }
      )
    ) @@ TestAspect.sequential @@ TestAspect.before(resetMocks)
  }.provide(ServiceConfigurationControllerImpl.layer,
    ZLayer.succeed(serviceConfigurationsRepositoryMock),
    ZLayer.succeed(statusRepositoryMock),
    ZLayer.succeed(notificationRepositoryMock),
    ZLayer.succeed(monitoringControllerMock))
}
