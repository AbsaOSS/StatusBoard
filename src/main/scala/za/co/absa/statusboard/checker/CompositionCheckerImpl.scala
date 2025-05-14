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

package za.co.absa.statusboard.checker

import za.co.absa.statusboard.checker.CompositionCheckerImpl.{CriterionCheck, Fail, IntermittentFail, Pass}
import za.co.absa.statusboard.model.AppError.DatabaseError.RecordNotFoundDatabaseError
import za.co.absa.statusboard.model.RawStatus.{Amber, Green, Red}
import za.co.absa.statusboard.model.StatusCheckAction.CompositionItem
import za.co.absa.statusboard.model.{RawStatus, ServiceConfigurationReference, StatusCheckAction}
import za.co.absa.statusboard.repository.StatusRepository
import zio.{UIO, ZIO, ZLayer}

class CompositionCheckerImpl(statusRepository: StatusRepository) extends CompositionChecker {
  override def checkRawStatus(action: StatusCheckAction.Composition): UIO[RawStatus] = {
    for {
      greenChecks <- ZIO.succeed {
        action.greenRequiredForGreen.map(dependency => dependencyCheck(dependency, isGreen)) ++
          action.amberRequiredForGreen.map(dependency => dependencyCheck(dependency, isAtLeastAmber))
      }
      greenChecksCombined <- combineCriteriaCheck(greenChecks)
      amberChecks <- ZIO.succeed {
        action.greenRequiredForAmber.map(dependency => dependencyCheck(dependency, isGreen)) ++
          action.amberRequiredForAmber.map(dependency => dependencyCheck(dependency, isAtLeastAmber))
      }
      amberChecksCombined <- combineCriteriaCheck(amberChecks)
    } yield (greenChecksCombined, amberChecksCombined) match {
      case (Pass(), Pass()) => Green("OK")
      case (IntermittentFail(), Pass()) => Amber("PARTIAL", intermittent = true)
      case (Fail(), Pass()) => Amber("PARTIAL", intermittent = false)
      case (_, IntermittentFail()) => Red("NOT OK", intermittent = true)
      case (_, Fail()) => Red("NOT OK", intermittent = false)
    }
  }

  private def dependencyCheck(dependency: CompositionItem, referenceCheck: ServiceConfigurationReference => UIO[CriterionCheck]): UIO[CriterionCheck] = {
    dependency match {
      case CompositionItem.Direct(ref) => referenceCheck(ref)
      case partial @ CompositionItem.Partial(_, _) => dependencyCheck(partial, referenceCheck)
    }
  }

  private def dependencyCheck(dependency: CompositionItem.Partial, referenceCheck: ServiceConfigurationReference => UIO[CriterionCheck]): UIO[CriterionCheck] = {
    for {
      subResults <- ZIO.foreach(dependency.items)(item => dependencyCheck(item, referenceCheck))
      passCnt <- ZIO.succeed {
        subResults.count {
          case Pass() => true
          case _ => false
        }
      }
      intermittentSeen <- ZIO.succeed {
        subResults.exists {
          case IntermittentFail() => true
          case _ => false
        }
      }
    } yield {
      if (passCnt >= dependency.min) Pass()
      else if (intermittentSeen) IntermittentFail()
      else Fail()
    }
  }

  private def isGreen(dependency: ServiceConfigurationReference): UIO[CriterionCheck] =
    statusRepository.getLatestStatus(dependency.environment, dependency.service).flatMap(status =>
      status.status match {
        case Green(_) => ZIO.succeed(Pass())
        case status => ZIO.succeed(if (status.intermittent) IntermittentFail() else Fail())
      }).catchAll {
      case _: RecordNotFoundDatabaseError => ZIO.succeed(IntermittentFail())
      case error =>
        ZIO.logError(s"An error occurred while performing composition status check for $dependency: ${error.getMessage} Details: ${error.toString}")
          .as(IntermittentFail())
    }

  private def isAtLeastAmber(dependency: ServiceConfigurationReference): UIO[CriterionCheck] =
    statusRepository.getLatestStatus(dependency.environment, dependency.service).flatMap(status =>
    status.status match {
      case Green(_) => ZIO.succeed(Pass())
      case Amber(_, _) => ZIO.succeed(Pass())
      case status => ZIO.succeed(if (status.intermittent) IntermittentFail() else Fail())
    }).catchAll {
    case _: RecordNotFoundDatabaseError => ZIO.succeed(IntermittentFail())
    case error =>
      ZIO.logError(s"An error occurred while performing composition status check for $dependency: ${error.getMessage} Details: ${error.toString}")
        .as(IntermittentFail())
  }

  private def combineCriteriaCheck(criteriaCheck: Seq[UIO[CriterionCheck]]): UIO[CriterionCheck] = {
    criteriaCheck.foldLeft(ZIO.succeed(Pass(): CriterionCheck)) { (acc, check) =>
      acc.zipWith(check) {
        case (Pass(), Pass()) => Pass()
        case (Fail(), _) | (_, Fail()) => Fail()
        case _ => IntermittentFail()
      }
    }
  }
}

object CompositionCheckerImpl {
  private sealed trait CriterionCheck
  private case class Pass() extends CriterionCheck
  private case class IntermittentFail() extends CriterionCheck
  private case class Fail() extends CriterionCheck

  val layer: ZLayer[Any with StatusRepository, Throwable, CompositionChecker] = ZLayer {
    ZIO.serviceWith[StatusRepository](statusRepository => new CompositionCheckerImpl(statusRepository))
  }
}
