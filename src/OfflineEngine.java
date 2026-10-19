import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Offline design engine: matches the request against a library of build
 * archetypes (tower, lighthouse, castle, ...), each with a bill of
 * materials drawn from the catalog. The design is then scaled and
 * trimmed until it fits the user's piece and price limits.
 *
 * Every archetype's parts are grouped by build step, in the same order
 * as its step captions, so the instruction booklet can show a per-step
 * parts callout that always agrees with the final parts list — even
 * after trimming, because each catalog line belongs to exactly one step
 * and trimming only shrinks or drops whole lines.
 *
 * If a color is named in the request ("red race car"), bricks, plates
 * and slopes are swapped to that color wherever the catalog has them.
 *
 * This engine needs no internet and no API key, so the app always works.
 * When an Anthropic API key is present, ClaudeEngine is used instead.
 */
public class OfflineEngine implements DesignEngine {

    private record Line(String elementId, int qty) {}
    private record Archetype(String[] keywords, String title, List<List<Line>> stepLines, String[] steps) {}

    private final LegoCatalog catalog;

    private static final Map<String, String> COLOR_WORDS = Map.ofEntries(
            Map.entry("red", "Red"), Map.entry("blue", "Blue"), Map.entry("yellow", "Yellow"),
            Map.entry("green", "Green"), Map.entry("black", "Black"), Map.entry("white", "White"),
            Map.entry("orange", "Orange"), Map.entry("tan", "Tan"),
            Map.entry("gray", "Light Gray"), Map.entry("grey", "Light Gray"),
            Map.entry("clear", "Transparent"), Map.entry("transparent", "Transparent"));

    private final List<Archetype> archetypes = new ArrayList<>();

    public OfflineEngine(LegoCatalog catalog) {
        this.catalog = catalog;
        defineArchetypes();
    }

