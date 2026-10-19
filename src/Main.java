import javax.swing.SwingUtilities;

/**
 * LEGO Generator — type what you want to build, set a piece limit and a
 * price limit, and get a parts list sourced from a LEGO catalog.
 *
 * Usage:
 *   java Main                     -> opens the GUI
 *   java Main --cli "Eiffel Tower" 300 75.00   -> prints a design in the terminal
 *
 * The design engine is Claude AI when ANTHROPIC_API_KEY is set in the
 * environment, and a built-in offline archetype engine otherwise.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        LegoCatalog catalog = LegoCatalog.load();

        if (args.length >= 4 && args[0].equals("--cli")) {
            String prompt = args[1];
            int maxPieces = Integer.parseInt(args[2]);
            double maxPrice = Double.parseDouble(args[3]);
            DesignEngine engine = ClaudeEngine.available()
                    ? new ClaudeEngine(catalog) : new OfflineEngine(catalog);
            Design design = engine.generate(prompt, maxPieces, maxPrice);
            System.out.print(design.render(maxPieces, maxPrice));
            return;
        }

        SwingUtilities.invokeLater(() -> new LegoUI(catalog).show());
    }
}
