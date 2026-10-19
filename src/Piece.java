/** One LEGO element in the catalog: a specific part in a specific color. */
public class Piece {
    public final String elementId;   // e.g. "3001-red"
    public final String name;        // e.g. "Brick 2 x 4"
    public final String category;    // Brick, Plate, Slope, Technic, ...
    public final String color;       // e.g. "Red"
    public final double priceUSD;    // estimated per-piece price

    public Piece(String elementId, String name, String category, String color, double priceUSD) {
        this.elementId = elementId;
        this.name = name;
        this.category = category;
        this.color = color;
        this.priceUSD = priceUSD;
    }

    @Override
    public String toString() {
        return name + " (" + color + ")";
    }
}
