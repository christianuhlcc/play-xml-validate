package play.api.libs

import java.io.InputStream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import de.codecentric.play.xml.validate.test.OrderType
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Port
import play.api.mvc.{ RequestHeader, _ }
import play.api.routing.sird._
import play.api.test.Helpers._
import play.api.test._
import play.core.server.Server

import scala.io.Source.fromInputStream

class IntegrationSpec extends PlaySpec with Results with XmlValidatingBinder with MockitoSugar {

  implicit val ec     = scala.concurrent.ExecutionContext.global
  implicit val system = ActorSystem()

  val LocalXml   = "sample_local.xml"
  val SchemaXml  = "sample.xml"
  val SampleXSD  = "sample.xsd"
  val InvalidXml = "sample_broken.xml"

  val requestHeader = mock[RequestHeader]

  "POST with a well formed XML" should {
    "perform databinding" in {
      implicit val materializer = ActorMaterializer()
      val rawXML                = scala.xml.XML.load(loadXmlAsStream(LocalXml))
      val action: EssentialAction =
        Action(XmlValidatingBinder.bindXml[OrderType](withXMLValidation = false)) { request =>
          Ok(request.body.name)
        }

      val request = FakeRequest("POST", "/").withXmlBody(rawXML)
      val result  = call(action, request)

      status(result) mustBe 200
      contentAsString(result) mustBe "someName"
    }
  }

  "POST with a reachable schema" should {
    "validate a valid xml" in {

      when(requestHeader.charset).thenReturn(Some("UTF-8"))
      implicit val materializer = ActorMaterializer()
      Server.withRouter() {
        case play.api.routing.sird.GET(p"/sample.xsd") =>
          Action {
            Results.Ok.sendResource(SampleXSD)
          }
      } { implicit port =>
        val xml = scala.xml.XML.loadString(loadXmlFromClasspathAndSetPort(SchemaXml, port))
        val action: EssentialAction = Action(XmlValidatingBinder.bindXml[OrderType]()) { request =>
          Ok(request.body.name)
        }

        val request = FakeRequest("POST", "/").withXmlBody(xml)
        val result  = call(action, request)
        status(result) mustBe 200
      }
    }

    "reject an invalid XML " in {
      when(requestHeader.charset).thenReturn(Some("UTF-8"))
      implicit val materializer = ActorMaterializer()
      Server.withRouter() {
        case play.api.routing.sird.GET(p"/sample.xsd") =>
          Action {
            Results.Ok.sendResource(SampleXSD)
          }
      } { implicit port =>
        val xml = scala.xml.XML.loadString(loadXmlFromClasspathAndSetPort(InvalidXml, port))
        val action: EssentialAction = Action(XmlValidatingBinder.bindXml[OrderType]()) { request =>
          Ok(request.body.name)
        }

        val request = FakeRequest("POST", "/").withXmlBody(xml)
        val result  = call(action, request)
        status(result) mustBe 400
      }
    }
  }

  private def loadXmlAsString(file: String): String =
    fromInputStream(loadXmlAsStream(file)).mkString

  private def loadXmlAsStream(file: String): InputStream =
    getClass.getClassLoader.getResourceAsStream(file)

  private def loadXmlFromClasspathAndSetPort(file: String, port: Port = new Port(0)) =
    fromInputStream(getClass.getClassLoader.getResourceAsStream(file)).mkString
      .replace("{xsdport}", port.toString())
}
