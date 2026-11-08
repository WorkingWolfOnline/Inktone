package com.inktone.app.data

import android.content.Context
import com.inktone.app.data.model.Card
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

object CardRepository {

    private const val FILE = "cards.json"

    fun loadAll(context: Context): List<Card> {
        val file = File(context.filesDir, FILE)
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            val list = mutableListOf<Card>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    Card(
                        id = o.getString("id"),
                        front = o.getString("front"),
                        back = o.getString("back"),
                        createdAt = o.optLong("createdAt", 0L)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(context: Context, front: String, back: String): Card {
        val card = Card(UUID.randomUUID().toString(), front, back, System.currentTimeMillis())
        val cards = loadAll(context).toMutableList()
        cards.add(card)
        saveAll(context, cards)
        return card
    }

    fun delete(context: Context, id: String) {
        saveAll(context, loadAll(context).filterNot { it.id == id })
    }

    private fun saveAll(context: Context, cards: List<Card>) {
        val arr = JSONArray()
        cards.forEach { c ->
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("front", c.front)
                    .put("back", c.back)
                    .put("createdAt", c.createdAt)
            )
        }
        File(context.filesDir, FILE).writeText(arr.toString())
    }
}
