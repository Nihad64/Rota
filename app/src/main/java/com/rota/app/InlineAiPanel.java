package com.rota.app;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.res.Resources;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Base64;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class InlineAiPanel {
    public interface Callback { void changed(); }

    private static final int BG = Color.rgb(9, 11, 15);
    private static final int SURFACE = Color.rgb(18, 22, 29);
    private static final int SURFACE2 = Color.rgb(25, 30, 38);
    private static final int SURFACE3 = Color.rgb(33, 39, 49);
    private static final int TEXT = Color.rgb(247, 248, 250);
    private static final int MUTED = Color.rgb(145, 153, 165);
    private static final int ORANGE = Color.rgb(255, 139, 0);
    private static final int ORANGE_DARK = Color.rgb(60, 39, 18);
    private static final Locale TR = new Locale("tr", "TR");

    private InlineAiPanel() {}

    public static void show(Activity a, List<Plan> plans, Callback callback) {
        String key = a.getSharedPreferences("rotam_settings", 0).getString("gemini_api_key", "");
        if (key.trim().isEmpty()) {
            toast(a, "Önce Ayarlar'dan Gemini API anahtarını bağla.");
            return;
        }

        Dialog dialog = new Dialog(a);
        dialog.setCancelable(true);

        LinearLayout root = col(a);
        root.setPadding(dp(a, 16), dp(a, 12), dp(a, 16), dp(a, 12));
        root.setBackground(round(BG, 28));

        LinearLayout header = row(a);
        TextView mark = txt(a, "✦", 22, Color.WHITE, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(round(ORANGE, 16));
        header.addView(mark, lp(a, 44, 44));

        LinearLayout heading = col(a);
        heading.setPadding(dp(a, 11), 0, 0, 0);
        heading.addView(txt(a, "Rotam AI", 20, TEXT, true));
        heading.addView(txt(a, "Gemini ile planlarını konuş", 11, MUTED, false), mt(a, 3));
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));

        TextView close = button(a, "×", TEXT, SURFACE2, 20);
        header.addView(close, lp(a, 42, 42));
        root.addView(header);

        LinearLayout mode = row(a);
        TextView liveBadge = button(a, "●  Gemini Live", ORANGE, ORANGE_DARK, 11);
        liveBadge.setGravity(Gravity.CENTER);
        mode.addView(liveBadge, new LinearLayout.LayoutParams(-2, dp(a, 30)));
        TextView privacy = txt(a, "Ses yalnızca sohbet açıkken iletilir", 10, MUTED, false);
        privacy.setGravity(Gravity.CENTER_VERTICAL);
        privacy.setPadding(dp(a, 8), 0, 0, 0);
        mode.addView(privacy, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(mode, mt(a, 8));

        View separator = new View(a);
        separator.setBackgroundColor(SURFACE3);
        root.addView(separator, mt(a, 10));
        LinearLayout.LayoutParams sepParams = (LinearLayout.LayoutParams) separator.getLayoutParams();
        sepParams.height = dp(a, 1);
        separator.setLayoutParams(sepParams);

        ScrollView scroll = new ScrollView(a);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, dp(a, 4), 0, dp(a, 4));
        LinearLayout messages = col(a);
        messages.setPadding(0, dp(a, 4), 0, dp(a, 8));
        scroll.addView(messages, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        addBubble(a, messages, scroll,
                "Merhaba. Gününü düzenlemene, plan eklemene ve mevcut planlarını değiştirmene yardım edebilirim. Mikrofonla doğal sesli sohbet başlatabilir veya buraya yazabilirsin.",
                false);

        TextView status = txt(a, "Yazılı sohbet hazır · Sesli sohbet için mikrofona dokun", 10, MUTED, false);
        root.addView(status, mt(a, 4));

        HorizontalScrollView suggestions = new HorizontalScrollView(a);
        suggestions.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = row(a);
        addSuggestion(a, chips, "Günümü planla", "Bugün için planlarımı düzenlememe yardım et.", status, null);
        gap(chips, a, 7);
        addSuggestion(a, chips, "Planlarımı özetle", "Mevcut planlarımı özetle ve boş zamanlarımı söyle.", status, null);
        gap(chips, a, 7);
        addSuggestion(a, chips, "Okuma hedefi", "Bugün 20 sayfa Risale-i Nur okumak için bir plan oluştur.", status, null);
        suggestions.addView(chips);
        root.addView(suggestions, mt(a, 8));

        LinearLayout composer = row(a);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        EditText input = new EditText(a);
        input.setSingleLine(false);
        input.setMinLines(1);
        input.setMaxLines(3);
        input.setTextSize(14);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setHint("Gemini'ye yaz…");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEND);
        input.setPadding(dp(a, 14), dp(a, 9), dp(a, 14), dp(a, 9));
        input.setBackground(round(SURFACE2, 18));
        composer.addView(input, new LinearLayout.LayoutParams(0, dp(a, 52), 1));
        chips.setTag(input);

        TextView mic = button(a, "🎙", TEXT, SURFACE3, 18);
        composer.addView(mic, lp(a, 48, 48));
        gap(composer, a, 7);
        TextView send = button(a, "↑", Color.WHITE, ORANGE, 21);
        composer.addView(send, lp(a, 48, 48));
        root.addView(composer, mt(a, 8));

        TextView help = txt(a, "Sesli mod, Gemini Live üzerinden gerçek sesli yanıt üretir.", 10, MUTED, false);
        help.setGravity(Gravity.CENTER);
        root.addView(help, mt(a, 7));

        LiveVoiceSession[] live = {null};
        final boolean[] dismissed = {false};

        Runnable submit = () -> {
            String q = input.getText().toString().trim();
            if (q.isEmpty()) return;
            if (live[0] != null && live[0].isReady()) {
                if (live[0].isRecording()) {
                    status.setText("Önce mikrofon düğmesine dokunup konuşmayı bitir.");
                    return;
                }
                input.setText("");
                addBubble(a, messages, scroll, q, true);
                status.setText("Gemini yanıtlıyor…");
                live[0].sendText(q);
                return;
            }

            input.setText("");
            addBubble(a, messages, scroll, q, true);
            send.setEnabled(false);
            status.setText("Gemini düşünüyor…");
            final String context = context(plans);
            new Thread(() -> {
                String raw = call(key, q, context);
                String reply = apply(a, plans, raw);
                a.runOnUiThread(() -> {
                    if (dismissed[0]) return;
                    addBubble(a, messages, scroll, reply, false);
                    status.setText("Yazılı sohbet hazır · İstersen mikrofona dokun.");
                    send.setEnabled(true);
                    if (callback != null) callback.changed();
                });
            }, "Rotam-Gemini-Text").start();
        };

        send.setOnClickListener(v -> submit.run());
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND
                    || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                submit.run();
                return true;
            }
            if (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER
                    && event.getAction() == android.view.KeyEvent.ACTION_UP
                    && !event.isShiftPressed()) {
                submit.run();
                return true;
            }
            return false;
        });

        addSuggestion(a, chips, "Plan ekle", "Yarın saat 18:00'de spor için bir plan oluştur.", status, submit);

        mic.setOnClickListener(v -> {
            tap(a, v);
            if (Build.VERSION.SDK_INT >= 23
                    && a.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                a.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 922);
                status.setText("Sesli sohbet için mikrofon izni gerekli.");
                return;
            }
            if (live[0] == null || live[0].isClosed()) {
                status.setText("Gemini Live'e bağlanıyor…");
                live[0] = new LiveVoiceSession(a, key, plans, callback, messages, scroll, status, mic);
                live[0].connectAndStart();
                return;
            }
            if (!live[0].isReady()) {
                status.setText("Gemini Live bağlantısı kuruluyor…");
            } else if (live[0].isRecording()) {
                live[0].stopRecording();
            } else {
                live[0].startRecording();
            }
        });

        close.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(v -> {
            dismissed[0] = true;
            if (live[0] != null) live[0].close();
        });

        dialog.setContentView(root);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(Gravity.BOTTOM);
            window.setLayout((int) (a.getResources().getDisplayMetrics().widthPixels * .98f),
                    (int) (a.getResources().getDisplayMetrics().heightPixels * .88f));
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private static void addSuggestion(Activity a, LinearLayout row, String title, String prompt,
                                      TextView unusedStatus, Runnable unusedSubmit) {
        TextView chip = button(a, title, TEXT, SURFACE2, 11);
        chip.setPadding(dp(a, 12), 0, dp(a, 12), 0);
        row.addView(chip, new LinearLayout.LayoutParams(-2, dp(a, 36)));
        chip.setOnClickListener(v -> {
            // Suggestions are intentionally small; place their prompt in the composer through the view tag.
            Object listener = row.getTag();
            if (listener instanceof EditText) {
                ((EditText) listener).setText(prompt);
                ((EditText) listener).requestFocus();
            } else if (unusedSubmit != null) {
                unusedSubmit.run();
            }
        });
    }

    private static class LiveVoiceSession {
        private final Activity activity;
        private final String apiKey;
        private final List<Plan> plans;
        private final Callback callback;
        private final LinearLayout messages;
        private final ScrollView scroll;
        private final TextView status;
        private final TextView mic;
        private final OkHttpClient client;
        private final LinkedBlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();

        private volatile WebSocket socket;
        private volatile boolean ready;
        private volatile boolean recording;
        private volatile boolean closed;
        private volatile boolean socketEnded;
        private volatile boolean modelBusy;
        private volatile boolean startAfterReady;
        private volatile AudioRecord recorder;
        private volatile AudioTrack player;
        private Thread recordThread;
        private Thread playerThread;
        private String inputTranscript = "";
        private String outputTranscript = "";

        LiveVoiceSession(Activity activity, String apiKey, List<Plan> plans, Callback callback,
                         LinearLayout messages, ScrollView scroll, TextView status, TextView mic) {
            this.activity = activity;
            this.apiKey = apiKey;
            this.plans = plans;
            this.callback = callback;
            this.messages = messages;
            this.scroll = scroll;
            this.status = status;
            this.mic = mic;
            this.client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .build();
        }

        void connectAndStart() {
            startAfterReady = true;
            try {
                String encoded = URLEncoder.encode(apiKey, "UTF-8");
                String url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=" + encoded;
                Request request = new Request.Builder().url(url).build();
                client.newWebSocket(request, new WebSocketListener() {
                    @Override public void onOpen(WebSocket webSocket, Response response) {
                        socket = webSocket;
                        sendSetup();
                    }

                    @Override public void onMessage(WebSocket webSocket, String text) {
                        handleMessage(text);
                    }

                    @Override public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                        ready = false;
                        socketEnded = true;
                        stopRecording();
                        setStatus("Gemini Live bağlantısı kurulamadı. API anahtarını ve kota erişimini kontrol et.");
                    }

                    @Override public void onClosed(WebSocket webSocket, int code, String reason) {
                        ready = false;
                        socketEnded = true;
                        if (!closed) setStatus("Gemini Live bağlantısı kapandı.");
                    }
                });
            } catch (Exception e) {
                setStatus("Sesli bağlantı başlatılamadı.");
            }
        }

        boolean isReady() { return ready && !closed; }
        boolean isRecording() { return recording; }
        boolean isClosed() { return closed || socketEnded; }

        void sendSetup() {
            try {
                JSONObject setup = new JSONObject();
                setup.put("model", "models/gemini-3.8-live");

                JSONObject generation = new JSONObject();
                JSONArray modalities = new JSONArray();
                modalities.put("AUDIO");
                generation.put("responseModalities", modalities);
                JSONObject prebuilt = new JSONObject().put("voiceName", "Aoede");
                JSONObject voice = new JSONObject().put("prebuiltVoiceConfig", prebuilt);
                generation.put("speechConfig", new JSONObject().put("voiceConfig", voice));
                setup.put("generationConfig", generation);
                setup.put("inputAudioTranscription", new JSONObject());
                setup.put("outputAudioTranscription", new JSONObject());

                String instruction = "Sen Rotam uygulamasının yerleşik kişisel planlama asistanısın. Türkçe konuş; cevapların doğal, sakin ve kısa olsun. Kullanıcının planlarını okuyabilir, oluşturabilir, güncelleyebilir, tamamlayabilir ve açıkça istediğinde silebilirsin. Bir işlem gerçekten gerçekleşmeden gerçekleştiğini söyleme. Plan oluşturmak veya değiştirmek için tanımlı araçları kullan. Tarih belirtilmezse bağlama göre bugünü veya açıkça belirtilen tarihi seç; emin değilsen sor. Saat belirtilmezse saat uydurma; esnek plan oluştur. Randevuya yol süresi gerekiyorsa bunu ayrı bir planla veya notla belirt. Bugünün tarihi: " + today() + ".\nKullanıcının mevcut planları:\n" + context(plans);
                JSONArray iparts = new JSONArray().put(new JSONObject().put("text", instruction));
                setup.put("systemInstruction", new JSONObject().put("parts", iparts));
                setup.put("tools", voiceTools());

                JSONObject packet = new JSONObject().put("setup", setup);
                WebSocket ws = socket;
                if (ws != null) ws.send(packet.toString());
            } catch (Exception e) {
                setStatus("Gemini Live ayarları gönderilemedi.");
            }
        }

        private JSONArray voiceTools() throws Exception {
            JSONArray functions = new JSONArray();
            functions.put(function("create_plan", "Yeni bir plan oluştur. Tarih YYYY-MM-DD formatında; saat 24 saat formatında olmalı.",
                    schema(new String[][]{
                            {"title","STRING"},{"date","STRING"},{"start","STRING"},{"end","STRING"},
                            {"category","STRING"},{"type","STRING"},{"all_day","BOOLEAN"},{"note","STRING"},
                            {"repeat","STRING"},{"details","STRING"}
                    }, new String[]{"title","date"})));
            functions.put(function("update_plan", "Var olan planı verilen id ile güncelle. Sadece değiştirilmesi istenen alanları gönder.",
                    schema(new String[][]{
                            {"id","INTEGER"},{"title","STRING"},{"date","STRING"},{"start","STRING"},
                            {"end","STRING"},{"category","STRING"},{"type","STRING"},{"all_day","BOOLEAN"},
                            {"note","STRING"},{"repeat","STRING"},{"details","STRING"}
                    }, new String[]{"id"})));
            functions.put(function("complete_plan", "Bir planı tamamlandı veya tamamlanmadı olarak işaretle.",
                    schema(new String[][]{{"id","INTEGER"},{"done","BOOLEAN"}}, new String[]{"id"})));
            functions.put(function("delete_plan", "Kullanıcı açıkça silmek istediğinde bir planı sil.",
                    schema(new String[][]{{"id","INTEGER"}}, new String[]{"id"})));
            functions.put(function("list_plans", "Mevcut planları ve kimliklerini listele.",
                    new JSONObject().put("type","OBJECT").put("properties",new JSONObject())));
            return new JSONArray().put(new JSONObject().put("functionDeclarations", functions));
        }

        private JSONObject function(String name, String description, JSONObject parameters) throws Exception {
            return new JSONObject().put("name", name).put("description", description).put("parameters", parameters);
        }

        private JSONObject schema(String[][] fields, String[] required) throws Exception {
            JSONObject props = new JSONObject();
            for (String[] f : fields) props.put(f[0], new JSONObject().put("type", f[1]));
            JSONArray req = new JSONArray();
            for (String r : required) req.put(r);
            return new JSONObject().put("type","OBJECT").put("properties", props).put("required", req);
        }

        void startRecording() {
            if (closed || !ready) return;
            if (modelBusy) {
                setStatus("Gemini yanıtını bitirsin; sonra tekrar konuşabilirsin.");
                return;
            }
            if (recording) return;
            int min = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (min <= 0) min = 6400;
            AudioRecord r;
            try {
                r = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        Math.max(min * 2, 6400));
                if (r.getState() != AudioRecord.STATE_INITIALIZED) {
                    r.release();
                    setStatus("Mikrofon açılamadı.");
                    return;
                }
                recorder = r;
                r.startRecording();
                recording = true;
                setMicState(true);
                setStatus("Dinliyorum… konuşman bitince mikrofona tekrar dokun.");
                recordThread = new Thread(() -> {
                    byte[] buffer = new byte[3200];
                    while (recording && !closed) {
                        int read;
                        try {
                            read = r.read(buffer, 0, buffer.length);
                        } catch (Exception e) {
                            break;
                        }
                        if (read > 0 && recording && !closed) {
                            byte[] part = Arrays.copyOf(buffer, read);
                            String encoded = Base64.encodeToString(part, Base64.NO_WRAP);
                            try {
                                JSONObject blob = new JSONObject()
                                        .put("mimeType", "audio/pcm;rate=16000")
                                        .put("data", encoded);
                                JSONObject message = new JSONObject()
                                        .put("realtimeInput", new JSONObject().put("audio", blob));
                                WebSocket ws = socket;
                                if (ws != null && ready) ws.send(message.toString());
                            } catch (Exception ignored) {}
                        }
                    }
                }, "Rotam-Live-Microphone");
                recordThread.start();
            } catch (Exception e) {
                recording = false;
                setMicState(false);
                setStatus("Mikrofon başlatılamadı. Mikrofon iznini kontrol et.");
            }
        }

        void stopRecording() {
            recording = false;
            AudioRecord r = recorder;
            recorder = null;
            if (r != null) {
                try { if (r.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) r.stop(); }
                catch (Exception ignored) {}
                try { r.release(); } catch (Exception ignored) {}
            }
            setMicState(false);
            if (!closed && ready) {
                try {
                    JSONObject msg = new JSONObject().put("realtimeInput",
                            new JSONObject().put("audioStreamEnd", true));
                    WebSocket ws = socket;
                    if (ws != null) ws.send(msg.toString());
                    modelBusy = true;
                    setStatus("Gemini yanıtlıyor…");
                } catch (Exception ignored) {}
            }
        }

        void sendText(String text) {
            if (!ready || closed) {
                setStatus("Gemini Live hazır değil.");
                return;
            }
            try {
                JSONArray parts = new JSONArray().put(new JSONObject().put("text", text));
                JSONArray turns = new JSONArray().put(new JSONObject().put("role","user").put("parts",parts));
                JSONObject message = new JSONObject().put("clientContent",
                        new JSONObject().put("turns",turns).put("turnComplete",true));
                modelBusy = true;
                socket.send(message.toString());
            } catch (Exception e) {
                setStatus("Mesaj gönderilemedi.");
            }
        }

        private void handleMessage(String raw) {
            try {
                JSONObject root = new JSONObject(raw);
                if (root.has("error")) {
                    JSONObject error = root.optJSONObject("error");
                    String msg = error == null ? "Gemini Live isteği reddedildi." : error.optString("message","Gemini Live isteği reddedildi.");
                    ready = false;
                    setStatus("Sesli erişim hatası: " + msg);
                    return;
                }
                if (root.has("setupComplete")) {
                    ready = true;
                    startPlayer();
                    setStatus("Gemini Live hazır · konuşmak için mikrofona dokun.");
                    if (startAfterReady) {
                        startAfterReady = false;
                        startRecording();
                    }
                    return;
                }

                JSONObject toolCall = root.optJSONObject("toolCall");
                if (toolCall != null) handleToolCall(toolCall);

                JSONObject server = root.optJSONObject("serverContent");
                if (server != null) {
                    JSONObject in = server.optJSONObject("inputTranscription");
                    if (in != null) inputTranscript = in.optString("text", inputTranscript);
                    JSONObject out = server.optJSONObject("outputTranscription");
                    if (out != null) outputTranscript = out.optString("text", outputTranscript);

                    JSONObject turn = server.optJSONObject("modelTurn");
                    JSONArray parts = turn == null ? null : turn.optJSONArray("parts");
                    if (parts != null && parts.length() > 0) {
                        modelBusy = true;
                        for (int i=0;i<parts.length();i++) {
                            JSONObject part = parts.optJSONObject(i);
                            JSONObject inline = part == null ? null : part.optJSONObject("inlineData");
                            if (inline != null) {
                                String base64 = inline.optString("data","");
                                if (!base64.isEmpty()) {
                                    try { audioQueue.offer(Base64.decode(base64, Base64.DEFAULT)); }
                                    catch (Exception ignored) {}
                                }
                            }
                        }
                    }
                    if (server.optBoolean("interrupted", false)) {
                        audioQueue.clear();
                        outputTranscript = "";
                        modelBusy = false;
                    }
                    if (server.optBoolean("turnComplete", false)) {
                        final String inText = inputTranscript.trim();
                        final String outText = outputTranscript.trim();
                        inputTranscript = "";
                        outputTranscript = "";
                        modelBusy = false;
                        activity.runOnUiThread(() -> {
                            if (!inText.isEmpty()) addBubble(activity, messages, scroll, inText, true);
                            if (!outText.isEmpty()) addBubble(activity, messages, scroll, outText, false);
                            setStatus(recording
                                    ? "Dinliyorum… konuşman bitince mikrofona tekrar dokun."
                                    : "Gemini Live hazır · konuşmak için mikrofona dokun.");
                            if (callback != null && !inText.isEmpty()) callback.changed();
                        });
                    }
                }
            } catch (Exception e) {
                setStatus("Gemini Live yanıtı okunamadı.");
            }
        }

        private void handleToolCall(JSONObject toolCall) {
            JSONArray calls = toolCall.optJSONArray("functionCalls");
            if (calls == null || calls.length() == 0) return;
            activity.runOnUiThread(() -> {
                JSONArray responses = new JSONArray();
                boolean changed = false;
                for (int i=0;i<calls.length();i++) {
                    JSONObject call = calls.optJSONObject(i);
                    if (call == null) continue;
                    String id = call.optString("id", UUID.randomUUID().toString());
                    String name = call.optString("name","");
                    JSONObject args = call.optJSONObject("args");
                    if (args == null) args = new JSONObject();
                    JSONObject result = executeAction(activity, plans, name, args);
                    if (result.optBoolean("changed",false)) changed = true;
                    try {
                        responses.put(new JSONObject()
                                .put("id", id)
                                .put("name", name)
                                .put("response", result));
                    } catch (Exception ignored) {}
                }
                try {
                    WebSocket ws = socket;
                    if (ws != null) ws.send(new JSONObject().put("toolResponse",
                            new JSONObject().put("functionResponses", responses)).toString());
                } catch (Exception ignored) {}
                if (changed && callback != null) callback.changed();
            });
        }

        private void startPlayer() {
            if (playerThread != null) return;
            try {
                int min = AudioTrack.getMinBufferSize(24000, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);
                player = new AudioTrack(AudioManager.STREAM_MUSIC, 24000,
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        Math.max(min * 4, 9600), AudioTrack.MODE_STREAM);
                player.play();
                playerThread = new Thread(() -> {
                    while (!closed) {
                        try {
                            byte[] audio = audioQueue.take();
                            if (audio == null || audio.length == 0 || closed) continue;
                            AudioTrack track = player;
                            if (track == null) break;
                            int offset = 0;
                            while (offset < audio.length && !closed) {
                                int wrote = track.write(audio, offset, audio.length - offset);
                                if (wrote <= 0) break;
                                offset += wrote;
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception ignored) {}
                    }
                }, "Rotam-Live-Audio");
                playerThread.start();
            } catch (Exception e) {
                setStatus("Ses çıkışı başlatılamadı.");
            }
        }

        private void setMicState(boolean on) {
            activity.runOnUiThread(() -> {
                if (!closed) {
                    mic.setText(on ? "■" : "🎙");
                    mic.setBackground(round(on ? ORANGE : SURFACE3, 18));
                    mic.setTextColor(on ? Color.WHITE : TEXT);
                }
            });
        }

        private void setStatus(String value) {
            activity.runOnUiThread(() -> {
                if (!closed) status.setText(value);
            });
        }

        void close() {
            closed = true;
            stopRecording();
            WebSocket ws = socket;
            socket = null;
            if (ws != null) ws.close(1000, "Rotam AI kapatıldı");
            audioQueue.offer(new byte[0]);
            AudioTrack track = player;
            player = null;
            if (track != null) {
                try { track.pause(); track.flush(); track.stop(); } catch (Exception ignored) {}
                try { track.release(); } catch (Exception ignored) {}
            }
            try { client.dispatcher().executorService().shutdown(); } catch (Exception ignored) {}
            try { client.connectionPool().evictAll(); } catch (Exception ignored) {}
        }
    }

    private static String call(String key, String user, String ctx) {
        HttpURLConnection conn = null;
        try {
            String today = today();
            String prompt = "Sen Rotam uygulamasının kişisel planlama asistanısın. Türkçe ve kısa konuş. Bugünün tarihi: " + today
                    + ". Kullanıcının mevcut planları:\n" + ctx
                    + "\nKullanıcı isteği: " + user
                    + "\nİstek planları değiştiriyorsa actions dizisinde işlem tanımla. Geçerli actions türleri: create_plan, update_plan, complete_plan, delete_plan."
                    + " create_plan alanları title,date,start,end,category,type,all_day,note,repeat,details; update_plan ayrıca id içerir; complete_plan id ve done; delete_plan id."
                    + " Kategori değerleri: book, work, video, sport, faith, appointment, food, personal."
                    + " Tarihi YYYY-MM-DD, saati HH:MM biçiminde yaz. Bilmediğin saati uydurma; esnek plan için start alanını boş bırak ve type=Esnek kullan."
                    + " Yanıtı SADECE {\"reply\":\"Türkçe kısa yanıt\",\"actions\":[]} JSON formatında ver.";
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash-lite:generateContent");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(35000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-goog-api-key", key);

            JSONObject part = new JSONObject().put("text", prompt);
            JSONObject content = new JSONObject().put("parts", new JSONArray().put(part));
            JSONObject body = new JSONObject().put("contents", new JSONArray().put(content));
            JSONObject cfg = new JSONObject().put("maxOutputTokens", 1800).put("responseMimeType", "application/json");
            body.put("generationConfig", cfg);

            try (OutputStream out = conn.getOutputStream()) {
                out.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                    StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) response.append(line);
            br.close();

            if (code < 200 || code >= 300) {
                String message = "Gemini isteği başarısız (HTTP " + code + ").";
                try {
                    JSONObject err = new JSONObject(response.toString()).optJSONObject("error");
                    if (err != null) message = err.optString("message", message);
                } catch (Exception ignored) {}
                return new JSONObject().put("reply", message).put("actions", new JSONArray()).toString();
            }

            JSONObject root = new JSONObject(response.toString());
            JSONArray candidates = root.optJSONArray("candidates");
            if (candidates == null || candidates.length() == 0)
                return new JSONObject().put("reply","Gemini boş yanıt verdi. Tekrar dener misin?").put("actions",new JSONArray()).toString();
            JSONObject co = candidates.getJSONObject(0).optJSONObject("content");
            JSONArray parts = co == null ? null : co.optJSONArray("parts");
            String result = parts == null || parts.length() == 0 ? "" : parts.optJSONObject(0).optString("text","");
            new JSONObject(result); // verify it is valid JSON before passing it onward
            return result;
        } catch (Exception e) {
            try {
                return new JSONObject().put("reply","Gemini bağlantısında hata oluştu. API anahtarını, interneti ve ücretsiz kota durumunu kontrol et.")
                        .put("actions",new JSONArray()).toString();
            } catch (Exception ignored) { return "{\"reply\":\"Bağlantı hatası.\",\"actions\":[]}"; }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String apply(Activity a, List<Plan> plans, String raw) {
        try {
            JSONObject root = new JSONObject(raw == null ? "" : raw.trim());
            JSONArray actions = root.optJSONArray("actions");
            int changedCount = 0;
            if (actions != null) {
                for (int i=0;i<actions.length();i++) {
                    JSONObject action = actions.optJSONObject(i);
                    if (action == null) continue;
                    String type = action.optString("type", "");
                    JSONObject result = executeAction(a, plans, type, action);
                    if (result.optBoolean("changed",false)) changedCount++;
                }
            }
            String reply = root.optString("reply", "Tamamdır.");
            if (changedCount > 0) reply += " (" + changedCount + " işlem tamamlandı.)";
            return reply;
        } catch (Exception e) {
            return "Gemini yanıtı okunamadı. İsteğini daha kısa yazarak tekrar dener misin?";
        }
    }

    private static JSONObject executeAction(Activity a, List<Plan> plans, String type, JSONObject args) {
        JSONObject out = new JSONObject();
        boolean changed = false;
        try {
            if ("create_plan".equals(type)) {
                String title = args.optString("title","").trim();
                if (title.isEmpty()) title = "Yeni plan";
                String date = args.optString("date", today());
                if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) date = today();
                String start = args.optString("start","");
                String end = args.optString("end","");
                boolean allDay = args.optBoolean("all_day", false) || Plan.ALL_DAY.equals(args.optString("type",""));
                String planType = allDay ? Plan.ALL_DAY : (!start.isEmpty() ? Plan.HOURLY : Plan.FLEX);
                if (allDay) { start = ""; end = ""; }
                String category = normalCategory(args.optString("category","personal"));
                String repeat = normalRepeat(args.optString("repeat",Plan.REPEAT_NONE));
                Plan p = new Plan(PlanStore.nextId(plans), date, title, planType, start,
                        Plan.durationBetween(start,end), false);
                p.startTime = start; p.endTime = end; p.time = start;
                p.repeat = repeat; p.category = category;
                p.note = args.optString("note","");
                p.details = args.optString("details","");
                p.status = Plan.STATUS_PLANNED;
                PlanStore.addWithRepeat(plans,p);
                persist(a,plans);
                changed = true;
                out.put("message","Plan oluşturuldu: " + title);
                out.put("plan_id",p.id);
            } else if ("update_plan".equals(type) || "reschedule_plan".equals(type)) {
                long id = args.optLong("id", -1);
                Plan p = findPlan(plans,id);
                if (p == null) {
                    out.put("message","Bu kimlikle plan bulunamadı.");
                } else {
                    if (args.has("title")) p.title = args.optString("title",p.title);
                    if (args.has("date")) {
                        String date=args.optString("date",p.date);
                        if(date.matches("\\d{4}-\\d{2}-\\d{2}")) p.date=date;
                    }
                    if (args.has("category")) p.category = normalCategory(args.optString("category",p.category));
                    if (args.has("note")) p.note = args.optString("note","");
                    if (args.has("details")) p.details = args.optString("details","");
                    if (args.has("repeat")) p.repeat = normalRepeat(args.optString("repeat",p.repeat));
                    boolean allDay = args.optBoolean("all_day", Plan.ALL_DAY.equals(p.type));
                    if (args.has("start")) p.startTime = args.optString("start","");
                    if (args.has("end")) p.endTime = args.optString("end","");
                    if (allDay) {
                        p.type = Plan.ALL_DAY; p.startTime = ""; p.endTime = "";
                    } else {
                        p.type = !p.startTime.isEmpty() ? Plan.HOURLY : Plan.FLEX;
                    }
                    p.time = p.startTime;
                    p.duration = Plan.durationBetween(p.startTime,p.endTime);
                    p.done = false;
                    p.status = Plan.STATUS_PLANNED;
                    persist(a,plans);
                    changed = true;
                    out.put("message","Plan güncellendi: " + p.title);
                    out.put("plan_id",p.id);
                }
            } else if ("complete_plan".equals(type)) {
                Plan p = findPlan(plans,args.optLong("id",-1));
                if (p == null) {
                    out.put("message","Tamamlanacak plan bulunamadı.");
                } else {
                    p.done = args.optBoolean("done",true);
                    p.status = p.done ? Plan.STATUS_DONE : Plan.STATUS_PLANNED;
                    if(p.done) ReminderScheduler.cancel(a,p);
                    else ReminderScheduler.rescheduleAll(a,plans);
                    persist(a,plans); changed = true;
                    out.put("message",p.done ? "Plan tamamlandı: " + p.title : "Plan tekrar açık.");
                }
            } else if ("delete_plan".equals(type)) {
                Plan p = findPlan(plans,args.optLong("id",-1));
                if(p == null) out.put("message","Silinecek plan bulunamadı.");
                else {
                    ReminderScheduler.cancel(a,p);
                    plans.remove(p);
                    persist(a,plans); changed = true;
                    out.put("message","Plan silindi: " + p.title);
                }
            } else if ("list_plans".equals(type)) {
                JSONArray list = new JSONArray();
                for (Plan p : plans) {
                    if (Plan.STATUS_CANCELLED.equals(p.status)) continue;
                    list.put(new JSONObject().put("id",p.id).put("title",p.title).put("date",p.date)
                            .put("start",p.startTime).put("end",p.endTime).put("category",p.category));
                }
                out.put("message","Planlar listelendi.");
                out.put("plans",list);
            } else {
                out.put("message","Bilinmeyen işlem.");
            }
            out.put("changed",changed);
        } catch (Exception e) {
            try { out.put("message","İşlem uygulanamadı."); out.put("changed",false); }
            catch (Exception ignored) {}
        }
        return out;
    }

    private static Plan findPlan(List<Plan> plans, long id) {
        for (Plan p : plans) if (p.id == id) return p;
        return null;
    }

    private static void persist(Activity a, List<Plan> plans) {
        PlanStore.save(a,plans);
        ReminderScheduler.rescheduleAll(a,plans);
    }

    private static String normalCategory(String value) {
        if ("book".equals(value) || "work".equals(value) || "video".equals(value)
                || "sport".equals(value) || "faith".equals(value) || "appointment".equals(value)
                || "food".equals(value) || "personal".equals(value)) return value;
        return "personal";
    }

    private static String normalRepeat(String value) {
        if (Plan.REPEAT_DAILY.equals(value) || Plan.REPEAT_WEEKDAYS.equals(value)
                || Plan.REPEAT_WEEKLY.equals(value)) return value;
        return Plan.REPEAT_NONE;
    }

    private static String context(List<Plan> plans) {
        StringBuilder b = new StringBuilder();
        int shown = 0;
        for (Plan p : plans) {
            if (Plan.STATUS_CANCELLED.equals(p.status)) continue;
            b.append("id=").append(p.id).append(" | ").append(p.date).append(" | ")
                    .append(p.startTime == null ? "" : p.startTime).append("–")
                    .append(p.endTime == null ? "" : p.endTime).append(" | ")
                    .append(p.title).append(" | category=").append(p.category)
                    .append(" | status=").append(p.status);
            if (p.note != null && !p.note.trim().isEmpty())
                b.append(" | note=").append(p.note.trim());
            if (p.details != null && !p.details.trim().isEmpty())
                b.append(" | details=").append(p.details.trim());
            b.append("\n");
            if (++shown >= 60) break;
        }
        return b.length() == 0 ? "Henüz plan yok.\n" : b.toString();
    }

    private static void tap(Activity a, View v) {
        v.animate().scaleX(.97f).scaleY(.97f).setDuration(55)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
    }

    private static void addBubble(Activity a, LinearLayout messages, ScrollView scroll,
                                  String text, boolean user) {
        if (text == null || text.trim().isEmpty()) return;
        LinearLayout line = row(a);
        line.setGravity(user ? Gravity.END : Gravity.START);
        TextView bubble = txt(a, text.trim(), 14, TEXT, false);
        bubble.setMaxWidth(dp(a, 300));
        bubble.setPadding(dp(a, 13), dp(a, 11), dp(a, 13), dp(a, 11));
        bubble.setBackground(round(user ? ORANGE_DARK : SURFACE2, 18));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2,-2);
        p.setMargins(user ? dp(a, 36) : 0, dp(a, 5), user ? 0 : dp(a, 36), dp(a, 5));
        line.addView(bubble,p);
        messages.addView(line, new LinearLayout.LayoutParams(-1,-2));
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private static LinearLayout col(Activity a) {
        LinearLayout l = new LinearLayout(a);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private static LinearLayout row(Activity a) {
        LinearLayout l = col(a);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private static TextView txt(Activity a, String s, float z, int c, boolean bold) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(z);
        t.setTextColor(c);
        t.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        t.setLineSpacing(dp(a,2),1.05f);
        return t;
    }

    private static TextView button(Activity a, String s, int fg, int bg, float size) {
        TextView t = txt(a, s, size, fg, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(round(bg, 17));
        return t;
    }

    private static LinearLayout.LayoutParams lp(Activity a, int w, int h) {
        return new LinearLayout.LayoutParams(dp(a,w),dp(a,h));
    }

    private static LinearLayout.LayoutParams mt(Activity a, int n) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=dp(a,n); return p;
    }

    private static void gap(LinearLayout l, Activity a, int w) {
        l.addView(new View(a), lp(a,w,1));
    }

    private static int dp(Activity a, int n) {
        return (int)(n*a.getResources().getDisplayMetrics().density+.5f);
    }

    private static android.graphics.drawable.GradientDrawable round(int c, int r) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(c);
        g.setCornerRadius(r * Resources.getSystem().getDisplayMetrics().density);
        return g;
    }

    private static String today() {
        Calendar c=Calendar.getInstance();
        return String.format(Locale.GERMANY,"%04d-%02d-%02d",
                c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH));
    }

    private static Toast toast(Activity a, String s) {
        Toast t=Toast.makeText(a,s,Toast.LENGTH_SHORT); t.show(); return t;
    }
}
