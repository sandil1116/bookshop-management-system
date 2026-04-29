package ui;

import dao.AuthDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class RegisterFrame extends JFrame {

    private final JTextField txtUser = new JTextField(18);
    private final JPasswordField txtPass = new JPasswordField(18);
    private final JPasswordField txtPass2 = new JPasswordField(18);

private final JComboBox<String> roleCombo = new JComboBox<>(new String[]{"CUSTOMER", "CASHIER"});

    private final JButton btnCreate = new JButton("Create");
    private final JButton btnBack = new JButton("Back");

    private final AuthDAO authDAO = new AuthDAO();
    private final JFrame parent;

    public RegisterFrame(JFrame parent) {
        this.parent = parent;

        setTitle("Book Shop - Register");
        setSize(460, 320);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel("Create New User", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Username:"), c);
        c.gridx = 1; form.add(txtUser, c);

        c.gridx = 0; c.gridy = 1; form.add(new JLabel("Password:"), c);
        c.gridx = 1; form.add(txtPass, c);

        c.gridx = 0; c.gridy = 2; form.add(new JLabel("Confirm Password:"), c);
        c.gridx = 1; form.add(txtPass2, c);

        c.gridx = 0; c.gridy = 3; form.add(new JLabel("Role:"), c);
        c.gridx = 1; form.add(roleCombo, c);

        root.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.add(btnCreate);
        buttons.add(btnBack);
        root.add(buttons, BorderLayout.SOUTH);

        add(root);

        btnCreate.addActionListener(e -> doRegister());
        btnBack.addActionListener(e -> {
            parent.setVisible(true);
            dispose();
        });
    }

    private void doRegister() {
        String u = txtUser.getText().trim();
        String p1 = new String(txtPass.getPassword());
        String p2 = new String(txtPass2.getPassword());
        String role = roleCombo.getSelectedItem().toString();

        if (u.isEmpty() || p1.isEmpty() || p2.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fill all fields!");
            return;
        }
        if (u.length() < 3) {
            JOptionPane.showMessageDialog(this, "Username must be at least 3 characters!");
            return;
        }
        if (!p1.equals(p2)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match!");
            return;
        }
        if (p1.length() < 4) {
            JOptionPane.showMessageDialog(this, "Password too short!");
            return;
        }

        if (authDAO.usernameExists(u)) {
            JOptionPane.showMessageDialog(this, "Username already exists!");
            return;
        }

        boolean ok = authDAO.register(u, p1, role);
        if (ok) {
            JOptionPane.showMessageDialog(this, "User created! Now login.");
            parent.setVisible(true);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to create user!");
        }
    }
}
