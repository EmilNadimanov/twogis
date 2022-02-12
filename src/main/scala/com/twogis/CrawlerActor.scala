package com.twogis

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.pipe
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.StatusCodes.*
import akka.http.scaladsl.unmarshalling.Unmarshal

import scala.util.{Failure, Success, Try}
import scala.concurrent.{ExecutionContext, Future}
import java.net.URL
import com.twogis.HttpCrawlerUtils.toUrl
import org.jsoup.Jsoup

import java.util.concurrent.Executors

case class CrawlerActor(http: HttpExt)(sendRequest: HttpRequest => Future[HttpResponse])
  extends Actor with ActorLogging {

  import CrawlerActor.*
  given system: ActorSystem = context.system
  given ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
  val redirectCodesList = List(MovedPermanently, Found, TemporaryRedirect, PermanentRedirect)


  def followRedirect(request: HttpRequest, depth: Int = 3): Future[HttpResponse] = {
    sendRequest(request)
      .flatMap{
        case response @ HttpResponse(code, headers, _, _) if depth > 0 && redirectCodesList.contains(code) =>
          headers
            .find(_.lowercaseName == "location")
            .map(locationHeader => {
              val request = HttpRequest(uri = locationHeader.value())
              followRedirect(request, depth - 1)
            })
            .getOrElse(Future(response))
        case otherResponse => Future(otherResponse)
      }
  }


  override def receive: Receive = {
    case CrawlTask(link) =>
      log.info(s"Got task: $link")
      val title = Try {
        val request = HttpRequest(uri = link.toUrl.toString)
        followRedirect(request)
          .flatMap(response => Unmarshal(response.entity).to[String])
          .map(htmlDoc => Jsoup.parse(htmlDoc).title())
      } match {
        case Success(title) => title
        case Failure(e) => Future(s"")
      }
      title pipeTo sender
    case other => log.error(s"Bad message: $other")
  }
}

object CrawlerActor {
  case class CrawlTask(link: String)

  def props(http: HttpExt)(using sendRequest: HttpRequest => Future[HttpResponse] = http.singleRequest(_)): Props =
    Props(CrawlerActor(http)(sendRequest))
}