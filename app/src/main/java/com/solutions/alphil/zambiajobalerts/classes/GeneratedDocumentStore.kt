package com.solutions.alphil.zambiajobalerts.classes

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

class GeneratedDocumentStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAll(): List<GeneratedDocument> {
        val raw = prefs.getString(KEY_DOCUMENTS_JSON, null)
        if (raw.isNullOrBlank()) return ArrayList()

        return try {
            val type = object : TypeToken<ArrayList<GeneratedDocument>>() {}.type
            gson.fromJson<List<GeneratedDocument>>(raw, type) ?: ArrayList()
        } catch (_: JsonSyntaxException) {
            ArrayList()
        }
    }

    fun save(item: GeneratedDocument) {
        var items = getAll().toMutableList()
        items.add(0, item)

        if (items.size > MAX_DOCUMENTS) {
            items = ArrayList(items.subList(0, MAX_DOCUMENTS))
        }

        saveAll(items)
    }

    fun remove(documentId: String?) {
        val items = getAll().toMutableList()
        items.removeAll { documentId != null && documentId == it.id }
        saveAll(items)
    }

    fun clear() {
        prefs.edit().remove(KEY_DOCUMENTS_JSON).apply()
    }

    private fun saveAll(items: List<GeneratedDocument>) {
        prefs.edit().putString(KEY_DOCUMENTS_JSON, gson.toJson(items)).apply()
    }

    companion object {
        private const val PREFS_NAME = "generated_documents_prefs"
        private const val KEY_DOCUMENTS_JSON = "generated_documents_json"
        private const val MAX_DOCUMENTS = 200
    }
}
