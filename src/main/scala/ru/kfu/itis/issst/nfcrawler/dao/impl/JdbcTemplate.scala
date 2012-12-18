/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.dao.impl
import javax.sql.DataSource
import java.sql.ResultSet
import java.sql.Connection
import java.sql.PreparedStatement
import org.apache.commons.io.IOUtils
import java.util.Date
import java.sql.Types
import java.sql.Statement
import java.sql.Timestamp

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
trait JdbcTemplate {

  protected type ParamSetter = PreparedStatement => Unit

  protected val ds: DataSource

  protected def getSingleResult[R](queryStr: String, paramSetters: ParamSetter*)(rsHandler: ResultSet => R): Option[R] =
    withResultSet(queryStr)(paramSetters: _*)(rs => {
      if (rs.next()) {
        val result = Some(rsHandler(rs))
        if (rs.next())
          throw new IllegalStateException("More than one result for query:\n%s".format(queryStr))
        result
      } else None
    })

  protected def withResultSet[R](queryStr: String)(paramSetters: ParamSetter*)(rsHandler: ResultSet => R): R =
    withPreparedStatement(queryStr)(stmt => {
      paramSetters.foreach(_(stmt))
      val rs = stmt.executeQuery()
      try {
        rsHandler(rs)
      } finally {
        rs.close()
      }
    })

  protected def update(stmtStr: String)(paramSetters: ParamSetter*): Unit =
    update(stmtStr, None)(paramSetters: _*)

  protected def update(stmtStr: String, expectedChanges: Int)(paramSetters: ParamSetter*): Unit =
    update(stmtStr, Some(expectedChanges))(paramSetters: _*)

  protected def update(stmtStr: String, expectedChanges: Option[Int])(paramSetters: ParamSetter*): Unit =
    withPreparedStatement(stmtStr)(stmt => {
      paramSetters.foreach(_(stmt))
      val actualChanges = stmt.executeUpdate()
      if (expectedChanges.isDefined && actualChanges != expectedChanges.get)
        throw new IllegalStateException("Unexpected number of changes for statement:\n%s\n" +
          "Expected: %s, actual: %s".format(stmtStr, expectedChanges.get))
    })

  /**
   * KT - key type (Int, Long, etc)
   */
  protected def insertSingle[KT](stmtStr: String, keyExtractor: ResultSet => KT)(paramSetters: ParamSetter*): KT =
    withPreparedStatementGK(stmtStr)(stmt => {
      paramSetters.foreach(_(stmt))
      val changes = stmt.executeUpdate()
      if (changes == 0)
        throw new IllegalStateException("No changes caused by statement:\n%s".format(stmtStr))
      val gkrs = stmt.getGeneratedKeys()
      if (gkrs.next()) {
        val result = keyExtractor(gkrs)
        if (gkrs.next())
          throw new IllegalStateException("Generated keys result set contains > 1 entry")
        result
      } else throw new IllegalStateException("No rows in generated keys result set")
    })

  protected def withPreparedStatement[R](queryStr: String)(stmtHandler: PreparedStatement => R): R =
    withConnection(con => {
      val stmt = con.prepareStatement(queryStr)
      try {
        stmtHandler(stmt)
      } finally {
        stmt.close()
      }
    })

  /**
   * with generated keys
   */
  protected def withPreparedStatementGK[R](queryStr: String)(stmtHandler: PreparedStatement => R): R =
    withConnection(con => {
      val stmt = con.prepareStatement(queryStr, Statement.RETURN_GENERATED_KEYS)
      try {
        stmtHandler(stmt)
      } finally {
        stmt.close()
      }
    })

  protected def withConnection[R](conHandler: Connection => R): R = {
    val con = ds.getConnection()
    try {
      conHandler(con)
    } finally {
      con.close()
    }
  }

  protected def readClob(rs: ResultSet, colName: String): String = {
    val clob = rs.getClob(colName)
    try {
      val reader = clob.getCharacterStream()
      IOUtils.toString(reader)
    } finally {
      clob.free()
    }
  }

  protected def setTimestamp(stmt: PreparedStatement, param: Int, value: Date): Unit =
    if (value != null)
      stmt.setTimestamp(param, new java.sql.Timestamp(value.getTime()))
    else stmt.setNull(param, Types.TIMESTAMP)

  protected def getTimestamp(rs: ResultSet, colName: String): Date =
    rs.getTimestamp(colName) match {
      case null => null
      case x: Timestamp => new Date(x.getTime())
    }
}