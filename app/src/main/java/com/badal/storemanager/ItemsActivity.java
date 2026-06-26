package com.badal.storemanager;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ItemsActivity extends AppCompatActivity {
    private DatabaseHelper db;
    private List<Item> allItems = new ArrayList<>();
    private List<Item> filteredItems = new ArrayList<>();
    private ItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_items);
        db = new DatabaseHelper(this);
        RecyclerView rv = findViewById(R.id.rvItems);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ItemAdapter();
        rv.setAdapter(adapter);
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { filterItems(s.toString()); }
            public void afterTextChanged(Editable s) {}
        });
        findViewById(R.id.fabAddItem).setOnClickListener(v -> showAddItemDialog(null));
        loadItems();
    }

    private void loadItems() {
        allItems = db.getAllItems();
        filteredItems = new ArrayList<>(allItems);
        adapter.notifyDataSetChanged();
    }

    private void filterItems(String query) {
        filteredItems.clear();
        for (Item item : allItems) {
            if (item.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredItems.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showAddItemDialog(Item existing) {
        View v = LayoutInflater.from(this).inflate(android.R.layout.activity_list_item, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(existing == null ? "নতুন আইটেম যোগ করো" : "আইটেম এডিট করো");
        View dialogView = LayoutInflater.from(this).inflate(android.R.layout.activity_list_item, null);
        EditText etName = new EditText(this);
        etName.setHint("আইটেমের নাম (যেমন: Diesel, Coupler)");
        EditText etUnit = new EditText(this);
        etUnit.setHint("একক (যেমন: Litre, Pcs, Kg)");
        EditText etOpening = new EditText(this);
        etOpening.setHint("Opening Stock");
        etOpening.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (existing != null) {
            etName.setText(existing.getName());
            etUnit.setText(existing.getUnit());
            etOpening.setText(String.valueOf(existing.getOpeningStock()));
        }
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);
        layout.addView(etName);
        layout.addView(etUnit);
        layout.addView(etOpening);
        builder.setView(layout);
        builder.setPositiveButton("সেভ করো", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String unit = etUnit.getText().toString().trim();
            String openingStr = etOpening.getText().toString().trim();
            if (name.isEmpty()) return;
            double opening = openingStr.isEmpty() ? 0 : Double.parseDouble(openingStr);
            if (existing == null) {
                Item item = new Item();
                item.setName(name);
                item.setUnit(unit);
                item.setOpeningStock(opening);
                db.addItem(item);
            } else {
                db.updateOpeningStock(existing.getId(), opening);
            }
            loadItems();
        });
        builder.setNegativeButton("বাতিল", null);
        builder.show();
    }

    class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvStock, tvUnit;
            Button btnEdit, btnDelete;
            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvItemName);
                tvStock = v.findViewById(R.id.tvItemStock);
                tvUnit = v.findViewById(R.id.tvItemUnit);
                btnEdit = v.findViewById(R.id.btnEdit);
                btnDelete = v.findViewById(R.id.btnDelete);
            }
        }
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row, parent, false));
        }
        public void onBindViewHolder(VH holder, int position) {
            Item item = filteredItems.get(position);
            holder.tvName.setText(item.getName());
            holder.tvStock.setText("Current: " + item.getCurrentStock() + " | Opening: " + item.getOpeningStock());
            holder.tvUnit.setText(item.getUnit());
            holder.btnEdit.setOnClickListener(v -> showAddItemDialog(item));
            holder.btnDelete.setOnClickListener(v -> new AlertDialog.Builder(ItemsActivity.this)
                .setTitle("Delete করবে?")
                .setMessage(item.getName() + " এবং এর সব এন্ট্রি মুছে যাবে।")
                .setPositiveButton("হ্যাঁ", (d, w) -> { db.deleteItem(item.getId()); loadItems(); })
                .setNegativeButton("না", null).show());
        }
        public int getItemCount() { return filteredItems.size(); }
    }
}
