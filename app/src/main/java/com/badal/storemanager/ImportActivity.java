package com.badal.storemanager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ImportActivity extends AppCompatActivity {
    private static final int PICK_CSV = 101;
    private DatabaseHelper db;
    private TextView tvLog;
    private int imported = 0, skipped = 0, newItems = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);
        db = new DatabaseHelper(this);
        tvLog = findViewById(R.id.tvLog);
        findViewById(R.id.btnPickFile).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "CSV ফাইল বেছে নাও"), PICK_CSV);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CSV && resultCode == RESULT_OK && data != null) {
            importCsv(data.getData());
        }
    }

    private void importCsv(Uri uri) {
        imported = 0; skipped = 0; newItems = 0;
        StringBuilder log = new StringBuilder();
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;
            boolean firstLine = true;
            String[] headers = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("\uFEFF")) line = line.substring(1);
                String[] cols = line.split(",", -1);

                if (firstLine) {
                    headers = cols;
                    firstLine = false;
                    log.append("কলাম পাওয়া গেছে: ").append(cols.length).append("টি\n\n");
                    continue;
                }

                if (cols.length < 3) { skipped++; continue; }

                int dateIdx = findCol(headers, "date", "তারিখ");
                int typeIdx = findCol(headers, "type", "ধরন");
                int itemIdx = findCol(headers, "item", "আইটেম");
                int qtyIdx = findCol(headers, "qty", "quantity", "পরিমাণ");
                int towerIdx = findCol(headers, "tower", "টাওয়ার");
                int contractorIdx = findCol(headers, "contractor", "ঠিকাদার");
                int engineerIdx = findCol(headers, "engineer", "ইঞ্জিনিয়ার");
                int purposeIdx = findCol(headers, "purpose", "উদ্দেশ্য");
                int dcIdx = findCol(headers, "dc", "dc_number", "dc number");
                int gstIdx = findCol(headers, "gst", "gst_number", "gst number");
                int remarksIdx = findCol(headers, "remarks", "মন্তব্য");

                String itemName = itemIdx >= 0 && itemIdx < cols.length ? cols[itemIdx].trim() : "";
                String type = typeIdx >= 0 && typeIdx < cols.length ? cols[typeIdx].trim().toUpperCase() : "OUT";
                String date = dateIdx >= 0 && dateIdx < cols.length ? cols[dateIdx].trim() : "";
                String qtyStr = qtyIdx >= 0 && qtyIdx < cols.length ? cols[qtyIdx].trim() : "0";

                if (itemName.isEmpty()) { skipped++; continue; }

                double qty = 0;
                try { qty = Double.parseDouble(qtyStr); } catch (Exception e) { skipped++; continue; }

                Item existingItem = findOrCreateItem(itemName);
                if (existingItem == null) { skipped++; continue; }

                StockEntry entry = new StockEntry();
                entry.setItemId(existingItem.getId());
                entry.setItemName(itemName);
                entry.setType(type.equals("IN") ? "IN" : "OUT");
                entry.setQuantity(qty);
                entry.setDate(date);
                entry.setTower(towerIdx >= 0 && towerIdx < cols.length ? cols[towerIdx].trim() : "");
                entry.setContractor(contractorIdx >= 0 && contractorIdx < cols.length ? cols[contractorIdx].trim() : "");
                entry.setEngineer(engineerIdx >= 0 && engineerIdx < cols.length ? cols[engineerIdx].trim() : "");
                entry.setPurpose(purposeIdx >= 0 && purposeIdx < cols.length ? cols[purposeIdx].trim() : "");
                entry.setDcNumber(dcIdx >= 0 && dcIdx < cols.length ? cols[dcIdx].trim() : "");
                entry.setGstNumber(gstIdx >= 0 && gstIdx < cols.length ? cols[gstIdx].trim() : "");
                entry.setRemarks(remarksIdx >= 0 && remarksIdx < cols.length ? cols[remarksIdx].trim() : "");

                db.addStockEntry(entry);
                db.updateItemStock(existingItem.getId(), qty, type.equals("IN"));
                imported++;
            }
            reader.close();

            log.append("✅ Import সফল!\n");
            log.append("মোট এন্ট্রি import: ").append(imported).append("\n");
            log.append("নতুন আইটেম তৈরি: ").append(newItems).append("\n");
            log.append("Skip হয়েছে: ").append(skipped).append("\n\n");
            log.append("Stock এখন আপডেট হয়ে গেছে।");
            tvLog.setText(log.toString());
            Toast.makeText(this, imported + "টি এন্ট্রি import হয়েছে!", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            tvLog.setText("Error: " + e.getMessage());
            Toast.makeText(this, "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Item findOrCreateItem(String name) {
        List<Item> items = db.getAllItems();
        for (Item item : items) {
            if (item.getName().equalsIgnoreCase(name)) return item;
        }
        Item newItem = new Item();
        newItem.setName(name);
        newItem.setUnit("Pcs");
        newItem.setOpeningStock(0);
        long id = db.addItem(newItem);
        newItem.setId((int) id);
        newItems++;
        return newItem;
    }

    private int findCol(String[] headers, String... keys) {
        if (headers == null) return -1;
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].trim().toLowerCase();
            for (String key : keys) {
                if (h.contains(key.toLowerCase())) return i;
            }
        }
        return -1;
    }
}
