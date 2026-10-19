import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A generated build: a list of catalog pieces with quantities, plus
 * build-step notes. Knows its own totals and can trim itself down to
 * fit a piece-count limit and a price limit.
 */
public class Design {
    private final LegoCatalog catalog;
    private final Map<String, Integer> quantities = new LinkedHashMap<>();
    private final Map<String, Integer> stepOf = new HashMap<>();
    private final List<String> steps = new ArrayList<>();
    private String title = "Custom build";
    private String engineNote = "";

    public Design(LegoCatalog catalog) {
        this.catalog = catalog;
    }

    public void setTitle(String title) { this.title = title; }
    public String title() { return title; }

    /** Snapshot of elementId -> quantity, for rendering helpers (e.g. the preview panel). */
    public Map<String, Integer> quantities() { return Map.copyOf(quantities); }
    public void setEngineNote(String note) { this.engineNote = note; }
    public void addStep(String step) { steps.add(step); }
    public List<String> steps() { return steps; }

    public void add(String elementId, int qty) {
        if (catalog.byId(elementId) == null || qty <= 0) return;
        quantities.merge(elementId, qty, Integer::sum);
    }

    /** Add parts as part of a numbered build step (0-based), for the instruction booklet. */
    public void addInStep(String elementId, int qty, int step) {
        if (catalog.byId(elementId) == null || qty <= 0) return;
        add(elementId, qty);
        stepOf.putIfAbsent(elementId, step);
    }

    /** Which build step a part belongs to, or -1 if the design has no step data. */
    public int stepOf(String elementId) {
        return stepOf.getOrDefault(elementId, -1);
    }

    public int totalPieces() {
        return quantities.values().stream().mapToInt(Integer::intValue).sum();
    }

    public double totalPrice() {
        double total = 0;
        for (Map.Entry<String, Integer> e : quantities.entrySet()) {
            total += catalog.byId(e.getKey()).priceUSD * e.getValue();
        }
        return total;
    }

    /**
     * Shrink the design until it fits both limits.
     * - Over the piece limit: shave the largest stack down by one.
     * - Over the price limit: cut the line with the highest total cost
     *   (qty x unit price) — halve it, or drop it if it is a single piece.
     *   A $15 baseplate inside an $8 budget is therefore the first thing
     *   to go, which is correct: the build survives without it.
     */
    public void trimTo(int maxPieces, double maxPrice) {
        int guard = 0;
        while ((totalPieces() > maxPieces || totalPrice() > maxPrice) && guard++ < 100000) {
            if (quantities.isEmpty()) break;
            if (totalPieces() > maxPieces) {
                String biggestId = null;
                int biggestQty = 0;
                for (Map.Entry<String, Integer> e : quantities.entrySet()) {
                    if (e.getValue() > biggestQty) {
                        biggestQty = e.getValue();
                        biggestId = e.getKey();
                    }
                }
                if (biggestId == null) break;
                if (biggestQty <= 1) quantities.remove(biggestId);
                else quantities.put(biggestId, biggestQty - 1);
            } else {
                String costliestId = null;
                double costliest = -1;
                for (Map.Entry<String, Integer> e : quantities.entrySet()) {
                    double lineCost = catalog.byId(e.getKey()).priceUSD * e.getValue();
                    if (lineCost > costliest) {
                        costliest = lineCost;
                        costliestId = e.getKey();
                    }
                }
                if (costliestId == null) break;
                int qty = quantities.get(costliestId);
                if (qty <= 1) quantities.remove(costliestId);
                else quantities.put(costliestId, qty / 2);
            }
        }
        quantities.values().removeIf(q -> q <= 0);
    }

    /** Human-readable parts list, grouped and sorted, with totals. */
    public String render(int maxPieces, double maxPrice) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(title).append(" ===\n");
        if (!engineNote.isEmpty()) sb.append("Engine: ").append(engineNote).append("\n");
        sb.append("\nPARTS LIST (sourced from the LEGO catalog)\n");

        List<Map.Entry<String, Integer>> lines = new ArrayList<>(quantities.entrySet());
        lines.sort(Comparator
                .comparing((Map.Entry<String, Integer> e) -> catalog.byId(e.getKey()).category)
                .thenComparing(e -> catalog.byId(e.getKey()).name)
                .thenComparing(e -> catalog.byId(e.getKey()).color));
        for (Map.Entry<String, Integer> e : lines) {
            Piece p = catalog.byId(e.getKey());
            sb.append(String.format("  %3dx  %-55s %6.2f%n",
                    e.getValue(), p.toString(), p.priceUSD * e.getValue()));
        }

        sb.append(String.format("%nTOTAL: %d pieces, estimated $%.2f%n", totalPieces(), totalPrice()));
        sb.append(String.format("LIMITS: max %d pieces, max $%.2f -> %s%n",
                maxPieces, maxPrice,
                (totalPieces() <= maxPieces && totalPrice() <= maxPrice) ? "WITHIN LIMITS" : "OVER LIMIT"));

        if (!steps.isEmpty()) {
            sb.append("\nBUILD ORDER\n");
            for (int i = 0; i < steps.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(steps.get(i)).append("\n");
            }
        }
        sb.append("\n(Prices are per-piece estimates for planning, not store prices.)\n");
        return sb.toString();
    }
}
