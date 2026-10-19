import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * The instruction booklet: one page per build step, LEGO-style.
 * Each page shows the model as far as it is built at that step
 * (bottom-up reveal on the preview sketch), the step's instruction
 * text, and a parts callout — the pieces that step adds, with
 * quantities and color chips.
 *
 * The callouts are grouped from the design's final parts list using
 * the step each catalog line was assigned to, so they always agree
 * with the totals — even after the design was trimmed to fit limits.
 * Pages whose parts were all trimmed away are dropped.
 *
 * The booklet can also be exported: one PNG per page plus an
 * index.html that lays them out as a printable book.
 */
public class InstructionWindow {

    /** One booklet page: caption + the parts that step adds (piece, qty). */
    public record Page(String caption, List<Map.Entry<Piece, Integer>> parts) {}

    private final LegoCatalog catalog;
    private final String prompt;
    private final Design design;
    private final List<Page> pages;
    private int index = 0;

    private JFrame frame;
    private JLabel header;
    private JTextArea captionArea;
    private JPanel callout;
    private BuildPreview preview;
    private JButton prevBtn;
    private JButton nextBtn;

    public InstructionWindow(LegoCatalog catalog, String prompt, Design design) {
        this.catalog = catalog;
        this.prompt = prompt;
        this.design = design;
        this.pages = pagesFor(catalog, design);
    }

    /** Group a design's final parts list into booklet pages. */
    public static List<Page> pagesFor(LegoCatalog catalog, Design design) {
        Map<Integer, List<Map.Entry<Piece, Integer>>> byStep = new TreeMap<>();
        for (Map.Entry<String, Integer> e : design.quantities().entrySet()) {
            Piece piece = catalog.byId(e.getKey());
            if (piece == null) continue;
            int step = design.stepOf(e.getKey());
            byStep.computeIfAbsent(step, k -> new ArrayList<>())
                    .add(Map.entry(piece, e.getValue()));
        }
        List<Page> pages = new ArrayList<>();
        List<String> captions = design.steps();
        if (byStep.size() == 1 && byStep.containsKey(-1)) {
            // No step data (e.g. Claude engine): one page with everything.
            pages.add(new Page("Complete build — all parts", byStep.get(-1)));
            return pages;
        }
        for (Map.Entry<Integer, List<Map.Entry<Piece, Integer>>> e : byStep.entrySet()) {
            int step = e.getKey();
            if (step < 0) step = 0;
            String caption = (step < captions.size()) ? captions.get(step)
                    : "Step " + (step + 1);
            List<Map.Entry<Piece, Integer>> parts = e.getValue();
            parts.sort((a, b) -> a.getKey().toString().compareTo(b.getKey().toString()));
            pages.add(new Page(caption, parts));
        }
        return pages;
    }

