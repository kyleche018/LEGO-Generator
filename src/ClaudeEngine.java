import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI design engine: sends the request and the catalog to the Claude API
 * and parses the reply into a parts list. Used automatically when the
 * ANTHROPIC_API_KEY environment variable is set; any failure falls back
 * to the OfflineEngine so the app never dead-ends.
 *
 * The model is asked to reply with ONLY lines of the form:
 *     3001-red x24
 * which this class parses with a regex — simple and robust.
 */
public class ClaudeEngine implements DesignEngine {
    private final LegoCatalog catalog;
    private final OfflineEngine fallback;

    public ClaudeEngine(LegoCatalog catalog) {
        this.catalog = catalog;
        this.fallback = new OfflineEngine(catalog);
    }

    public static boolean available() {
        String key = System.getenv("ANTHROPIC_API_KEY");
        return key != null && !key.isBlank();
    }

    @Override
    public Design generate(String prompt, int maxPieces, double maxPrice) {
        try {
            String catalogText = buildCatalogText();
            String userMessage = String.format(Locale.US,
                    "A user wants to build this out of LEGO: \"%s\".%n"
                    + "Design a buildable model using ONLY pieces from this catalog.%n"
                    + "Hard limits: at most %d pieces total and at most $%.2f estimated total.%n"
                    + "Reply with ONLY one line per piece, exactly like: 3001-red x24%n"
                    + "No explanations, no other text.%n%nCATALOG (elementId, name, color, price):%n%s",
                    prompt, maxPieces, maxPrice, catalogText);

            String body = "{\"model\":\"claude-sonnet-4-5\",\"max_tokens\":1500,"
                    + "\"messages\":[{\"role\":\"user\",\"content\":\""
                    + escapeJson(userMessage) + "\"}]}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.anthropic.com/v1/messages"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", System.getenv("ANTHROPIC_API_KEY"))
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("API status " + response.statusCode());
            }

            Design design = new Design(catalog);
            design.setTitle("AI design: " + prompt);
            Pattern linePattern = Pattern.compile("([0-9]{3,6}-[a-z]+)\\s*x\\s*(\\d+)");
            Matcher m = linePattern.matcher(response.body());
            int found = 0;
            while (m.find()) {
                if (catalog.byId(m.group(1)) != null) {
                    design.add(m.group(1), Integer.parseInt(m.group(2)));
                    found++;
                }
            }
            if (found == 0) throw new RuntimeException("No parseable pieces in AI reply");
            design.setEngineNote("Claude AI engine (API)");
            design.addStep("Sort the pieces by color before you start.");
            design.addStep("Build from the base upward in layers, checking the design as you go.");
            design.trimTo(maxPieces, maxPrice);
            return design;
        } catch (Exception e) {
            Design design = fallback.generate(prompt, maxPieces, maxPrice);
            design.setEngineNote("Offline archetype engine (Claude call failed: " + e.getMessage() + ")");
            return design;
        }
    }

    private String buildCatalogText() {
        StringBuilder sb = new StringBuilder();
        for (Piece p : catalog.all()) {
            sb.append(String.format(Locale.US, "%s, %s, %s, %.2f%n",
                    p.elementId, p.name, p.color, p.priceUSD));
        }
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
