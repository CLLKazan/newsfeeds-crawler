/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.dao.impl

import ru.kfu.itis.issst.nfcrawler.dao.FeedArticleDao
import ru.kfu.itis.issst.nfcrawler.dao.DaoConfig
import javax.sql.DataSource
import org.apache.commons.dbcp.BasicDataSource
import ru.kfu.itis.issst.nfcrawler.dao.FeedArticleDao

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class MysqlFeedArticleDao private(ds : DataSource) extends FeedArticleDao {

  
  
}

object MysqlFeedArticleDao {
  def build(config : DaoConfig):FeedArticleDao = {
    val ds = new BasicDataSource
    ds.setDriverClassName("com.mysql.jdbc.Driver")
    ds.setUrl(config.dbUrl)
    ds.setUsername(config.dbUserName)
    ds.setPassword(config.dbPassword)
    ds.setInitialSize(1)
    new MysqlFeedArticleDao(ds)
  }
}