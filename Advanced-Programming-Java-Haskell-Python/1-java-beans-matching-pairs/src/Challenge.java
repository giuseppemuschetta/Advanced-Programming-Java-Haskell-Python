/**
 * Matching Pairs Game - Challenge Bean (best score)
 * Version: 2.0
 * Author: Giuseppe Muschetta
 */
import javax.swing.JLabel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;

/**
 * Optional extension 2: a JavaBean that extends {@link JLabel} and displays the best score seen
 * since the start of the game, i.e. the smallest number of moves needed to complete a round.
 * <p>
 * It interacts only through events: it follows the {@code flipCount} property of the
 * {@link Counter} (the moves of the current round) and, when the {@code gameFinished} property of
 * the {@link Controller} becomes {@code true}, compares those moves with the best score.
 * Bound property: {@code bestScore} (int, 0 until a round is completed).
 */
public class Challenge extends JLabel implements Serializable, PropertyChangeListener {

    private static final long serialVersionUID = 1L;

    // Smallest number of moves needed to complete a round; 0 until the first round is completed.
    private int bestScore;
    // Moves of the current round, as published by the Counter.
    private int currentMoves;

    /** Creates a Challenge label with no best score yet. */
    public Challenge() {
        setHorizontalAlignment(CENTER);
        updateLabel();
    }

    /**
     * Returns the best score.
     * @return the smallest number of moves needed to complete a round, or 0 if no round was completed.
     */
    public int getBestScore() {
        return bestScore;
    }

    private void setBestScore(int newValue) {
        int oldValue = bestScore;
        bestScore = newValue;
        updateLabel();
        firePropertyChange("bestScore", oldValue, newValue);
    }

    private void updateLabel() {
        setText(bestScore == 0 ? "Best score: –" : "Best score: " + bestScore + " moves");
    }

    /**
     * Follows the moves of the current round and updates the best score at the end of each round.
     * @param evt a {@code flipCount} change of the Counter or a {@code gameFinished} change of the Controller.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("flipCount".equals(evt.getPropertyName())) {
            currentMoves = (Integer) evt.getNewValue();
        } else if ("gameFinished".equals(evt.getPropertyName()) && Boolean.TRUE.equals(evt.getNewValue())) {
            if (bestScore == 0 || currentMoves < bestScore) {
                setBestScore(currentMoves);
            }
        }
    }
}
