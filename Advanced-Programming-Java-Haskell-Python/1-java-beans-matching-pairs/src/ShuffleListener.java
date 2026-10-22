/**
 * Matching Pairs Game - Shuffle Listener Interface
 * Version: 2.0
 * Author: Giuseppe Muschetta
 */
import java.util.EventListener;

/**
 * Listener of {@link ShuffleEvent}s, published by the {@link Board}.
 * Implemented by {@link Card}, {@link Controller} and {@link Counter}.
 */
public interface ShuffleListener extends EventListener {
    /**
     * Invoked when the cards are shuffled and a new round starts.
     * @param evt the event carrying the new sequence of card values.
     */
    void shufflePerformed(ShuffleEvent evt);
}
