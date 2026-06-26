package com.badal.storemanager;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class ReportActivity extends AppCompatActivity {
    private DatabaseHelper db;
    private List<StockEntry> entries = new ArrayList<>();
    private EntryAdapter adapter;
    private Spinner spinnerTower, spinnerContractor, spinnerEngineer, spinnerItem;
    private EditText etFromDate, etToDate;

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
        RecyclerView rv = findViewById(R.id.rvReport);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EntryAdapter();
        rv.setAdapter(adapter);
        loadSpinners();
        loadEntries("", "", "", "", "", "");
        findViewById(R.id.btnFilter).setOnClickListener(v -> applyFilter());
        findViewById(R.id.btnExportCsv).setOnClickListener(v -> exportCsv());
        findViewById(R.id.btnExportPdf).setOnClickListener(v -> exportPdf());
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

    private void exportCsv() {
        try {
            File dir = new File(getExternalFilesDir(null), "StoreManager");
            dir.mkdirs();
            File file = new File(dir, "report_" + System.currentTimeMillis() + ".csv");
            FileWriter fw = new FileWriter(file);
            fw.write("Date,Type,Item,Quantity,Tower,Contractor,Engineer,Purpose,Remarks\n");
            for (StockEntry e : entries) {
                fw.write(e.getDate() + "," + e.getType() + "," + e.getItemName() + "," + e.getQuantity() + "," + e.getTower() + "," + e.getContractor() + "," + e.getEngineer() + "," + e.getPurpose() + "," + e.getRemarks() + "\n");
            }
            fw.close();
            Toast.makeText(this, "CSV সেভ হয়েছে: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportPdf() {
        try {
            File dir = new File(getExternalFilesDir(null), "StoreManager");
            dir.mkdirs();
            File file = new File(dir, "report_" + System.currentTimeMillis() + ".html");
            FileWriter fw = new FileWriter(file);
            fw.write("<html><head><meta charset='UTF-8'><style>body{font-family:Arial;font-size:12px;}table{width:100%;border-collapse:collapse;}th,td{border:1px solid #333;padding:6px;text-align:left;}th{background:#1565C0;color:#fff;}tr:nth-child(even){background:#f5f5f5;}</style></head><body>");
            fw.write("<h2>Store Manager Report</h2><p>Badal Biswas — Store Incharge</p>");
            fw.write("<table><tr><th>Date</th><th>Type</th><th>Item</th><th>Qty</th><th>Tower</th><th>Contractor</th><th>Engineer</th><th>Purpose</th><th>Remarks</th></tr>");
            for (StockEntry e : entries) {
                String color = e.getType().equals("IN") ? "#E8F5E9" : "#FFEBEE";
                fw.write("<tr style='background:" + color + "'><td>" + e.getDate() + "</td><td>" + e.getType() + "</td><td>" + e.getItemName() + "</td><td>" + e.getQuantity() + "</td><td>" + e.getTower() + "</td><td>" + e.getContractor() + "</td><td>" + e.getEngineer() + "</td><td>" + e.getPurpose() + "</td><td>" + e.getRemarks() + "</td></tr>");
            }
            fw.write("</table></body></html>");
            fw.close();
            Toast.makeText(this, "HTML Report সেভ হয়েছে: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
            String color = e.getType().equals("IN") ? "🟢" : "🔴";
            holder.tv.setText(color + " " + e.getDate() + " | " + e.getItemName() + " | " + e.getQuantity() + "\nটাওয়ার: " + e.getTower() + " | ঠিকাদার: " + e.getContractor() + " | ইঞ্জিনিয়ার: " + e.getEngineer());
            holder.tv.setBackgroundColor(e.getType().equals("IN") ? 0xFFE8F5E9 : 0xFFFFEBEE);
        }
        public int getItemCount() { return entries.size(); }
    }
}
