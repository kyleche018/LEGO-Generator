# LEGO Generator

Type what you want to build, set a **max piece count** and a **max price**, and the
generator designs a model for you and sources the pieces from a LEGO catalog —
a parts list with quantities, an estimated total price, and a build order.

This is a from-scratch rebuild (October 2026) of my original Tourist-Attraction-to-LEGO
Generator after the original files were lost. Same concept, new code, written to be
explainable line by line.

## Features

- **Prompt-driven design** — type "Eiffel Tower", "red race car", "lighthouse", ...
- **Real catalog sourcing** — every part in a design comes from `data/catalog.csv`,
  a catalog of real LEGO elements (part number + color + estimated price)
- **Hard limits** — designs are scaled and trimmed until they fit BOTH the piece
  limit and the price limit; if a $15 baseplate busts an $8 budget, it goes first
- **Color requests** — name a color ("blue castle") and bricks/plates/slopes are
  swapped to that color wherever the catalog stocks them
- **Two engines**
  - *Offline archetype engine* (default): matches the request against a library of
    build archetypes (tower, lighthouse, castle, house, race car, bridge, pyramid,
    rocket, boat, train). No internet or API key needed.
  - *Claude AI engine*: if the `ANTHROPIC_API_KEY` environment variable is set, the
    request and catalog are sent to the Claude API, which composes a custom parts
    list. Any API failure falls back to the offline engine automatically.
- **LEGO-styled UI** — brick-red/yellow/blue theme with chunky type. Drop a LEGO-style
  font file at `fonts/lego.ttf` and the title picks it up automatically.
- **Preview panel** — every build also draws a flat brick-style sketch of the model
  in the UI: the silhouette follows the matched archetype and is colored with the
  design's own most-used brick colors (so "blue castle" previews blue). It's an
  artist's-impression sketch from the parts palette, not a 3D render.
- **Instruction booklet** — the INSTRUCTIONS button opens a step-by-step booklet,
  one page per build step: the sketch reveals the model bottom-up as you advance,
  each page captions the step and lists a parts callout (quantities + color chips)
  for just what that step adds. Callouts are grouped from the final parts list, so
  they always sum to the design totals, even after budget trimming. The booklet
  exports to PNG pages + a printable index.html.

## Run it

Requires a JDK (17+).

```bash
# build
javac -d out src/*.java

# GUI
java -cp out Main

# terminal mode (prompt, max pieces, max price)
java -cp out Main --cli "Eiffel Tower" 300 75.00
java -cp out Main --cli "red race car" 150 25.00
java -cp out Main --cli "lighthouse" 60 8.00
```

Or use `./build.sh` then `./run.sh`.

## Project layout

| File | What it does |
|---|---|
| `src/Main.java` | Entry point: GUI, or `--cli` terminal mode |
| `src/LegoUI.java` | Swing interface (LEGO theme, prompt + limits form, results) |
| `src/DesignEngine.java` | Engine interface |
| `src/OfflineEngine.java` | Archetype library, color swapping, generic fallback |
| `src/ClaudeEngine.java` | Claude API engine (parses `elementId xQTY` lines) |
| `src/Design.java` | A design: parts + quantities, totals, limit trimming |
| `src/LegoCatalog.java` / `src/Piece.java` | Catalog loading and the part model |
| `data/catalog.csv` | ~130 real LEGO elements with estimated per-piece prices |

## Honest notes

- Prices are **estimates for planning**, not live store prices.
- The catalog is a representative subset of the real LEGO catalog, not all of it.
- The offline engine composes from archetypes; the Claude engine is the one that
  free-forms a design from any prompt.
