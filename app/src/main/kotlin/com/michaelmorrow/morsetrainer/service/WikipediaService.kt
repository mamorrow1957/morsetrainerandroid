package com.michaelmorrow.morsetrainer.service

import com.michaelmorrow.morsetrainer.model.ArticleModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object WikipediaService {

    private const val API_URL = "https://en.wikipedia.org/api/rest_v1/page/random/summary"
    private const val TIMEOUT_MS = 10_000

    suspend fun fetchRandomArticle(): ArticleModel = withContext(Dispatchers.IO) {
        val connection = URL(API_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "MorseTrainerAndroid/1.0")

        try {
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP $responseCode")
            }
            val json = connection.inputStream.bufferedReader().readText()
            parseArticle(json)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseArticle(json: String): ArticleModel {
        val obj = JSONObject(json)
        val title = obj.getString("title")
        val extract = obj.optString("extract", "")

        val urls = obj.optJSONObject("content_urls")
        val url = urls?.optJSONObject("mobile")?.optString("page", "")
            ?: urls?.optJSONObject("desktop")?.optString("page", "")
            ?: ""

        return ArticleModel(
            title = title,
            sentence = SentenceExtractor.extract(extract),
            url = url,
        )
    }
}
