import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class NotepadApp extends JFrame implements ActionListener {

    JTextArea textArea;
    JScrollPane scrollPane;
    JMenuBar menuBar;
    JMenu fileMenu, editMenu, formatMenu;
    JMenuItem newItem, openItem, saveItem, exitItem;
    JMenuItem cutItem, copyItem, pasteItem, findReplaceItem;
    JMenuItem fontItem, colorItem;
    JLabel statusLabel;

    JMenu recentMenu;
    LinkedList<String> recentFiles = new LinkedList<>();

    public NotepadApp() {
        // Window setup
        setTitle("Notepad App");
        setSize(700, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Text area
        textArea = new JTextArea();
        scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        // Menu
        menuBar = new JMenuBar();

        // File Menu
        fileMenu = new JMenu("File");
        newItem = new JMenuItem("New");
        openItem = new JMenuItem("Open");
        saveItem = new JMenuItem("Save");
        exitItem = new JMenuItem("Exit");

        newItem.addActionListener(this);
        openItem.addActionListener(this);
        saveItem.addActionListener(this);
        exitItem.addActionListener(this);

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Edit Menu
        editMenu = new JMenu("Edit");
        cutItem = new JMenuItem("Cut");
        copyItem = new JMenuItem("Copy");
        pasteItem = new JMenuItem("Paste");
        findReplaceItem = new JMenuItem("Find & Replace");

        cutItem.addActionListener(this);
        copyItem.addActionListener(this);
        pasteItem.addActionListener(this);
        findReplaceItem.addActionListener(this);

        editMenu.add(cutItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);
        editMenu.addSeparator();
        editMenu.add(findReplaceItem);

        // Format Menu
        formatMenu = new JMenu("Format");
        fontItem = new JMenuItem("Font");
        colorItem = new JMenuItem("Text Color");

        fontItem.addActionListener(this);
        colorItem.addActionListener(this);

        formatMenu.add(fontItem);
        formatMenu.add(colorItem);

        // Add menus to menu bar
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(formatMenu);
        setJMenuBar(menuBar);

        // Status bar
        statusLabel = new JLabel("Ready");
        add(statusLabel, BorderLayout.SOUTH);

        // Update status bar on typing
        textArea.addCaretListener(e -> {
            try {
                int pos = textArea.getCaretPosition();
                int line = textArea.getLineOfOffset(pos);
                int col = pos - textArea.getLineStartOffset(line);
                statusLabel.setText("Line: " + (line + 1) + ", Column: " + (col + 1));
            } catch (Exception ex) {
                statusLabel.setText("Ready");
            }
        });

        recentMenu = new JMenu("Recent Files");
        fileMenu.add(recentMenu);  // add below Exit

        initializeDatabase();      // create DB if not exists
        loadRecentFilesFromDB();   // load from DB


    }

    private void updateRecentMenu() {
        recentMenu.removeAll();
        for (String path : recentFiles) {
            JMenuItem recentItem = new JMenuItem(path);
            recentItem.addActionListener(e -> openRecentFile(path));
            recentMenu.add(recentItem);
        }
    }

    private void openRecentFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            textArea.setText("");
            String line;
            while ((line = br.readLine()) != null) {
                textArea.append(line + "\n");
            }
            statusLabel.setText("Opened: " + new File(filePath).getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error opening recent file.");
        }
    }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:notepad.db");
             Statement stmt = conn.createStatement()) {

            String createTableSQL = "CREATE TABLE IF NOT EXISTS recent_files (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "path TEXT NOT NULL UNIQUE," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");";

            stmt.execute(createTableSQL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addRecentFileToDB(String filePath) {
        String insertSQL = "INSERT OR REPLACE INTO recent_files (path, timestamp) VALUES (?, CURRENT_TIMESTAMP)";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:notepad.db");
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setString(1, filePath);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        loadRecentFilesFromDB();  // Refresh UI
    }

    private void loadRecentFilesFromDB() {
        recentFiles.clear();  // clear LinkedList
        String querySQL = "SELECT path FROM recent_files ORDER BY timestamp DESC LIMIT 5";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:notepad.db");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(querySQL)) {

            while (rs.next()) {
                recentFiles.add(rs.getString("path"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        updateRecentMenu();  // Refresh menu items
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == newItem) {
            textArea.setText("");

        } else if (e.getSource() == openItem) {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    textArea.setText("");
                    String line;
                    while ((line = br.readLine()) != null) {
                        textArea.append(line + "\n");
                    }
                    statusLabel.setText("Opened: " + file.getName());
                    addRecentFileToDB(file.getAbsolutePath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error opening file.");
                }
            }

        } else if (e.getSource() == saveItem) {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                    bw.write(textArea.getText());
                    statusLabel.setText("Saved: " + file.getName());
                    addRecentFileToDB(file.getAbsolutePath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving file.");
                }
            }

        } else if (e.getSource() == exitItem) {
            System.exit(0);

        } else if (e.getSource() == cutItem) {
            textArea.cut();
        } else if (e.getSource() == copyItem) {
            textArea.copy();
        } else if (e.getSource() == pasteItem) {
            textArea.paste();

        } else if (e.getSource() == findReplaceItem) {
            findReplaceDialog();

        } else if (e.getSource() == fontItem) {
            String fontName = JOptionPane.showInputDialog(this, "Enter font name (e.g., Arial, Times New Roman):");
            String fontSizeStr = JOptionPane.showInputDialog(this, "Enter font size (e.g., 16):");
            try {
                int fontSize = Integer.parseInt(fontSizeStr);
                textArea.setFont(new Font(fontName, Font.PLAIN, fontSize));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid font/size.");
            }

        } else if (e.getSource() == colorItem) {
            Color newColor = JColorChooser.showDialog(this, "Choose Text Color", textArea.getForeground());
            if (newColor != null) {
                textArea.setForeground(newColor);
            }
        }
    }


    // Find & Replace dialog
    private void findReplaceDialog() {
        JPanel panel = new JPanel(new GridLayout(3, 2));
        JTextField findField = new JTextField();
        JTextField replaceField = new JTextField();

        panel.add(new JLabel("Find:"));
        panel.add(findField);
        panel.add(new JLabel("Replace with:"));
        panel.add(replaceField);

        int option = JOptionPane.showConfirmDialog(this, panel, "Find & Replace", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String text = textArea.getText();
            String find = findField.getText();
            String replace = replaceField.getText();
            if (!find.isEmpty()) {
                textArea.setText(text.replace(find, replace));
                statusLabel.setText("Replaced all occurrences of: " + find);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new NotepadApp().setVisible(true);
        });
    }
}