    public void show() {
        frame = new JFrame("Instruction Booklet — " + design.title());
        frame.setSize(820, 660);
        frame.setLayout(new BorderLayout());

        header = new JLabel();
        header.setFont(new Font("SansSerif", Font.BOLD, 17));
        header.setBorder(BorderFactory.createEmptyBorder(10, 14, 2, 14));
        frame.add(header, BorderLayout.NORTH);

        preview = new BuildPreview(catalog);
        preview.setPreferredSize(new Dimension(430, 470));
        preview.showDesign(prompt, design);
        JPanel previewWrap = new JPanel(new FlowLayout(FlowLayout.CENTER));
        previewWrap.add(preview);
        frame.add(previewWrap, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout());
        right.setPreferredSize(new Dimension(330, 0));
        captionArea = new JTextArea();
        captionArea.setEditable(false);
        captionArea.setLineWrap(true);
        captionArea.setWrapStyleWord(true);
        captionArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        captionArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));
        right.add(captionArea, BorderLayout.NORTH);

        callout = new JPanel();
        callout.setLayout(new BoxLayout(callout, BoxLayout.Y_AXIS));
        callout.setBorder(BorderFactory.createEmptyBorder(4, 10, 10, 10));
        JScrollPane calloutScroll = new JScrollPane(callout);
        calloutScroll.setBorder(BorderFactory.createTitledBorder("Parts for this step"));
        right.add(calloutScroll, BorderLayout.CENTER);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        prevBtn = new JButton("◀ Prev");
        nextBtn = new JButton("Next ▶");
        JButton exportBtn = new JButton("Export booklet…");
        prevBtn.addActionListener(ev -> { if (index > 0) { index--; refresh(); } });
        nextBtn.addActionListener(ev -> { if (index < pages.size() - 1) { index++; refresh(); } });
        exportBtn.addActionListener(ev -> exportWithChooser());
        nav.add(prevBtn);
        nav.add(nextBtn);
        nav.add(exportBtn);
        right.add(nav, BorderLayout.SOUTH);

        frame.add(right, BorderLayout.EAST);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        refresh();
    }

    private void refresh() {
        Page page = pages.get(index);
        header.setText("Step " + (index + 1) + " of " + pages.size());
        captionArea.setText(page.caption());
        preview.setReveal((index + 1) / (double) pages.size());
        callout.removeAll();
        for (Map.Entry<Piece, Integer> e : page.parts()) {
            Piece p = e.getKey();
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
            JPanel chip = new JPanel();
            chip.setPreferredSize(new Dimension(16, 16));
            chip.setBackground(BuildPreview.colorFor(p.color));
            chip.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            row.add(chip);
            row.add(new JLabel(e.getValue() + "×  " + p.name + " (" + p.color + ")"));
            callout.add(row);
        }
        callout.revalidate();
        callout.repaint();
        prevBtn.setEnabled(index > 0);
        nextBtn.setEnabled(index < pages.size() - 1);
    }

    private void exportWithChooser() {
        JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.home")));
        chooser.setDialogTitle("Export booklet into this folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        File dir = new File(chooser.getSelectedFile(), "LEGO-Booklet");
        try {
            exportBooklet(catalog, prompt, design, dir);
            JOptionPane.showMessageDialog(frame,
                    "Booklet exported to:\n" + dir.getAbsolutePath()
                            + "\nOpen index.html in a browser to read or print it.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Export failed: " + ex.getMessage());
        }
    }

    /**
     * Write the booklet as page PNGs + a printable index.html.
     * Static so it can be tested headlessly.
     */
    public static void exportBooklet(LegoCatalog catalog, String prompt, Design design, File dir)
            throws IOException {
        List<Page> pages = pagesFor(catalog, design);
        dir.mkdirs();
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>")
                .append(escape(design.title())).append(" — Instruction Booklet</title>")
                .append("<style>body{font-family:Georgia,serif;background:#fdf6e3;color:#222;"
                        + "max-width:760px;margin:2em auto;padding:0 1em}"
                        + ".page{border:2px solid #c41111;border-radius:10px;padding:1em 1.4em;"
                        + "margin-bottom:2em;page-break-inside:avoid;background:#fffdf4}"
                        + "h1{color:#c41111}h2{margin:.2em 0}.cap{font-size:1.1em}"
                        + "img{width:340px;display:block;margin:.6em auto}"
                        + "li{margin:.15em 0}.chip{display:inline-block;width:.8em;height:.8em;"
                        + "border:1px solid #444;margin-right:.4em}</style></head><body>")
                .append("<h1>").append(escape(design.title())).append("</h1>")
                .append("<p>").append(design.totalPieces()).append(" pieces · estimated $")
                .append(String.format("%.2f", design.totalPrice()))
                .append(" · ").append(pages.size()).append(" steps</p>")
                .append("<p><i>Preview sketches are artist's impressions drawn from the "
                        + "parts palette, not 3D renders.</i></p>");

        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            BuildPreview p = new BuildPreview(catalog);
            p.showDesign(prompt, design);
            p.setReveal((i + 1) / (double) pages.size());
            p.setSize(430, 470);
            BufferedImage img = new BufferedImage(430, 470, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            p.printAll(g);
            g.dispose();
            String imgName = String.format("step-%02d.png", i + 1);
            ImageIO.write(img, "png", new File(dir, imgName));

            html.append("<div class=\"page\"><h2>Step ").append(i + 1).append(" of ")
                    .append(pages.size()).append("</h2>")
                    .append("<p class=\"cap\">").append(escape(page.caption())).append("</p>")
                    .append("<img src=\"").append(imgName).append("\">")
                    .append("<p><b>Parts for this step:</b></p><ul>");
            for (Map.Entry<Piece, Integer> e : page.parts()) {
                Piece piece = e.getKey();
                html.append("<li><span class=\"chip\" style=\"background:")
                        .append(String.format("#%02x%02x%02x",
                                BuildPreview.colorFor(piece.color).getRed(),
                                BuildPreview.colorFor(piece.color).getGreen(),
                                BuildPreview.colorFor(piece.color).getBlue()))
                        .append("\"></span>").append(e.getValue()).append("× ")
                        .append(escape(piece.name)).append(" (").append(escape(piece.color))
                        .append(")</li>");
            }
            html.append("</ul></div>");
        }
        html.append("</body></html>");
        Files.writeString(new File(dir, "index.html").toPath(), html.toString(), StandardCharsets.UTF_8);
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** For tests: how many pages a design's booklet has. */
    public int pageCount() { return pages.size(); }

    static {
        // Touch SwingUtilities so the class loads cleanly in headless tests too.
        SwingUtilities.class.getName();
    }
}
