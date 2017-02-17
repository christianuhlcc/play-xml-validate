# play-xml-validate

[![Build Status](https://travis-ci.org/christianuhlcc/play-xml-validate.svg?branch=master)](https://travis-ci.org/christianuhlcc/play-xml-validate)

Adds XML Validation and Databinding to the Play Framework

This Project is inspired by [play-circe](https://github.com/jilen/play-circe). It allows you to validate incoming XML and bind against generated case classes 

## Usage / Example


To use, extend your Controller with the `XmlValidatingBinder` trait and use the provided Action to modify the Request

```scala
@Singleton
class SampleController @Inject() extends Controller with XmlValidatingBinder {
	def receiveXML(): Action[YourXmlClass] = Action.async(XmlValidatingBinder.bindXml[YourXmlClass]()) { request =>
		Future { 
			val content : YourXmlClass = request.body
    		...
     	}
  	}
}
```
## Set up

1.Include Dependency

```scala
  libraryDependencies += "de.codecentric" %% "play-xml-validate" % "0.0.1-SNAPSHOT",
```

2. Generate case classes with scalaxb

- include the scalaxb-sbt plugin in your `plugins.sbt`

```scala
resolvers += Resolver.sonatypeRepo("public")
addSbtPlugin("org.scalaxb" % "sbt-scalaxb" % "1.5.0")
```

- configure the scalaxb plugin in your `build.sbt`

```scala
lazy val root = (project in file(".")).
  enablePlugins(PlayScala, ScalaxbPlugin).
  settings(
    scalaxbPackageName in(Compile, scalaxb) := "de.codecentric.play.xml.validate.sample",
    scalaxbXsdSource in(Compile, scalaxb) := baseDirectory.value / "conf" / "xsd"
  )

```

3. Include in your Controller 

```scala
@Singleton
class SampleController @Inject() extends Controller with XmlValidatingBinder {
	def receiveXML(): Action[YourXmlClass] = Action.async(XmlValidatingBinder.bindXml[YourXmlClass]()) { request =>
		Future { 
			val content : YourXmlClass = request.body
    		...
     	}
  	}
}
```

There is a comprehensive sample project in [sample](/sample)

## Compatibility

This project supports Play 2.5.

## References
We're relying on [scalaxb](http://scalaxb.org) for nearly everything