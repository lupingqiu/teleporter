package teleporter.integration.cluster.broker

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import teleporter.integration.cluster.broker.http.HttpServer
import teleporter.integration.cluster.broker.leveldb.LevelDBService
import teleporter.integration.cluster.broker.tcp.{ConnectionKeeper, RpcServer}
import teleporter.integration.cluster.rpc.proto.Rpc.TeleporterEvent
import teleporter.integration.component.leveldb.LevelDBs
import teleporter.integration.utils.EventListener

import scala.collection.concurrent.TrieMap

/**
  * Created by kui.dai on 2016/8/9.
  */
object Broker {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load("broker")
    implicit val system = ActorSystem("broker", config)
    implicit val mater = ActorMaterializer()
    val configService: PersistentService = LevelDBService(LevelDBs.applyTable("teleporter", "/config"))
    val runtimeService: PersistentService = LevelDBService(LevelDBs.applyTable("teleporter", "/runtime"))
    val connectionKeepers = TrieMap[String, ConnectionKeeper]()
    val configNotify = system.actorOf(Props(classOf[ConfigNotify], connectionKeepers, configService, runtimeService))
    val eventListener = EventListener[TeleporterEvent]()
    val brokerConfig = config.getConfig("teleporter")
    val (bind, port, tcpPort) = (brokerConfig.getString("bind"), brokerConfig.getInt("port"), brokerConfig.getInt("tcpPort"))
    RpcServer(bind, tcpPort, configService, runtimeService, connectionKeepers, eventListener)
    HttpServer(bind, port, configNotify, configService, runtimeService, connectionKeepers, eventListener)
  }
}