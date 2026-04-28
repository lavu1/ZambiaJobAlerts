package com.solutions.alphil.zambiajobalerts.classes;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GeneratedDocumentStore {

    private static final String PREFS_NAME = "generated_documents_prefs";
    private static final String KEY_DOCUMENTS_JSON = "generated_documents_json";
    private static final int MAX_DOCUMENTS = 200;

    private final SharedPreferences prefs;
    private final Gson gson;

    public GeneratedDocumentStore(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public List<GeneratedDocument> getAll() {
        String raw = prefs.getString(KEY_DOCUMENTS_JSON, null);
        if (raw == null || raw.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Type type = new TypeToken<ArrayList<GeneratedDocument>>() {}.getType();
            List<GeneratedDocument> items = gson.fromJson(raw, type);
            return items == null ? new ArrayList<>() : items;
        } catch (JsonSyntaxException e) {
            return new ArrayList<>();
        }
    }

    public void save(GeneratedDocument item) {
        List<GeneratedDocument> items = getAll();
        items.add(0, item);

        if (items.size() > MAX_DOCUMENTS) {
            items = new ArrayList<>(items.subList(0, MAX_DOCUMENTS));
        }

        saveAll(items);
    }

    public void remove(String documentId) {
        List<GeneratedDocument> items = getAll();
        Iterator<GeneratedDocument> iterator = items.iterator();
        while (iterator.hasNext()) {
            if (documentId != null && documentId.equals(iterator.next().getId())) {
                iterator.remove();
            }
        }
        saveAll(items);
    }

    public void clear() {
        prefs.edit().remove(KEY_DOCUMENTS_JSON).apply();
    }

    private void saveAll(List<GeneratedDocument> items) {
        String raw = gson.toJson(items);
        prefs.edit().putString(KEY_DOCUMENTS_JSON, raw).apply();
    }
}
