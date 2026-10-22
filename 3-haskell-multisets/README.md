# 3 · Haskell: multisets

Exercises 1 and 2 of the second assignment, Part 1. The **solution format** they require: *"A Haskell source file called MultiSet.hs containing a Module called MultiSet"* and *"A Haskell source file TestMSet.hs with the functions described above, which can be executed using runghc"*.

The module is described in the [main README](../README.md#3--haskell-multisets) and in Section 4 of the [report](../REPORT.pdf).

## Files

| File | Role | What it is |
|:---|:---|:---|
| `MultiSet.hs` | **solution** (exercise 1) | the module: the type `MSet`, its operations and its `Eq` and `Foldable` instances |
| `TestMSet.hs` | **solution** (exercise 2) | `readMSet`, `writeMSet` and the `main` that checks the anagram facts |
| `aux_files/anagram.txt`, `anagram-s1.txt`, `anagram-s2.txt`, `margana2.txt` | provided with the assignment | the four word lists read by `TestMSet.hs` |

The word lists come from the archive `aux_files.zip` distributed with the assignment and are included unchanged. The assignment text calls two of them `anagram_s1.txt` and `anagram_s2.txt`, but in the archive their names use a hyphen, so the program reads the files with their actual names.

## Run

```bash
runghc TestMSet.hs
```

`runghc` runs a Haskell program without compiling it first. The program loads the four files from `aux_files/`, prints the two facts required by the assignment and writes the multisets `m1` and `m4` to `anag-out.txt` and `gana-out.txt` in this folder.
