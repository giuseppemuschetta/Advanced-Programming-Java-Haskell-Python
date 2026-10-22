# 1 · Java Beans: the Matching Pairs game

Exercise 1 of the first assignment. The **solution format** it requires: *"Suitably commented source files for the beans and one jar archive for each bean."*

Everything in `src/` and `jars/` is part of the solution: no file in this folder was provided with the assignment. The design is described in the [main README](../README.md#1--java-beans-the-matching-pairs-game) and in Section 2 of the [report](../REPORT.pdf).

## Files

| File | Role | What it is |
|:---|:---|:---|
| `src/Board.java` | solution · bean | the main window (`JFrame`): creates the other beans, wires them together and publishes the shuffle event |
| `src/Card.java` | solution · bean | a card (`JButton`) with the bound and constrained property `state` |
| `src/Controller.java` | solution · bean | the rules of the game: vetoes the forbidden moves and publishes the matched event; shows the pairs found (`JLabel`) |
| `src/Counter.java` | solution · bean | the number of moves (`JLabel`) |
| `src/Challenge.java` | solution · bean | optional extension 2: the best score (`JLabel`) |
| `src/CardState.java` | solution | the three states of a card and their colors |
| `src/ShuffleEvent.java`, `src/ShuffleListener.java` | solution | the shuffle event and its listener interface |
| `src/MatchedEvent.java`, `src/MatchedListener.java` | solution | the matched event and its listener interface |
| `jars/Board.raj` … `jars/Counter.raj` | solution | one archive per bean, with the `.raj` suffix required for submission (see below) |
| `docs/` | documentation | screenshots and animation shown in the READMEs |

## Why the archives end in `.raj`

The course instructions ask to send the solutions by email and warn that Gmail, which the lecturer uses, filters out `.jar` attachments, even inside compressed archives: the `.jar` files must therefore be renamed `.raj` before sending them. The archives are kept here exactly as they are delivered.

To use them, rename all five back to `.jar` (each archive refers to the others by their `.jar` names) and start the game:

```bash
cd jars
for f in *.raj; do mv "$f" "${f%.raj}.jar"; done
java -jar Board.jar
```

## Run from the sources

```bash
javac -d out src/*.java
java -cp out Board
```

`javac` compiles the sources into the folder `out`; `java -cp out Board` starts the game, whose `main` method is in `Board`. A JDK 17 or newer is needed.
