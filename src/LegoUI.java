import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/**
 * LEGO-styled Swing UI: bright brick colors, chunky bold type, and a
 * stud-dot banner. If a LEGO-style font file is dropped into fonts/
 * (e.g. fonts/lego.ttf) it is picked up automatically for the title;
 * otherwise a heavy bold sans-serif stands in.
 */
public class LegoUI {
    private static final Color LEGO_RED = new Color(0xC4, 0x11, 0x11);
    private static final Color LEGO_YELLOW = new Color(0xF2, 0xCD, 0x37);
    private static final Color LEGO_BLUE = new Color(0x00, 0x55, 0xBF);
    private static final Color LEGO_GREEN = new Color(0x23, 0x7D, 0x29);

    private final LegoCatalog catalog;
    private final DesignEngine engine;
    private final BuildPreview preview;
    private Design lastDesign;
    private String lastPrompt;

    public LegoUI(LegoCatalog catalog) {
        this.catalog = catalog;
        this.engine = ClaudeEngine.available() ? new ClaudeEngine(catalog) : new OfflineEngine(catalog);
        this.preview = new BuildPreview(catalog);
    }

    private Font titleFont(float size) {
        File fontFile = new File("fonts/lego.ttf");
        if (!fontFile.exists()) fontFile = new File("lego-generator-rebuild/fonts/lego.ttf");
        if (fontFile.exists()) {
            try {
                Font f = Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(size);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
                return f;
            } catch (FontFormatException | IOException ignored) {
                // fall through to the bold stand-in
            }
        }
        return new Font("SansSerif", Font.BOLD, (int) size);
    }

    public void show() {
        JFrame frame = new JFrame("LEGO Generator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1140, 700);
        frame.setLayout(new BorderLayout());

        // ---- Header: LEGO red band, yellow chunky title, stud dots ----
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(LEGO_RED);
        header.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel title = new JLabel("LEGO GENERATOR");
        title.setFont(titleFont(40f));
        title.setForeground(LEGO_YELLOW);
        JLabel subtitle = new JLabel("Type what you want to build — the engine sources the pieces.");
        subtitle.setForeground(Color.WHITE);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        frame.add(header, BorderLayout.NORTH);

        // ---- Form ----
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        JTextField promptField = new JTextField(26);
        promptField.setToolTipText("e.g. Eiffel Tower, red race car, lighthouse");
        JSpinner maxPieces = new JSpinner(new SpinnerNumberModel(300, 20, 5000, 10));
        maxPieces.setPreferredSize(new Dimension(80, 26));
        JTextField maxPrice = new JTextField("75.00", 7);
        JButton generate = new JButton("BUILD IT!");
        generate.setBackground(LEGO_YELLOW);
        generate.setFont(new Font("SansSerif", Font.BOLD, 14));
        generate.setOpaque(true);
        JButton booklet = new JButton("INSTRUCTIONS");
        booklet.setBackground(LEGO_BLUE);
        booklet.setForeground(Color.WHITE);
        booklet.setFont(new Font("SansSerif", Font.BOLD, 14));
        booklet.setOpaque(true);
        booklet.setEnabled(false);
        booklet.setToolTipText("Open the step-by-step instruction booklet for the last build");
        booklet.addActionListener(ev -> {
            if (lastDesign != null) new InstructionWindow(catalog, lastPrompt, lastDesign).show();
        });
        form.add(new JLabel("Build:"));
        form.add(promptField);
        form.add(new JLabel("Max pieces:"));
        form.add(maxPieces);
        form.add(new JLabel("Max price $:"));
        form.add(maxPrice);
        form.add(generate);
        form.add(booklet);

        // ---- Results ----
        JTextArea results = new JTextArea(
                "Tell me what to build! Try: Eiffel Tower, lighthouse, castle, red race car, pyramid...\n"
                + "Set a piece limit and a price limit, then hit BUILD IT.");
        results.setFont(new Font("Monospaced", Font.PLAIN, 13));
        results.setEditable(false);
        results.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane scroll = new JScrollPane(results);

        JLabel status = new JLabel("Engine: " + (ClaudeEngine.available()
                ? "Claude AI (API key found)" : "Offline archetype engine (no API key)"));
        status.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel middle = new JPanel(new BorderLayout());
        middle.add(scroll, BorderLayout.CENTER);
        middle.add(preview, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout());
        center.add(form, BorderLayout.NORTH);
        center.add(middle, BorderLayout.CENTER);
        center.add(status, BorderLayout.SOUTH);
        frame.add(center, BorderLayout.CENTER);

        // ---- Stud strip footer ----
        JLabel studs = new JLabel("● ● ● ● ● ● ● ●", JLabel.CENTER);
        studs.setForeground(LEGO_BLUE);
        studs.setFont(new Font("SansSerif", Font.BOLD, 16));
        frame.add(studs, BorderLayout.SOUTH);

        generate.addActionListener(ev -> {
            String prompt = promptField.getText().trim();
            if (prompt.isEmpty()) {
                results.setText("Type something to build first!");
                return;
            }
            int pieces = (Integer) maxPieces.getValue();
            double price;
            try {
                price = Double.parseDouble(maxPrice.getText().trim());
            } catch (NumberFormatException ex) {
                results.setText("The max price needs to be a number, like 75.00");
                return;
            }
            results.setText("Designing...");
            generate.setEnabled(false);
            new Thread(() -> {
                Design design = engine.generate(prompt, pieces, price);
                String text = design.render(pieces, price);
                SwingUtilities.invokeLater(() -> {
                    results.setText(text);
                    results.setCaretPosition(0);
                    preview.showDesign(prompt, design);
                    lastDesign = design;
                    lastPrompt = prompt;
                    booklet.setEnabled(true);
                    status.setText(String.format("Done: %d pieces, estimated $%.2f — %s",
                            design.totalPieces(), design.totalPrice(),
                            (design.totalPieces() <= pieces && design.totalPrice() <= price)
                                    ? "within your limits" : "could not fit the limits"));
                    generate.setEnabled(true);
                });
            }).start();
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
