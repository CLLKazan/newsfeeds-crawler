/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.http

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
trait HttpConfig {

  val httpWorkersNumber: Int
  val hostAccessInterval: Int

}