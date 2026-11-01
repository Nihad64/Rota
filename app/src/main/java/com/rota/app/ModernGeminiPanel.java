package com.rota.app;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Build;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Compact, non-Live Gemini assistant: speech input -> Gemini text -> optional Gemini TTS. */
public final class ModernGeminiPanel {
    public interface Callback { void changed(); }

    private static final int BG = Color.rgb(10, 12, 16);
    private static final int SURFACE = Color.rgb(20, 24, 31);
    private static final int SURFACE2 = Color.rgb(29, 34, 43);
    private static final int SURFACE3 = Color.rgb(38, 44, 55);
    private static final int TEXT = Color.rgb(247, 248, 250);
    private static final int MUTED = Color.rgb(148, 156, 168);
    private static final int ORANGE = Color.rgb(255, 139, 0);
    private static final int ORANGE_SOFT = Color.rgb(62, 40, 17);
    private static final int USER_BUBBLE = Color.rgb(78, 48, 16);
    private static final Locale TR = new Locale("tr", "TR");

    private ModernGeminiPanel() {}

    public static void show(Activity a, List<Plan> plans, Callback callback) {
        String key = a.getSharedPreferences("rotam_settings", 0).getString("gemini_api_key", "");
        if (key.trim().isEmpty()) {
            toast(a, "Önce Ayarlar > Rotam AI bölümünden Gemini anahtarını bağla.");
            return;
        }

        final Dialog dialog = new Dialog(a);
        dialog.setCancelable(true);

        LinearLayout root = col(a);
        root.setPadding(dp(a, 15), dp(a, 10), dp(a, 15), dp(a, 12));
        root.setBackground(round(BG, 28));

        LinearLayout grab = row(a);
        grab.setGravity(Gravity.CENTER);
        View handle = new View(a);
        handle.setBackground(round(SURFACE3, 5));
        grab.addView(handle, lp(a, 42, 4));
        root.addView(grab, mt(a, 0));

        LinearLayout header = row(a);
        TextView mark = txt(a, "✦", 18, Color.WHITE, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(round(ORANGE, 15));
        header.addView(mark, lp(a, 40, 40));
        LinearLayout titleBox = col(a);
        titleBox.setPadding(dp(a, 10), 0, 0, 0);
        titleBox.addView(txt(a, "Gemini", 18, TEXT, true));
        titleBox.addView(txt(a, "Rotam'ın planlama asistanı", 10, MUTED, false), mt(a, 2));
        header.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));

        TextView speaker = button(a, "🔊", TEXT, SURFACE2, 16);
        speaker.setContentDescription("Gemini sesini aç/kapat");
        header.addView(speaker, lp(a, 42, 42));
        gap(header, a, 7);
        TextView close = button(a, "×", TEXT, SURFACE2, 20);
        header.addView(close, lp(a, 42, 42));
        root.addView(header, mt(a, 5));

        LinearLayout statusRow = row(a);
        TextView dot = txt(a, "●", 10, ORANGE, true);
        statusRow.addView(dot);
        TextView status = txt(a, "Hazır", 10, MUTED, false);
        statusRow.addView(status, mtX(a, 5));
        root.addView(statusRow, mt(a, 7));

