import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.{ImplicitSender, TestKit}
import com.twogis.CrawlerActor.CrawlTask
import com.twogis.{CrawlerActor, HttpServer}
import org.scalamock.function.MockFunction1
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.{AnyWordSpec, AnyWordSpecLike}
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future
import concurrent.duration.DurationInt
import scala.io.{BufferedSource, Source}

class CrawlerActorSpec  extends TestKit(ActorSystem("TestProbeSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with MockFactory {
  import CrawlerActorSpec.*

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "CrawlerActor" should {
    type MockRequest = MockFunction1[HttpRequest, Future[HttpResponse]]

    val validPayload: String = Source.fromResource("requests/validWebsitesPayload.json").read
    val validUsaGovResponseHtml: String = Source.fromResource("responses/validWebsitesResponse_USAGOV.html").read
    val validUsaGovTitle: String = "Official Guide to Government Information and Services | USAGov"
    val validVkResponseHtml: String = Source.fromResource("responses/validWebsitesResponse_VK.html").read
    val validVkTitle: String = "Мобильная версия ВКонтакте | ВКонтакте"

    "return titles on correct input" in {
      given mockSendRequest: MockRequest = mockFunction[HttpRequest, Future[HttpResponse]]
      mockSendRequest.expects(*).onCall((req: HttpRequest) => {
        val reqString = req.uri.toString()
        val response =
          if (reqString.equalsIgnoreCase(`usa.gov`))
            HttpResponse(StatusCodes.OK, entity = HttpEntity(validUsaGovResponseHtml))
          else if (reqString.equalsIgnoreCase(`vk.com`))
            HttpResponse(StatusCodes.OK, entity = HttpEntity(validVkResponseHtml))
          else
            HttpResponse(StatusCodes.BadRequest, entity = HttpEntity("bad request"))
        Future.successful(response)
      }).twice

      val crawler = system.actorOf(CrawlerActor.props(Http())(using mockSendRequest))
      crawler ! CrawlTask(`usa.gov`)
      crawler ! CrawlTask(`vk.com`)

      expectMsgAllOf(5.seconds, validUsaGovTitle, validVkTitle)
    }
  }
}

object CrawlerActorSpec {
  val `vk.com` = "http://www.vk.com"
  val `usa.gov` = "http://www.usa.gov"

  extension(source: BufferedSource) {
    def read: String = {
      val str = source.mkString
      source.close()
      str
    }
  }
}