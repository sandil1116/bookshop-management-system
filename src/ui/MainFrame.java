package ui;

import model.User;

import dao.BookDAO;
import db.DBConnection;
import model.Book;
import model.CartItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {

    // ====== DAO + Data ======
    private final BookDAO bookDAO = new BookDAO();
    private final List<CartItem> cart = new ArrayList<>();
    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    // current user (for role-based tabs + logout)
    private final User currentUser;

    // ====== Components: Books tab ======
    private JTable tblBooks;
    private DefaultTableModel booksModel;

    private JTextField txtTitle, txtAuthor, txtPrice, txtStock, txtSearch;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear, btnRefresh;

    private int selectedBookId = -1;

    // ====== Components: Sell tab ======
    private JTable tblSellBooks;
    private DefaultTableModel sellBooksModel;

    private JTable tblCart;
    private DefaultTableModel cartModel;

    private JTextField txtSellSearch, txtQty, txtUpdateQty;
    private JLabel lblTotal;

    private int sellSelectedBookId = -1;

    // ✅ UPDATED: constructor now receives the logged-in user
    public MainFrame(User user) {
        this.currentUser = user;

        setTitle("Book Shop - " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
        setSize(1050, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Simple modern-ish look
        setUITheme();

        // top bar with logout
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel lblUser = new JLabel("Logged in: " + currentUser.getUsername() + " | Role: " + currentUser.getRole());
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Logout now?",
                    "Confirm Logout",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                new LoginFrame().setVisible(true); // back to login
                dispose();
            }
        });

        topBar.add(lblUser, BorderLayout.WEST);
        topBar.add(btnLogout, BorderLayout.EAST);

       JTabbedPane tabs = new JTabbedPane();

    if (currentUser.isAdmin()) {
        tabs.addTab("📚 Manage Books", buildBooksPanel());
        tabs.addTab("🧾 Sell Books", buildSellPanel());
}   else if ("CASHIER".equalsIgnoreCase(currentUser.getRole())) {
        tabs.addTab("🧾 Sell Books", buildSellPanel());
}   else { // CUSTOMER
        tabs.addTab("🛒 Buy Books", buildSellPanel()); 
}


        JPanel main = new JPanel(new BorderLayout());
        main.add(topBar, BorderLayout.NORTH);
        main.add(tabs, BorderLayout.CENTER);

        add(main);

        // load data
        if (currentUser.isAdmin()) {
            loadBooksToManageTable();
        }
        loadBooksToSellTable();
    }

    // ===================== UI THEME =====================
    private void setUITheme() {
        UIManager.put("Table.rowHeight", 26);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
    }

    private JPanel buildBooksPanel() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Top: search + refresh
        JPanel top = new JPanel(new BorderLayout(8, 8));
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        txtSearch = new JTextField(25);
        JButton btnSearch = new JButton("Search");
        btnRefresh = new JButton("Refresh");

        searchPanel.add(new JLabel("Search (Title/Author):"));
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnRefresh);

        top.add(searchPanel, BorderLayout.WEST);
        root.add(top, BorderLayout.NORTH);

        // Center: table
        booksModel = new DefaultTableModel(new Object[]{"ID", "Title", "Author", "Price", "Stock"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblBooks = new JTable(booksModel);
        JScrollPane sp = new JScrollPane(tblBooks);
        root.add(sp, BorderLayout.CENTER);

        // Right: form
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createTitledBorder("Book Details"));

        txtTitle = new JTextField(20);
        txtAuthor = new JTextField(20);
        txtPrice = new JTextField(20);
        txtStock = new JTextField(20);

        form.add(fieldRow("Title", txtTitle));
        form.add(fieldRow("Author", txtAuthor));
        form.add(fieldRow("Price", txtPrice));
        form.add(fieldRow("Stock", txtStock));

        JPanel btnRow = new JPanel(new GridLayout(3, 2, 8, 8));
        btnAdd = new JButton("Add");
        btnUpdate = new JButton("Update");
        btnDelete = new JButton("Delete");
        btnClear = new JButton("Clear");
        JButton btnFillSelected = new JButton("Load Selected");
        JButton btnDummy = new JButton(); // spacer
        btnDummy.setVisible(false);

        btnRow.add(btnAdd);
        btnRow.add(btnUpdate);
        btnRow.add(btnDelete);
        btnRow.add(btnClear);
        btnRow.add(btnFillSelected);
        btnRow.add(btnDummy);

        form.add(Box.createVerticalStrut(10));
        form.add(btnRow);

        root.add(form, BorderLayout.EAST);

        // ===== Events =====
        btnSearch.addActionListener(e -> searchManageBooks());
        btnRefresh.addActionListener(e -> {
            clearManageForm();
            loadBooksToManageTable();
        });

        btnAdd.addActionListener(e -> addBook());
        btnUpdate.addActionListener(e -> updateBook());
        btnDelete.addActionListener(e -> deleteBook());
        btnClear.addActionListener(e -> clearManageForm());

        btnFillSelected.addActionListener(e -> loadSelectedBookToForm());

        tblBooks.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tblBooks.getSelectedRow();
                if (row >= 0) {
                    selectedBookId = Integer.parseInt(booksModel.getValueAt(row, 0).toString());
                }
            }
        });

        return root;
    }

    private JPanel buildSellPanel() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Top: search
        JPanel top = new JPanel(new BorderLayout(8, 8));
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        txtSellSearch = new JTextField(25);
        JButton btnSellSearch = new JButton("Search");
        JButton btnSellRefresh = new JButton("Refresh");

        searchPanel.add(new JLabel("Search Books:"));
        searchPanel.add(txtSellSearch);
        searchPanel.add(btnSellSearch);
        searchPanel.add(btnSellRefresh);

        top.add(searchPanel, BorderLayout.WEST);
        root.add(top, BorderLayout.NORTH);

        // Left: Books table + qty add
        JPanel left = new JPanel(new BorderLayout(8, 8));
        left.setBorder(BorderFactory.createTitledBorder("Available Books"));

        sellBooksModel = new DefaultTableModel(new Object[]{"ID", "Title", "Author", "Price", "Stock"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblSellBooks = new JTable(sellBooksModel);
        left.add(new JScrollPane(tblSellBooks), BorderLayout.CENTER);

        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        txtQty = new JTextField(6);
        JButton btnAddToCart = new JButton("Add To Cart");
        addPanel.add(new JLabel("Qty:"));
        addPanel.add(txtQty);
        addPanel.add(btnAddToCart);
        left.add(addPanel, BorderLayout.SOUTH);

        // Right: Cart table + controls
        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.setBorder(BorderFactory.createTitledBorder("Cart"));

        cartModel = new DefaultTableModel(new Object[]{"BookID", "Title", "Unit Price", "Qty", "Line Total"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblCart = new JTable(cartModel);
        right.add(new JScrollPane(tblCart), BorderLayout.CENTER);

        JPanel cartControls = new JPanel(new GridLayout(4, 1, 8, 8));
        JPanel updRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        txtUpdateQty = new JTextField(6);
        JButton btnUpdateCartQty = new JButton("Update Qty");
        updRow.add(new JLabel("New Qty:"));
        updRow.add(txtUpdateQty);
        updRow.add(btnUpdateCartQty);

        JButton btnRemoveItem = new JButton("Remove Selected");
        JButton btnClearCart = new JButton("Clear Cart");
        JButton btnCheckout = new JButton("Checkout");

        cartControls.add(updRow);
        cartControls.add(btnRemoveItem);
        cartControls.add(btnClearCart);
        cartControls.add(btnCheckout);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        lblTotal = new JLabel("Total: 0.00");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 16));
        bottom.add(lblTotal, BorderLayout.WEST);

        JPanel bottomRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottomRight.add(cartControls);
        bottom.add(bottomRight, BorderLayout.EAST);

        right.add(bottom, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.55);
        root.add(split, BorderLayout.CENTER);

        // ===== Events =====
        btnSellSearch.addActionListener(e -> searchSellBooks());
        btnSellRefresh.addActionListener(e -> {
            txtSellSearch.setText("");
            loadBooksToSellTable();
        });

        tblSellBooks.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tblSellBooks.getSelectedRow();
                if (row >= 0) {
                    sellSelectedBookId = Integer.parseInt(sellBooksModel.getValueAt(row, 0).toString());
                }
            }
        });

        btnAddToCart.addActionListener(e -> addSelectedBookToCart());

        btnUpdateCartQty.addActionListener(e -> updateSelectedCartQty());
        btnRemoveItem.addActionListener(e -> removeSelectedCartItem());
        btnClearCart.addActionListener(e -> clearCart());
        btnCheckout.addActionListener(e -> checkout());

        return root;
    }

    private JPanel fieldRow(String label, JTextField field) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        p.add(new JLabel(label + ":"));
        field.setPreferredSize(new Dimension(210, 28));
        p.add(field);
        return p;
    }

    // ===================== LOAD TABLES =====================
    private void loadBooksToManageTable() {
        booksModel.setRowCount(0);
        List<Book> books = bookDAO.getAllBooks();
        for (Book b : books) {
            booksModel.addRow(new Object[]{
                    b.getId(), b.getTitle(), b.getAuthor(), df.format(b.getPrice()), b.getStock()
            });
        }
    }

    private void loadBooksToSellTable() {
        sellBooksModel.setRowCount(0);
        List<Book> books = bookDAO.getAllBooks();
        for (Book b : books) {
            sellBooksModel.addRow(new Object[]{
                    b.getId(), b.getTitle(), b.getAuthor(), df.format(b.getPrice()), b.getStock()
            });
        }
    }

    private void searchManageBooks() {
        String k = txtSearch.getText().trim();
        if (k.isEmpty()) {
            loadBooksToManageTable();
            return;
        }
        booksModel.setRowCount(0);
        for (Book b : bookDAO.searchBooks(k)) {
            booksModel.addRow(new Object[]{b.getId(), b.getTitle(), b.getAuthor(), df.format(b.getPrice()), b.getStock()});
        }
    }

    private void searchSellBooks() {
        String k = txtSellSearch.getText().trim();
        if (k.isEmpty()) {
            loadBooksToSellTable();
            return;
        }
        sellBooksModel.setRowCount(0);
        for (Book b : bookDAO.searchBooks(k)) {
            sellBooksModel.addRow(new Object[]{b.getId(), b.getTitle(), b.getAuthor(), df.format(b.getPrice()), b.getStock()});
        }
    }

    // ===================== BOOKS TAB ACTIONS =====================
    private void loadSelectedBookToForm() {
        int row = tblBooks.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a book from the table first!");
            return;
        }
        selectedBookId = Integer.parseInt(booksModel.getValueAt(row, 0).toString());
        txtTitle.setText(booksModel.getValueAt(row, 1).toString());
        txtAuthor.setText(booksModel.getValueAt(row, 2).toString());
        txtPrice.setText(booksModel.getValueAt(row, 3).toString().replace(",", ""));
        txtStock.setText(booksModel.getValueAt(row, 4).toString());
    }

    private void addBook() {
        String title = txtTitle.getText().trim();
        String author = txtAuthor.getText().trim();
        String priceS = txtPrice.getText().trim();
        String stockS = txtStock.getText().trim();

        if (title.isEmpty() || author.isEmpty() || priceS.isEmpty() || stockS.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fill all fields!");
            return;
        }

        double price;
        int stock;
        try {
            price = Double.parseDouble(priceS);
            stock = Integer.parseInt(stockS);
            if (price < 0 || stock < 0) throw new Exception();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid price/stock!");
            return;
        }

        Book b = new Book(0, title, author, price, stock);
        boolean ok = bookDAO.addBook(b);

        if (ok) {
            JOptionPane.showMessageDialog(this, "Book added!");
            clearManageForm();
            loadBooksToManageTable();
            loadBooksToSellTable();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to add book!");
        }
    }

    private void updateBook() {
        if (selectedBookId == -1) {
            JOptionPane.showMessageDialog(this, "Select a book first (Load Selected)!");
            return;
        }

        String title = txtTitle.getText().trim();
        String author = txtAuthor.getText().trim();
        String priceS = txtPrice.getText().trim();
        String stockS = txtStock.getText().trim();

        if (title.isEmpty() || author.isEmpty() || priceS.isEmpty() || stockS.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fill all fields!");
            return;
        }

        double price;
        int stock;
        try {
            price = Double.parseDouble(priceS);
            stock = Integer.parseInt(stockS);
            if (price < 0 || stock < 0) throw new Exception();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid price/stock!");
            return;
        }

        Book b = new Book(selectedBookId, title, author, price, stock);
        boolean ok = bookDAO.updateBook(b);

        if (ok) {
            JOptionPane.showMessageDialog(this, "Book updated!");
            clearManageForm();
            loadBooksToManageTable();
            loadBooksToSellTable();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update!");
        }
    }

    private void deleteBook() {
        if (selectedBookId == -1) {
            JOptionPane.showMessageDialog(this, "Select a book first!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Delete this book?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        boolean ok = bookDAO.deleteBook(selectedBookId);
        if (ok) {
            JOptionPane.showMessageDialog(this, "Book deleted!");
            clearManageForm();
            loadBooksToManageTable();
            loadBooksToSellTable();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to delete!");
        }
    }

    private void clearManageForm() {
        selectedBookId = -1;
        txtTitle.setText("");
        txtAuthor.setText("");
        txtPrice.setText("");
        txtStock.setText("");
        tblBooks.clearSelection();
    }

    // ===================== SELL TAB ACTIONS =====================
    private void addSelectedBookToCart() {
        int row = tblSellBooks.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a book first!");
            return;
        }

        int bookId = Integer.parseInt(sellBooksModel.getValueAt(row, 0).toString());
        String title = sellBooksModel.getValueAt(row, 1).toString();
        double price = Double.parseDouble(sellBooksModel.getValueAt(row, 3).toString().replace(",", ""));
        int stock = Integer.parseInt(sellBooksModel.getValueAt(row, 4).toString());

        int qty;
        try {
            qty = Integer.parseInt(txtQty.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Enter a valid qty!");
            return;
        }

        if (qty <= 0) {
            JOptionPane.showMessageDialog(this, "Qty must be > 0");
            return;
        }
        if (qty > stock) {
            JOptionPane.showMessageDialog(this, "Not enough stock! Available: " + stock);
            return;
        }

        // If already in cart, increase qty
        for (CartItem ci : cart) {
            if (ci.getBookId() == bookId) {
                int newQty = ci.getQty() + qty;
                if (newQty > stock) {
                    JOptionPane.showMessageDialog(this, "Cart qty exceeds stock! Available: " + stock);
                    return;
                }
                ci.setQty(newQty);
                refreshCartTable();
                txtQty.setText("");
                return;
            }
        }

        cart.add(new CartItem(bookId, title, price, qty));
        refreshCartTable();
        txtQty.setText("");
    }

    private void refreshCartTable() {
        cartModel.setRowCount(0);
        for (CartItem ci : cart) {
            cartModel.addRow(new Object[]{
                    ci.getBookId(),
                    ci.getTitle(),
                    df.format(ci.getUnitPrice()),
                    ci.getQty(),
                    df.format(ci.getLineTotal())
            });
        }
        lblTotal.setText("Total: " + df.format(getCartTotal()));
    }

    private double getCartTotal() {
        double total = 0;
        for (CartItem ci : cart) total += ci.getLineTotal();
        return total;
    }

    private void updateSelectedCartQty() {
        int row = tblCart.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an item in cart first!");
            return;
        }

        int bookId = Integer.parseInt(cartModel.getValueAt(row, 0).toString());
        int newQty;
        try {
            newQty = Integer.parseInt(txtUpdateQty.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Enter a valid qty!");
            return;
        }

        if (newQty <= 0) {
            JOptionPane.showMessageDialog(this, "Qty must be > 0 (or remove item)");
            return;
        }

        // check stock from sell table (quick check)
        int stock = getStockFromSellTable(bookId);
        if (stock != -1 && newQty > stock) {
            JOptionPane.showMessageDialog(this, "Not enough stock! Available: " + stock);
            return;
        }

        for (CartItem ci : cart) {
            if (ci.getBookId() == bookId) {
                ci.setQty(newQty);
                refreshCartTable();
                txtUpdateQty.setText("");
                return;
            }
        }
    }

    private int getStockFromSellTable(int bookId) {
        for (int i = 0; i < sellBooksModel.getRowCount(); i++) {
            int id = Integer.parseInt(sellBooksModel.getValueAt(i, 0).toString());
            if (id == bookId) {
                return Integer.parseInt(sellBooksModel.getValueAt(i, 4).toString());
            }
        }
        return -1;
    }

    private void removeSelectedCartItem() {
        int row = tblCart.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an item in cart first!");
            return;
        }

        int bookId = Integer.parseInt(cartModel.getValueAt(row, 0).toString());
        cart.removeIf(ci -> ci.getBookId() == bookId);
        refreshCartTable();
    }

    private void clearCart() {
        cart.clear();
        refreshCartTable();
    }

    // Checkout: reduces stock for all items in ONE transaction (safe)
    private void checkout() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Confirm checkout?\nTotal: " + df.format(getCartTotal()),
                "Checkout", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            boolean ok = reduceStockForCartTransaction();
            if (!ok) {
                JOptionPane.showMessageDialog(this, "Checkout failed (maybe stock changed). Refresh and try again.");
                loadBooksToSellTable();
                refreshCartTable();
                return;
            }

            // Success: receipt
            String receipt = buildReceiptText();
            JOptionPane.showMessageDialog(this, receipt, "Receipt", JOptionPane.INFORMATION_MESSAGE);

            clearCart();
            if (currentUser.isAdmin()) {
                loadBooksToManageTable();
            }
            loadBooksToSellTable();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error during checkout: " + e.getMessage());
        }
    }

    // Transaction method: checks stock and updates stock for each cart item
    private boolean reduceStockForCartTransaction() throws Exception {
        String checkSql = "SELECT stock FROM books WHERE id=? FOR UPDATE";
        String updateSql = "UPDATE books SET stock = stock - ? WHERE id=?";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            // 1) Check all stocks first (locked)
            for (CartItem ci : cart) {
                try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                    ps.setInt(1, ci.getBookId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }
                        int stock = rs.getInt("stock");
                        if (ci.getQty() <= 0 || ci.getQty() > stock) {
                            con.rollback();
                            return false;
                        }
                    }
                }
            }

            // 2) Update stocks
            for (CartItem ci : cart) {
                try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                    ps.setInt(1, ci.getQty());
                    ps.setInt(2, ci.getBookId());
                    int rows = ps.executeUpdate();
                    if (rows <= 0) {
                        con.rollback();
                        return false;
                    }
                }
            }

            con.commit();
            con.setAutoCommit(true);
            return true;
        }
    }

    private String buildReceiptText() {
        StringBuilder sb = new StringBuilder();
        sb.append("BOOK SHOP RECEIPT\n");
        sb.append("------------------------------\n");
        for (CartItem ci : cart) {
            sb.append(ci.getTitle())
              .append("  x").append(ci.getQty())
              .append("  = ").append(df.format(ci.getLineTotal()))
              .append("\n");
        }
        sb.append("------------------------------\n");
        sb.append("TOTAL: ").append(df.format(getCartTotal())).append("\n");
        sb.append("\nThank you!");
        return sb.toString();
    }

    
}
