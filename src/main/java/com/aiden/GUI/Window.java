package com.aiden.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.Scanner;

import com.aiden.IterationResult;
import com.aiden.Problem;
import org.yaml.snakeyaml.Yaml;

public class Window extends JFrame {
    private String code;
    private JTextArea textArea = new JTextArea();
    private JTextField filePathField = new JTextField();
    private String FILE_PATH;
    private WorkspacePanel workspacePanel; // custom panel with drawing
    private Map<String, Object> yamlData;
    private Map<String, CodeBlock> variableMap = new HashMap<>();

    public void runWindow() {
        JFrame frame = new JFrame("CSP Engine");

        FILE_PATH = "STARTING_SEQUENCE_PIN:012398895466738895";
        filePathField.setText(FILE_PATH!="STARTING_SEQUENCE_PIN:012398895466738895" ? FILE_PATH : "path/to/file/here");

        JTabbedPane tabs = new JTabbedPane();

        JPanel mainPanel = new JPanel(new BorderLayout());

        // === Game Design Tab ===
        GameDesignPanel gameDesignPanel = new GameDesignPanel(this);

        // === File path bar at the top ===
        filePathField.setFont(new Font("Consolas", Font.PLAIN, 14));
        filePathField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mainPanel.add(filePathField, BorderLayout.NORTH);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setMinimumSize(new Dimension(650, 0));

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setMinimumSize(new Dimension(650, 0));

        // Custom workspace panel with line-drawing logic
        workspacePanel = new WorkspacePanel();
        workspacePanel.setLayout(null);
        workspacePanel.setBackground(new Color(245, 245, 245));
        rightPanel.add(new JScrollPane(workspacePanel), BorderLayout.CENTER);

        // --- Buttons ---
        JPanel buttonPanel = new JPanel();
        buttonPanel.setPreferredSize(new Dimension(200, 1000));
        buttonPanel.setBackground(Color.WHITE);

        JButton saveButton = new JButton("SAVE");
        saveButton.setPreferredSize(new Dimension(200, 30));
        saveButton.addActionListener(new SaveButtonListener());
        buttonPanel.add(saveButton);

        JButton runButton = new JButton("RUN CSP");
        runButton.setPreferredSize(new Dimension(200, 30));
        runButton.addActionListener(new RunButtonListener());
        buttonPanel.add(runButton);

        rightPanel.add(buttonPanel, BorderLayout.EAST);

        // --- text stuff ---

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(750);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        JLabel editorLabel = new JLabel("YAML Editor");
        editorLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 16));
        editorLabel.setForeground(new Color(180, 200, 255));
        editorLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        editorLabel.setOpaque(true);
        editorLabel.setBackground(new Color(30, 32, 42));

        textArea = new JTextArea();
        textArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 15));
        textArea.setForeground(new Color(220, 220, 230));
        textArea.setBackground(new Color(55, 58, 67)); // softer than pure black
        textArea.setCaretColor(new Color(100, 180, 255));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setMargin(new Insets(12, 14, 12, 14));

// slight shadow border look
        textArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(60, 60, 80)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

// 🌈 on focus, gentle highlight
        textArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                textArea.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(100, 170, 255)),
                        BorderFactory.createEmptyBorder(8, 8, 8, 8)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                textArea.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(60, 60, 80)),
                        BorderFactory.createEmptyBorder(8, 8, 8, 8)
                ));
            }
        });

