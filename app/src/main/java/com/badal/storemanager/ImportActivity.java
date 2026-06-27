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
import java.util.List;

public class ImportActivity extends AppCompatActivity {
    private static final int PICK_FILE = 101;
    private DatabaseHelper db;
    private TextView tvLog;

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
            startActivityForResult(Intent.createChooser(intent, "ফাইল বেছে নাও"), PICK_FILE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            String fileName = getFileName(uri);
            tvLog.setText("ফাইল পড়া হচ্ছে: " + fileName + "\n\nঅনুগ্রহ করে অপেক্ষা করুন...");
            if (fileName != null && fileName.toLowerCase().endsWith(".csv")) {
                importCsv(uri);
            } else {
                tvLog.setText("❌ শুধু CSV ফাইল সাপোর্ট করে।\n\nExcel (.xlsx) থেকে CSV বানাও:\n1. Excel খোলো\n2. File → Save As\n3. Format: CSV (Comma delimited)\n4. সেই CSV ফাইলটা import করো\n\nতোমার Diesel.xlsx এর জন্য:\nColumn order হবে:\nSL,Date,DC/Invoice No,Vehicle Name,Vehicle No,Unit,Opening,Received Qty,Total,Issue Qty,Stock,Meter,Hours,Location,Area,Remarks");
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private void importCsv(Uri uri) {
        int imported = 0, skipped = 0, newItems = 0;
        StringBuilder log = new StringBuilder();
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;
            boolean headerFound = false;
            String[] headers = null;
            int dateIdx=-1, typeIdx=-1, itemIdx=-1, qtyIdx=-1, towerIdx=-1,
                contractorIdx=-1, engineerIdx=-1, purposeIdx=-1, dcIdx=-1,
                gstIdx=-1, remarksIdx=-1, receivedIdx=-1, vehicleIdx=-1, locationIdx=-1;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("\uFEFF")) line = line.substring(1);
                String[] cols = splitCsv(line);

                if (!headerFound) {
                    boolean looksLikeHeader = false;
                    for (String col : cols) {
                        String c = col.trim().toLowerCase();
                        if (c.contains("date") || c.contains("তারিখ") || c.contains("sl") ||
                            c.contains("item") || c.contains("vehicle") || c.contains("invoice")) {
                            looksLikeHeader = true;
                            break;
                        }
                    }
                    if (looksLikeHeader) {
                        headers = cols;
                        headerFound = true;
                        dateIdx = findCol(headers, "date", "তারিখ");
                        typeIdx = findCol(headers, "type", "ধরন");
                        itemIdx = findCol(headers, "item", "vehicle name", "equipment", "আইটেম");
                        vehicleIdx = findCol(headers, "vehicle name", "vehicle", "equipment name");
                        qtyIdx = findCol(headers, "issue qty", "quantity issued", "qty", "পরিমাণ");
                        receivedIdx = findCol(headers, "received qty", "received", "receive");
                        towerIdx = findCol(headers, "tower", "location", "টাওয়ার");
                        locationIdx = findCol(headers, "tower / location", "locations", "area");
                        contractorIdx = findCol(headers, "contractor", "ঠিকাদার");
                        engineerIdx = findCol(headers, "engineer", "driver", "operator", "ইঞ্জিনিয়ার");
                        purposeIdx = findCol(headers, "purpose", "উদ্দেশ্য");
                        dcIdx = findCol(headers, "dc", "invoice", "gate pass", "issue time");
                        gstIdx = findCol(headers, "gst");
                        remarksIdx = findCol(headers, "remarks", "মন্তব্য");
                        log.append("Header পাওয়া গেছে। কলাম: ").append(cols.length).append("টি\n");
                        continue;
                    }
                    continue;
                }

                if (cols.length < 3) { skipped++; continue; }

                String rawDate = safeGet(cols, dateIdx);
                if (rawDate.isEmpty() || rawDate.equals("---") || rawDate.equals("--")) { skipped++; continue; }
                if (rawDate.startsWith("=")) { skipped++; continue; }

                String itemName = safeGet(cols, itemIdx);
                if (itemName.isEmpty()) itemName = safeGet(cols, vehicleIdx);
                if (itemName.isEmpty() || itemName.equals("---") || itemName.equals("--")) { skipped++; continue; }

                String qtyStr = safeGet(cols, qtyIdx);
                if (qtyStr.isEmpty() || qtyStr.startsWith("=")) {
                    qtyStr = safeGet(cols, receivedIdx);
                }
                if (qtyStr.isEmpty() || qtyStr.startsWith("=") || qtyStr.equals("---")) { skipped++; continue; }

                double qty = 0;
                try { qty = Double.parseDouble(qtyStr.replaceAll("[^0-9.]", "")); }
                catch (Exception e) { skipped++; continue; }
                if (qty <= 0) { skipped++; continue; }

                String type = safeGet(cols, typeIdx).toUpperCase();
                if (type.isEmpty()) {
                    boolean hasReceived = !safeGet(cols, receivedIdx).isEmpty() && !safeGet(cols, receivedIdx).startsWith("=");
                    type = hasReceived ? "IN" : "OUT";
                }
                if (!type.equals("IN")) type = "OUT";

                String date = rawDate;
                if (date.contains(" ")) date = date.split(" ")[0];

                String tower = safeGet(cols, towerIdx);
                if (tower.isEmpty()) tower = safeGet(cols, locationIdx);
                String purpose = safeGet(cols, purposeIdx);
                String dc = safeGet(cols, dcIdx);
                String engineer = safeGet(cols, engineerIdx);
                String remarks = safeGet(cols, remarksIdx);

                Item existingItem = findOrCreateItem(itemName, db);
                if (existingItem == null) { skipped++; continue; }
                if (existingItem.getId() < 0) { newItems++; existingItem.setId((int)-existingItem.getId()); }

                StockEntry entry = new StockEntry();
                entry.setItemId(existingItem.getId());
                entry.setItemName(itemName);
                entry.setType(type);
                entry.setQuantity(qty);
                entry.setDate(date);
                entry.setTower(tower.equals("---") || tower.equals("--") ? "" : tower);
                entry.setContractor(safeGet(cols, contractorIdx));
                entry.setEngineer(engineer.equals("---") ? "" : engineer);
                entry.setPurpose(purpose);
                entry.setDcNumber(dc.equals("---") ? "" : dc);
                entry.setGstNumber(safeGet(cols, gstIdx));
                entry.setRemarks(remarks);

                db.addStockEntry(entry);
                db.updateItemStock(existingItem.getId(), qty, type.equals("IN"));
                imported++;
            }
            reader.close();

            log.append("\n✅ Import সফল!\n");
            log.append("মোট এন্ট্রি import: ").append(imported).append("\n");
            log.append("নতুন আইটেম তৈরি: ").append(newItems).append("\n");
            log.append("Skip হয়েছে: ").append(skipped).append("\n");
            log.append("Stock আপডেট হয়ে গেছে।");
            tvLog.setText(log.toString());
            Toast.makeText(this, imported + "টি এন্ট্রি import হয়েছে!", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            tvLog.setText("Error: " + e.getMessage());
        }
    }

    private String[] splitCsv(String line) {
        return line.split(",", -1);
    }

    private String safeGet(String[] cols, int idx) {
        if (idx < 0 || idx >= cols.length) return "";
        return cols[idx].trim().replaceAll("^\"|\"$", "");
    }

    private Item findOrCreateItem(String name, DatabaseHelper db) {
        List<Item> items = db.getAllItems();
        for (Item item : items) {
            if (item.getName().equalsIgnoreCase(name)) return item;
        }
        Item newItem = new Item();
        newItem.setName(name);
        newItem.setUnit("LTR");
        newItem.setOpeningStock(0);
        long id = db.addItem(newItem);
        newItem.setId((int) -id);
        return newItem;
    }

    private int findCol(String[] headers, String... keys) {
        if (headers == null) return -1;
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].trim().toLowerCase().replaceAll("^\"|\"$", "");
            for (String key : keys) {
                if (h.contains(key.toLowerCase())) return i;
            }
        }
        return -1;
    }
}
