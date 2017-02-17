
name := """play-xml-validate-sample"""

version := "1.0-SNAPSHOT"

lazy val dispatchV = "0.11.3"

lazy val root = (project in file(".")).
  enablePlugins(PlayScala, ScalaxbPlugin).
  settings(
    scalaxbPackageName in(Compile, scalaxb) := "de.codecentric.play.xml.validate.sample",
    scalaxbXsdSource in(Compile, scalaxb) := baseDirectory.value / "conf" / "xsd"
  )

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "de.codecentric" %% "play-xml-validate" % "0.0.1-SNAPSHOT",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)