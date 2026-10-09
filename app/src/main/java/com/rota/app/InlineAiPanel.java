package com.rota.app;

import android.app.Activity;
import java.util.List;

/** Compatibility entry point. The actual assistant UI is the compact ModernGeminiPanel. */
public final class InlineAiPanel {
    public interface Callback { void changed(); }
    private InlineAiPanel() {}

    public static void show(Activity a, List<Plan> plans, Callback callback) {
        ModernGeminiPanel.show(a, plans, () -> {
            if (callback != null) callback.changed();
        });
    }
}
