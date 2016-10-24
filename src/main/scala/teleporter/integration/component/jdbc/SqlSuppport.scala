package teleporter.integration.component.jdbc

import java.sql.{Connection, PreparedStatement, ResultSet, Statement}
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

import com.google.common.cache.{CacheBuilder, CacheLoader}
import org.apache.commons.dbutils.DbUtils
import teleporter.integration.script.Template
import teleporter.integration.utils.Use

/**
 * Author: kui.dai
 * Date: 2015/11/25.
 */
sealed trait Action

case class Upsert(up: Sql, sert: Sql) extends Action

case class Update(sql: Sql) extends Action

sealed trait Sql

case class NameSql(sql: String, binds: Map[String, Any]) extends Sql {
  def toPreparedSql: PreparedSql = PreparedSql(this)
}

case class PreparedSql(sql: String, params: Seq[Any]) extends Sql

object PreparedSql {
  val paramRegex = "#\\{.+?\\}".r
  val paramGroupRegex = "#\\{(.+?)\\}".r

  case class PredefinedSql(sql: String, paramNames: Seq[String])

  lazy val preparedSqlCache = CacheBuilder.newBuilder()
    .maximumSize(1000).expireAfterWrite(1, TimeUnit.HOURS).build(new CacheLoader[String, PredefinedSql]() {
    override def load(nameSql: String): PredefinedSql = {
      val preparedParams = paramGroupRegex.findAllMatchIn(nameSql).map(_.group(1)).toIndexedSeq
      val preparedSql = paramRegex.replaceAllIn(nameSql, "?")
      PredefinedSql(preparedSql, preparedParams)
    }
  })

  /**
   * inert into table (id,name) values(#{id},#{name})
   */
  def apply(nameSql: NameSql): PreparedSql = {
    val predefinedSql = preparedSqlCache.get(nameSql.sql)
    val params = predefinedSql.paramNames.map {
      name ⇒
        nameSql.binds.applyOrElse(name, null) match {
          case opt: Option[Any] ⇒ opt.orNull
          case v ⇒ v
        }
    }
    val sql = Template(predefinedSql.sql, nameSql.binds)
    PreparedSql(sql, params)
  }
}

case class SqlResult[T](conn: Connection, ps: Statement, rs: ResultSet, result: T) {
  def close(): Unit = {
    DbUtils.closeQuietly(conn, ps, rs)
  }
}

trait SqlSupport extends Use {
  def paramsDefined(col: String): String = s"#{$col}"

  def nameColumns(traversableOnce: TraversableOnce[String]): String = traversableOnce.map(paramsDefined).mkString(",")

  def nameColumnsSet(traversableOnce: TraversableOnce[String]): String = traversableOnce.map(col ⇒ s"$col=${paramsDefined(col)}").mkString(",")

  def doAction(action: Action, ds: DataSource): Unit = action match {
    case Update(sql) ⇒ update(ds.getConnection, sql)
    case Upsert(up, sert) ⇒ if (update(ds.getConnection, up) == 0) update(ds.getConnection, sert)
  }

  def insertSql(tableName: String, data: Map[String, Any]): Sql = {
    val keys = data.keys
    NameSql( s"""insert into $tableName (${keys.mkString(",")}) values (${nameColumns(keys)})""", data)
  }

  def insertIgnoreSql(tableName: String, data: Map[String, Any]): Sql = {
    val keys = data.keys
    NameSql( s"""insert ignore into $tableName (${keys.mkString(",")}) values (${nameColumns(keys)})""", data)
  }

  def updateSql(tableName: String, primaryKeys: String, data: Map[String, Any]): Sql = {
    val keys = data.keys.filter(_ == primaryKeys)
    NameSql( s"""update $tableName set ${nameColumnsSet(keys)} where $primaryKeys=${paramsDefined(primaryKeys)}""", data)
  }

  def updateSql(tableName: String, primaryKeys: String, version: String, data: Map[String, Any]): Sql = {
    val keys = data.keys.filter(_ == primaryKeys)
    NameSql( s"""update $tableName set ${nameColumnsSet(keys)} where $primaryKeys=${paramsDefined(primaryKeys)} and $version > ${paramsDefined(version)}""", data)
  }

