package com.rota.app;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

public final class InlineAiPanel {
    public interface Callback { void changed(); }

    private static final int BG=Color.rgb(14,16,20);
    private static final int SURFACE=Color.rgb(24,27,33);
    private static final int SURFACE2=Color.rgb(32,36,43);
    private static final int TEXT=Color.rgb(245,245,245);
    private static final int MUTED=Color.rgb(157,163,172);
    private static final int ORANGE=Color.rgb(255,139,0);

    private InlineAiPanel() {}
    public static void show(Activity a,List<Plan> plans,Callback callback){
        SharedPreferences pref=a.getSharedPreferences("rotam_settings",0);
        String key=pref.getString("gemini_api_key","");
        if(key==null||key.trim().isEmpty()){toast(a,"Önce Ayarlar'dan Gemini'yi bağla.");return;}

        final Dialog d=new Dialog(a);d.setCancelable(true);
        LinearLayout box=col(a);box.setPadding(dp(a,18),dp(a,14),dp(a,18),dp(a,18));box.setBackground(round(BG,28));
        LinearLayout head=row(a);LinearLayout titles=col(a);
        titles.addView(txt(a,"✦ Rotam AI",20,TEXT,true));
        titles.addView(txt(a,"Buradan doğrudan planlarını yönet.",10,MUTED,false),mt(a,3));
        head.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView close=button(a,"×",TEXT,SURFACE);head.addView(close,lp(a,42,42));box.addView(head);

        TextView chat=txt(a,"Hazırım. Ne yapmamı istersin?",13,TEXT,false);
        chat.setPadding(dp(a,13),dp(a,12),dp(a,13),dp(a,12));chat.setBackground(round(SURFACE2,18));box.addView(chat,mt(a,9));

        LinearLayout ir=row(a);
        EditText input=new EditText(a);input.setSingleLine(true);input.setTextColor(TEXT);input.setHintTextColor(MUTED);input.setHint("Örn. Bugün 14:00'te randevum var...");
        input.setTextSize(15);input.setPadding(dp(a,14),0,dp(a,14),0);input.setBackground(round(SURFACE2,18));
        ir.addView(input,new LinearLayout.LayoutParams(0,dp(a,50),1));
        TextView mic=button(a,"🎙",TEXT,SURFACE2);ir.addView(mic,lp(a,50,50));space(ir,a,7);
        TextView send=button(a,"➤",Color.WHITE,ORANGE);ir.addView(send,lp(a,50,50));box.addView(ir,mt(a,9));

        d.setContentView(box);d.show();
        Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);android.view.WindowManager.LayoutParams p=w.getAttributes();p.gravity=Gravity.BOTTOM;w.setAttributes(p);w.setLayout((int)(a.getResources().getDisplayMetrics().widthPixels*.96f),-2);}

        Runnable ask=()->{
            String q=input.getText().toString().trim();if(q.isEmpty())return;
            send.setEnabled(false);chat.setText("Gemini düşünüyor…");
            new Thread(()->{
                String raw=call(a,key,q,context(plans));String reply=apply(a,plans,raw);
                a.runOnUiThread(()->{chat.setText(reply);input.setText("");send.setEnabled(true);if(callback!=null)callback.changed();});
            }).start();
        };
        send.setOnClickListener(v->ask.run());input.setOnEditorActionListener((v,action,event)->{ask.run();return true;});

        final SpeechRecognizer[] speech={null};
        Runnable stop=()->{try{if(speech[0]!=null){speech[0].cancel();speech[0].destroy();speech[0]=null;}}catch(Exception ignored){}};
        mic.setOnClickListener(v->{
            try{
                if(a.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){a.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},922);return;}
                speech[0]=SpeechRecognizer.createSpeechRecognizer(a);
                speech[0].setRecognitionListener(new RecognitionListener(){
                    public void onReadyForSpeech(android.os.Bundle b){chat.setText("Dinliyorum…");}
                    public void onBeginningOfSpeech(){}
                    public void onRmsChanged(float v){}
                    public void onBufferReceived(byte[] b){}
                    public void onEndOfSpeech(){}
                    public void onError(int e){chat.setText("Sesli giriş tamamlanamadı.");}
                    public void onResults(android.os.Bundle b){ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty()){input.setText(r.get(0));ask.run();}}
                    public void onPartialResults(android.os.Bundle b){}
                    public void onEvent(int type,android.os.Bundle b){}
                });
                Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"tr-TR");speech[0].startListening(i);
            }catch(Exception e){toast(a,"Sesli giriş başlatılamadı.");}
        });
        close.setOnClickListener(v->{stop.run();d.dismiss();});d.setOnDismissListener(v->stop.run());
    }


}
