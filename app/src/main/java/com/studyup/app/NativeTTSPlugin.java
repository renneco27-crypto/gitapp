package com.studyup.app;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.util.Locale;

@CapacitorPlugin(name = "NativeTTS")
public class NativeTTSPlugin extends Plugin implements TextToSpeech.OnInitListener {

    private static final String TAG = "NativeTTS";
    private TextToSpeech tts;
    private boolean isInitialized = false;

    @Override
    public void load() {
        super.load();
        initTTS();
    }

    private synchronized void initTTS() {
        if (tts == null && getContext() != null) {
            try {
                tts = new TextToSpeech(getContext().getApplicationContext(), this);
            } catch (Exception e) {
                Log.e(TAG, "Failed creating TextToSpeech instance", e);
            }
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS && tts != null) {
            int res = tts.setLanguage(Locale.US);
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "US English TTS not supported or missing data, trying default");
                tts.setLanguage(Locale.getDefault());
            }

            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    JSObject ret = new JSObject();
                    ret.put("utteranceId", utteranceId);
                    ret.put("event", "start");
                    notifyListeners("ttsEvent", ret);
                }

                @Override
                public void onDone(String utteranceId) {
                    JSObject ret = new JSObject();
                    ret.put("utteranceId", utteranceId);
                    ret.put("event", "done");
                    notifyListeners("ttsEvent", ret);
                }

                @Override
                public void onError(String utteranceId) {
                    JSObject ret = new JSObject();
                    ret.put("utteranceId", utteranceId);
                    ret.put("event", "error");
                    notifyListeners("ttsEvent", ret);
                }
            });

            isInitialized = true;
            Log.i(TAG, "Native Android TextToSpeech successfully initialized on-device (Locale.US).");
        } else {
            Log.e(TAG, "Native TTS initialization failed with status: " + status);
            isInitialized = false;
        }
    }

    @PluginMethod
    public void isAvailable(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("available", isInitialized && tts != null);
        call.resolve(ret);
    }

    @PluginMethod
    public void speak(PluginCall call) {
        String text = call.getString("text", "");
        if (text == null || text.trim().isEmpty()) {
            call.reject("Text cannot be empty");
            return;
        }

        double rate = call.getDouble("rate", 1.0);
        double pitch = call.getDouble("pitch", 1.0);

        if (!isInitialized || tts == null) {
            initTTS();
            call.reject("TTS engine initializing, please retry");
            return;
        }

        try {
            tts.stop();
            tts.setSpeechRate((float) rate);
            tts.setPitch((float) pitch);

            String utteranceId = "utt_" + System.currentTimeMillis();
            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);

            int result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
            if (result == TextToSpeech.SUCCESS) {
                JSObject ret = new JSObject();
                ret.put("success", true);
                ret.put("utteranceId", utteranceId);
                call.resolve(ret);
            } else {
                call.reject("TTS speak returned error code: " + result);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in speak()", e);
            call.reject("Speech failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void stop(PluginCall call) {
        try {
            if (tts != null) {
                tts.stop();
            }
            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Failed stopping TTS: " + e.getMessage());
        }
    }

    @Override
    protected void handleOnDestroy() {
        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {}
            tts = null;
        }
        super.handleOnDestroy();
    }
}