  def updateSql(tableName: String, primaryKeys: Seq[String], version: String, data: Map[String, Any]): Sql = {
    val keys = data.keySet -- primaryKeys
    val keysFilter = primaryKeys.map(keys ⇒ s"$keys=${paramsDefined(keys)}").mkString(" and ")
    NameSql( s"""update $tableName set ${nameColumnsSet(keys)} where $keysFilter and $version<${paramsDefined(version)}""", data)
  }

  def update(conn: Connection, sql: Sql): Int =
    sql match {
      case nameSql: NameSql ⇒ update(conn, nameSql)
      case preparedSql: PreparedSql ⇒ update(conn, preparedSql)
    }

  def update(conn: Connection, nameSql: NameSql): Int = {
    update(conn, PreparedSql(nameSql))
  }

  def update(conn: Connection, preparedSql: PreparedSql): Int =
    using(conn) {
      _conn ⇒
        using(_conn.prepareStatement(preparedSql.sql)) {
          ps ⇒
            conn.prepareStatement(preparedSql.sql)
            val params = preparedSql.params
            var i = 1
            for (param ← params) {
              ps.setObject(i, param)
              i += 1
            }
            ps.executeUpdate()
        }
    }

  def toMap(rs: ResultSet) = {
    val metaData = rs.getMetaData
    (1 to rs.getMetaData.getColumnCount).foldLeft(Map.newBuilder[String, Any]) { (b, i) ⇒
      val label = metaData.getColumnLabel(i)
      b += (label → rs.getObject(i))
    }.result()
  }

  def bulkQueryToMap(conn: Connection, preparedSql: PreparedSql): SqlResult[Iterator[Map[String, Any]]] = {
    bulkQuery(conn, preparedSql)(toMap)
  }

  def bulkQuery[T](conn: Connection, preparedSql: PreparedSql)(mapper: ResultSet ⇒ T): SqlResult[Iterator[T]] = {
    var ps: PreparedStatement = null
    var rs: ResultSet = null
    try {
      ps = conn.prepareStatement(preparedSql.sql)
      val params = preparedSql.params
      var i = 1
      for (param ← params) {
        ps.setObject(i, param)
        i += 1
      }
      rs = ps.executeQuery()
      logger.info(s"bulk query sql: ${preparedSql.sql}")
      SqlResult[Iterator[T]](
        conn = conn,
        ps = ps,
        rs = rs,
        result = new Iterator[T] {
          var isTakeOut = true
          var _next = false
          override def hasNext: Boolean =
          if(isTakeOut) {
            _next = rs.next()
            isTakeOut = false
            if (!_next) DbUtils.closeQuietly(conn, ps, rs)
            _next
          } else {
            _next
          }
          override def next(): T = {
            isTakeOut = true
            mapper(rs)
          }
        })
    } catch {
      case e: Exception ⇒
        logger.error(e.getLocalizedMessage, e)
        DbUtils.closeQuietly(conn, ps, rs)
        throw e
    }
  }

  def one(conn: Connection, preparedSql: PreparedSql): Option[Map[String, Any]] = queryToMap(conn, preparedSql).headOption

  def queryToMap(conn: Connection, preparedSql: PreparedSql): Iterable[Map[String, Any]] = {
    query(conn, preparedSql)(toMap)
  }

  def query[T](conn: Connection, preparedSql: PreparedSql)(mapper: ResultSet ⇒ T): Iterable[T] = {
    using(conn) {
      _conn ⇒
        using(conn.prepareStatement(preparedSql.sql)) {
          ps ⇒
            conn.prepareStatement(preparedSql.sql)
            val params = preparedSql.params
            var i = 1
            for (param ← params) {
              ps.setObject(i, param)
              i += 1
            }
            using(ps.executeQuery()) {
              rs ⇒ new Iterator[T] {
                override def hasNext: Boolean = rs.next()

                override def next(): T = mapper(rs)
              }.toIndexedSeq
            }
        }
    }
  }
}

object SqlSupport extends SqlSupport