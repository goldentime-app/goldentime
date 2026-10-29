package ai.seroai.goldentime.data.hospital

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** 국립중앙의료원 공공데이터 API 공통 호출 (XML) */
internal object NemcApi {
    /** XML 응답의 <item> 목록을 태그→값 맵으로 */
    fun items(url: String, params: Map<String, String>, serviceKey: String): List<Map<String, String>> {
        val query = (params + ("serviceKey" to serviceKey)).entries.joinToString("&") { (k, v) ->
            // 포털에서 받은 Encoding 키는 이미 인코딩되어 있으므로 그대로 사용
            if (k == "serviceKey" && v.contains('%')) "$k=$v" else "$k=${URLEncoder.encode(v, "UTF-8")}"
        }
        val conn = URL("$url?$query").openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        return conn.inputStream.use { stream ->
            val parser = Xml.newPullParser().apply { setInput(stream, "UTF-8") }
            val out = mutableListOf<Map<String, String>>()
            var cur: MutableMap<String, String>? = null
            var tag: String? = null
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> {
                        tag = parser.name
                        if (tag == "item") cur = mutableMapOf()
                    }
                    XmlPullParser.TEXT -> if (cur != null && tag != null) cur[tag] = parser.text.trim()
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item") cur?.let { out += it }.also { cur = null }
                        tag = null
                    }
                }
            }
            out
        }.also { conn.disconnect() }
    }
}
