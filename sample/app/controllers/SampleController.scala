package controllers

import javax.inject._

import com.google.common.base.Charsets
import com.google.common.io.Resources
import de.codecentric.play.xml.validate.sample.OrderType
import play.api.libs.XmlValidatingBinder
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._

import scala.concurrent.Future


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class SampleController @Inject() extends Controller with XmlValidatingBinder {

  /**
    * This route uses the XmlValidatingBinder to bind the XML against the OrderType Case classes
    * generated by Scalaxb
    *
    * If given a network-available XSD, it will also vailidate
    */
  def receiveXML(): Action[OrderType] = Action.async(XmlValidatingBinder.bindXml[OrderType]()) { request =>
    Future{ Ok(s"Order: ${request.body}") }
  }


  /**
    * Little Helper that returns the XSD referenced in the XML to give a sample
    * for network-referenced XSD validation and binding
    * */
  def xsdProvider(filename: String) = Action{
    val xsd = Resources.toString(this.getClass.getClassLoader.getResource("xsd/sample.xsd"), Charsets.UTF_8)
    Ok(xsd)
  }
}