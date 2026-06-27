package com.badal.storemanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "storemanager.db";
    private static final int DB_VERSION = 2;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, unit TEXT, opening_stock REAL DEFAULT 0, current_stock REAL DEFAULT 0)");
        db.execSQL("CREATE TABLE stock_entries (id INTEGER PRIMARY KEY AUTOINCREMENT, item_id INTEGER, item_name TEXT, type TEXT, quantity REAL, tower TEXT, contractor TEXT, engineer TEXT, purpose TEXT, date TEXT, remarks TEXT, dc_number TEXT, gst_number TEXT, FOREIGN KEY(item_id) REFERENCES items(id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try { db.execSQL("ALTER TABLE stock_entries ADD COLUMN dc_number TEXT"); } catch (Exception e) {}
        try { db.execSQL("ALTER TABLE stock_entries ADD COLUMN gst_number TEXT"); } catch (Exception e) {}
    }

    public long addItem(Item item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", item.getName());
        cv.put("unit", item.getUnit());
        cv.put("opening_stock", item.getOpeningStock());
        cv.put("current_stock", item.getOpeningStock());
        long id = db.insert("items", null, cv);
        db.close();
        return id;
    }

    public List<Item> getAllItems() {
        List<Item> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM items ORDER BY name", null);
        while (c.moveToNext()) {
            Item item = new Item();
            item.setId(c.getInt(0));
            item.setName(c.getString(1));
            item.setUnit(c.getString(2));
            item.setOpeningStock(c.getDouble(3));
            item.setCurrentStock(c.getDouble(4));
            list.add(item);
        }
        c.close();
        db.close();
        return list;
    }

    public void updateItemStock(int itemId, double qty, boolean isIn) {
        SQLiteDatabase db = this.getWritableDatabase();
        String op = isIn ? "current_stock + " : "current_stock - ";
        db.execSQL("UPDATE items SET current_stock = " + op + qty + " WHERE id = " + itemId);
        db.close();
    }

    public long addStockEntry(StockEntry entry) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("item_id", entry.getItemId());
        cv.put("item_name", entry.getItemName());
        cv.put("type", entry.getType());
        cv.put("quantity", entry.getQuantity());
        cv.put("tower", entry.getTower());
        cv.put("contractor", entry.getContractor());
        cv.put("engineer", entry.getEngineer());
        cv.put("purpose", entry.getPurpose());
        cv.put("date", entry.getDate());
        cv.put("remarks", entry.getRemarks());
        cv.put("dc_number", entry.getDcNumber());
        cv.put("gst_number", entry.getGstNumber());
        long id = db.insert("stock_entries", null, cv);
        db.close();
        return id;
    }

    public List<StockEntry> getFilteredEntries(String tower, String contractor, String engineer, String itemName, String fromDate, String toDate) {
        List<StockEntry> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        StringBuilder query = new StringBuilder("SELECT * FROM stock_entries WHERE 1=1");
        List<String> args = new ArrayList<>();
        if (tower != null && !tower.isEmpty()) { query.append(" AND tower LIKE ?"); args.add("%" + tower + "%"); }
        if (contractor != null && !contractor.isEmpty()) { query.append(" AND contractor LIKE ?"); args.add("%" + contractor + "%"); }
        if (engineer != null && !engineer.isEmpty()) { query.append(" AND engineer LIKE ?"); args.add("%" + engineer + "%"); }
        if (itemName != null && !itemName.isEmpty()) { query.append(" AND item_name LIKE ?"); args.add("%" + itemName + "%"); }
        if (fromDate != null && !fromDate.isEmpty()) { query.append(" AND date >= ?"); args.add(fromDate); }
        if (toDate != null && !toDate.isEmpty()) { query.append(" AND date <= ?"); args.add(toDate); }
        query.append(" ORDER BY date DESC");
        Cursor c = db.rawQuery(query.toString(), args.toArray(new String[0]));
        while (c.moveToNext()) {
            StockEntry e = new StockEntry();
            e.setId(c.getInt(0));
            e.setItemId(c.getInt(1));
            e.setItemName(c.getString(2));
            e.setType(c.getString(3));
            e.setQuantity(c.getDouble(4));
            e.setTower(c.getString(5));
            e.setContractor(c.getString(6));
            e.setEngineer(c.getString(7));
            e.setPurpose(c.getString(8));
            e.setDate(c.getString(9));
            e.setRemarks(c.getString(10));
            e.setDcNumber(c.getColumnIndex("dc_number") >= 0 ? c.getString(c.getColumnIndex("dc_number")) : "");
            e.setGstNumber(c.getColumnIndex("gst_number") >= 0 ? c.getString(c.getColumnIndex("gst_number")) : "");
            list.add(e);
        }
        c.close();
        db.close();
        return list;
    }

    public List<String> getDistinctValues(String column) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT DISTINCT " + column + " FROM stock_entries WHERE " + column + " IS NOT NULL AND " + column + " != '' ORDER BY " + column, null);
        while (c.moveToNext()) { list.add(c.getString(0)); }
        c.close();
        db.close();
        return list;
    }

    public void deleteItem(int itemId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("items", "id=?", new String[]{String.valueOf(itemId)});
        db.delete("stock_entries", "item_id=?", new String[]{String.valueOf(itemId)});
        db.close();
    }

    public void updateOpeningStock(int itemId, double opening) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE items SET opening_stock = " + opening + ", current_stock = " + opening + " WHERE id = " + itemId);
        db.close();
    }
}
