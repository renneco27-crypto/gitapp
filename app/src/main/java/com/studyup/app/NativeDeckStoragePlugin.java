package com.studyup.app;

import android.content.Context;
import android.util.Log;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

@CapacitorPlugin(name = "NativeDeckStorage")
public class NativeDeckStoragePlugin extends Plugin {

    private static final String TAG = "NativeDeckStorage";
    private static final String DECKS_DIR_NAME = "decks";

    private File getDecksDir() {
        Context context = getContext();
        File dir = context.getExternalFilesDir(DECKS_DIR_NAME);
        if (dir == null) {
            dir = new File(context.getFilesDir(), DECKS_DIR_NAME);
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    @PluginMethod
    public void saveDeckCSV(PluginCall call) {
        String id = call.getString("id");
        String title = call.getString("title", "Untitled Deck");
        String csvContent = call.getString("csvContent");
        String subject = call.getString("subject", "General");

        if (id == null || id.isEmpty()) {
            call.reject("Missing required deck id");
            return;
        }

        if (csvContent == null) {
            call.reject("Missing csvContent");
            return;
        }

        try {
            File dir = getDecksDir();
            File csvFile = new File(dir, id + ".csv");
            File metaFile = new File(dir, id + ".json");

            // Write CSV
            try (FileOutputStream fos = new FileOutputStream(csvFile)) {
                fos.write(csvContent.getBytes(StandardCharsets.UTF_8));
            }

            // Write Meta
            JSObject meta = new JSObject();
            meta.put("id", id);
            meta.put("title", title);
            meta.put("subject", subject);
            meta.put("updatedAt", System.currentTimeMillis());

            try (FileOutputStream fos = new FileOutputStream(metaFile)) {
                fos.write(meta.toString().getBytes(StandardCharsets.UTF_8));
            }

            JSObject result = new JSObject();
            result.put("success", true);
            result.put("filePath", csvFile.getAbsolutePath());
            call.resolve(result);
        } catch (Exception e) {
            Log.e(TAG, "Error saving deck CSV", e);
            call.reject("Failed to save deck CSV: " + e.getMessage());
        }
    }

    @PluginMethod
    public void getDeckCSV(PluginCall call) {
        String id = call.getString("id");
        if (id == null || id.isEmpty()) {
            call.reject("Missing deck id");
            return;
        }

        try {
            File dir = getDecksDir();
            File csvFile = new File(dir, id + ".csv");
            File metaFile = new File(dir, id + ".json");

            if (!csvFile.exists()) {
                call.reject("Deck file not found: " + id);
                return;
            }

            String csvContent;
            try (FileInputStream fis = new FileInputStream(csvFile)) {
                byte[] data = new byte[(int) csvFile.length()];
                fis.read(data);
                csvContent = new String(data, StandardCharsets.UTF_8);
            }

            String title = "Untitled Deck";
            String subject = "General";

            if (metaFile.exists()) {
                try (FileInputStream fis = new FileInputStream(metaFile)) {
                    byte[] data = new byte[(int) metaFile.length()];
                    fis.read(data);
                    String metaJson = new String(data, StandardCharsets.UTF_8);
                    JSONObject obj = new JSONObject(metaJson);
                    title = obj.optString("title", title);
                    subject = obj.optString("subject", subject);
                } catch (Exception ignored) {}
            }

            JSObject result = new JSObject();
            result.put("id", id);
            result.put("title", title);
            result.put("subject", subject);
            result.put("csvContent", csvContent);
            call.resolve(result);
        } catch (Exception e) {
            Log.e(TAG, "Error getting deck CSV", e);
            call.reject("Failed to get deck CSV: " + e.getMessage());
        }
    }

    @PluginMethod
    public void listSavedDecks(PluginCall call) {
        try {
            File dir = getDecksDir();
            File[] files = dir.listFiles((d, name) -> name.endsWith(".csv"));
            JSArray deckList = new JSArray();

            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    String id = name.substring(0, name.length() - 4);
                    File metaFile = new File(dir, id + ".json");

                    String title = id;
                    String subject = "General";

                    if (metaFile.exists()) {
                        try (FileInputStream fis = new FileInputStream(metaFile)) {
                            byte[] data = new byte[(int) metaFile.length()];
                            fis.read(data);
                            String metaJson = new String(data, StandardCharsets.UTF_8);
                            JSONObject obj = new JSONObject(metaJson);
                            title = obj.optString("title", title);
                            subject = obj.optString("subject", subject);
                        } catch (Exception ignored) {}
                    }

                    JSObject item = new JSObject();
                    item.put("id", id);
                    item.put("title", title);
                    item.put("subject", subject);
                    item.put("sizeBytes", file.length());
                    item.put("lastModified", file.lastModified());
                    deckList.put(item);
                }
            }

            JSObject result = new JSObject();
            result.put("decks", deckList);
            call.resolve(result);
        } catch (Exception e) {
            Log.e(TAG, "Error listing saved decks", e);
            call.reject("Failed to list saved decks: " + e.getMessage());
        }
    }

    @PluginMethod
    public void deleteDeckCSV(PluginCall call) {
        String id = call.getString("id");
        if (id == null || id.isEmpty()) {
            call.reject("Missing deck id");
            return;
        }

        try {
            File dir = getDecksDir();
            File csvFile = new File(dir, id + ".csv");
            File metaFile = new File(dir, id + ".json");

            boolean deleted = false;
            if (csvFile.exists()) {
                deleted = csvFile.delete();
            }
            if (metaFile.exists()) {
                metaFile.delete();
            }

            JSObject result = new JSObject();
            result.put("success", deleted);
            call.resolve(result);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting deck CSV", e);
            call.reject("Failed to delete deck CSV: " + e.getMessage());
        }
    }
}
