package model;

public class CartItem {

    private int bookId;
    private String title;
    private double unitPrice;
    private int qty;

    public CartItem(int bookId, String title, double unitPrice, int qty) {
        this.bookId = bookId;
        this.title = title;
        this.unitPrice = unitPrice;
        this.qty = qty;
    }

    public int getBookId() { return bookId; }
    public String getTitle() { return title; }
    public double getUnitPrice() { return unitPrice; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public double getLineTotal() {
        return unitPrice * qty;
    }
}
