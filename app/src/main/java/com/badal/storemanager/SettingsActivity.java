package com.badal.storemanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.TextView;
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
        String[] languages = {"বাংলা", "English", "हिन्दी", "తెలుగు"};
        String[] codes = {"bn", "en", "hi", "te"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("ভাষা বেছে নাও / Select Language")
            .setItems(languages, (dialog, which) -> {
                setLocale(codes[which]);
                SharedPreferences.Editor editor = getSharedPreferences("settings", Context.MODE_PRIVATE).edit();
                editor.putString("language", codes[which]);
                editor.apply();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }).show();
    }

    private void setLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }
}
