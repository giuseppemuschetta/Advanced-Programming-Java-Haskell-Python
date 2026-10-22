/**
 * Matching Pairs Game - Card Bean
 * Version: 2.0
 * Author: Giuseppe Muschetta
 */
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.beans.PropertyVetoException;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * A card of the Matching Pairs game: a JavaBean that extends {@link JButton}.
 * <p>
 * Properties:
 * <ul>
 *   <li>{@code state} ({@link CardState}): <b>bound</b> and <b>constrained</b>. Before every change
 *       a vetoable change event is fired, so that a listener (the {@link Controller}) can veto it;
 *       after the change a property change event notifies the bound listeners.</li>
 *   <li>{@code value} (int, read-only): the value shown when the card is face up. It is taken
 *       from the {@link ShuffleEvent} fired by the Board, at the position given by {@code index}.</li>
 *   <li>{@code index} (int): the position of the card on the board.</li>
 * </ul>
 * As the specification requires, the state changes only through {@link #setState(CardState)}:
 * after a click, after a matched event and after a shuffle. The bean relies on the bound and
 * constrained property support that {@code JComponent} already provides.
 */
public class Card extends JButton implements Serializable, ShuffleListener, MatchedListener {

    private static final long serialVersionUID = 1L;

    // Text color of a face-up card.
    private static final Color VALUE_COLOR = new Color(31, 27, 22);

    // The position of the card on the board: it selects the card's value in a shuffle sequence.
    private int index;
    // The value displayed when the card is face up. The two cards of a pair have the same value.
    private int value;
    // The current state of the card: bound and constrained property.
    private CardState state = CardState.FACE_DOWN;

    /**
     * Creates a face-down card at index 0. A public no-argument constructor is required by the
     * JavaBeans conventions, so that builder tools can instantiate the bean.
     */
    public Card() {
        this(0);
    }

    /**
     * Creates a face-down card at the given position on the board.
     * @param index the zero-based position of the card on the board.
     */
    public Card(int index) {
        this.index = index;
        setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
        setForeground(VALUE_COLOR);
        setPreferredSize(new Dimension(92, 124));
        setFocusPainted(false);
        // Without an opaque background some look-and-feels (e.g. macOS Aqua) ignore the card color.
        setOpaque(true);
        // A click asks the card to change its state; the Controller may veto the request.
        addActionListener(e -> clicked());
        updateAppearance();
    }

    // --- index property ---

    /**
     * Returns the position of the card on the board.
     * @return the zero-based index of the card.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Sets the position of the card on the board (used by builder tools with the no-argument constructor).
     * @param index the zero-based index of the card.
     */
    public void setIndex(int index) {
        this.index = index;
    }

    // --- value property (read-only) ---

    /**
     * Returns the value of the card.
     * @return the value displayed when the card is face up.
     */
    public int getValue() {
        return value;
    }

    // --- state property (bound and constrained) ---

    /**
     * Returns the current state of the card.
     * @return the current {@link CardState}.
     */
    public CardState getState() {
        return state;
    }

    /**
     * Changes the state of the card. The change is first proposed to the vetoable change listeners,
     * which can reject it by throwing a {@link PropertyVetoException}; if nobody vetoes it, the
     * state is updated, the card is repainted and the bound listeners are notified.
     *
     * @param newState the requested state.
     * @throws PropertyVetoException if a vetoable change listener (the Controller) rejects the change.
     */
    public void setState(CardState newState) throws PropertyVetoException {
        Objects.requireNonNull(newState, "state");
        CardState oldState = this.state;
        if (newState == oldState) {
            return; // nothing changes, so no event is fired
        }
        fireVetoableChange("state", oldState, newState); // constrained: listeners may veto
        this.state = newState;
        updateAppearance();
        firePropertyChange("state", oldState, newState);  // bound: listeners are notified
    }

    /**
     * Reaction to a click, as described in the specification: a face-down card asks to turn face up,
     * while a face-up or excluded card asks to turn face down. The Controller vetoes the second kind
     * of request, and the first one when two cards are already face up.
     */
    private void clicked() {
        CardState requested = (state == CardState.FACE_DOWN) ? CardState.FACE_UP : CardState.FACE_DOWN;
        try {
            setState(requested);
        } catch (PropertyVetoException vetoed) {
            // The Controller rejected the move: the card keeps its current state.
        }
    }

    /**
     * Paints the card according to its state: green when face down, red when excluded,
     * white with its value when face up.
     */
    private void updateAppearance() {
        setBackground(state.getColor());
        setText(state == CardState.FACE_UP ? String.valueOf(value) : "");
    }

    // --- Listener implementations ---

    /**
     * Takes the new value from the shuffle sequence and turns the card face down for the new round.
     * The change goes through {@link #setState(CardState)}; the Controller, notified first by the
     * Board, authorizes it.
     * @param evt the shuffle event fired by the Board.
     */
    @Override
    public void shufflePerformed(ShuffleEvent evt) {
        List<Integer> values = evt.getValues();
        if (index < 0 || index >= values.size()) {
            throw new IllegalStateException("No value for card " + index + " in a sequence of " + values.size());
        }
        value = values.get(index);
        try {
            setState(CardState.FACE_DOWN);
        } catch (PropertyVetoException e) {
            throw new IllegalStateException("The Controller must authorize a new round", e);
        }
        updateAppearance();
    }

    /**
     * Reacts to the outcome of a turn. The event is ignored unless the card is face up: in that case
     * the card becomes excluded if the two revealed cards matched, and turns face down otherwise.
     * @param evt the matched event fired by the Controller.
     */
    @Override
    public void matchResult(MatchedEvent evt) {
        if (state != CardState.FACE_UP) {
            return;
        }
        try {
            setState(evt.isMatch() ? CardState.EXCLUDED : CardState.FACE_DOWN);
        } catch (PropertyVetoException e) {
            throw new IllegalStateException("The Controller must authorize the outcome of a turn", e);
        }
    }
}