    private void defineArchetypes() {
        archetypes.add(new Archetype(
                new String[]{"eiffel", "tower", "skyscraper", "building"},
                "Tower / Landmark",
                List.of(
                        List.of(new Line("914108-gray", 1), new Line("3070-gray", 20)),
                        List.of(new Line("32000-gray", 16), new Line("2780-black", 24), new Line("3308-gray", 4)),
                        List.of(new Line("3020-gray", 12), new Line("3028-gray", 2)),
                        List.of(new Line("3001-gray", 40), new Line("3004-gray", 60),
                                new Line("3003-gray", 24), new Line("3039-gray", 16)),
                        List.of(new Line("3941-gray", 4)),
                        List.of(new Line("4589-black", 1), new Line("3957-black", 1))),
                new String[]{"Lay the baseplate and the ground-floor tile border.",
                        "Build the four legs as a lattice using the Technic bricks and pins.",
                        "Cap the legs with the first platform of plates.",
                        "Taper the middle section inward with slopes as you rise.",
                        "Add the second platform, then the round-brick crown.",
                        "Finish with the cone and antenna spire."}));

        archetypes.add(new Archetype(
                new String[]{"lighthouse"},
                "Lighthouse",
                List.of(
                        List.of(new Line("914108-blue", 1), new Line("3028-green", 1)),
                        List.of(new Line("3001-white", 30), new Line("3001-red", 20), new Line("3003-white", 16),
                                new Line("3004-white", 20), new Line("3020-white", 8), new Line("3023-white", 10)),
                        List.of(new Line("6141-clear", 4), new Line("3065-clear", 4)),
                        List.of(new Line("3039-black", 12), new Line("4589-black", 1))),
                new String[]{"Set the blue baseplate as the sea and the green plate as the point of land.",
                        "Raise the tower in alternating white and red bands.",
                        "Ring the top with clear pieces for the lamp room.",
                        "Cap with black slopes, a cone, and the round clear lamp."}));

        archetypes.add(new Archetype(
                new String[]{"castle", "fortress", "medieval"},
                "Castle",
                List.of(
                        List.of(new Line("914108-green", 1), new Line("3070-gray", 16)),
                        List.of(new Line("3004-gray", 40), new Line("3308-gray", 6), new Line("3020-gray", 14)),
                        List.of(new Line("3001-gray", 55), new Line("3003-gray", 30)),
                        List.of(new Line("4589-red", 4), new Line("10049-red", 2)),
                        List.of(new Line("60592-tan", 1), new Line("60596-tan", 1))),
                new String[]{"Lay the green baseplate and mark the courtyard with tiles.",
                        "Raise the curtain walls with arches for the gate opening.",
                        "Build the four corner turrets taller than the walls.",
                        "Top the turrets with red cones and flags.",
                        "Fit the door into its frame at the gate."}));

        archetypes.add(new Archetype(
                new String[]{"house", "cottage", "cabin", "home"},
                "House",
                List.of(
                        List.of(new Line("914108-green", 1), new Line("3028-green", 2)),
                        List.of(new Line("3001-tan", 26), new Line("3004-white", 24), new Line("3023-tan", 12)),
                        List.of(new Line("60594-clear", 4), new Line("60592-tan", 1), new Line("60596-tan", 1)),
                        List.of(new Line("3037-red", 10), new Line("3040-red", 12)),
                        List.of(new Line("3070-white", 10), new Line("4589-yellow", 2))),
                new String[]{"Start with the green baseplate and the floor plates.",
                        "Raise the walls, leaving gaps for the windows and door frame.",
                        "Fit the windows and hang the door.",
                        "Build the roof from red slopes, ridge first.",
                        "Add the tile walkway and cone lamps."}));

        archetypes.add(new Archetype(
                new String[]{"car", "race", "f1", "formula", "vehicle", "truck"},
                "Race Car",
                List.of(
                        List.of(new Line("3020-red", 3), new Line("3023-black", 8)),
                        List.of(new Line("98138-black", 4), new Line("50950-black", 4), new Line("4624-black", 2)),
                        List.of(new Line("3001-red", 6), new Line("3003-red", 4), new Line("3004-red", 8),
                                new Line("3040-red", 4), new Line("6141-clear", 2)),
                        List.of(new Line("3069-gray", 4), new Line("3817-tan", 1),
                                new Line("3815-white", 1), new Line("3626-yellow", 1))),
                new String[]{"Build the chassis from stacked plates.",
                        "Press the wheels onto their rims and clip them under the body.",
                        "Shape the nose with red slopes and the cockpit with black plates.",
                        "Add the rear wing tiles and seat the driver minifigure."}));

        archetypes.add(new Archetype(
                new String[]{"bridge"},
                "Bridge",
                List.of(
                        List.of(new Line("914108-blue", 1)),
                        List.of(new Line("3008-gray", 10), new Line("3009-gray", 10), new Line("32000-gray", 12)),
                        List.of(new Line("3028-gray", 4), new Line("3308-gray", 4), new Line("3020-gray", 10)),
                        List.of(new Line("2780-black", 20), new Line("30374-black", 6)),
                        List.of(new Line("3070-gray", 24))),
                new String[]{"Set the blue baseplate as the river.",
                        "Raise two towers of long gray bricks on each bank.",
                        "Span the deck with big plates resting on the arches.",
                        "Run the black bars as suspension cables down to the deck.",
                        "Finish the roadway with gray tiles."}));

        archetypes.add(new Archetype(
                new String[]{"pyramid", "egypt"},
                "Pyramid",
                List.of(
                        List.of(new Line("10700-tan", 1)),
                        List.of(new Line("3001-tan", 50), new Line("3004-tan", 40)),
                        List.of(new Line("3003-tan", 30), new Line("3002-tan", 16), new Line("3021-tan", 20)),
                        List.of(new Line("3023-tan", 24), new Line("3024-tan", 10))),
                new String[]{"Lay the tan baseplate as desert sand.",
                        "Build the widest square layer of bricks.",
                        "Step each new layer inward on all four sides.",
                        "Finish the point with plates and a single 1 x 1 brick."}));

        archetypes.add(new Archetype(
                new String[]{"rocket", "spaceship", "space ship", "shuttle"},
                "Rocket",
                List.of(
                        List.of(new Line("3001-white", 22), new Line("3003-white", 12), new Line("3941-white", 6),
                                new Line("3020-gray", 4), new Line("3028-gray", 1)),
                        List.of(new Line("3065-clear", 4), new Line("6141-clear", 2), new Line("6141-yellow", 3)),
                        List.of(new Line("3039-red", 4), new Line("3040-red", 6)),
                        List.of(new Line("4589-red", 1))),
                new String[]{"Stack the white body tube from bricks and round bricks.",
                        "Add the clear bricks as the cockpit window band.",
                        "Attach the red slope fins around the base.",
                        "Top it with the red cone nose and light studs."}));

        archetypes.add(new Archetype(
                new String[]{"boat", "ship", "sailboat", "pirate"},
                "Boat",
                List.of(
                        List.of(new Line("914108-blue", 1)),
                        List.of(new Line("3001-red", 18), new Line("3003-blue", 10), new Line("3004-white", 16)),
                        List.of(new Line("3020-white", 8), new Line("3023-tan", 14)),
                        List.of(new Line("30374-black", 2), new Line("10049-red", 1), new Line("93348-gray", 1))),
                new String[]{"Set the blue baseplate as open water.",
                        "Build the hull up in red and blue layers, higher at the stern.",
                        "Deck it with white plates and a tan plank strip.",
                        "Raise the mast from black bars and fly the flag."}));

        archetypes.add(new Archetype(
                new String[]{"train", "locomotive"},
                "Train",
                List.of(
                        List.of(new Line("3020-black", 5), new Line("3028-gray", 2)),
                        List.of(new Line("98138-black", 6), new Line("50950-black", 6)),
                        List.of(new Line("3001-black", 10), new Line("3003-red", 8),
                                new Line("3004-red", 12), new Line("4589-black", 1)),
                        List.of(new Line("3070-gray", 12), new Line("6141-yellow", 2), new Line("3817-tan", 1),
                                new Line("3815-white", 1), new Line("3626-yellow", 1))),
                new String[]{"Build the long black chassis plates.",
                        "Fit six wheels under the frame.",
                        "Raise the red boiler body and black cone smokestack.",
                        "Tile the track bed and seat the engineer."}));
    }

