import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.twogis.HttpServer
import org.scalamock.function.MockFunction1
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future
import scala.io.{BufferedSource, Source}

class HttpCrawlerSpec extends AnyWordSpec 
  with Matchers 
  with ScalatestRouteTest
  with DefaultJsonProtocol
  with SprayJsonSupport {
  import HttpCrawlerSpec.*
  val crawlerApi: Route = HttpServer.api

  "HTTP crawler" should {
    "return empty string on error" in {
      val invalidPayload = Source.fromResource("requests/invalidWebsitesPayload.json").read
      val expected = List("", "")

      Post("/crawl").withEntity(ContentTypes.`application/json`, invalidPayload) ~> crawlerApi ~> check {
        status shouldBe StatusCodes.OK
        entityAs[List[String]] shouldBe expected
      }
    }
  }
}

object HttpCrawlerSpec {
  extension(source: BufferedSource) {
    def read: String = {
      val str = source.mkString
      source.close()
      str
    }
  }
}