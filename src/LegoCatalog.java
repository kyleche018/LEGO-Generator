import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the LEGO parts catalog from data/catalog.csv.
 * The catalog is a representative subset of real LEGO elements with
 * estimated per-piece prices (see README).
 */
public class LegoCatalog {
    private final Map<String, Piece> byId = new LinkedHashMap<>();
    private final List<Piece> all = new ArrayList<>();

    public static LegoCatalog load() throws IOException {
        Path[] candidates = {
            Path.of("data/catalog.csv"),
            Path.of("lego-generator-rebuild/data/catalog.csv"),
            Path.of("../data/catalog.csv")
        };
        Path found = null;
        for (Path p : candidates) {
            if (Files.exists(p)) { found = p; break; }
        }
        if (found == null) throw new IOException("catalog.csv not found (looked in data/catalog.csv)");

        LegoCatalog catalog = new LegoCatalog();
        for (String line : Files.readAllLines(found)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("elementId")) continue;
            String[] f = line.split(",", -1);
            Piece piece = new Piece(f[0].trim(), f[1].trim(), f[2].trim(), f[3].trim(),
                    Double.parseDouble(f[4].trim()));
            catalog.byId.put(piece.elementId, piece);
            catalog.all.add(piece);
        }
        return catalog;
    }

    public Piece byId(String elementId) {
        return byId.get(elementId);
    }

    public List<Piece> all() {
        return all;
    }

    /** Find the same part in a different color, or null if the catalog has none. */
    public Piece samePartInColor(Piece piece, String color) {
        for (Piece p : all) {
            if (p.name.equals(piece.name) && p.color.equalsIgnoreCase(color)) return p;
        }
        return null;
    }
}
