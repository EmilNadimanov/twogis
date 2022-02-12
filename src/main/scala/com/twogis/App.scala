package com.twogis

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object App {

  def main(args: Array[String]): Unit = {
    HttpServer.run("localhost", 16666)
  }
}
