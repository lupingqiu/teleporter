package teleporter.integration.component

import java.util.Date

import akka.actor.ActorLogging
import akka.stream.actor.ActorSubscriberMessage.OnNext
import akka.stream.actor.{ActorSubscriber, RequestStrategy}
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.engine.{DocumentAlreadyExistsException, VersionConflictEngineException}
import teleporter.integration.conf.Conf
import teleporter.integration.core._

import scala.annotation.tailrec

/**
 * Created by Yukai.wu on 2015/9/24.
 */
class ElasticSearchAddressBuilder(override val conf: Conf.Address)(implicit val center: TeleporterCenter)
  extends AddressBuilder[TransportClient] {
  override def build: Address[TransportClient] = {
    val settings = ImmutableSettings.settingsBuilder().put("cluster.name", conf.props("clusterName").toString).build()
    val transportClient = new TransportClient(settings)
    val port = conf.props.getOrElse("clusterPort", "9300").toString.toInt
    conf.props("clusterHosts")
      .toString
      .split(";")
      .foreach(ip => transportClient.addTransportAddress(new InetSocketTransportAddress(ip, port)))
    new AutoCloseAddress[TransportClient](conf, transportClient)
  }
}


class ElasticSubscriber(val id: Int)(implicit center: TeleporterCenter)
  extends ActorSubscriber with Component with ActorLogging {

  import teleporter.integration.component.ElasticComponent._

  val conf = center.sinkFactory.loadConf(id)
  val client = center.addressing[TransportClient](conf.addressId.get, conf)
  override protected val requestStrategy: RequestStrategy = RequestStrategyManager(conf.props)

  def receive = {
    case OnNext(element: TeleporterElasticRecord) =>
      val record = element.data
      record.op match {
        case ElasticAction.INSERT => doUpsert(record)
        case ElasticAction.DELETE => delete(record)
        case _ => log.error(s"[ElasticComponent] : Not has the operation : ${record.op.toString}")
      }
      element.toNext(element)
    case _ => log.error("[ElasticComponent] : Receive wrong message!")
  }

  /**
   * 执行insert, update任务逻辑
   **/
  private def doUpsert(element: ElasticRecord): Unit = {
    val getResponse: GetResponse = client
      .prepareGet(element.indexName, element.typeName, element.id)
      .setRouting(element.routing)
      .execute().actionGet()
    if (getResponse != null) {
      if (getResponse.isExists) {
        val sourceMap = getResponse.getSource
        element.modified match {
          case Some(modified) =>
            log.info("[ElasticComponent] : Updating or inserting with modified")
            val esModified = sourceMap.get("modified")
            if (esModified == null || modified.after(new Date(esModified.asInstanceOf[Long]))) {
              update(element, getResponse)
            }
          case None => update(element, getResponse)
        }
      } else {
        try {
          insert(element)
        } catch {
          case e: DocumentAlreadyExistsException =>
            log.warning("[ElasticComponent] : Document already exists and will try again", e)
            update(element, getResponse)
        }
      }
    }
  }

  /**
   * 当es中数据不存在时，插入操作。
   **/
  private def insert(element: ElasticRecord): Unit = {
    val response = client
      .prepareIndex(element.indexName, element.typeName)
      .setId(element.id).setRouting(element.routing)
      .setOpType(IndexRequest.OpType.CREATE)
      .setSource(element.doc.getBytes("utf-8"))
      .execute().actionGet()
    if (response != null && response.isCreated) {
      log.debug(s"[ElasticComponente] : Insert successful with id: ${element.id}")
    } else {
      log.warning(s"[ElasticComponente] : Index failed for response is null  or created false for id:${element.id}")
    }
  }

  /**
   * 当es中数据已存在时，更新操作。
   **/
  @tailrec
  private def update(element: ElasticRecord, getResponse: GetResponse): Unit = {
    try {
      val updateResponse = client
        .prepareUpdate(element.indexName, element.typeName, element.id)
        .setDoc(element.doc.getBytes("utf-8"))
        .setVersion(getResponse.getVersion)
        .setRouting(element.routing)
        .execute().actionGet()
      if (updateResponse != null && updateResponse.getVersion == getResponse.getVersion + 1)
        log.info(s"[ElasticComponent] : update successful with id:${element.id}")
    } catch {
      case e: VersionConflictEngineException =>
        log.warning("[ElasticComponent] : update version conflict and will try again", e)
        update(element, getResponse)
    }
  }


  /**
   * 当es中数据已存在时，删除操作。数据不存在不执行操作
   **/
  private def delete(element: ElasticRecord): Unit = {
    log.info(s"[ElasticComponent] : Delete element id:${element.id}")
    val deleteResponse = client
      .prepareDelete(element.indexName, element.typeName, element.id)
      .setRouting(element.routing)
      .execute().actionGet()
    if (deleteResponse.isFound) {
      log.debug(s"[ElasticComponent] : Delete element id:${element.id} successful!")
    } else {
      log.warning(s"[ElasticComponent] : Element id:${element.id} is not existed in ElasticSearch, Delete failed!")
    }
  }
}


object ElasticComponent {

  /**
   * Elastic task
   * @param doc 存储文档 json格式
   * @param indexName 索引名称
   * @param typeName 文档类型
   * @param id 文档id,一般为 num_iid
   * @param routing 分片路径,一般为shop_id
   * @param modified 修改日期,没有设为None
   * @param op 执行的操作,目前有INSERT,DELETE
   **/
  case class ElasticRecord(doc: String,
                           indexName: String,
                           typeName: String,
                           id: String,
                           routing: String,
                           modified: Option[Date],
                           op: ElasticAction.Value)

}

object ElasticAction extends Enumeration {
  //Elastic 支持的操作方法
  type ElasticAction = Value
  val INSERT, DELETE = Value
}