    @Override
    public Design generate(String prompt, int maxPieces, double maxPrice) {
        String p = prompt.toLowerCase(Locale.ROOT);
        Archetype match = null;
        for (Archetype a : archetypes) {
            for (String kw : a.keywords()) {
                if (p.contains(kw)) { match = a; break; }
            }
            if (match != null) break;
        }

        String requestedColor = null;
        for (Map.Entry<String, String> e : COLOR_WORDS.entrySet()) {
            if (p.contains(e.getKey())) { requestedColor = e.getValue(); break; }
        }

        Design design = new Design(catalog);
        if (match == null) {
            design.setTitle("Custom build: " + prompt);
            // Generic structure in the requested color (default light gray), in two steps.
            String c = (requestedColor != null) ? requestedColor : "Light Gray";
            addSwapped(design, "914108-gray", 1, null, 0);
            addSwapped(design, "3070-gray", 12, null, 0);
            addSwapped(design, "3001-gray", 28, c, 1);
            addSwapped(design, "3004-gray", 30, c, 1);
            addSwapped(design, "3003-gray", 16, c, 1);
            addSwapped(design, "3020-gray", 10, c, 1);
            addSwapped(design, "3039-gray", 10, c, 1);
            design.addStep("No exact archetype matched, so the base comes first: baseplate and floor tiles.");
            design.addStep("Raise the walls from bricks and cap them with slopes.");
        } else {
            design.setTitle(match.title() + ": " + prompt);
            for (int step = 0; step < match.stepLines().size(); step++) {
                for (Line line : match.stepLines().get(step)) {
                    addSwapped(design, line.elementId(), line.qty(), requestedColor, step);
                }
            }
            for (String s : match.steps()) design.addStep(s);
        }
        design.setEngineNote("Offline archetype engine (no API key needed)");
        design.trimTo(maxPieces, maxPrice);
        return design;
    }

    /** Add a line in a build step, swapping colorable parts (Brick/Plate/Slope) to the requested color when possible. */
    private void addSwapped(Design design, String elementId, int qty, String requestedColor, int step) {
        Piece piece = catalog.byId(elementId);
        if (piece == null) return;
        if (requestedColor != null
                && (piece.category.equals("Brick") || piece.category.equals("Plate") || piece.category.equals("Slope"))
                && !piece.color.equalsIgnoreCase(requestedColor)) {
            Piece swapped = catalog.samePartInColor(piece, requestedColor);
            if (swapped != null) piece = swapped;
        }
        design.addInStep(piece.elementId, qty, step);
    }
}
