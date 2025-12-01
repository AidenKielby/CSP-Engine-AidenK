package com.aiden.GUI;

import com.aiden.IterationResult;
import com.aiden.Variable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

public class GameDesignPanel extends JPanel {
    private JTextField widthField = new JTextField("6", 4);
    private JTextField heightField = new JTextField("6", 4);
    private JComboBox<String> patternBox = new JComboBox<>(new String[]{"Random", "Vertical Lines", "Horizontal Lines", "Checkerboard"});
    private JPanel gridPanel = new JPanel();
    private JTextField tileTypesField = new JTextField("0, 1, 2, 3, 4, 5", 20);
    private JButton generateButton = new JButton("Generate Grid");
    private JButton patternButton = new JButton("Apply Pattern");
    private JButton exportButton = new JButton("Export to CSP Editor");
    private List<JButton> tiles = new ArrayList<>();
    private int gridW = 6, gridH = 6;
    private String[] tileTypes = tileTypesField.getText().split(", ");
    private Color[] colors = new Color[tileTypes.length];
    private Random rand = new Random();
    private Window parent;
    private JTextField filePathField = new JTextField();
    String FILE_PATH;

    public GameDesignPanel(Window parent) {
        this.parent = parent;
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());

        // === file path ===
        FILE_PATH = "";
        filePathField.setText(FILE_PATH!="" ? FILE_PATH : "path/to/file/here");

        filePathField.setFont(new Font("Consolas", Font.PLAIN, 14));
        filePathField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(filePathField, BorderLayout.NORTH);

        // === Control Bar ===
        JPanel controlBar = new JPanel();
        controlBar.add(new JLabel("Map Size:"));
        controlBar.add(widthField);
        controlBar.add(new JLabel("x"));
        controlBar.add(heightField);
        controlBar.add(new JLabel("Tile Types:"));
        controlBar.add(tileTypesField);
        controlBar.add(generateButton);
        controlBar.add(new JLabel("Pattern:"));
        controlBar.add(patternBox);
        controlBar.add(patternButton);
        controlBar.add(exportButton);
        mainPanel.add(controlBar, BorderLayout.NORTH);

        // === Grid Area ===
        gridPanel.setLayout(new GridLayout(gridH, gridW, 2, 2));
        mainPanel.add(gridPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        generateGrid();

        generateButton.addActionListener(e -> generateGrid());
        patternButton.addActionListener(e -> applyPattern());
        exportButton.addActionListener(e -> exportToYaml());
    }

    private void generateGrid() {
        gridPanel.removeAll();
        tiles.clear();
        try {
            gridW = Integer.parseInt(widthField.getText());
            gridH = Integer.parseInt(heightField.getText());
        } catch (NumberFormatException e) {
            gridW = 6;
            gridH = 6;
        }
        gridPanel.setLayout(new GridLayout(gridH, gridW, 2, 2));

        for (int y = 0; y < gridH; y++) {
            for (int x = 0; x < gridW; x++) {
                JButton tile = new JButton(" ");
                tile.setBackground(Color.WHITE);
                tile.setOpaque(true);
                tile.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                final int fx = x, fy = y;
                tile.addActionListener(e -> cycleTile(tile));
                tiles.add(tile);
                gridPanel.add(tile);
            }
        }

        tileTypes = tileTypesField.getText().split(", ");
        colors = new Color[tileTypes.length];

        for (int i = 0; i < tileTypes.length; i++) {
            int a = i*(255/tileTypes.length);
            colors[i] = new Color(a, a, a);
        }

        // === File path logic ===
        filePathField.addActionListener(e -> {
            FILE_PATH = filePathField.getText().trim();
        });

        revalidate();
        repaint();
    }

    private void cycleTile(JButton tile) {
        String current = tile.getText().trim();
        int index = -1;
        for (int i = 0; i < tileTypes.length; i++) {
            if (tileTypes[i].equals(current)) {
                index = i;
                break;
            }
        }
        int next = (index + 1) % tileTypes.length;
        tile.setText(tileTypes[next]);
        tile.setBackground(colors[next]);
    }

