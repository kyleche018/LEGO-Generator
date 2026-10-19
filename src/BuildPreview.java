import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.JPanel;

/**
 * Preview panel: a flat, brick-style sketch of what the generated build
 * looks like. The silhouette follows the matched archetype (tower,
 * lighthouse, castle, ...) and is colored with the design's own most-used
 * brick colors, so a color request ("red race car") shows up here too.
 *
 * It is an artist's-impression sketch drawn from the parts palette —
 * not a 3D render and not building instructions.
 */
public class BuildPreview extends JPanel {
    private final LegoCatalog catalog;
    private String archetype = "";
    private String captionTitle = "";
    private List<Color> palette = List.of();
    private double reveal = 1.0;

    /** The AWT color for a catalog color name (used for parts callout chips). */
    public static Color colorFor(String colorName) {
        return LEGO_COLORS.getOrDefault(colorName, Color.GRAY);
    }

    /**
     * How much of the model is "built" (0..1, bottom-up). The instruction
     * booklet lowers this per step so pages show the model in progress.
     */
    public void setReveal(double r) {
        this.reveal = Math.max(0.0, Math.min(1.0, r));
        repaint();
    }

    private static final Map<String, Color> LEGO_COLORS = Map.ofEntries(
            Map.entry("Red", new Color(0xC4, 0x11, 0x11)),
            Map.entry("Blue", new Color(0x00, 0x55, 0xBF)),
            Map.entry("Yellow", new Color(0xF2, 0xCD, 0x37)),
            Map.entry("Green", new Color(0x23, 0x7D, 0x29)),
            Map.entry("Black", new Color(0x2B, 0x2B, 0x2B)),
            Map.entry("White", new Color(0xF4, 0xF4, 0xF4)),
            Map.entry("Orange", new Color(0xFE, 0x8A, 0x18)),
            Map.entry("Tan", new Color(0xD7, 0xC5, 0x99)),
            Map.entry("Light Gray", new Color(0x9B, 0xA1, 0x9D)),
            Map.entry("Dark Gray", new Color(0x6B, 0x6E, 0x68)),
            Map.entry("Brown", new Color(0x58, 0x2A, 0x12)),
            Map.entry("Transparent", new Color(0xA4, 0xEE, 0xF5)));

    public BuildPreview(LegoCatalog catalog) {
        this.catalog = catalog;
        setPreferredSize(new Dimension(320, 0));
        setBackground(new Color(0xFD, 0xF6, 0xE3));
    }

