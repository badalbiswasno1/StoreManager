package com.badal.storemanager;

public class StockEntry {
    private int id;
    private int itemId;
    private String itemName;
    private String type;
    private double quantity;
    private String tower;
    private String contractor;
    private String engineer;
    private String purpose;
    private String date;
    private String remarks;
    private String dcNumber;
    private String gstNumber;

    public StockEntry() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public String getTower() { return tower; }
    public void setTower(String tower) { this.tower = tower; }
    public String getContractor() { return contractor; }
    public void setContractor(String contractor) { this.contractor = contractor; }
    public String getEngineer() { return engineer; }
    public void setEngineer(String engineer) { this.engineer = engineer; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getDcNumber() { return dcNumber; }
    public void setDcNumber(String dcNumber) { this.dcNumber = dcNumber; }
    public String getGstNumber() { return gstNumber; }
    public void setGstNumber(String gstNumber) { this.gstNumber = gstNumber; }
}
