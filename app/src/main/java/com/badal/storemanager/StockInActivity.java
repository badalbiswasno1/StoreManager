package com.badal.storemanager;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StockInActivity extends BaseActivity {
    private DatabaseHelper db;
    private List<Item> items = new ArrayList<>();
    private Spinner spinnerItem;
    private EditText etQuantity, etDcNumber, etGstNumber, etPurpose, etDate, etRemarks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_in);
        db = new DatabaseHelper(this);
        spinnerItem = findViewById(R.id.spinnerItem);
        etQuantity = findViewById(R.id.etQuantity);
        etDcNumber = findViewById(R.id.etDcNumber);
        etGstNumber = findViewById(R.id.etGstNumber);
        etPurpose = findViewById(R.id.etPurpose);
        etDate = findViewById(R.id.etDate);
        etRemarks = findViewById(R.id.etRemarks);
        etDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        items = db.getAllItems();
        List<String> names = new ArrayList<>();
        for (Item item : items) names.add(item.getName() + " (" + item.getUnit() + ")");
        spinnerItem.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        findViewById(R.id.btnSaveIn).setOnClickListener(v -> saveStockIn());
    }

    private void saveStockIn() {
        if (items.isEmpty()) { Toast.makeText(this, "আগে আইটেম যোগ করো", Toast.LENGTH_SHORT).show(); return; }
        String qtyStr = etQuantity.getText().toString().trim();
        if (qtyStr.isEmpty()) { Toast.makeText(this, "পরিমাণ লিখো", Toast.LENGTH_SHORT).show(); return; }
        double qty = Double.parseDouble(qtyStr);
        Item selected = items.get(spinnerItem.getSelectedItemPosition());
        StockEntry entry = new StockEntry();
        entry.setItemId(selected.getId());
        entry.setItemName(selected.getName());
        entry.setType("IN");
        entry.setQuantity(qty);
        entry.setDcNumber(etDcNumber.getText().toString().trim());
        entry.setGstNumber(etGstNumber.getText().toString().trim());
        entry.setPurpose(etPurpose.getText().toString().trim());
        entry.setDate(etDate.getText().toString().trim());
        entry.setRemarks(etRemarks.getText().toString().trim());
        entry.setTower("");
        entry.setContractor("");
        entry.setEngineer("");
        db.addStockEntry(entry);
        db.updateItemStock(selected.getId(), qty, true);
        Toast.makeText(this, "Stock IN সেভ হয়েছে!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