    private void applyPattern() {
        String pattern = (String) patternBox.getSelectedItem();

        tileTypes = tileTypesField.getText().split(", ");
        colors = new Color[tileTypes.length];

        for (int i = 0; i < tileTypes.length; i++) {
            int a = i*(255/tileTypes.length);
            colors[i] = new Color(a, a, a);

        }

        exportToYaml();

        IterationResult result = parent.runYAML();

        ArrayList<Variable> variables = result.getVariables().get(0);

        for (int y = 0; y < gridH; y++) {
            for (int x = 0; x < gridW; x++) {
                JButton tile = tiles.get(y * gridW + x);
                Variable variable = variables.get(y * gridW + x);

                tile.setText(Double.toString(variable.getValue()));
            }
        }

    }

    private void exportToYaml() {
        if (FILE_PATH != "") {
            StringBuilder yaml = new StringBuilder();
            String pattern = (String) patternBox.getSelectedItem();

            yaml.append("fairnessObjective:\n");
            yaml.append("  objective: \"balance\"\n\n");

            yaml.append("variables:\n");
            for (int y = 0; y < gridH; y++) {
                for (int x = 0; x < gridW; x++) {
                    String val = tiles.get(y * gridW + x).getText().trim();
                    yaml.append(String.format("  Tile%d-%d", x, y));
                    yaml.append(": [0,1,2,3,4,5]\n");
                    //yaml.append(String.format("    assigned: %s\n", val.isEmpty() ? "null" : val));
                }
            }
            yaml.append("\nconstraints:\n");

            switch (pattern) {
                case "Checkerboard":
                    // Adjacent tiles must be different
                    for (int y = 0; y < gridH; y++) {
                        for (int x = 0; x < gridW; x++) {
                            if (x + 1 < gridW) {
                                yaml.append("  - type: not_equal\n");
                                yaml.append(String.format("    vars: [Tile%d-%d, Tile%d-%d]\n", x, y, x + 1, y));
                                yaml.append("    modifier: 0\n");
                            }
                            if (y + 1 < gridH) {
                                yaml.append("  - type: not_equal\n");
                                yaml.append(String.format("    vars: [Tile%d-%d, Tile%d-%d]\n", x, y, x, y + 1));
                                yaml.append("    modifier: 0\n");
                            }
                        }
                    }
                    break;

                case "Vertical Lines":
                    // Each column’s tiles must be equal
                    for (int x = 0; x < gridW; x++) {
                        List<String> colVars = new ArrayList<>();
                        for (int y = 0; y < gridH; y++) {
                            colVars.add(String.format("Tile%d-%d", x, y));
                        }
                        yaml.append("  - type: equal\n");
                        yaml.append("    vars: [" + String.join(", ", colVars) + "]\n");
                        yaml.append("    modifier: 0\n");
                    }
                    break;

                case "Horizontal Lines":
                    // Each row’s tiles must be equal
                    for (int y = 0; y < gridH; y++) {
                        List<String> rowVars = new ArrayList<>();
                        for (int x = 0; x < gridW; x++) {
                            rowVars.add(String.format("Tile%d-%d", x, y));
                        }
                        yaml.append("  - type: equal\n");
                        yaml.append("    vars: [" + String.join(", ", rowVars) + "]\n");
                        yaml.append("    modifier: 0\n");
                    }
                    break;

                case "Random":
                    // Just randomize all tile variables
                    yaml.append("  - type: randomizer\n");
                    yaml.append("    vars: [");
                    for (int y = 0; y < gridH; y++) {
                        for (int x = 0; x < gridW; x++) {
                            yaml.append(String.format("Tile%d-%d", x, y));
                            if (!(x == gridW - 1 && y == gridH - 1)) yaml.append(", ");
                        }
                    }
                    yaml.append("]\n");
                    yaml.append("    modifier: 0\n");
                    break;
            }

            parent.setEditorText(yaml.toString());
            parent.setFilePath(FILE_PATH);
            parent.saveYAML();
        } else {
            System.err.println("Oops! Something went wrong!");
        }
    }

    public static int findIndex(String[] arr, String target) {
        if (arr == null) {
            return -1;
        }
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == target) {
                return i;
            }
        }
        return -1; // Item not found
    }
}

