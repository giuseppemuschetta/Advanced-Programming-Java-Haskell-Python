/**
 * Matching Pairs Game - Counter Bean
 * Version: 2.0
 * Author: Giuseppe Muschetta
 */
import javax.swing.JLabel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;

/**
 * A JavaBean that extends {@link JLabel} and displays the total number of times a card has been
 * turned face up in the current round (the moves). It observes the {@code state} property of every
 * card and resets on every {@link ShuffleEvent}.
 * Bound property: {@code flipCount} (int), observed by the {@link Challenge}.
 */
public class Counter extends JLabel implements Serializable, PropertyChangeListener, ShuffleListener {

    private static final long serialVersionUID = 1L;

    // Number of cards turned face up in the current round (bound property).
    private int flipCount;

    /** Creates a Counter showing 0 moves. */
    public Counter() {
        setHorizontalAlignment(CENTER);
        updateLabel();
    }

    /**
     * Returns the number of cards turned face up in the current round.
     * @return the number of moves.
     */
    public int getFlipCount() {
        return flipCount;
    }

    private void setFlipCount(int newValue) {
        int oldValue = flipCount;
        flipCount = newValue;
        updateLabel();
        firePropertyChange("flipCount", oldValue, newValue);
    }

    private void updateLabel() {
        setText("Moves: " + flipCount);
    }

    /**
     * Counts a move every time a card turns face up.
     * @param evt a change of a card's {@code state}.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("state".equals(evt.getPropertyName()) && evt.getNewValue() == CardState.FACE_UP) {
            setFlipCount(flipCount + 1);
        }
    }

    /**
     * Resets the count when a new round starts.
     * @param evt the shuffle event fired by the Board.
     */
    @Override
    public void shufflePerformed(ShuffleEvent evt) {
        setFlipCount(0);
    }
}
