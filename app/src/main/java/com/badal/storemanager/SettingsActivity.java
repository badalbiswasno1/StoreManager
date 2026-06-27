package com.badal.storemanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class SettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        findViewById(R.id.btnLanguage).setOnClickListener(v -> showLanguageDialog());
    }

    private void showLanguageDialog() {
        SharedPreferences prefs = getSharedPreferences("settings", Context.MODE_PRIVATE);
        String currentLang = prefs.getString("language", "bn");

        String[] languages = {"বাংলা", "English", "हिन्दी", "తెలుగు"};
        String[] codes = {"bn", "en", "hi", "te"};

        final int[] selected = {-1};
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(currentLang)) { selected[0] = i; break; }
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("ভাষা বেছে নাও / Select Language");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 8);

        TextView[] options = new TextView[languages.length];
        for (int i = 0; i < languages.length; i++) {
            final int idx = i;
            TextView tv = new TextView(this);
            tv.setText(languages[i]);
            tv.setTextSize(17);
            tv.setPadding(32, 28, 32, 28);
            tv.setClickable(true);
            tv.setFocusable(true);
            if (i == selected[0]) {
                tv.setBackgroundColor(Color.parseColor("#E3F2FD"));
                tv.setTextColor(Color.parseColor("#1565C0"));
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
            }
            tv.setOnClickListener(v -> {
                for (TextView t : options) {
                    t.setBackgroundColor(Color.TRANSPARENT);
                    t.setTextColor(Color.BLACK);
                    t.setTypeface(null, android.graphics.Typeface.NORMAL);
                }
                tv.setBackgroundColor(Color.parseColor("#E3F2FD"));
                tv.setTextColor(Color.parseColor("#1565C0"));
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                selected[0] = idx;
            });
            options[i] = tv;
            layout.addView(tv);
        }

        builder.setView(layout);
        builder.setPositiveButton("✔ Confirm", (dialog, which) -> {
            if (selected[0] < 0) return;
            String lang = codes[selected[0]];
            prefs.edit().putString("language", lang).apply();
            applyLocale(lang);
        });
        builder.setNegativeButton("বাতিল", null);
        builder.show();
    }

    private void applyLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration(getResources().getConfiguration());
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        recreate();
    }
}
