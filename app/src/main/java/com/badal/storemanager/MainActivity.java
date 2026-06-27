package com.badal.storemanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity {
    private DatabaseHelper db;

        SharedPreferences prefs = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String lang = prefs.getString("language", "bn");
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        super.attachBaseContext(newBase.createConfigurationContext(config));
    }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = new DatabaseHelper(this);
        findViewById(R.id.btnItems).setOnClickListener(v -> startActivity(new Intent(this, ItemsActivity.class)));
        findViewById(R.id.btnStockIn).setOnClickListener(v -> startActivity(new Intent(this, StockInActivity.class)));
        findViewById(R.id.btnStockOut).setOnClickListener(v -> startActivity(new Intent(this, StockOutActivity.class)));
        findViewById(R.id.btnReport).setOnClickListener(v -> startActivity(new Intent(this, ReportActivity.class)));
        findViewById(R.id.btnSettings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.btnAbout).setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("Store Manager")
            .setMessage("Developer: Badal Biswas\nStore Incharge — Construction Line\nVersion 1.0\n\nসব ধরনের মালের হিসাব রাখো সহজে।")
            .setPositiveButton("OK", null).show());
    }

        super.onResume();
        updateSummary();
    }

    private void updateSummary() {
        List<Item> items = db.getAllItems();
        TextView tvSummary = findViewById(R.id.tvSummary);
        if (items.isEmpty()) { tvSummary.setText("কোনো আইটেম নেই। প্রথমে আইটেম যোগ করুন।"); return; }
        StringBuilder sb = new StringBuilder();
        for (Item item : items) {
            sb.append("• ").append(item.getName()).append(": ").append(item.getCurrentStock()).append(" ").append(item.getUnit()).append("\n");
        }
        tvSummary.setText(sb.toString().trim());
    }
}
