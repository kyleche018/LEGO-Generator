/** Turns a plain-language request into a Design that fits the limits. */
public interface DesignEngine {
    Design generate(String prompt, int maxPieces, double maxPrice);
}
