package play.api.libs

import java.util.Locale
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import akka.stream.scaladsl.Sink
import akka.util.ByteString
import org.xml.sax.InputSource
import play.api.Logger
import play.api.libs.streams.Accumulator
import play.api.mvc._

import scala.collection.immutable.List
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.xml.XML
import scalaxb.XMLFormat

trait XmlValidatingBinder extends Results {

  val defaultContentType                    = "application/xml"
  private val xmlContentTypes: List[String] = List("text/xml", defaultContentType)

  object XmlValidatingBinder {

    import BodyParsers._

    @inline def DefaultMaxTextLength: Long = parse.DefaultMaxTextLength.toLong

    private val logger = Logger(classOf[XmlValidatingBinder])

    private lazy val schema = {
      val factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
      factory.newSchema()
    }

    def bindXml[BO](
        withXMLValidation: Boolean = true,
        withContentType: String = defaultContentType
    )(implicit format: XMLFormat[BO]): BodyParser[BO] =
      parse.when(
        _.contentType.exists { t =>
          val tl = t.toLowerCase(Locale.ENGLISH)
          xmlContentTypes
            .exists(tl.startsWith(_)) || parse.ApplicationXmlMatcher.pattern.matcher(tl).matches()
        },
        tolerantBodyParser("xml",
                           DefaultMaxTextLength,
                           withXMLValidation,
                           "Invalid XML",
                           withContentType,
                           format)(binder),
        createBadResult("Expecting XML body", withContentType, UnsupportedMediaType)
      )

    private[play] def binder[BO](request: RequestHeader,
                                 bytes: ByteString,
                                 withXMLValidation: Boolean,
                                 format: XMLFormat[BO]): BO = {
      def validateBody() =
        if (withXMLValidation) {
          val validator = schema.newValidator()
          validator.validate(new StreamSource(bytes.iterator.asInputStream))
        }

      def parseBody() = {
        val inputSource = new InputSource(bytes.iterator.asInputStream)
        request.charset
          .orElse(
            request.mediaType.collect {
              case mt if mt.mediaType == "text" => "iso-8859-1"
            }
          )
          .foreach { charset =>
            inputSource.setEncoding(charset)
          }
        XML.load(inputSource)
      }
      validateBody
      scalaxb.fromXML[BO](parseBody)(format)
    }

    private def createBadResult(errorMessage: String,
                                contentType: String,
                                status: Status = BadRequest): RequestHeader => Future[Result] = {
      _ =>
        Future.successful(status(errorMessage).as(contentType))
    }

    /**
      * THIS METHOD IS BLATANTLY STOLEN FROM PLAY
      * SINCE IT IS PRIVATE WE NEED A COPY OF IT
      */
    private def tolerantBodyParser[BO](
        name: String,
        maxLength: Long,
        validate: Boolean,
        errorMessage: String,
        contentType: String,
        format: XMLFormat[BO]
    )(parser: (RequestHeader, ByteString, Boolean, XMLFormat[BO]) => BO): BodyParser[BO] =
      BodyParser(name + ", maxLength=" + maxLength) { request =>
        import play.api.libs.iteratee.Execution.Implicits.trampoline
        parse.enforceMaxLength(
          request,
          maxLength,
          Accumulator(
            Sink.fold[ByteString, ByteString](ByteString.empty)((state, bs) => state ++ bs)
          ) mapFuture { bytes =>
            try {
              Future.successful(Right(parser(request, bytes, validate, format)))
            } catch {
              case NonFatal(e) =>
                logger.debug(errorMessage, e)
                createBadResult(errorMessage + ": " + e.getMessage, contentType)(request)
                  .map(Left(_))
            }
          }
        )
      }
  }

}
