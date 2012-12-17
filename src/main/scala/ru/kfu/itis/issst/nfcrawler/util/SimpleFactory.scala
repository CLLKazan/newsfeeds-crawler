/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.util

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
trait SimpleFactory[C, T] {
  type Builder = C => T
  private[this] var builder: Builder = defaultBuilder

  def setBuilder(newBuilder: Builder): Unit = this.builder = newBuilder

  def get(config: C): T = builder(config)

  protected def defaultBuilder(config: C): T
}