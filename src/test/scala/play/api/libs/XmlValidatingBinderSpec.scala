package play.api.libs

import akka.util.ByteString
import de.codecentric.play.xml.validate.test._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Assertions, Matchers, WordSpec }
import play.api.http.Port
import play.api.mvc.{ RequestHeader, _ }
import play.api.routing.sird._
import play.core.server.Server

import scala.io.Source.fromInputStream
import scala.xml.SAXParseException

class XmlValidatingBinderSpec
    extends WordSpec
    with Matchers
    with Assertions
    with XmlValidatingBinder
    with MockitoSugar {

  val testXml       = "sample.xml"
  val testXmlBroken = "sample_broken.xml"
  val testXmlLocal  = "sample_local.xml"
  val testXsd       = "sample.xsd"

  val requestHeader = mock[RequestHeader]
  when(requestHeader.charset).thenReturn(Some("UTF-8"))

  "The Validator without XSD Specification" should {

    "pass a valid XML with a network reachable XSD " in {
      Server.withRouter() {
        case GET(p"/sample.xsd") =>
          Action {
            Results.Ok.sendResource(testXsd)
          }
      } { implicit port =>
        val xml = loadXmlFromClasspathAndSetPort(testXml, port)
        noException should be thrownBy {
          val _ = XmlValidatingBinder.binder[OrderType](
            requestHeader,
            ByteString(xml),
            withXMLValidation = true,
            Decodecentricplayxmlvalidatetest_OrderTypeFormat
          )
        }
      }
    }

    "throw a SAXParseException for an invalid XML with a network reachable XSD" in {
      when(requestHeader.charset).thenReturn(Some("UTF-8"))

      Server.withRouter() {
        case GET(p"/sample.xsd") =>
          Action {
            Results.Ok.sendResource(testXsd)
          }
      } { implicit port =>
        val xml = loadXmlFromClasspathAndSetPort(testXmlBroken, port)
        a[SAXParseException] should be thrownBy {
          val _ = XmlValidatingBinder.binder[OrderType](
            requestHeader,
            ByteString(xml),
            withXMLValidation = true,
            Decodecentricplayxmlvalidatetest_OrderTypeFormat
          )
        }
      }
    }

    "omit validation of tag-contents when turned off" in {
      val xml = loadXmlFromClasspathAndSetPort(testXmlBroken)
      noException should be thrownBy {
        val _ = XmlValidatingBinder.binder[OrderType](
          requestHeader,
          ByteString(xml),
          withXMLValidation = false,
          Decodecentricplayxmlvalidatetest_OrderTypeFormat
        )
      }
    }
  }

  "The XML Case Class Binder" should {
    "bind to Case Classes for network reachable XSDs" in {
      Server.withRouter() {
        case GET(p"/sample.xsd") =>
          Action {
            Results.Ok.sendResource(testXsd)
          }
      } { implicit port =>
        val xml = loadXmlFromClasspathAndSetPort(testXml, port)
        val result =
          XmlValidatingBinder.binder[OrderType](requestHeader,
                                                ByteString(xml),
                                                true,
                                                Decodecentricplayxmlvalidatetest_OrderTypeFormat)
        result.name should be("someName")
      }
    }

    "bind to case classes for XML without any XSD" in {

      val xml = loadXmlFromClasspathAndSetPort(testXmlLocal)
      val order = XmlValidatingBinder.binder[OrderType](
        requestHeader,
        ByteString(xml),
        withXMLValidation = false,
        Decodecentricplayxmlvalidatetest_OrderTypeFormat
      )
      order.name should be("someName")
    }

  }

  private def loadXmlFromClasspathAndSetPort(file: String, port: Port = new Port(0)) =
    fromInputStream(getClass.getClassLoader.getResourceAsStream(file)).mkString
      .replace("{xsdport}", port.toString())
      .getBytes()

}
