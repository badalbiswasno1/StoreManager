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

public class StockOutActivity extends AppCompatActivity {
    private DatabaseHelper db;
    private List<Item> items = new ArrayList<>();
    private Spinner spinnerItem;
    private EditText etQuantity, etDcNumber, etTower, etContractor, etEngineer, etPurpose, etDate, etRemarks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_out);
        db = new DatabaseHelper(this);
        spinnerItem = findViewById(R.id.spinnerItem);
        etQuantity = findViewById(R.id.etQuantity);
        etDcNumber = findViewById(R.id.etDcNumber);
        etTower = findViewById(R.id.etTower);
        etContractor = findViewById(R.id.etContractor);
        etEngineer = findViewById(R.id.etEngineer);
        etPurpose = findViewById(R.id.etPurpose);
        etDate = findViewById(R.id.etDate);
        etRemarks = findViewById(R.id.etRemarks);
        etDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        items = db.getAllItems();
        List<String> names = new ArrayList<>();
        for (Item item : items) names.add(item.getName() + " (" + item.getUnit() + ")");
        spinnerItem.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        findViewById(R.id.btnSaveOut).setOnClickListener(v -> saveStockOut());
    }

    private void saveStockOut() {
        if (items.isEmpty()) { Toast.makeText(this, "আগে আইটেম যোগ করো", Toast.LENGTH_SHORT).show(); return; }
        String qtyStr = etQuantity.getText().toString().trim();
        if (qtyStr.isEmpty()) { Toast.makeText(this, "পরিমাণ লিখো", Toast.LENGTH_SHORT).show(); return; }
        double qty = Double.parseDouble(qtyStr);
        Item selected = items.get(spinnerItem.getSelectedItemPosition());
        if (qty > selected.getCurrentStock()) { Toast.makeText(this, "স্টক কম! Available: " + selected.getCurrentStock(), Toast.LENGTH_LONG).show(); return; }
        StockEntry entry = new StockEntry();
        entry.setItemId(selected.getId());
        entry.setItemName(selected.getName());
        entry.setType("OUT");
        entry.setQuantity(qty);
        entry.setDcNumber(etDcNumber.getText().toString().trim());
        entry.setGstNumber("");
        entry.setTower(etTower.getText().toString().trim());
        entry.setContractor(etContractor.getText().toString().trim());
        entry.setEngineer(etEngineer.getText().toString().trim());
        entry.setPurpose(etPurpose.getText().toString().trim());
        entry.setDate(etDate.getText().toString().trim());
        entry.setRemarks(etRemarks.getText().toString().trim());
        db.addStockEntry(entry);
        db.updateItemStock(selected.getId(), qty, false);
        Toast.makeText(this, "Stock OUT সেভ হয়েছে!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
