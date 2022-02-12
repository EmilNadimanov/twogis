package com.twogis

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.Timeout
import CrawlerActor.CrawlTask

import scala.concurrent.duration.DurationInt
import java.net.URL
import scala.util.Try

object HttpCrawlerUtils {
  given timeout: Timeout = Timeout(5.seconds)

  extension(link: String) {
    def toUrl: URL = new URL(link)
  }

  def buildCrawlStream(urls: List[String], router: ActorRef) = {
    val sink = Sink.fold[List[String], String](List.empty[String])((list, title) => list :+ title)
    Source(urls)
      .mapAsync(10)(url => (router ? CrawlTask(url)).mapTo[String])
      .toMat(sink)(Keep.right)
  }
}
