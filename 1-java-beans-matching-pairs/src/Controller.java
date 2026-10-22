/**
 * Matching Pairs Game - Controller Bean
 * Version: 2.0
 * Author: Giuseppe Muschetta
 */
import javax.swing.JLabel;
import javax.swing.Timer;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.Serializable;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The logic of the game, implemented as a JavaBean that extends {@link JLabel} and displays the
 * number of pairs found so far.
 * <p>
 * The Controller observes every card twice:
 * <ul>
 *   <li>as a {@link VetoableChangeListener} it enforces the rules, vetoing the state changes that a
 *       move is not allowed to make;</li>
 *   <li>as a {@link PropertyChangeListener} it records the values {@code v1} and {@code v2} of the
 *       two cards turned face up in a turn and, half a second after the second one, fires a
 *       {@link MatchedEvent} whose value says whether they are equal.</li>
 * </ul>
 * It also listens to {@link ShuffleEvent}s, which reset the count and start a new round.
 * Bound properties: {@code pairsFound} (int) and {@code gameFinished} (boolean).
 * <p>
 * Every method runs on the Swing Event Dispatch Thread: the half-second delay is a
 * {@link javax.swing.Timer}, whose action is executed on that thread.
 */
public class Controller extends JLabel implements Serializable,
        PropertyChangeListener, VetoableChangeListener, ShuffleListener {

    private static final long serialVersionUID = 1L;

    /** Delay between the second card of a turn and the matched event, in milliseconds. */
    public static final int MATCH_DELAY_MS = 500;

    // Number of pairs of the game (N in the Board): the round ends when all of them are found.
    private int totalPairs;
    // Number of pairs found in the current round (bound property).
    private int pairsFound;
    // True when every pair of the current round has been found (bound property).
    private boolean gameFinished;

    // Number of cards turned face up in the current turn (0, 1 or 2) and their values.
    private int cardsUp;
    private int v1;
    private int v2;

    // True while the matched event is delivered: the cards may then leave the FACE_UP state.
    private boolean resolving;
    // True from a shuffle until the first card of the new round is revealed: the cards may then
    // turn face down from any state.
    private boolean newRound;

    // Subscribers of the matched event (the cards).
    private final CopyOnWriteArrayList<MatchedListener> matchedListeners = new CopyOnWriteArrayList<>();
    // Single-shot timer that delays the matched event, so that the player can see the second card.
    private final Timer matchTimer;

    /**
     * Creates a Controller for a game of 4 pairs (public no-argument constructor required by the
     * JavaBeans conventions).
     */
    public Controller() {
        this(4);
    }

    /**
     * Creates a Controller for a game with the given number of pairs.
     * @param totalPairs the number of pairs of the game (N in the Board).
     */
    public Controller(int totalPairs) {
        this.totalPairs = totalPairs;
        this.matchTimer = new Timer(MATCH_DELAY_MS, e -> resolveTurn());
        this.matchTimer.setRepeats(false);
        setHorizontalAlignment(CENTER);
        updateLabel();
    }

    // --- Properties ---

    /**
     * Returns the number of pairs of the game.
     * @return the number of pairs to find in a round.
     */
    public int getTotalPairs() {
        return totalPairs;
    }

    /**
     * Sets the number of pairs of the game.
     * @param totalPairs the number of pairs to find in a round.
     */
    public void setTotalPairs(int totalPairs) {
        this.totalPairs = totalPairs;
        updateLabel();
    }

    /**
     * Returns the number of pairs found in the current round.
     * @return the number of matched pairs.
     */
    public int getPairsFound() {
        return pairsFound;
    }

    /**
     * Tells whether every pair of the current round has been found.
     * @return {@code true} if the round is over.
     */
    public boolean isGameFinished() {
        return gameFinished;
    }

    private void setPairsFound(int newValue) {
        int oldValue = pairsFound;
        pairsFound = newValue;
        updateLabel();
        firePropertyChange("pairsFound", oldValue, newValue);
    }

    private void setGameFinished(boolean newValue) {
        boolean oldValue = gameFinished;
        gameFinished = newValue;
        firePropertyChange("gameFinished", oldValue, newValue);
    }

    private void updateLabel() {
        setText("Pairs found: " + pairsFound + " / " + totalPairs);
    }

    // --- The rules: VetoableChangeListener ---

    /**
     * Vetoes the state changes that the rules forbid.
     * <ul>
     *   <li>A card may turn face up only while fewer than two cards are face up in the current turn.</li>
     *   <li>A card may leave the face-up or excluded state only when the Controller itself is the
     *       cause: while it delivers a matched event, or while a new round starts after a shuffle.
     *       Hence a click on a face-up or excluded card, which asks it to turn face down, is always
     *       vetoed, as the specification requires.</li>
     * </ul>
     * The method has no side effects, so the rollback notifications of the vetoable change support
     * are harmless.
     *
     * @param evt the proposed change of a card's {@code state}.
     * @throws PropertyVetoException if the change breaks a rule.
     */
    @Override
    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
        if (!"state".equals(evt.getPropertyName())) {
            return;
        }
        CardState from = (CardState) evt.getOldValue();
        CardState to = (CardState) evt.getNewValue();

        if (to == CardState.FACE_UP) {
            if (from != CardState.FACE_DOWN) {
                throw new PropertyVetoException("Only a face-down card can be turned face up", evt);
            }
            if (cardsUp >= 2) {
                throw new PropertyVetoException("Two cards are already face up", evt);
            }
            return;
        }
        if (resolving || newRound) {
            return; // the Controller itself is turning the card down or excluding it
        }
        throw new PropertyVetoException(from == CardState.EXCLUDED
                ? "The card is excluded from the game"
                : "A face-up card cannot be turned down by clicking it", evt);
    }

    // --- The turn: PropertyChangeListener ---

    /**
     * Records the value of every card turned face up. The first card of a turn sets {@code v1};
     * the second sets {@code v2} and starts the half-second timer that fires the matched event.
     * @param evt a change of a card's {@code state}.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (!"state".equals(evt.getPropertyName()) || evt.getNewValue() != CardState.FACE_UP) {
            return;
        }
        // The value is read from the card that fired the event: a getter of the event source.
        int value = ((Card) evt.getSource()).getValue();
        newRound = false;
        if (cardsUp == 0) {
            v1 = value;
            cardsUp = 1;
        } else {
            v2 = value;
            cardsUp = 2;
            matchTimer.restart();
        }
    }

    /**
     * Ends the current turn, half a second after the second card was revealed: updates the count,
     * fires the matched event (the two face-up cards become excluded or turn face down) and, if the
     * last pair was found, sets {@code gameFinished}.
     */
    private void resolveTurn() {
        boolean match = (v1 == v2);
        if (match) {
            setPairsFound(pairsFound + 1);
        }
        cardsUp = 0;
        resolving = true;
        try {
            fireMatched(new MatchedEvent(this, match));
        } finally {
            resolving = false;
        }
        if (match && pairsFound == totalPairs) {
            setGameFinished(true);
        }
    }

    // --- New round: ShuffleListener ---

    /**
     * Starts a new round: a pending turn is discarded, the count is reset to 0, and the cards are
     * allowed to turn face down whatever their state. The Board notifies the Controller before the
     * cards, so this happens before they react to the same event.
     * @param evt the shuffle event fired by the Board.
     */
    @Override
    public void shufflePerformed(ShuffleEvent evt) {
        matchTimer.stop();
        cardsUp = 0;
        newRound = true;
        setPairsFound(0);
        setGameFinished(false);
    }

    // --- Matched event publication ---

    /**
     * Subscribes a listener to the matched event.
     * @param listener the listener to add (typically a Card).
     */
    public void addMatchedListener(MatchedListener listener) {
        matchedListeners.add(listener);
    }

    /**
     * Unsubscribes a listener from the matched event.
     * @param listener the listener to remove.
     */
    public void removeMatchedListener(MatchedListener listener) {
        matchedListeners.remove(listener);
    }

    private void fireMatched(MatchedEvent event) {
        for (MatchedListener listener : matchedListeners) {
            listener.matchResult(event);
        }
    }
}
