/**
 * Matching Pairs Game - Shuffle Event Definition
 * Version: 2.0
 * Author: Giuseppe Muschetta
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

/**
 * Event fired by the {@link Board} at the start of the game and every time the Shuffle button
 * is clicked. It wraps the new sequence of card values: 2 x N integers in random order, made of
 * N distinct values each repeated twice. Every {@link Card} takes from the sequence the value at
 * its own index; the {@link Controller} and the {@link Counter} reset themselves.
 */
public class ShuffleEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    // The new card values, one per card, in board order.
    private final ArrayList<Integer> values;

    /**
     * Creates a shuffle event.
     * @param source the object that fired the event (the Board).
     * @param values the new card values; a copy is stored.
     */
    public ShuffleEvent(Object source, List<Integer> values) {
        super(source);
        this.values = new ArrayList<>(values);
    }

    /**
     * Returns the new card values, in board order.
     * @return an unmodifiable list with one value per card.
     */
    public List<Integer> getValues() {
        return Collections.unmodifiableList(values);
    }
}