    /** Point the panel at a finished design. */
    public void showDesign(String prompt, Design design) {
        String p = (prompt + " " + design.title()).toLowerCase(Locale.ROOT);
        archetype = detectArchetype(p);
        captionTitle = design.title();

        // Palette: the design's colors, most-used first.
        Map<String, Integer> byColor = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : design.quantities().entrySet()) {
            Piece piece = catalog.byId(e.getKey());
            if (piece != null) byColor.merge(piece.color, e.getValue(), Integer::sum);
        }
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(byColor.entrySet());
        sorted.sort(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()));
        List<Color> colors = new ArrayList<>();
        for (Map.Entry<String, Integer> e : sorted) {
            Color c = LEGO_COLORS.get(e.getKey());
            if (c != null && !colors.contains(c)) colors.add(c);
            if (colors.size() >= 5) break;
        }
        palette = colors.isEmpty() ? List.of(Color.GRAY) : colors;
        repaint();
    }

    private static String detectArchetype(String p) {
        if (p.contains("eiffel") || p.contains("tower") || p.contains("skyscraper") || p.contains("building")) return "tower";
        if (p.contains("lighthouse")) return "lighthouse";
        if (p.contains("castle") || p.contains("fortress") || p.contains("medieval")) return "castle";
        if (p.contains("house") || p.contains("cottage") || p.contains("cabin") || p.contains("home")) return "house";
        if (p.contains("car") || p.contains("race") || p.contains("f1") || p.contains("formula") || p.contains("vehicle") || p.contains("truck")) return "car";
        if (p.contains("bridge")) return "bridge";
        if (p.contains("pyramid") || p.contains("egypt")) return "pyramid";
        if (p.contains("rocket") || p.contains("spaceship") || p.contains("space ship") || p.contains("shuttle")) return "rocket";
        if (p.contains("boat") || p.contains("ship") || p.contains("sailboat") || p.contains("pirate")) return "boat";
        if (p.contains("train") || p.contains("locomotive")) return "train";
        return "generic";
    }

    private Color pick(int i) { return palette.get(Math.abs(i) % palette.size()); }

    private Color named(String colorName, Color fallback) {
        return LEGO_COLORS.getOrDefault(colorName, fallback);
    }

    /** One LEGO brick: filled rounded rect, darker edge, studs on top. */
    private void brick(Graphics2D g, int x, int y, int w, int h, Color c) {
        g.setColor(c);
        g.fillRoundRect(x, y, w, h, 4, 4);
        g.setColor(c.darker());
        g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(x, y, w, h, 4, 4);
        g.setColor(c.brighter());
        int studs = Math.max(1, w / 26);
        for (int i = 0; i < studs; i++) {
            int sx = x + 6 + i * 26;
            if (sx + 12 <= x + w) g.fillOval(sx, y + 2, 12, 5);
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int W = getWidth(), H = getHeight();

        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("PREVIEW", 14, 24);

        if (archetype.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.drawString("Your build preview appears here", 14, 60);
            g.drawString("after you hit BUILD IT!", 14, 80);
            return;
        }

        int ground = H - 64;
        // Ground / baseplate strip
        g.setColor(named("Green", Color.GRAY));
        if (archetype.equals("lighthouse") || archetype.equals("boat") || archetype.equals("bridge")) {
            g.setColor(named("Blue", Color.GRAY));
        }
        if (archetype.equals("pyramid")) g.setColor(named("Tan", Color.GRAY));
        g.fillRect(14, ground, W - 28, 14);
        g.setColor(Color.DARK_GRAY);
        g.drawRect(14, ground, W - 28, 14);

        int cx = W / 2;
        switch (archetype) {
            case "tower" -> drawTower(g, cx, ground);
            case "lighthouse" -> drawLighthouse(g, cx, ground);
            case "castle" -> drawCastle(g, cx, ground, W);
            case "house" -> drawHouse(g, cx, ground);
            case "car" -> drawCar(g, cx, ground, W);
            case "bridge" -> drawBridge(g, ground, W);
            case "pyramid" -> drawPyramid(g, cx, ground);
            case "rocket" -> drawRocket(g, cx, ground);
            case "boat" -> drawBoat(g, cx, ground);
            case "train" -> drawTrain(g, cx, ground, W);
            default -> drawGeneric(g, cx, ground);
        }

        // Booklet mode: veil the not-yet-built part of the model.
        if (reveal < 1.0) {
            int top = modelTop(ground);
            int builtY = ground - (int) ((ground - top) * reveal);
            g.setColor(new Color(0xFD, 0xF6, 0xE3, 205));
            g.fillRect(0, 28, W, Math.max(0, builtY - 28));
            g.setColor(new Color(0xC4, 0x11, 0x11));
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{7f, 5f}, 0f));
            g.drawLine(10, builtY, W - 10, builtY);
            g.setStroke(new BasicStroke(1f));
        }

        // Honest caption
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("SansSerif", Font.ITALIC, 11));
        g.drawString("Preview sketch — an artist's impression", 14, H - 30);
        g.drawString("from the parts list, not a 3D render.", 14, H - 15);
    }

    /** Top edge (y) of each archetype's drawing, mirroring the draw methods. */
    private int modelTop(int ground) {
        return switch (archetype) {
            case "tower" -> ground - 258;
            case "lighthouse" -> ground - 228;
            case "castle" -> ground - 212;
            case "house" -> ground - 158;
            case "car" -> ground - 92;
            case "bridge" -> ground - 136;
            case "pyramid" -> ground - 156;
            case "rocket" -> ground - 205;
            case "boat" -> ground - 150;
            case "train" -> ground - 108;
            default -> ground - 112;
        };
    }

    private void drawTower(Graphics2D g, int cx, int ground) {
        Color main = pick(0);
        int rowH = 16;
        // Tapering stack of brick rows (the Eiffel silhouette), arched base.
        for (int i = 0; i < 14; i++) {
            int wdt = 190 - i * 11;
            int y = ground - (i + 1) * rowH;
            if (i < 2) { // legs with an arch gap in the middle
                brick(g, cx - wdt / 2, y, wdt / 3, rowH - 2, main);
                brick(g, cx + wdt / 2 - wdt / 3, y, wdt / 3, rowH - 2, main);
                if (i == 1) brick(g, cx - wdt / 2, y, wdt, rowH - 2, main);
            } else {
                brick(g, cx - wdt / 2, y, wdt, rowH - 2, main);
            }
        }
        int topY = ground - 14 * rowH;
        g.setColor(named("Black", Color.DARK_GRAY));
        g.fillRect(cx - 2, topY - 26, 4, 26);               // antenna
        g.fillOval(cx - 5, topY - 34, 9, 9);                // beacon
    }

    private void drawLighthouse(Graphics2D g, int cx, int ground) {
        int wdt = 74, rowH = 17;
        for (int i = 0; i < 10; i++) {
            Color band = (i % 2 == 0) ? named("White", pick(0)) : named("Red", pick(1));
            brick(g, cx - wdt / 2 + i, ground - (i + 1) * rowH, wdt - 2 * i, rowH - 2, band);
        }
        int top = ground - 10 * rowH;
        g.setColor(new Color(0xFF, 0xE7, 0x8A));
        g.fillOval(cx - 17, top - 30, 34, 26);              // lamp room glow
        g.setColor(named("Black", Color.DARK_GRAY));
        g.drawOval(cx - 17, top - 30, 34, 26);
        int[] xs = {cx - 26, cx + 26, cx};
        int[] ys = {top - 30, top - 30, top - 58};
        g.fillPolygon(xs, ys, 3);                            // cap
    }

    private void drawCastle(Graphics2D g, int cx, int ground, int W) {
        Color wall = pick(0);
        int wallW = Math.min(W - 60, 250), wallH = 120;
        brick(g, cx - wallW / 2, ground - wallH, wallW, 26, wall);
        brick(g, cx - wallW / 2, ground - wallH + 28, wallW, 26, wall);
        brick(g, cx - wallW / 2, ground - wallH + 56, wallW, 26, wall);
        brick(g, cx - wallW / 2, ground - 40, wallW, 40, wall);
        // Gate arch
        g.setColor(getBackground());
        g.fillArc(cx - 22, ground - 52, 44, 60, 0, 180);
        g.fillRect(cx - 22, ground - 30, 44, 30);
        // Turrets with cone roofs
        for (int side : new int[]{-1, 1}) {
            int tx = cx + side * (wallW / 2) - (side < 0 ? 34 : 0) - (side < 0 ? 0 : 0);
            int x = (side < 0) ? cx - wallW / 2 - 6 : cx + wallW / 2 - 28;
            brick(g, x, ground - wallH - 52, 34, 52, wall);
            g.setColor(named("Red", pick(1)));
            int[] xs = {x - 4, x + 38, x + 17};
            int[] ys = {ground - wallH - 52, ground - wallH - 52, ground - wallH - 92};
            g.fillPolygon(xs, ys, 3);
        }
    }

    private void drawHouse(Graphics2D g, int cx, int ground) {
        Color wall = pick(0);
        brick(g, cx - 85, ground - 92, 170, 30, wall);
        brick(g, cx - 85, ground - 62, 170, 30, wall);
        brick(g, cx - 85, ground - 32, 170, 32, wall);
        // Roof
        g.setColor(named("Red", pick(1)));
        int[] xs = {cx - 105, cx + 105, cx};
        int[] ys = {ground - 92, ground - 92, ground - 158};
        g.fillPolygon(xs, ys, 3);
        g.setColor(Color.DARK_GRAY);
        g.drawPolygon(xs, ys, 3);
        // Door + windows
        g.setColor(named("Brown", Color.DARK_GRAY));
        g.fillRect(cx - 14, ground - 52, 28, 52);
        g.setColor(new Color(0xA4, 0xEE, 0xF5));
        g.fillRect(cx - 68, ground - 74, 26, 24);
        g.fillRect(cx + 42, ground - 74, 26, 24);
        g.setColor(Color.DARK_GRAY);
        g.drawRect(cx - 68, ground - 74, 26, 24);
        g.drawRect(cx + 42, ground - 74, 26, 24);
    }

    private void drawCar(Graphics2D g, int cx, int ground, int W) {
        Color body = pick(0);
        int carW = Math.min(W - 50, 260);
        int x0 = cx - carW / 2;
        brick(g, x0, ground - 52, carW, 26, body);                       // lower body
        brick(g, x0 + 18, ground - 78, carW - 60, 26, body);             // cabin band
        g.setColor(new Color(0xA4, 0xEE, 0xF5));                          // windshield
        g.fillRect(x0 + carW / 2 - 8, ground - 76, 34, 20);
        brick(g, x0 + carW - 44, ground - 92, 40, 14, body);             // rear wing
        g.setColor(named("Black", Color.DARK_GRAY));
        g.fillRect(x0 + carW - 26, ground - 80, 5, 12);                  // wing strut
        // Wheels
        for (int wx : new int[]{x0 + 34, x0 + carW - 58}) {
            g.setColor(named("Black", Color.DARK_GRAY));
            g.fillOval(wx, ground - 30, 34, 34);
            g.setColor(Color.LIGHT_GRAY);
            g.fillOval(wx + 10, ground - 20, 14, 14);
        }
    }

    private void drawBridge(Graphics2D g, int ground, int W) {
        Color main = pick(0);
        int x0 = 26, x1 = W - 26;
        for (int tx : new int[]{x0 + 34, x1 - 62}) {
            for (int i = 0; i < 8; i++) brick(g, tx, ground - (i + 1) * 17, 30, 15, main);
        }
        brick(g, x0, ground - 24, x1 - x0, 16, pick(1));                 // deck
        g.setColor(named("Black", Color.DARK_GRAY));
        g.setStroke(new BasicStroke(2f));
        int deckY = ground - 24, topY = ground - 8 * 17;
        g.drawLine(x0 + 49, topY, cx2(x0, x1), deckY);                    // cables
        g.drawLine(x1 - 47, topY, cx2(x0, x1), deckY);
        g.drawLine(x0 + 49, topY, x0 + 6, deckY);
        g.drawLine(x1 - 47, topY, x1 - 6, deckY);
    }

    private int cx2(int a, int b) { return (a + b) / 2; }

    private void drawPyramid(Graphics2D g, int cx, int ground) {
        Color sand = named("Tan", pick(0));
        int rows = 8, rowH = 18;
        for (int i = 0; i < rows; i++) {
            int wdt = 230 - i * 27;
            brick(g, cx - wdt / 2, ground - (i + 1) * rowH, wdt, rowH - 2, sand);
        }
        g.setColor(named("Yellow", Color.ORANGE));
        g.fillRect(cx - 7, ground - rows * rowH - 12, 14, 12);          // capstone
    }

    private void drawRocket(Graphics2D g, int cx, int ground) {
        Color body = named("White", pick(0));
        for (int i = 0; i < 9; i++) brick(g, cx - 30, ground - (i + 1) * 17, 60, 15, body);
        int top = ground - 9 * 17;
        g.setColor(named("Red", pick(1)));
        int[] xs = {cx - 30, cx + 30, cx};
        int[] ys = {top, top, top - 52};
        g.fillPolygon(xs, ys, 3);                                        // nose cone
        for (int side : new int[]{-1, 1}) {                              // fins
            int[] fx = {cx + side * 30, cx + side * 62, cx + side * 30};
            int[] fy = {ground - 44, ground, ground};
            g.fillPolygon(fx, fy, 3);
        }
        g.setColor(new Color(0xA4, 0xEE, 0xF5));                          // window
        g.fillOval(cx - 11, top + 26, 22, 22);
        g.setColor(Color.DARK_GRAY);
        g.drawOval(cx - 11, top + 26, 22, 22);
        g.setColor(new Color(0xFF, 0xB3, 0x47));                          // flame
        int[] flameX = {cx - 12, cx + 12, cx};
        int[] flameY = {ground, ground, ground + 18};
        g.fillPolygon(flameX, flameY, 3);
    }

    private void drawBoat(Graphics2D g, int cx, int ground) {
        Color hull = pick(0);
        int[] hx = {cx - 105, cx + 105, cx + 74, cx - 74};
        int[] hy = {ground - 44, ground - 44, ground, ground};
        g.setColor(hull);
        g.fillPolygon(hx, hy, 4);
        g.setColor(hull.darker());
        g.drawPolygon(hx, hy, 4);
        g.setColor(named("Black", Color.DARK_GRAY));
        g.fillRect(cx - 3, ground - 150, 6, 106);                        // mast
        g.setColor(named("White", Color.WHITE));                         // sail
        int[] sx = {cx + 6, cx + 6, cx + 66};
        int[] sy = {ground - 146, ground - 52, ground - 52};
        g.fillPolygon(sx, sy, 3);
        g.setColor(Color.DARK_GRAY);
        g.drawPolygon(sx, sy, 3);
        g.setColor(named("Red", pick(1)));                               // flag
        g.fillRect(cx + 3, ground - 150, 22, 12);
    }

    private void drawTrain(Graphics2D g, int cx, int ground, int W) {
        Color body = pick(0);
        int trainW = Math.min(W - 50, 260);
        int x0 = cx - trainW / 2;
        brick(g, x0 + 30, ground - 84, trainW - 30, 30, body);           // boiler/cab band
        brick(g, x0, ground - 56, trainW, 28, body);                     // body
        g.setColor(named("Black", Color.DARK_GRAY));
        g.fillRect(x0 + 52, ground - 108, 16, 26);                       // smokestack
        g.setColor(new Color(0xA4, 0xEE, 0xF5));                         // cab window
        g.fillRect(x0 + trainW - 52, ground - 80, 26, 20);
        for (int i = 0; i < 4; i++) {                                    // wheels
            int wx = x0 + 22 + i * 58;
            g.setColor(named("Black", Color.DARK_GRAY));
            g.fillOval(wx, ground - 26, 28, 28);
            g.setColor(Color.LIGHT_GRAY);
            g.fillOval(wx + 8, ground - 18, 12, 12);
        }
    }

    private void drawGeneric(Graphics2D g, int cx, int ground) {
        // Stacked-brick skyline, widths seeded by the title so it is stable.
        int seed = Math.abs(captionTitle.hashCode());
        int x = 34;
        int col = 0;
        while (x < getWidth() - 70) {
            int wdt = 44 + (seed >> (col % 12)) % 3 * 16;
            int rowsHere = 3 + (seed >> (col % 9)) % 5;
            for (int r = 0; r < rowsHere; r++) {
                brick(g, x, ground - (r + 1) * 16, wdt, 14, pick(col + r));
            }
            x += wdt + 8;
            col++;
        }
    }
}
