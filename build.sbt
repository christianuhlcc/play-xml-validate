organization := "de.codecentric"
name := "play-xml-validate"
version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.8"

lazy val dispatchV = "0.11.3"

lazy val root = (project in file(".")).
  enablePlugins(ScalaxbPlugin).
  settings(
    scalaxbDispatchVersion in(Compile, scalaxb) := dispatchV,
    scalaxbPackageName in(Compile, scalaxb) := "de.codecentric.play.xml.validate.test",
    scalaxbXsdSource in(Compile, scalaxb) := baseDirectory.value / "src" / "test" / "resources"
  )

libraryDependencies ++= {
  Seq(
    "com.typesafe.play"       %%  "play"                      % "2.5.10"  % "provided",
    "org.scalaxb"             %   "scalaxb_2.11"              % "1.5.0",
    "org.scala-lang.modules"  %%  "scala-xml"                 % "1.0.2",
    "org.scala-lang.modules"  %%  "scala-parser-combinators"  % "1.0.1",
    "net.databinder.dispatch" %%  "dispatch-core"             % dispatchV,
    "org.scalatestplus.play"  %%  "scalatestplus-play"        % "1.5.1"   % Test,
    "org.mockito"             %   "mockito-core"              % "2.7.0"   % Test
  )
}

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture",
  "-Ywarn-unused-import")

unmanagedClasspath in Test += baseDirectory.value / "src" / "test" / "resources"

coverageExcludedPackages := "<empty>;de.codecentric.play.xml.validate.test.*;scalaxb"