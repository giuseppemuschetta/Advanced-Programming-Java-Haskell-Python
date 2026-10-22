/**
 * Matching Pairs Game - Matched Event Definition
 * Version: 2.0
 * Author: Giuseppe Muschetta
 */
import java.util.EventObject;

/**
 * Event fired by the {@link Controller} half a second after the second card of a turn has been
 * turned face up. Its value is {@code true} if the two revealed cards have the same value.
 */
public class MatchedEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    // True if the two revealed cards form a pair.
    private final boolean match;

    /**
     * Creates a matched event.
     * @param source the object that fired the event (the Controller).
     * @param match  {@code true} if the two revealed cards have the same value.
     */
    public MatchedEvent(Object source, boolean match) {
        super(source);
        this.match = match;
    }

    /**
     * Returns the outcome of the turn.
     * @return {@code true} if the two revealed cards matched.
     */
    public boolean isMatch() {
        return match;
    }
}
