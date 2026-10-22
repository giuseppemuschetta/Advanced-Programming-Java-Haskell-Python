/**
 * Matching Pairs Game - Matched Listener Interface
 * Version: 2.0
 * Author: Giuseppe Muschetta
 */
import java.util.EventListener;

/**
 * Listener of {@link MatchedEvent}s, published by the {@link Controller}.
 * Implemented by {@link Card}: a face-up card becomes excluded on a match and turns
 * face down otherwise; all the other cards ignore the event.
 */
public interface MatchedListener extends EventListener {
    /**
     * Invoked when the Controller has compared the two cards revealed in a turn.
     * @param evt the event carrying the outcome of the comparison.
     */
    void matchResult(MatchedEvent evt);
}
