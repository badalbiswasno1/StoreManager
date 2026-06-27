package com.badal.storemanager;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class ReportActivity extends BaseActivity {
    private DatabaseHelper db;
    private List<StockEntry> entries = new ArrayList<>();
    private EntryAdapter adapter;
    private Spinner spinnerTower, spinnerContractor, spinnerEngineer, spinnerItem;
    private EditText etFromDate, etToDate;
    private RecyclerView rvReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        db = new DatabaseHelper(this);
        spinnerTower = findViewById(R.id.spinnerTower);
        spinnerContractor = findViewById(R.id.spinnerContractor);
        spinnerEngineer = findViewById(R.id.spinnerEngineer);
        spinnerItem = findViewById(R.id.spinnerItem);
        etFromDate = findViewById(R.id.etFromDate);
        etToDate = findViewById(R.id.etToDate);
        rvReport = findViewById(R.id.rvReport);
        rvReport.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EntryAdapter();
        rvReport.setAdapter(adapter);
        loadSpinners();
        loadEntries("", "", "", "", "", "");
        findViewById(R.id.btnFilter).setOnClickListener(v -> applyFilter());
        findViewById(R.id.btnExportCsv).setOnClickListener(v -> exportExcel());
        findViewById(R.id.btnExportPdf).setOnClickListener(v -> showExportOptions());
    }

    private void loadSpinners() {
        setSpinner(spinnerTower, db.getDistinctValues("tower"));
        setSpinner(spinnerContractor, db.getDistinctValues("contractor"));
        setSpinner(spinnerEngineer, db.getDistinctValues("engineer"));
        setSpinner(spinnerItem, db.getDistinctValues("item_name"));
    }

    private void setSpinner(Spinner spinner, List<String> values) {
        List<String> opts = new ArrayList<>();
        opts.add("সব");
        opts.addAll(values);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, opts));
    }

    private void applyFilter() {
        String tower = spinnerTower.getSelectedItemPosition() == 0 ? "" : spinnerTower.getSelectedItem().toString();
        String contractor = spinnerContractor.getSelectedItemPosition() == 0 ? "" : spinnerContractor.getSelectedItem().toString();
        String engineer = spinnerEngineer.getSelectedItemPosition() == 0 ? "" : spinnerEngineer.getSelectedItem().toString();
        String item = spinnerItem.getSelectedItemPosition() == 0 ? "" : spinnerItem.getSelectedItem().toString();
        String from = etFromDate.getText().toString().trim();
        String to = etToDate.getText().toString().trim();
        loadEntries(tower, contractor, engineer, item, from, to);
    }

    private void loadEntries(String tower, String contractor, String engineer, String item, String from, String to) {
        entries = db.getFilteredEntries(tower, contractor, engineer, item, from, to);
        adapter.notifyDataSetChanged();
    }

    private void exportExcel() {
        try {
            File dir = new File(getExternalFilesDir(null), "StoreManager");
            dir.mkdirs();
            File file = new File(dir, "report_" + System.currentTimeMillis() + ".csv");
            FileWriter fw = new FileWriter(file);
            fw.write("\uFEFF");
            fw.write("তারিখ,ধরন,আইটেম,পরিমাণ,টাওয়ার,ঠিকাদার,ইঞ্জিনিয়ার,উদ্দেশ্য,মন্তব্য\n");
            for (StockEntry e : entries) {
                fw.write(e.getDate() + "," + e.getType() + "," + e.getItemName() + "," + e.getQuantity() + "," + e.getTower() + "," + e.getContractor() + "," + e.getEngineer() + "," + e.getPurpose() + "," + e.getRemarks() + "\n");
            }
            fw.write("\nStock Summary\n");
            fw.write("আইটেম,Opening Stock,Current Stock,একক\n");
            for (Item item : db.getAllItems()) {
                fw.write(item.getName() + "," + item.getOpeningStock() + "," + item.getCurrentStock() + "," + item.getUnit() + "\n");
            }
            fw.close();
            shareFile(file, "text/csv");
            Toast.makeText(this, "Excel CSV তৈরি হয়েছে!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showExportOptions() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Export ফরম্যাট বেছে নাও")
            .setItems(new String[]{"PDF (Printing)", "JPG (Image)"}, (dialog, which) -> {
                if (which == 0) exportPdf();
                else exportJpg();
            }).show();
    }

    private void exportPdf() {
        try {
            PdfDocument doc = new PdfDocument();
            int pageWidth = 595;
            int rowH = 22;
            int cols = 9;
            int[] colW = {55, 30, 70, 40, 50, 65, 65, 70, 60};
            int totalRows = entries.size() + 3;
            int pageHeight = Math.max(842, totalRows * rowH + 150);
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = doc.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            paint.setAntiAlias(true);

            paint.setColor(Color.parseColor("#1565C0"));
            canvas.drawRect(0, 0, pageWidth, 50, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(16);
            paint.setFakeBoldText(true);
            canvas.drawText("Store Manager — Badal Biswas", 20, 22, paint);
            paint.setTextSize(11);
            paint.setFakeBoldText(false);
            canvas.drawText("Stock Report | Total Entries: " + entries.size(), 20, 40, paint);

            String[] headers = {"Date","Type","Item","Qty","Tower","Contractor","Engineer","Purpose","Remarks"};
            int y = 65;
            paint.setColor(Color.parseColor("#0D47A1"));
            canvas.drawRect(10, y, pageWidth - 10, y + rowH, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(9);
            paint.setFakeBoldText(true);
            int x = 12;
            for (int i = 0; i < headers.length; i++) {
                canvas.drawText(headers[i], x, y + 15, paint);
                x += colW[i];
            }
            y += rowH;
            paint.setFakeBoldText(false);
            paint.setTextSize(8);
            for (int r = 0; r < entries.size(); r++) {
                StockEntry e = entries.get(r);
                if (r % 2 == 0) {
                    paint.setColor(Color.parseColor("#F5F5F5"));
                    canvas.drawRect(10, y, pageWidth - 10, y + rowH, paint);
                }
                paint.setColor(e.getType().equals("IN") ? Color.parseColor("#1B5E20") : Color.parseColor("#B71C1C"));
                x = 12;
                String[] row = {e.getDate(), e.getType(), e.getItemName(), String.valueOf(e.getQuantity()), e.getTower(), e.getContractor(), e.getEngineer(), e.getPurpose(), e.getRemarks()};
                for (int i = 0; i < row.length; i++) {
                    String cell = row[i] == null ? "" : row[i];
                    if (cell.length() > 10) cell = cell.substring(0, 10) + "..";
                    canvas.drawText(cell, x, y + 15, paint);
                    x += colW[i];
                }
                paint.setColor(Color.LTGRAY);
                canvas.drawLine(10, y + rowH, pageWidth - 10, y + rowH, paint);
                y += rowH;
            }

            y += 20;
            paint.setColor(Color.parseColor("#1565C0"));
            paint.setTextSize(12);
            paint.setFakeBoldText(true);
            canvas.drawText("Stock Summary", 20, y, paint);
            y += 18;
            paint.setFakeBoldText(false);
            paint.setTextSize(9);
            paint.setColor(Color.BLACK);
            for (Item item : db.getAllItems()) {
                canvas.drawText(item.getName() + " | Opening: " + item.getOpeningStock() + " | Current: " + item.getCurrentStock() + " " + item.getUnit(), 20, y, paint);
                y += 16;
            }

            doc.finishPage(page);
            File dir = new File(getExternalFilesDir(null), "StoreManager");
            dir.mkdirs();
            File file = new File(dir, "report_" + System.currentTimeMillis() + ".pdf");
            FileOutputStream fos = new FileOutputStream(file);
            doc.writeTo(fos);
            doc.close();
            fos.close();
            shareFile(file, "application/pdf");
            Toast.makeText(this, "PDF তৈরি হয়েছে!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportJpg() {
        try {
            int rowH = 40;
            int width = 1080;
            int headerH = 100;
            int summaryH = (db.getAllItems().size() + 2) * 35;
            int height = headerH + (entries.size() + 1) * rowH + summaryH + 60;
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setAntiAlias(true);

            paint.setColor(Color.WHITE);
            canvas.drawRect(0, 0, width, height, paint);
            paint.setColor(Color.parseColor("#1565C0"));
            canvas.drawRect(0, 0, width, headerH, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(36);
            paint.setFakeBoldText(true);
            canvas.drawText("Store Manager — Badal Biswas", 30, 45, paint);
            paint.setTextSize(22);
            paint.setFakeBoldText(false);
            canvas.drawText("Stock Report | Entries: " + entries.size(), 30, 80, paint);

            int y = headerH + 5;
            String[] headers = {"Date","Type","Item","Qty","Tower","Contractor","Engineer"};
            int[] cw = {140, 80, 180, 80, 130, 180, 190};
            paint.setColor(Color.parseColor("#0D47A1"));
            canvas.drawRect(0, y, width, y + rowH, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(20);
            paint.setFakeBoldText(true);
            int x = 10;
            for (int i = 0; i < headers.length; i++) {
                canvas.drawText(headers[i], x, y + 27, paint);
                x += cw[i];
            }
            y += rowH;
            paint.setFakeBoldText(false);
            paint.setTextSize(18);
            for (int r = 0; r < entries.size(); r++) {
                StockEntry e = entries.get(r);
                if (r % 2 == 0) {
                    paint.setColor(Color.parseColor("#F5F5F5"));
                    canvas.drawRect(0, y, width, y + rowH, paint);
                }
                paint.setColor(e.getType().equals("IN") ? Color.parseColor("#1B5E20") : Color.parseColor("#B71C1C"));
                x = 10;
                String[] row = {e.getDate(), e.getType(), e.getItemName(), String.valueOf(e.getQuantity()), e.getTower(), e.getContractor(), e.getEngineer()};
                for (int i = 0; i < row.length; i++) {
                    String cell = row[i] == null ? "" : row[i];
                    if (cell.length() > 12) cell = cell.substring(0, 12) + "..";
                    canvas.drawText(cell, x, y + 27, paint);
                    x += cw[i];
                }
                paint.setColor(Color.LTGRAY);
                canvas.drawLine(0, y + rowH, width, y + rowH, paint);
                y += rowH;
            }

            y += 30;
            paint.setColor(Color.parseColor("#1565C0"));
            paint.setTextSize(28);
            paint.setFakeBoldText(true);
            canvas.drawText("Stock Summary", 30, y, paint);
            y += 35;
            paint.setFakeBoldText(false);
            paint.setTextSize(20);
            paint.setColor(Color.BLACK);
            for (Item item : db.getAllItems()) {
                canvas.drawText(item.getName() + "  |  Opening: " + item.getOpeningStock() + "  |  Current: " + item.getCurrentStock() + " " + item.getUnit(), 30, y, paint);
                y += 35;
            }

            File dir = new File(getExternalFilesDir(null), "StoreManager");
            dir.mkdirs();
            File file = new File(dir, "report_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
            fos.close();
            shareFile(file, "image/jpeg");
            Toast.makeText(this, "JPG তৈরি হয়েছে!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareFile(File file, String mimeType) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share করো"));
        } catch (Exception e) {
            Toast.makeText(this, "Share error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            TextView tv;
            VH(View v) { super(v); tv = (TextView) v; }
        }
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(24, 16, 24, 16);
            tv.setTextSize(13);
            return new VH(tv);
        }
        public void onBindViewHolder(VH holder, int position) {
            StockEntry e = entries.get(position);
            String icon = e.getType().equals("IN") ? "🟢" : "🔴";
            holder.tv.setText(icon + " " + e.getDate() + " | " + e.getItemName() + " | " + e.getQuantity() + "\nটাওয়ার: " + e.getTower() + " | ঠিকাদার: " + e.getContractor() + " | ইঞ্জিনিয়ার: " + e.getEngineer());
            holder.tv.setBackgroundColor(e.getType().equals("IN") ? 0xFFE8F5E9 : 0xFFFFEBEE);
        }
        public int getItemCount() { return entries.size(); }
    }
}
