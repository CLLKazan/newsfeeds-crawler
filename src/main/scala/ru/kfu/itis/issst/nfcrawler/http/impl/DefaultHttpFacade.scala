/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.http.impl

import ru.kfu.itis.issst.nfcrawler.http.HttpFacade
import grizzled.slf4j.Logging
import org.apache.http.params.HttpParams
import org.apache.http.params.SyncBasicHttpParams
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.params.HttpClientParams
import ru.kfu.itis.issst.nfcrawler.http.HttpConfig
import java.io.InputStream
import org.apache.http.client.methods.HttpGet
import org.apache.http.HttpStatus
import org.apache.http.util.EntityUtils
import java.io.Reader
import java.net.URL
import DefaultHttpFacade._
import org.apache.http.HttpResponse
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.HttpResponseInterceptor
import org.apache.http.HttpResponse
import org.apache.http.protocol.HttpContext
import org.apache.http.client.entity.GzipDecompressingEntity
import org.apache.http.params.HttpParamsNames
import scala.collection.JavaConversions.iterableAsScalaIterable

/**
 * @author Rinat Gareev
 *
 */
private[http] class DefaultHttpFacade(cfg: HttpConfig) extends HttpFacade with Logging {

  private val httpClient = {
    // default parameters of PoolingClientConnectionManager seems to be good enough
    val conManager = new PoolingClientConnectionManager
    new DefaultHttpClient(conManager, getHttpClientParams())
  }
  // print http-client parameters (they can be overridden by request parameters)
  info("Http parameters of %s:\n%s".format(httpClient.getClass().getSimpleName(),
    printHttpParams(httpClient.getParams())))
  // add gzipped content handler
  httpClient.addResponseInterceptor(new HttpResponseInterceptor() {
    override def process(response: HttpResponse, context: HttpContext) {
      val entity = response.getEntity();
      if (entity != null) {
        val ceheader = entity.getContentEncoding();
        if (ceheader != null) {
          val codecs = ceheader.getElements();
          if (codecs.exists(c => "gzip".equalsIgnoreCase(c.getName)))
            response.setEntity(new GzipDecompressingEntity(response.getEntity()));
          return ;
        }
      }
    }
  });

  override def getContent(url: URL): String = {
    val httpGet = new HttpGet(url.toURI)
    debug("Request %s is about to be executed. Parameters:\n%s".format(
      httpGet, printHttpParams(httpGet.getParams())))
    val response = httpClient.execute(httpGet)

    val statusLine = response.getStatusLine()
    assert(statusLine != null, "Status line is null in response to GET %s".format(url))

    val statusCode = statusLine.getStatusCode()
    statusCode match {
      case HttpStatus.SC_OK => {
        val entity = response.getEntity()
        assert(entity != null, "Empty response entity for GET %s".format(url))
        EntityUtils.toString(entity, DefaultContentCharset)
      }
      case _ => {
        deallocateConnectionResources(response)
        // TODO flush response body to file for debug purposes
        error("Response to HTTP GET %s has status code : %s".format(url, statusCode))
        null
      }
    }
  }

  override def close() {
    httpClient.getConnectionManager().shutdown()
  }

  private def deallocateConnectionResources(response: HttpResponse) {
    val entity = response.getEntity()
    if (entity != null) {
      // deallocate resources
      EntityUtils.consumeQuietly(entity)
    }
  }

  private def getHttpClientParams(): HttpParams = {
    val params = new SyncBasicHttpParams()
    DefaultHttpClient.setDefaultHttpParams(params)
    // set parameters from facade cfg
    for ((paramName, paramVal) <- cfg.clientHttpParams)
      params.setParameter(paramName, paramVal)
    params
  }

  private def printHttpParams(params: HttpParams): String = params match {
    case pNames: HttpParamsNames =>
      val pValues = for (pname <- pNames.getNames())
        yield pname -> params.getParameter(pname)
      pValues.toMap.mkString("\n")
    case _ => "Unknown subtype of HttpParams: %s".format(params.getClass())
  }

}

private object DefaultHttpFacade {
  val DefaultContentCharset = "utf-8"
}