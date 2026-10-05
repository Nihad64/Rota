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
}