// scroll pane with modern scrollbar + borderless frame
        JScrollPane textScrollPane = new JScrollPane(textArea);
        textScrollPane.setBorder(BorderFactory.createEmptyBorder());
        textScrollPane.getVerticalScrollBar().setBackground(new Color(35, 38, 47));
        textScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        textScrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(90, 100, 120);
                this.trackColor = new Color(35, 38, 47);
            }
        });


        leftPanel.add(editorLabel, BorderLayout.NORTH);
        leftPanel.add(textScrollPane, BorderLayout.CENTER);

        tabs.addTab("CSP Editor", mainPanel);
        tabs.addTab("Game Design", gameDesignPanel);

        frame.getContentPane().add(tabs);

        // === File path logic ===
        filePathField.addActionListener(e -> {
            FILE_PATH = filePathField.getText().trim();
            reloadFile();
        });

        reloadFile();

        frame.setSize(1500, 1000);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        new Window().runWindow();
    }

    private void reloadFile() {
        StringBuilder text = new StringBuilder();
        try {
            if (FILE_PATH == "STARTING_SEQUENCE_PIN:012398895466738895"){
                // Load from inside JAR
                try (InputStream inputStream = getClass().getResourceAsStream("/instructions.yaml")) {
                    if (inputStream == null) {
                        System.err.println("Default instructions.yaml not found in JAR!");
                        textArea.setText("");
                        return;
                    }

                    Scanner scanner = new Scanner(inputStream);
                    while (scanner.hasNextLine()) {
                        text.append(scanner.nextLine()).append("\n");
                    }
                    scanner.close();

                    textArea.setText(text.toString());
                    System.out.println("Loaded internal resource: instructions.yaml");
                    code = textArea.getText();
                    parseYamlAndBuildBlocks();

                    // show default name in text field but still editable
                    filePathField.setText("(default) instructions.yaml");
                    return;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            File myFile = new File(FILE_PATH);
            if (!myFile.exists()) {
                textArea.setText("");
                return;
            }
            Scanner scanner = new Scanner(myFile);
            while (scanner.hasNextLine()) text.append(scanner.nextLine()).append("\n");
            scanner.close();
            textArea.setText(text.toString());
            System.out.println("Loaded file: " + FILE_PATH);
            code = textArea.getText();
            parseYamlAndBuildBlocks();
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + e.getMessage());
        }
    }

    private void saveToFile() {
        code = textArea.getText();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            writer.write(code);
        } catch (IOException err) {
            System.err.println("Error saving text to file: " + err.getMessage());
        }

        parseYamlAndBuildBlocks();
    }

    class SaveButtonListener implements ActionListener {
        @Override public void actionPerformed(ActionEvent e) {
            FILE_PATH = filePathField.getText().trim();
            saveToFile();
        }
    }

    class RunButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            FILE_PATH = filePathField.getText().trim();
            Problem problem = new Problem(FILE_PATH);
            IterationResult result = problem.executeCSP();

            // open new results window
            SwingUtilities.invokeLater(() -> new ResultWindow(result));
        }
    }

    private Point clampToWorkspace(int x, int y, int width, int height) {
        int maxX = Math.max(0, workspacePanel.getWidth() - width);
        int maxY = Math.max(0, workspacePanel.getHeight() - height);
        int newX = Math.max(0, Math.min(x, maxX));
        int newY = Math.max(0, Math.min(y, maxY));
        return new Point(newX, newY);
    }

    @SuppressWarnings("unchecked")
    private void parseYamlAndBuildBlocks() {
        workspacePanel.removeAll();
        variableMap.clear();

        try {
            Yaml yaml = new Yaml();
            yamlData = yaml.load(code);
            if (yamlData == null) return;

            Map<String, Object> variables = (Map<String, Object>) yamlData.get("variables");
            List<Map<String, Object>> constraints = (List<Map<String, Object>>) yamlData.get("constraints");

            int maxBlockWidth = 200;
            int maxBlockHeight = 100;
            int maxSpacingX = 20;
            int maxSpacingY = 40;
            int startX = 50;
            int startY = 60;
            int maxCols = 3;

            int totalVars = variables != null ? variables.size() : 0;
            int totalCons = constraints != null ? constraints.size() : 0;

            // Calculate scaling factor based on workspace width
            int availableWidth = workspacePanel.getWidth() - startX * 2;
            int cols = Math.min(totalVars, maxCols);
            double scaleX = 1.0;
            if (cols > 0) {
                double neededWidth = cols * maxBlockWidth + (cols - 1) * maxSpacingX;
                scaleX = Math.min(1.0, availableWidth / neededWidth);
            }
            double scaleY = 1.0; // can also scale Y if needed

            int blockWidth = (int) (maxBlockWidth * scaleX);
            int blockHeight = (int) (maxBlockHeight * scaleY);
            int spacingX = (int) (maxSpacingX * scaleX);
            int spacingY = (int) (maxSpacingY * scaleY);

            // Place variables in simple grid
            if (variables != null) {
                int col = 0, row = 0;
                for (var entry : variables.entrySet()) {
                    int x = startX + col * (blockWidth + spacingX);
                    int y = startY + row * (blockHeight + spacingY);

                    CodeBlock block = new CodeBlock("Variable: " + entry.getKey(), entry.getValue().toString(), true);
                    Point clamped = clampToWorkspace(x, y, blockWidth, blockHeight);
                    block.setLocation(clamped);
                    block.setSize(blockWidth, blockHeight);
                    workspacePanel.add(block);
                    variableMap.put(entry.getKey(), block);

                    col++;
                    if (col >= maxCols) {
                        col = 0;
                        row++;
                    }
                }
            }

            // Place constraints in a grid below variables
            if (constraints != null) {
                int conCols = (workspacePanel.getWidth()/blockWidth);
                int conCol = 0, conRow = 0;
                int constraintStartY = startY + ((totalVars + maxCols - 1) / maxCols) * (blockHeight + spacingY) + spacingY;

                for (Map<String, Object> c : constraints) {
                    StringBuilder content = new StringBuilder();
                    content.append("Type: ").append(c.get("type")).append("\n");
                    content.append("Comparison: ").append(c.get("comparison")).append("\n");
                    content.append("Variables: ").append(c.get("vars")).append("\n");
                    content.append("Value: ").append(c.get("value")).append("\n");
                    content.append("Modifier: ").append(c.get("modifier")).append("\n");

                    CodeBlock block = new CodeBlock("Constraint", content.toString(), false);

                    int x = startX + conCol * (blockWidth + spacingX);
                    int y = constraintStartY + conRow * (blockHeight + spacingY);

                    Point clamped = clampToWorkspace(x, y, blockWidth, blockHeight);
                    block.setBounds(clamped.x, clamped.y , blockWidth, blockHeight);
                    workspacePanel.add(block);

                    Object vars = c.get("vars");
                    if (vars instanceof java.util.List)
                        block.setConstraintVars((java.util.List<String>) vars);

                    conCol++;
                    if (conCol >= conCols) {
                        conCol = 0;
                        conRow++;
                    }
                }
            }

            // Resize workspace panel to fit all blocks
            int rowsVars = (totalVars + maxCols - 1) / maxCols;
            int rowsCons = (constraints != null) ? (totalCons + 1) / 2 : 0;
            int totalHeight = startY + (rowsVars + rowsCons) * (blockHeight + spacingY) + spacingY;
            workspacePanel.revalidate();
            workspacePanel.repaint();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // === Workspace panel draws connecting lines ===
    class WorkspacePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(2.0f));
            g2.setColor(new Color(255, 100, 100, 180));

            for (Component comp : getComponents()) {
                if (comp instanceof CodeBlock cb && !cb.getConstraintVars().isEmpty()) {
                    Point start = new Point(cb.getX(), cb.getY() + cb.getHeight() / 2);
                    for (String varName : cb.getConstraintVars()) {
                        CodeBlock varBlock = variableMap.get(varName);
                        if (varBlock != null) {
                            Point end = new Point(varBlock.getX() + varBlock.getWidth(), varBlock.getY() + varBlock.getHeight() / 2);
                            drawCurvedLine(g2, start, end);
                        }
                    }
                }
            }
        }

        private void drawCurvedLine(Graphics2D g2, Point start, Point end) {
            int ctrlX = (start.x + end.x) / 2;
            int ctrlY1 = start.y;
            int ctrlY2 = end.y;

            java.awt.geom.CubicCurve2D curve = new java.awt.geom.CubicCurve2D.Double(
                    start.x, start.y, ctrlX, ctrlY1, ctrlX, ctrlY2, end.x, end.y);
            g2.draw(curve);
        }
    }


    // === CodeBlock class (draggable blocks) ===
    class CodeBlock extends JPanel {
        private Point initialClick;
        private List<String> constraintVars = new ArrayList<>();

        public void setConstraintVars(List<String> vars) {
            if (vars != null) this.constraintVars = vars;
        }

        public List<String> getConstraintVars() {
            return constraintVars;
        }

        public CodeBlock(String title, String content, boolean isVariable) {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(180, 180, 180), 1, true),
                    BorderFactory.createEmptyBorder(4, 4, 4, 4)
            ));
            setBackground(isVariable ? new Color(217, 234, 255) : new Color(255, 230, 204));
            setOpaque(true);

            JLabel header = new JLabel(title, SwingConstants.CENTER);
            header.setFont(new Font("Segoe UI", Font.BOLD, 13));
            header.setOpaque(true);
            header.setBackground(isVariable ? new Color(153, 204, 255) : new Color(255, 204, 153));
            header.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

            JTextArea body = new JTextArea(content);
            body.setEditable(false);
            body.setFont(new Font("Consolas", Font.PLAIN, 10));
            body.setBackground(getBackground());
            body.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            body.setLineWrap(true);
            body.setWrapStyleWord(true);
            body.setOpaque(false);
            body.setFocusable(false);
            add(header, BorderLayout.NORTH);
            add(body, BorderLayout.CENTER);

            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    initialClick = e.getPoint();
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    int x = getX() + e.getX() - initialClick.x;
                    int y = getY() + e.getY() - initialClick.y;
                    setLocation(x, y);
                    workspacePanel.repaint();
                }
            });
        }
    }

    class ResultWindow extends JFrame {
        public ResultWindow(IterationResult result) {
            setTitle("CSP Solver Results");
            setSize(800, 600);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.setBackground(new Color(35, 38, 47));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            // === Header ===
            JLabel titleLabel = new JLabel("Solver Output", SwingConstants.CENTER);
            titleLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 22));
            titleLabel.setForeground(new Color(180, 200, 255));
            titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
            mainPanel.add(titleLabel, BorderLayout.NORTH);

            // === Result text ===
            JTextArea resultArea = new JTextArea();
            resultArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 15));
            resultArea.setForeground(new Color(220, 220, 230));
            resultArea.setBackground(new Color(45, 48, 57));
            resultArea.setCaretColor(new Color(100, 180, 255));
            resultArea.setMargin(new Insets(10, 12, 10, 12));
            resultArea.setEditable(false);
            resultArea.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(70, 70, 90)),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
            ));

            // build content
            StringBuilder sb = new StringBuilder();
            sb.append("Best Value: ").append(result.getBestValue()).append("\n\n");
            sb.append("Variable States:\n");

            var variableGroups = result.getVariables();
            for (int i = 0; i < variableGroups.size(); i++) {
                sb.append("Iteration ").append(i + 1).append(":\n");
                for (var variable : variableGroups.get(i)) {
                    sb.append("  ").append(variable.getVariableName())
                            .append(" = ").append(variable.getValue()).append("\n");
                }
                sb.append("\n");
            }

            resultArea.setText(sb.toString());

            // === Scroll Pane ===
            JScrollPane scrollPane = new JScrollPane(resultArea);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.getVerticalScrollBar().setBackground(new Color(35, 38, 47));
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
                @Override
                protected void configureScrollBarColors() {
                    this.thumbColor = new Color(90, 100, 120);
                    this.trackColor = new Color(35, 38, 47);
                }
            });

            mainPanel.add(scrollPane, BorderLayout.CENTER);

            // === Close button ===
            JButton closeBtn = new JButton("Close");
            closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            closeBtn.setForeground(Color.WHITE);
            closeBtn.setBackground(new Color(100, 130, 250));
            closeBtn.setFocusPainted(false);
            closeBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            closeBtn.addActionListener(e -> dispose());

            JPanel btnPanel = new JPanel();
            btnPanel.setBackground(new Color(35, 38, 47));
            btnPanel.add(closeBtn);
            mainPanel.add(btnPanel, BorderLayout.SOUTH);

            add(mainPanel);
            setVisible(true);
        }
    }

    public void setEditorText(String text) {
        textArea.setText(text);
    }

    public void setFilePath(String path) {
        filePathField.setText(path);
        FILE_PATH = path;
    }

    public void saveYAML() {
        saveToFile();
    }

    public IterationResult runYAML(){
        FILE_PATH = filePathField.getText().trim();
        Problem problem = new Problem(FILE_PATH);
        IterationResult result = problem.executeCSP();
        return result;
    }

}
