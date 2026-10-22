/**
 * Matching Pairs Game - Board Bean (main window)
 * Version: 2.0
 * Author: Giuseppe Muschetta
 */
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The main window of the Matching Pairs game: a JavaBean that extends {@link JFrame}.
 * <p>
 * At startup the Board creates all the other beans (2 x N {@link Card}s, the {@link Controller},
 * the {@link Counter} and the {@link Challenge}), lays them out together with the Shuffle and Exit
 * buttons, and registers each bean as a listener of the events it needs (Observer pattern).
 * The Board is also the publisher of the {@link ShuffleEvent}: it generates a random sequence of
 * 2 x N values, made of N integers each repeated twice, and fires it at the start of the game and
 * every time Shuffle is clicked.
 */
public class Board extends JFrame {

    private static final long serialVersionUID = 1L;

    /**
     * Optional extension 1: the number of pairs. The game uses 2 x N cards everywhere; the other
     * beans receive N at run time, so changing it requires recompiling the Board only.
     */
    private static final int N = 4;

    private final ArrayList<Card> cards = new ArrayList<>();
    private final Controller controller = new Controller(N);
    private final Counter counter = new Counter();
    private final Challenge challenge = new Challenge();
    private final JButton shuffleButton = new JButton("Shuffle");
    private final JButton exitButton = new JButton("Exit");

    // Subscribers of the shuffle event, notified in registration order.
    private final CopyOnWriteArrayList<ShuffleListener> shuffleListeners = new CopyOnWriteArrayList<>();
    private final Random random = new Random();

    /**
     * Creates the Board and all the other beans, wires them together and deals the first round.
     * The window is not shown: call {@code setVisible(true)}.
     */
    public Board() {
        super("Matching Pairs");
        for (int i = 0; i < 2 * N; i++) {
            cards.add(new Card(i));
        }
        registerListeners();
        layoutBeans();

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExit(); // closing the window behaves like the Exit button
            }
        });
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);

        shuffle(); // first deal
    }

    /**
     * Registers every bean as a listener of the events it needs (Observer pattern).
     * <pre>
     *   Board      --ShuffleEvent-------------------&gt; Controller, Counter, Cards
     *   Card       --vetoable change of "state"-----&gt; Controller (may veto)
     *   Card       --property change of "state"-----&gt; Controller (v1, v2), Counter (moves)
     *   Controller --MatchedEvent-------------------&gt; Cards
     *   Counter    --property change "flipCount"----&gt; Challenge
     *   Controller --property change "gameFinished"-&gt; Challenge, Board (victory message)
     * </pre>
     */
    private void registerListeners() {
        // The Controller must receive the shuffle event before the cards: it opens the new round,
        // which authorizes every card, excluded or face up, to turn face down again.
        addShuffleListener(controller);
        addShuffleListener(counter);

        for (Card card : cards) {
            addShuffleListener(card);
            card.addVetoableChangeListener(controller);
            card.addPropertyChangeListener("state", controller);
            card.addPropertyChangeListener("state", counter);
            controller.addMatchedListener(card);
        }

        counter.addPropertyChangeListener("flipCount", challenge);
        controller.addPropertyChangeListener("gameFinished", challenge);
        controller.addPropertyChangeListener("gameFinished", evt -> {
            if (Boolean.TRUE.equals(evt.getNewValue())) {
                announceVictory();
            }
        });

        shuffleButton.addActionListener(e -> shuffle());
        exitButton.addActionListener(e -> confirmExit());
    }

    /**
     * Places the labels and the buttons at the top of the window and the cards in a grid below.
     * With N = 4 the cards form 2 rows of 4; with larger N the grid is roughly square.
     */
    private void layoutBeans() {
        // The window is sized once, on the initial texts: each label reserves the width of the
        // longest text it can display, so that no text is ever cut.
        reserveWidth(controller, "Pairs found: " + N + " / " + N);
        reserveWidth(counter, "Moves: 9999");
        reserveWidth(challenge, "Best score: 9999 moves");
        JPanel labels = new JPanel(new GridLayout(1, 3, 18, 0));
        labels.add(controller);
        labels.add(counter);
        labels.add(challenge);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(shuffleButton);
        buttons.add(exitButton);

        JPanel top = new JPanel(new BorderLayout(18, 0));
        top.add(labels, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.EAST);

        int columns = (N <= 4) ? N : (int) Math.ceil(Math.sqrt(2.0 * N));
        JPanel grid = new JPanel(new GridLayout(0, columns, 10, 10));
        for (Card card : cards) {
            grid.add(card);
        }

        JComponent content = (JComponent) getContentPane();
        content.setLayout(new BorderLayout(0, 14));
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        content.add(top, BorderLayout.NORTH);
        content.add(grid, BorderLayout.CENTER);
    }

    /**
     * Sets the preferred width of a label to the width of the given text.
     * @param label       the label.
     * @param longestText the longest text the label will display.
     */
    private static void reserveWidth(JLabel label, String longestText) {
        Insets insets = label.getInsets();
        int width = label.getFontMetrics(label.getFont()).stringWidth(longestText) + insets.left + insets.right + 2;
        label.setPreferredSize(new Dimension(width, label.getPreferredSize().height));
    }

    // --- Shuffle event publication ---

    /**
     * Subscribes a listener to the shuffle event.
     * @param listener the listener to add.
     */
    public void addShuffleListener(ShuffleListener listener) {
        shuffleListeners.add(listener);
    }

    /**
     * Unsubscribes a listener from the shuffle event.
     * @param listener the listener to remove.
     */
    public void removeShuffleListener(ShuffleListener listener) {
        shuffleListeners.remove(listener);
    }

    /**
     * Generates a random sequence of 2 x N values, N integers each repeated twice, and fires it
     * to the subscribers in a {@link ShuffleEvent}.
     */
    private void shuffle() {
        List<Integer> values = new ArrayList<>(2 * N);
        for (int v = 1; v <= N; v++) {
            values.add(v);
            values.add(v);
        }
        Collections.shuffle(values, random);
        ShuffleEvent event = new ShuffleEvent(this, values);
        for (ShuffleListener listener : shuffleListeners) {
            listener.shufflePerformed(event);
        }
    }

    // --- Dialogs ---

    /**
     * Congratulates the player at the end of a round. The number of moves is read with a getter of
     * the Counter: the Board owns every bean, and composing a message is not an interaction between
     * beans. The dialog is shown only when the window is visible.
     */
    private void announceVictory() {
        if (!isShowing()) {
            return;
        }
        int moves = counter.getFlipCount();
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                "You found all " + N + " pairs in " + moves + " moves.",
                "You won!", JOptionPane.INFORMATION_MESSAGE));
    }

    /** Asks the player for confirmation and, if positive, terminates the game. */
    private void confirmExit() {
        int choice = JOptionPane.showConfirmDialog(this, "Do you really want to exit the game?",
                "Exit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            System.exit(0);
        }
    }

    /**
     * Starts the game on the Event Dispatch Thread. The cross-platform look-and-feel is used so that
     * the card colors look the same on Windows, macOS and Linux.
     * @param args not used.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                // keep the default look-and-feel
            }
            new Board().setVisible(true);
        });
    }
}
