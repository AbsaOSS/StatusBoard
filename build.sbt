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
import Dependencies.*
import JacocoSetup.*

import scala.collection.Seq

ThisBuild / scalaVersion     := Versions.scala213
ThisBuild / organization     := "za.co.absa.status-board"
ThisBuild / version          := { IO.read(file("VERSION")).trim }

Global / onChangedBuildSource := ReloadOnSourceChanges
publish / skip := true

lazy val printScalaVersion = taskKey[Unit]("Print Scala version the project is being built for.")

val mergeStrategy = assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "services", xs @ _*) => MergeStrategy.filterDistinctLines
  case PathList("META-INF", "maven", "org.webjars", "swagger-ui", "pom.properties") => MergeStrategy.singleOrError
  case PathList("META-INF", "resources", "webjars", "swagger-ui", _*) => MergeStrategy.singleOrError
  case PathList("META-INF", _*) => MergeStrategy.discard
  case PathList("META-INF", "versions", "9", xs@_*) => MergeStrategy.discard
  case PathList("module-info.class") => MergeStrategy.discard
  case "application.conf" => MergeStrategy.concat
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}

lazy val root = (project in file("."))
  .settings(
    name := "status-board",
    libraryDependencies ++= commonDependencies,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-Ymacro-annotations"),
    Compile / packageBin / publishArtifact := false,
    printScalaVersion := {
      val log = streams.value.log
      log.info(s"Building ${name.value} with Scala ${scalaVersion.value}")
    },
    (Compile / compile) := ((Compile / compile) dependsOn printScalaVersion).value,
    packageBin := (Compile / assembly).value,
    artifactPath / (Compile / packageBin) := baseDirectory.value / s"target/${name.value}-${version.value}.jar",
    mergeStrategy,
    Test / parallelExecution := false,
    (assembly / test) := {},
    publish := {},
    jacocoReportSettings := jacocoSettings(scalaVersion.value, "status-board"),
    jacocoExcludes := jacocoProjectExcludes()
  )
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(AssemblyPlugin)
