package ui;

import dao.AuthDAO;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {

    private final JTextField txtUser = new JTextField(18);
    private final JPasswordField txtPass = new JPasswordField(18);
    private final JButton btnLogin = new JButton("Login");
    private final JButton btnRegister = new JButton("Create Account");

    private final AuthDAO authDAO = new AuthDAO();

    public LoginFrame() {
        setTitle("Book Shop - Login");
        setSize(420, 260);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel("Book Shop System", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Username:"), c);
        c.gridx = 1; form.add(txtUser, c);

        c.gridx = 0; c.gridy = 1; form.add(new JLabel("Password:"), c);
        c.gridx = 1; form.add(txtPass, c);

        root.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.add(btnLogin);
        buttons.add(btnRegister);
        root.add(buttons, BorderLayout.SOUTH);

        add(root);

        btnLogin.addActionListener(e -> doLogin());
        btnRegister.addActionListener(e -> {
            new RegisterFrame(this).setVisible(true);
            setVisible(false);
        });

        getRootPane().setDefaultButton(btnLogin);
    }

    private void doLogin() {
        String u = txtUser.getText().trim();
        String p = new String(txtPass.getPassword());

        if (u.isEmpty() || p.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter username and password!");
            return;
        }

        User user = authDAO.login(u, p);
        if (user == null) {
            JOptionPane.showMessageDialog(this, "Invalid login!");
            return;
        }

        // open main based on role
        MainFrame mf = new MainFrame(user);
        mf.setVisible(true);
        dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
