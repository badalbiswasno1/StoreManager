package com.badal.storemanager;

public class Item {
    private int id;
    private String name;
    private String unit;
    private double openingStock;
    private double currentStock;

    public Item() {}

    public Item(int id, String name, String unit, double openingStock, double currentStock) {
        this.id = id;
        this.name = name;
        this.unit = unit;
        this.openingStock = openingStock;
        this.currentStock = currentStock;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public double getOpeningStock() { return openingStock; }
    public void setOpeningStock(double openingStock) { this.openingStock = openingStock; }
    public double getCurrentStock() { return currentStock; }
    public void setCurrentStock(double currentStock) { this.currentStock = currentStock; }
}
