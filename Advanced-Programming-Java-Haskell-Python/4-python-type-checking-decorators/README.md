# 4 · Python: type-checking decorators

Exercise 3 of the second assignment, Part 2. The **solution format** it requires: *"A single file called type_checking_decorators.py containing the definition of the decorators print_types, type_check and bb_type_check"*, such that running `python3 testN.py` raises no error and prints essentially the same output as `logN.txt`.

The decorators are described in the [main README](../README.md#4--python-type-checking-decorators) and in Section 5 of the [report](../REPORT.pdf).

## Files

| File | Role | What it is |
|:---|:---|:---|
| `type_checking_decorators.py` | **solution** | the three decorators |
| `test1.py`, `test2.py`, `test3.py` | provided with the assignment | the test programs, one per decorator |
| `log1.txt`, `log2.txt`, `log3.txt` | provided with the assignment | the expected output of each test program |

In the story of the assignment, the lecturer lost the file with the decorators and kept only the tests and their logs; they are included unchanged, so that anyone can run the tests and compare the output with the logs.

## Run and verify

```bash
python3 test1.py
```

Python 3.10 or newer is needed. Each log starts with the command line (`$ python3 test1.py`) followed by the output, so the comparison skips its first line (`diff` prints nothing when two outputs coincide):

```bash
python3 test1.py | diff - <(tail -n +2 log1.txt) && echo identical
python3 test2.py | diff - <(tail -n +2 log2.txt) && echo identical
python3 test3.py | diff - <(tail -n +2 log3.txt) && echo identical
```
