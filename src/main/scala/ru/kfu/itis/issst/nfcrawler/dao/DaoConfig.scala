/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.dao

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
trait DaoConfig {
  // val daoActorsNumber: Int
  val dbUserName:String
  val dbPassword:String
  val dbUrl:String
  val dbDriverClass:String
}