/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.parser

import java.net.URL
import java.util.Date

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class ParsedFeed(val pubDate: Date, val items: List[ParsedFeedItem])

class ParsedFeedItem(val url: URL, val pubDate: Date)
