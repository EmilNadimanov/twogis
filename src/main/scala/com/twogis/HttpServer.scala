package com.twogis

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.*
import akka.routing.{RoundRobinGroup, RoundRobinPool, Router}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import spray.json.*

import java.net.URL
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import HttpCrawlerUtils.{buildCrawlStream, toUrl}
import akka.http.scaladsl.server.Route
import com.twogis.CrawlerActor



object HttpServer extends DefaultJsonProtocol with SprayJsonSupport  {
  given system: ActorSystem = ActorSystem("2gisHttpCrawler")
  given ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(20))

  val routeesPaths: Seq[String] = (1 to 10)
    .map(_ => system.actorOf{CrawlerActor.props(Http())})
    .map(_.path.toString)
  val router: ActorRef = system.actorOf(RoundRobinGroup(routeesPaths).props)

  /**
   * HTTP POST <br/>
   * <b>Endpoint</b>: /crawl <br/>
   * <b>ContentType</b>: application/json <br/>
   * <b>Body_Schema</b>: <br/>
   * - urls: array[string] | Non-empty array of urls, from which titles will be extracted <br/>
   * <b>Returns</b>: <br/>
   * - array[string] | titles of the webpages that were passed. Order is retained. <br/>
   * Absence of a title on a page results in an empty string at the corresponding position in this array. Non-valid, non-reachable urls will will yield the same result.
   */
  val api: Route = path("crawl" ) {
    parameter("verbose".optional) { verbose =>
    (post & requestEntityPresent) {
      entity(as[List[String]]) { urls =>
        onComplete {
          val v: Boolean = verbose.isDefined
          buildCrawlStream(urls, router, v).run()
        } {
          case Success(v) => complete(v.toJson)
          case Failure(e) => complete(List.empty[String].toJson)
        }
      }
    }
  }}

  def run(address: String, port: Int): Future[Http.ServerBinding] = {
    Http().newServerAt(address, port).bind(api)
  }
}