        ScrollView scroll = new ScrollView(a);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        LinearLayout messages = col(a);
        messages.setPadding(0, dp(a, 6), 0, dp(a, 8));
        scroll.addView(messages, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        addBubble(a, messages, scroll,
                "Merhaba. Planlarını oluşturabilir, değiştirebilir veya gününü birlikte düzenleyebiliriz.", false);

        HorizontalScrollView quickScroll = new HorizontalScrollView(a);
        quickScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout quick = row(a);
        addChip(a, quick, "Bugünümü planla", "Bugün için mevcut planlarımı düzenle ve boşluklarımı değerlendir.");
        gap(quick, a, 6);
        addChip(a, quick, "Planlarımı özetle", "Bugünkü ve yaklaşan planlarımı kısa şekilde özetle.");
        gap(quick, a, 6);
        addChip(a, quick, "Okuma planı", "Bugün 20 sayfa Risale-i Nur okumak için plan oluştur.");
        quickScroll.addView(quick);
        root.addView(quickScroll, mt(a, 5));

        LinearLayout composer = row(a);
        composer.setGravity(Gravity.BOTTOM);
        EditText input = new EditText(a);
        input.setSingleLine(false);
        input.setMinLines(1);
        input.setMaxLines(4);
        input.setTextSize(14);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setHint("Gemini'ye bir şey söyle…");
        input.setPadding(dp(a, 14), dp(a, 10), dp(a, 14), dp(a, 10));
        input.setBackground(round(SURFACE2, 19));
        composer.addView(input, new LinearLayout.LayoutParams(0, dp(a, 50), 1));
        gap(composer, a, 7);
        TextView mic = button(a, "🎙", TEXT, SURFACE2, 17);
        composer.addView(mic, lp(a, 50, 50));
        gap(composer, a, 7);
        TextView send = button(a, "↑", Color.WHITE, ORANGE, 21);
        composer.addView(send, lp(a, 50, 50));
        root.addView(composer, mt(a, 8));

        TextView hint = txt(a, "Mikrofona dokun → konuş → Gemini yanıtını yazıyla ve sesle versin.", 9, MUTED, false);
        hint.setGravity(Gravity.CENTER);
        root.addView(hint, mt(a, 6));

        final boolean[] speak = {true};
        final SpeechRecognizer[] recognizer = {null};
        final MediaPlayer[] player = {null};
        final boolean[] closed = {false};

        speaker.setOnClickListener(v -> {
            speak[0] = !speak[0];
            speaker.setText(speak[0] ? "🔊" : "🔇");
            speaker.setBackground(round(speak[0] ? ORANGE_SOFT : SURFACE2, 16));
        });

        Runnable submit = () -> {
            String q = input.getText().toString().trim();
            if (q.isEmpty()) return;
            input.setText("");
            addBubble(a, messages, scroll, q, true);
            send.setEnabled(false);
            mic.setEnabled(false);
            status.setText("Gemini düşünüyor…");
            final String context = context(plans);
            new Thread(() -> {
                String raw = callGemini(key, q, context);
                String reply = applyActions(a, plans, raw);
                a.runOnUiThread(() -> {
                    if (closed[0]) return;
                    addBubble(a, messages, scroll, reply, false);
                    status.setText("Hazır");
                    send.setEnabled(true);
                    mic.setEnabled(true);
                    if (callback != null) callback.changed();
                });
                if (speak[0] && !closed[0] && reply != null && !reply.trim().isEmpty()) {
                    String wav = callTts(key, reply);
                    if (wav != null && !closed[0]) {
                        a.runOnUiThread(() -> playWav(a, wav, player, status, closed));
                    }
                }
            }, "Rotam-Gemini").start();
        };

        send.setOnClickListener(v -> { tap(v); submit.run(); });
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND ||
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                submit.run();
                return true;
            }
            return false;
        });

        mic.setOnClickListener(v -> {
            tap(v);
            if (Build.VERSION.SDK_INT >= 23 &&
                    a.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                a.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 922);
                status.setText("Mikrofon izni gerekli.");
                return;
            }
            if (!SpeechRecognizer.isRecognitionAvailable(a)) {
                status.setText("Bu cihazda sesli yazma hizmeti bulunamadı.");
                return;
            }
            if (recognizer[0] != null) {
                try { recognizer[0].stopListening(); } catch (Exception ignored) {}
                return;
            }
            recognizer[0] = SpeechRecognizer.createSpeechRecognizer(a);
            recognizer[0].setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(android.os.Bundle params) { status.setText("Dinliyorum…"); mic.setText("■"); mic.setBackground(round(ORANGE, 17)); }
                @Override public void onBeginningOfSpeech() { status.setText("Konuşuyorsun…"); }
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() { status.setText("Ses çözümleniyor…"); }
                @Override public void onError(int error) {
                    resetRecognizer(recognizer, mic, status);
                    status.setText(error == SpeechRecognizer.ERROR_NO_MATCH ? "Seni anlayamadım; tekrar deneyebilirsin." : "Sesli giriş başarısız oldu.");
                }
                @Override public void onResults(android.os.Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    resetRecognizer(recognizer, mic, status);
                    if (matches != null && !matches.isEmpty()) {
                        input.setText(matches.get(0));
                        input.setSelection(input.length());
                        submit.run();
                    }
                }
                @Override public void onPartialResults(android.os.Bundle partialResults) {}
                @Override public void onEvent(int eventType, android.os.Bundle params) {}
            });
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            recognizer[0].startListening(intent);
        });

        close.setOnClickListener(v -> { tap(v); dialog.dismiss(); });
        dialog.setOnDismissListener(v -> {
            closed[0] = true;
            if (recognizer[0] != null) {
                try { recognizer[0].cancel(); recognizer[0].destroy(); } catch (Exception ignored) {}
                recognizer[0] = null;
            }
            stopPlayer(player);
        });

        dialog.setContentView(root);
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            w.setGravity(Gravity.BOTTOM);
            w.setDimAmount(0.18f);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            w.setLayout((int)(a.getResources().getDisplayMetrics().widthPixels * .97f),
                    (int)Math.min(dp(a, 610), a.getResources().getDisplayMetrics().heightPixels * .68f));
        }
    }

    private static void addChip(Activity a, LinearLayout row, String title, String prompt) {
        TextView chip = button(a, title, TEXT, SURFACE2, 10);
        chip.setPadding(dp(a, 11), 0, dp(a, 11), 0);
        row.addView(chip, new LinearLayout.LayoutParams(-2, dp(a, 34)));
        chip.setOnClickListener(v -> {
            Object tag = row.getTag();
            if (tag instanceof EditText) {
                EditText e = (EditText) tag;
                e.setText(prompt);
                e.requestFocus();
                ((InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(e, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private static String callGemini(String key, String user, String ctx) {
        HttpURLConnection conn = null;
        try {
            String system = "Sen Rotam uygulamasının kişisel planlama asistanısın. Türkçe konuş. Kısa, doğal ve doğrudan cevap ver. Kullanıcının mevcut planlarını bağlam olarak kullan. İstenen işlemleri gerçekten uygulatmak için actions kullan. Kullanıcı açıkça istemedikçe plan silme. Saat uydurma. Tarih verilmezse bugünü kullan. Bugünün tarihi: " + today() +
                    "\nMevcut planlar:\n" + ctx +
                    "\nCevabı SADECE JSON ver: {\\\"reply\\\":\\\"kısa Türkçe cevap\\\",\\\"actions\\\":[]} . Action tipleri: create_plan, update_plan, complete_plan, delete_plan. create_plan: title,date,start,end,category,type,all_day,note,repeat,details. update_plan: id ve değiştirmek istediğin alanlar. complete_plan: id,done. delete_plan: id. Kategoriler: book, work, video, sport, faith, appointment, food, personal. Tarih YYYY-MM-DD, saat HH:MM.";
            String prompt = system + "\nKullanıcı: " + user;

            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent");
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(35000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-goog-api-key", key);

            JSONObject body = new JSONObject();
            JSONObject content = new JSONObject();
            content.put("parts", new JSONArray().put(new JSONObject().put("text", prompt)));
            body.put("contents", new JSONArray().put(content));
            JSONObject cfg = new JSONObject().put("maxOutputTokens", 1800).put("responseMimeType", "application/json");
            body.put("generationConfig", cfg);
            try (OutputStream out = conn.getOutputStream()) { out.write(body.toString().getBytes(StandardCharsets.UTF_8)); }

            int code = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder raw = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) raw.append(line);
            br.close();
            if (code < 200 || code >= 300) {
                JSONObject err = new JSONObject(raw.toString()).optJSONObject("error");
                return new JSONObject().put("reply", err == null ? "Gemini isteği başarısız oldu." : err.optString("message", "Gemini isteği başarısız oldu.")).put("actions", new JSONArray()).toString();
            }
            JSONObject root = new JSONObject(raw.toString());
            JSONArray candidates = root.optJSONArray("candidates");
            if (candidates == null || candidates.length() == 0) return new JSONObject().put("reply", "Gemini boş yanıt verdi.").put("actions", new JSONArray()).toString();
            JSONObject c = candidates.optJSONObject(0);
            JSONObject co = c == null ? null : c.optJSONObject("content");
            JSONArray parts = co == null ? null : co.optJSONArray("parts");
            String text = parts == null || parts.length() == 0 ? "" : parts.optJSONObject(0).optString("text", "");
            return new JSONObject(text).toString();
        } catch (Exception e) {
            try { return new JSONObject().put("reply", "Gemini bağlantısında hata oluştu. İnternetini ve API anahtarını kontrol et.").put("actions", new JSONArray()).toString(); }
            catch (Exception ignored) { return "{\"reply\":\"Bağlantı hatası.\",\"actions\":[]}"; }
        } finally { if (conn != null) conn.disconnect(); }
    }

    /** Gemini TTS (gemini-2.5-flash-preview-tts): returns raw 24kHz 16-bit mono PCM, wrapped here as WAV. */
    private static String callTts(String key, String reply) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent");
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(35000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-goog-api-key", key);
            JSONObject body = new JSONObject()
                    .put("contents", new JSONArray().put(new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", "Sıcak, sakin ve doğal bir Türkçe ile söyle: " + reply)))))
                    .put("generationConfig", new JSONObject()
                            .put("responseModalities", new JSONArray().put("AUDIO"))
                            .put("speechConfig", new JSONObject().put("voiceConfig", new JSONObject().put("prebuiltVoiceConfig", new JSONObject().put("voiceName", "Sulafat")))));
            try (OutputStream out = conn.getOutputStream()) { out.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
            int code = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder raw = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) raw.append(line); br.close();
            if (code < 200 || code >= 300) return null;
            JSONArray cands = new JSONObject(raw.toString()).optJSONArray("candidates");
            if (cands == null || cands.length() == 0) return null;
            JSONObject content = cands.optJSONObject(0).optJSONObject("content");
            JSONArray parts = content == null ? null : content.optJSONArray("parts");
            if (parts == null) return null;
            for (int i = 0; i < parts.length(); i++) {
                JSONObject inline = parts.optJSONObject(i) == null ? null : parts.optJSONObject(i).optJSONObject("inlineData");
                if (inline == null) continue;
                String b64 = inline.optString("data", "");
                if (b64.isEmpty()) continue;
                byte[] pcm = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
                File f = File.createTempFile("rotam_gemini_", ".wav");
                try (FileOutputStream out = new FileOutputStream(f)) { out.write(wavHeader(pcm.length, 24000, 1, 16)); out.write(pcm); }
                return f.getAbsolutePath();
            }
        } catch (Exception ignored) {
        } finally { if (conn != null) conn.disconnect(); }
        return null;
    }

    private static byte[] wavHeader(int dataLen, int rate, int channels, int bits) {
        java.nio.ByteBuffer b = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        int byteRate = rate * channels * bits / 8;
        b.put("RIFF".getBytes()).putInt(36 + dataLen).put("WAVE".getBytes())
         .put("fmt ".getBytes()).putInt(16).putShort((short)1).putShort((short)channels)
         .putInt(rate).putInt(byteRate).putShort((short)(channels * bits / 8)).putShort((short)bits)
         .put("data".getBytes()).putInt(dataLen);
        return b.array();
    }

    private static void playWav(Activity a, String path, MediaPlayer[] holder, TextView status, boolean[] closed) {
        try {
            stopPlayer(holder);
            MediaPlayer mp = new MediaPlayer();
            holder[0] = mp;
            mp.setDataSource(path);
            mp.setOnPreparedListener(p -> { if (!closed[0]) { status.setText("Gemini konuşuyor…"); p.start(); } });
            mp.setOnCompletionListener(p -> { status.setText("Hazır"); stopPlayer(holder); new File(path).delete(); });
            mp.setOnErrorListener((p, what, extra) -> { status.setText("Ses oynatılamadı."); stopPlayer(holder); new File(path).delete(); return true; });
            mp.prepareAsync();
        } catch (Exception e) { new File(path).delete(); }
    }

    private static void stopPlayer(MediaPlayer[] holder) {
        if (holder[0] != null) {
            try { holder[0].stop(); } catch (Exception ignored) {}
            try { holder[0].release(); } catch (Exception ignored) {}
            holder[0] = null;
        }
    }

    private static String applyActions(Activity a, List<Plan> plans, String raw) {
        try {
            JSONObject root = new JSONObject(raw == null ? "{}" : raw.trim());
            JSONArray actions = root.optJSONArray("actions");
            int changed = 0;
            if (actions != null) for (int i=0;i<actions.length();i++) {
                JSONObject action = actions.optJSONObject(i); if (action == null) continue;
                JSONObject result = executeAction(a, plans, action.optString("type", ""), action);
                if (result.optBoolean("changed", false)) changed++;
            }
            String reply = root.optString("reply", "Tamamdır.");
            if (changed > 0 && !reply.toLowerCase(TR).contains("oluştur") && !reply.toLowerCase(TR).contains("güncel") && !reply.toLowerCase(TR).contains("tamam")) reply += " Tamamlandı.";
            return reply;
        } catch (Exception e) { return "Gemini yanıtı okunamadı. Tekrar dener misin?"; }
    }

    private static JSONObject executeAction(Activity a, List<Plan> plans, String type, JSONObject args) {
        JSONObject out = new JSONObject(); boolean changed = false;
        try {
            if ("create_plan".equals(type)) {
                String title=args.optString("title", "Yeni plan").trim();
                String date=args.optString("date", today());
                if(!date.matches("\\d{4}-\\d{2}-\\d{2}")) date=today();
                String start=args.optString("start", ""), end=args.optString("end", "");
                boolean allDay=args.optBoolean("all_day", false) || Plan.ALL_DAY.equals(args.optString("type", ""));
                String planType=allDay?Plan.ALL_DAY:(!start.isEmpty()?Plan.HOURLY:Plan.FLEX);
                if(allDay){start="";end="";}
                Plan p=new Plan(PlanStore.nextId(plans), date, title, planType, start, Plan.durationBetween(start,end), false);
                p.startTime=start;p.endTime=end;p.time=start;p.category=normalCategory(args.optString("category","personal"));p.repeat=normalRepeat(args.optString("repeat",Plan.REPEAT_NONE));p.note=args.optString("note","");p.details=args.optString("details","");p.status=Plan.STATUS_PLANNED;
                PlanStore.addWithRepeat(plans,p); persist(a,plans); changed=true; out.put("message","Plan oluşturuldu: "+title);
            } else if ("update_plan".equals(type) || "reschedule_plan".equals(type)) {
                Plan p=findPlan(plans,args.optLong("id",-1));
                if(p==null) out.put("message","Plan bulunamadı."); else {
                    if(args.has("title"))p.title=args.optString("title",p.title);
                    if(args.has("date")&&args.optString("date").matches("\\d{4}-\\d{2}-\\d{2}"))p.date=args.optString("date");
                    if(args.has("category"))p.category=normalCategory(args.optString("category",p.category));
                    if(args.has("note"))p.note=args.optString("note","");
                    if(args.has("details"))p.details=args.optString("details","");
                    if(args.has("repeat"))p.repeat=normalRepeat(args.optString("repeat",p.repeat));
                    if(args.has("start"))p.startTime=args.optString("start","");
                    if(args.has("end"))p.endTime=args.optString("end","");
                    boolean allDay=args.optBoolean("all_day",Plan.ALL_DAY.equals(p.type));
                    if(allDay){p.type=Plan.ALL_DAY;p.startTime="";p.endTime="";} else p.type=p.startTime.isEmpty()?Plan.FLEX:Plan.HOURLY;
                    p.time=p.startTime;p.duration=Plan.durationBetween(p.startTime,p.endTime);p.done=false;p.status=Plan.STATUS_PLANNED;persist(a,plans);changed=true;out.put("message","Plan güncellendi: "+p.title);
                }
            } else if ("complete_plan".equals(type)) {
                Plan p=findPlan(plans,args.optLong("id",-1));
                if(p!=null){p.done=args.optBoolean("done",true);p.status=p.done?Plan.STATUS_DONE:Plan.STATUS_PLANNED;persist(a,plans);changed=true;out.put("message",p.done?"Plan tamamlandı.":"Plan tekrar açıldı.");} else out.put("message","Plan bulunamadı.");
            } else if ("delete_plan".equals(type)) {
                Plan p=findPlan(plans,args.optLong("id",-1));
                if(p!=null){ReminderScheduler.cancel(a,p);plans.remove(p);persist(a,plans);changed=true;out.put("message","Plan silindi.");} else out.put("message","Plan bulunamadı.");
            } else if ("list_plans".equals(type)) {
                JSONArray list=new JSONArray();for(Plan p:plans)if(!Plan.STATUS_CANCELLED.equals(p.status))list.put(new JSONObject().put("id",p.id).put("title",p.title).put("date",p.date).put("start",p.startTime).put("end",p.endTime).put("category",p.category));out.put("plans",list).put("message","Planlar listelendi.");
            }
            out.put("changed",changed);
        } catch(Exception e){try{out.put("changed",false).put("message","İşlem uygulanamadı.");}catch(Exception ignored){}}
        return out;
    }

    private static Plan findPlan(List<Plan> plans,long id){for(Plan p:plans)if(p.id==id)return p;return null;}
    private static void persist(Activity a,List<Plan> plans){PlanStore.save(a,plans);ReminderScheduler.rescheduleAll(a,plans);}
    private static String normalCategory(String v){return "book".equals(v)||"work".equals(v)||"video".equals(v)||"sport".equals(v)||"faith".equals(v)||"appointment".equals(v)||"food".equals(v)||"personal".equals(v)?v:"personal";}
    private static String normalRepeat(String v){return Plan.REPEAT_DAILY.equals(v)||Plan.REPEAT_WEEKDAYS.equals(v)||Plan.REPEAT_WEEKLY.equals(v)?v:Plan.REPEAT_NONE;}

    private static String context(List<Plan> plans){
        StringBuilder b=new StringBuilder();int shown=0;
        for(Plan p:plans){if(Plan.STATUS_CANCELLED.equals(p.status))continue;b.append("id=").append(p.id).append(" | ").append(p.date).append(" | ").append(p.startTime==null?"":p.startTime).append("–").append(p.endTime==null?"":p.endTime).append(" | ").append(p.title).append(" | category=").append(p.category).append(" | status=").append(p.status);if(p.note!=null&&!p.note.trim().isEmpty())b.append(" | note=").append(p.note.trim());if(p.details!=null&&!p.details.trim().isEmpty())b.append(" | details=").append(p.details.trim());b.append("\n");if(++shown>=70)break;}
        return b.length()==0?"Henüz plan yok.\n":b.toString();
    }

    private static void resetRecognizer(SpeechRecognizer[] r, TextView mic, TextView status){if(r[0]!=null){try{r[0].destroy();}catch(Exception ignored){}r[0]=null;}mic.setText("🎙");mic.setBackground(round(SURFACE2,17));}
    private static void addBubble(Activity a,LinearLayout messages,ScrollView scroll,String text,boolean user){if(text==null||text.trim().isEmpty())return;LinearLayout line=row(a);line.setGravity(user?Gravity.END:Gravity.START);TextView b=txt(a,text.trim(),14,TEXT,false);b.setMaxWidth(dp(a,305));b.setPadding(dp(a,13),dp(a,10),dp(a,13),dp(a,10));b.setBackground(round(user?USER_BUBBLE:SURFACE2,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.setMargins(user?dp(a,40):0,dp(a,4),user?0:dp(a,40),dp(a,4));line.addView(b,p);messages.addView(line,new LinearLayout.LayoutParams(-1,-2));scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));}
    private static LinearLayout col(Activity a){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private static LinearLayout row(Activity a){LinearLayout l=col(a);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private static TextView txt(Activity a,String s,float z,int c,boolean bold){TextView t=new TextView(a);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));t.setIncludeFontPadding(false);t.setLineSpacing(dp(a,2),1.04f);return t;}
    private static TextView button(Activity a,String s,int fg,int bg,float size){TextView t=txt(a,s,size,fg,true);t.setGravity(Gravity.CENTER);t.setBackground(round(bg,16));return t;}
    private static LinearLayout.LayoutParams lp(Activity a,int w,int h){return new LinearLayout.LayoutParams(dp(a,w),dp(a,h));}
    private static LinearLayout.LayoutParams mt(Activity a,int n){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(a,n);return p;}
    private static LinearLayout.LayoutParams mtX(Activity a,int n){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.leftMargin=dp(a,n);return p;}
    private static void gap(LinearLayout l,Activity a,int w){l.addView(new View(a),lp(a,w,1));}
    private static int dp(Activity a,int n){return(int)(n*a.getResources().getDisplayMetrics().density+.5f);}
    private static android.graphics.drawable.GradientDrawable round(int c,int r){android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(c);g.setCornerRadius(r*android.content.res.Resources.getSystem().getDisplayMetrics().density);return g;}
    private static void tap(View v){v.animate().scaleX(.97f).scaleY(.97f).setDuration(55).withEndAction(()->v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();}
    private static String today(){Calendar c=Calendar.getInstance();return String.format(Locale.GERMANY,"%04d-%02d-%02d",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH));}
    private static Toast toast(Activity a,String s){Toast t=Toast.makeText(a,s,Toast.LENGTH_SHORT);t.show();return t;}
}
