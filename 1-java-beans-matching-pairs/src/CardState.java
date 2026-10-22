/**
 * Matching Pairs Game - Card State Definition
 * Version: 2.0
 * Author: Giuseppe Muschetta
 */
import java.awt.Color;

/**
 * The three states a {@link Card} can be in, each with the color used to paint it.
 * As the specification requires, a face-down card is green, an excluded card is red,
 * and a face-up card shows its value.
 */
public enum CardState {
    /** The value is hidden. Painted green. */
    FACE_DOWN(new Color(46, 125, 50)),

    /** The value is visible. Painted white, with the value in dark text. */
    FACE_UP(Color.WHITE),

    /** The card belongs to a pair already found and is out of play. Painted red. */
    EXCLUDED(new Color(198, 40, 40));

    // The color associated with this card state.
    private final Color color;

    CardState(Color color) {
        this.color = color;
    }

    /**
     * Returns the color used to paint a card in this state.
     * @return the background color of the card.
     */
    public Color getColor() {
        return color;
    }
}
