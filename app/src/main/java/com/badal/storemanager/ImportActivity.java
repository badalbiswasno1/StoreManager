package com.badal.storemanager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImportActivity extends AppCompatActivity {
    private static final int PICK_FILE = 101;
    private DatabaseHelper db;
    private TextView tvLog;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);
        db = new DatabaseHelper(this);
        tvLog = findViewById(R.id.tvLog);
        progressBar = findViewById(R.id.progressBar);
        findViewById(R.id.btnPickFile).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Excel বা CSV বেছে নাও"), PICK_FILE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            String fileName = getFileName(uri);
            tvLog.setText("পড়া হচ্ছে: " + fileName + "...");
            progressBar.setVisibility(View.VISIBLE);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(() -> {
                String result = processFile(uri, fileName);
                handler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvLog.setText(result);
                });
            });
        }
    }

    private String processFile(Uri uri, String fileName) {
        if (fileName != null && (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".xls"))) {
            return importExcel(uri, fileName.toLowerCase().endsWith(".xls"));
        } else {
            return importCsv(uri);
        }
    }

    private String importExcel(Uri uri, boolean isXls) {
        int imported = 0, skipped = 0, newItems = 0;
        StringBuilder log = new StringBuilder();
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Workbook wb = isXls ? new HSSFWorkbook(is) : new XSSFWorkbook(is);
            DataFormatter formatter = new DataFormatter();
            log.append("Sheet পাওয়া গেছে: ").append(wb.getNumberOfSheets()).append("টি\n\n");

            for (int si = 0; si < wb.getNumberOfSheets(); si++) {
                Sheet sheet = wb.getSheetAt(si);
                String sheetName = sheet.getSheetName();
                if (sheetName.equalsIgnoreCase("DASHBOARD") || sheetName.equalsIgnoreCase("Price List") || sheetName.equalsIgnoreCase("Sheet1") || sheetName.equalsIgnoreCase("Indent")) continue;
                log.append("=== ").append(sheetName).append(" ===\n");

                Row headerRow1 = null, headerRow2 = null;
                int dataStartRow = -1;

                for (int r = 0; r <= Math.min(10, sheet.getLastRowNum()); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    String firstCell = formatter.formatCellValue(row.getCell(0)).trim().toLowerCase();
                    if (firstCell.equals("sl.no") || firstCell.equals("sl no") || firstCell.equals("si no") || firstCell.equals("sno") || firstCell.equals("s.no")) {
                        headerRow1 = row;
                        Row next = sheet.getRow(r + 1);
                        if (next != null) {
                            String nextFirst = formatter.formatCellValue(next.getCell(0)).trim();
                            if (nextFirst.isEmpty() || nextFirst.equals("null")) {
                                headerRow2 = next;
                                dataStartRow = r + 2;
                            } else {
                                dataStartRow = r + 1;
                            }
                        }
                        break;
                    }
                }

                if (headerRow1 == null || dataStartRow < 0) { log.append("Header পাওয়া যায়নি, skip\n\n"); continue; }

                int dateCol = -1, dcCol = -1, contractorCol = -1, engineerCol = -1, remarksCol = -1;
                List<Integer> itemCols = new ArrayList<>();
                List<String> itemNames = new ArrayList<>();

                for (int c = 0; c < headerRow1.getLastCellNum(); c++) {
                    String h = formatter.formatCellValue(headerRow1.getCell(c)).trim().toLowerCase();
                    String h2 = headerRow2 != null ? formatter.formatCellValue(headerRow2.getCell(c)).trim().toLowerCase() : "";
                    if (h.contains("date")) dateCol = c;
                    else if (h.contains("dc") || h.contains("invoice") || h.contains("gate pass") || h.contains("issue time")) dcCol = c;
                    else if (h.contains("contractor")) contractorCol = c;
                    else if (h.contains("issue by") || h.contains("engineer") || h.contains("driver") || h.contains("operator")) engineerCol = c;
                    else if (h.contains("remark")) remarksCol = c;
                    else if (!h.isEmpty() && !h.equals("sl.no") && !h.equals("sl no") && !h.equals("si no") && !h.equals("unit") && !h.equals("totals") && !h.equals("total") && !h.contains("balance") && !h.contains("stock") && !h.contains("meter") && !h.contains("running") && !h.contains("floor") && !h.contains("pour") && !h.contains("concrete") && !h.contains("location") && !h.contains("area") && !h.contains("block")) {
                        String fullName = h;
                        if (!h2.isEmpty()) fullName = h + " " + h2;
                        fullName = fullName.trim();
                        itemCols.add(c);
                        itemNames.add(capitalizeFirst(fullName));
                    }
                }

                log.append("আইটেম কলাম: ").append(itemCols.size()).append("টি\n");
                int sheetImported = 0;

                for (int r = dataStartRow; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    String firstCell = formatter.formatCellValue(row.getCell(0)).trim();
                    if (firstCell.isEmpty() || firstCell.equalsIgnoreCase("total") || firstCell.equalsIgnoreCase("totals")) continue;

                    String dateStr = "";
                    if (dateCol >= 0) {
                        Cell dc = row.getCell(dateCol);
                        if (dc != null && dc.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dc)) {
                            Date d = dc.getDateCellValue();
                            dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(d);
                        } else {
                            dateStr = formatter.formatCellValue(dc).trim();
                        }
                    }
                    if (dateStr.isEmpty() || dateStr.equals("---") || dateStr.equals("--")) { skipped++; continue; }

                    String dcNum = dcCol >= 0 ? formatter.formatCellValue(row.getCell(dcCol)).trim() : "";
                    String contractor = contractorCol >= 0 ? formatter.formatCellValue(row.getCell(contractorCol)).trim() : "";
                    String engineer = engineerCol >= 0 ? formatter.formatCellValue(row.getCell(engineerCol)).trim() : "";
                    String remarks = remarksCol >= 0 ? formatter.formatCellValue(row.getCell(remarksCol)).trim() : "";

                    for (int ci = 0; ci < itemCols.size(); ci++) {
                        int col = itemCols.get(ci);
                        String qtyStr = formatter.formatCellValue(row.getCell(col)).trim();
                        if (qtyStr.isEmpty() || qtyStr.equals("0") || qtyStr.equals("---") || qtyStr.equals("--") || qtyStr.equals("-")) continue;
                        double qty = 0;
                        try { qty = Double.parseDouble(qtyStr.replaceAll("[^0-9.]", "")); } catch (Exception e) { continue; }
                        if (qty <= 0) continue;

                        String itemName = sheetName + " - " + itemNames.get(ci);
                        Item item = findOrCreateItem(itemName);
                        if (item.getId() < 0) { newItems++; item.setId((int)-item.getId()); }

                        StockEntry entry = new StockEntry();
                        entry.setItemId(item.getId());
                        entry.setItemName(itemName);
                        entry.setType("OUT");
                        entry.setQuantity(qty);
                        entry.setDate(dateStr);
                        entry.setDcNumber(dcNum.equals("---") ? "" : dcNum);
                        entry.setContractor(contractor.equals("---") ? "" : contractor);
                        entry.setEngineer(engineer.equals("---") ? "" : engineer);
                        entry.setRemarks(remarks);
                        entry.setTower("");
                        entry.setPurpose(sheetName);
                        entry.setGstNumber("");
                        db.addStockEntry(entry);
                        db.updateItemStock(item.getId(), qty, false);
                        imported++;
                        sheetImported++;
                    }
                }
                log.append("Import হয়েছে: ").append(sheetImported).append("টি এন্ট্রি\n\n");
            }
            wb.close();

            log.append("✅ সম্পূর্ণ Import শেষ!\n");
            log.append("মোট এন্ট্রি: ").append(imported).append("\n");
            log.append("নতুন আইটেম তৈরি: ").append(newItems).append("\n");
            log.append("Skip: ").append(skipped);
            return log.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String importCsv(Uri uri) {
        int imported = 0, skipped = 0, newItems = 0;
        StringBuilder log = new StringBuilder();
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, "UTF-8"));
            String line;
            boolean headerFound = false;
            String[] headers = null;
            int dateIdx=-1,typeIdx=-1,itemIdx=-1,qtyIdx=-1,towerIdx=-1,contractorIdx=-1,engineerIdx=-1,purposeIdx=-1,dcIdx=-1,gstIdx=-1,remarksIdx=-1,receivedIdx=-1;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("\uFEFF")) line = line.substring(1);
                String[] cols = line.split(",", -1);
                if (!headerFound) {
                    for (String col : cols) {
                        String c = col.trim().toLowerCase();
                        if (c.contains("date") || c.contains("তারিখ") || c.contains("item") || c.contains("vehicle") || c.contains("invoice") || c.contains("sl")) { headerFound = true; break; }
                    }
                    if (headerFound) {
                        headers = cols;
                        dateIdx = findCol(headers, "date","তারিখ");
                        typeIdx = findCol(headers, "type","ধরন");
                        itemIdx = findCol(headers, "item","vehicle name","আইটেম");
                        qtyIdx = findCol(headers, "issue qty","quantity issued","qty","পরিমাণ");
                        receivedIdx = findCol(headers, "received qty","received");
                        towerIdx = findCol(headers, "tower","location","টাওয়ার");
                        contractorIdx = findCol(headers, "contractor","ঠিকাদার");
                        engineerIdx = findCol(headers, "engineer","driver","operator","issue by","ইঞ্জিনিয়ার");
                        purposeIdx = findCol(headers, "purpose","উদ্দেশ্য");
                        dcIdx = findCol(headers, "dc","invoice","gate pass");
                        gstIdx = findCol(headers, "gst");
                        remarksIdx = findCol(headers, "remarks","মন্তব্য");
                    }
                    continue;
                }
                if (cols.length < 3) { skipped++; continue; }
                String rawDate = safeGet(cols, dateIdx);
                if (rawDate.isEmpty() || rawDate.startsWith("=") || rawDate.equals("---")) { skipped++; continue; }
                String itemName = safeGet(cols, itemIdx);
                if (itemName.isEmpty()) { skipped++; continue; }
                String qtyStr = safeGet(cols, qtyIdx);
                if (qtyStr.isEmpty() || qtyStr.startsWith("=")) qtyStr = safeGet(cols, receivedIdx);
                if (qtyStr.isEmpty() || qtyStr.startsWith("=")) { skipped++; continue; }
                double qty = 0;
                try { qty = Double.parseDouble(qtyStr.replaceAll("[^0-9.]","")); } catch (Exception e) { skipped++; continue; }
                if (qty <= 0) { skipped++; continue; }
                String type = safeGet(cols, typeIdx).toUpperCase();
                if (type.isEmpty()) type = !safeGet(cols, receivedIdx).isEmpty() ? "IN" : "OUT";
                Item item = findOrCreateItem(itemName);
                if (item.getId() < 0) { newItems++; item.setId((int)-item.getId()); }
                StockEntry entry = new StockEntry();
                entry.setItemId(item.getId()); entry.setItemName(itemName); entry.setType(type.equals("IN")?"IN":"OUT");
                entry.setQuantity(qty); entry.setDate(rawDate.contains(" ")?rawDate.split(" ")[0]:rawDate);
                entry.setTower(safeGet(cols,towerIdx)); entry.setContractor(safeGet(cols,contractorIdx));
                entry.setEngineer(safeGet(cols,engineerIdx)); entry.setPurpose(safeGet(cols,purposeIdx));
                entry.setDcNumber(safeGet(cols,dcIdx)); entry.setGstNumber(safeGet(cols,gstIdx));
                entry.setRemarks(safeGet(cols,remarksIdx));
                db.addStockEntry(entry);
                db.updateItemStock(item.getId(), qty, type.equals("IN"));
                imported++;
            }
            reader.close();
            log.append("✅ CSV Import সফল!\nমোট: ").append(imported).append("\nনতুন আইটেম: ").append(newItems).append("\nSkip: ").append(skipped);
            return log.toString();
        } catch (Exception e) { return "Error: " + e.getMessage(); }
    }

    private Item findOrCreateItem(String name) {
        List<Item> items = db.getAllItems();
        for (Item item : items) { if (item.getName().equalsIgnoreCase(name)) return item; }
        Item newItem = new Item();
        newItem.setName(name); newItem.setUnit("Pcs"); newItem.setOpeningStock(0);
        long id = db.addItem(newItem); newItem.setId((int)-id);
        return newItem;
    }

    private String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String safeGet(String[] cols, int idx) {
        if (idx < 0 || idx >= cols.length) return "";
        return cols[idx].trim().replaceAll("^\"|\"$","");
    }

    private int findCol(String[] headers, String... keys) {
        if (headers == null) return -1;
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].trim().toLowerCase().replaceAll("^\"|\"$","");
            for (String key : keys) { if (h.contains(key.toLowerCase())) return i; }
        }
        return -1;
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try { if (cursor != null && cursor.moveToFirst()) { int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME); if (idx >= 0) result = cursor.getString(idx); } } finally { if (cursor != null) cursor.close(); }
        }
        if (result == null) { result = uri.getPath(); int cut = result.lastIndexOf('/'); if (cut != -1) result = result.substring(cut + 1); }
        return result;
    }
}
