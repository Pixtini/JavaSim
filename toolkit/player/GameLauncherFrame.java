package toolkit.player;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import toolkit.par.ParWorkbookModes;

/** Selects a game/PAR mode and launches its balancing player. */
public final class GameLauncherFrame extends JFrame {
    private final JComboBox<String> game = new JComboBox<>(
            new String[] {"expanding-wild", "G001-walkingwild"});
    private final JTextField workbook = new JTextField(36);
    private final JComboBox<String> mode = new JComboBox<>();

    private GameLauncherFrame() {
        super("JavaSim Game Launcher");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(18, 18, 8, 18));
        addRow(form, 0, "Game", game);
        JPanel workbookRow = new JPanel(new BorderLayout(6, 0));
        workbookRow.add(workbook, BorderLayout.CENTER);
        JButton browse = new JButton("Browse…");
        workbookRow.add(browse, BorderLayout.EAST);
        addRow(form, 1, "PAR workbook", workbookRow);
        addRow(form, 2, "Mode", mode);
        JLabel note = new JLabel("The player uses the selected PAR mode and its cost.");
        addRow(form, 3, "", note);
        JButton launch = new JButton("Launch player");
        JPanel actions = new JPanel();
        actions.add(launch);
        add(actions, BorderLayout.SOUTH);
        add(form, BorderLayout.CENTER);

        game.addActionListener(event -> updateWorkbookAndModes());
        browse.addActionListener(event -> chooseWorkbook());
        launch.addActionListener(event -> launchPlayer());
        workbook.addActionListener(event -> updateModes());
        updateWorkbookAndModes();
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    private static void addRow(JPanel panel, int row, String label, java.awt.Component input) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.LINE_END;
        left.insets = new Insets(6, 4, 6, 8);
        panel.add(new JLabel(label), left);
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1.0;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.anchor = GridBagConstraints.LINE_START;
        right.insets = new Insets(6, 4, 6, 4);
        panel.add(input, right);
    }

    private void updateWorkbookAndModes() {
        String selected = String.valueOf(game.getSelectedItem());
        Path par = selected.equals("G001-walkingwild")
                ? Path.of("game", "G001-walkingwild", "source", "G001-walkingWildsPAR.xlsx")
                : Path.of("game", "expandingwild", "expandingWildPAR.xlsx");
        workbook.setText(par.toString());
        updateModes();
    }

    private void updateModes() {
        String selectedMode = (String) mode.getSelectedItem();
        try {
            List<String> available = ParWorkbookModes.availableModes(Path.of(workbook.getText().trim()));
            mode.removeAllItems();
            available.forEach(mode::addItem);
            int selectedIndex = available.indexOf(selectedMode);
            int normalIndex = available.indexOf("Normal");
            mode.setSelectedIndex(selectedIndex >= 0 ? selectedIndex
                    : normalIndex >= 0 ? normalIndex : 0);
            mode.setEnabled(true);
        } catch (Exception exception) {
            mode.removeAllItems();
            mode.addItem("Normal");
            mode.setEnabled(false);
        }
    }

    private void chooseWorkbook() {
        JFileChooser chooser = new JFileChooser(Path.of(workbook.getText()).toFile());
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Excel workbooks (*.xlsx)", "xlsx"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            workbook.setText(chooser.getSelectedFile().getAbsolutePath());
            updateModes();
        }
    }

    private void launchPlayer() {
        try {
            PlayerGameAdapter adapter = PlayerAdapterFactory.create(
                    String.valueOf(game.getSelectedItem()), Path.of(workbook.getText().trim()),
                    String.valueOf(mode.getSelectedItem()), new Random());
            PrototypePlayerFrame.open(adapter);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(),
                    "Could not launch game", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void open() {
        SwingUtilities.invokeLater(() -> new GameLauncherFrame().setVisible(true));
    }
}
