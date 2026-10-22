"""
Type-checking decorators - type_checking_decorators.py
Version: 2.0
Author: Giuseppe Muschetta

Python ignores type hints at run time. The three decorators of this module use the reflection
facilities of the ``inspect`` module to compare, at every call, the type hints of a function
with the actual arguments and result:

* ``print_types``   prints, for every argument and for the result, the type hint and the actual
                    value with its type;
* ``type_check``    prints a message only when a value disagrees with its type hint, identifying
                    each parameter by its position;
* ``bb_type_check`` ("bounded-blocking" type check) also blocks a call whose arguments disagree
                    with the type hints, at most ``max_block`` times.

The decorated functions are assumed to use positional arguments only. A parameter without a type
hint has the annotation ``inspect.Parameter.empty``, printed as ``<class 'inspect._empty'>``.
"""
import inspect
from functools import wraps

_EMPTY = inspect.Parameter.empty  # the same object as inspect.Signature.empty


def _matches(value, hint):
    """True if ``value`` agrees with the type hint ``hint``.

    A missing hint accepts any value; ``None`` (as in ``-> None``) stands for ``type(None)``.
    Unions such as ``int | str`` are accepted by ``isinstance`` directly. A hint that cannot be
    checked at run time (for example a string or a parameterized generic) is not checked.
    """
    if hint is _EMPTY:
        return True
    if hint is None:
        hint = type(None)
    try:
        return isinstance(value, hint)
    except TypeError:
        return True


def _check_arguments(params, args):
    """Prints a message for every argument that disagrees with its type hint.

    Parameters are identified by their position, starting from 0.
    Returns True if at least one argument disagrees.
    """
    mismatch = False
    for position, (param, value) in enumerate(zip(params, args)):
        if not _matches(value, param.annotation):
            print(f"Parameter '{position}' has value '{value}', not of type '{param.annotation}'")
            mismatch = True
    return mismatch


def _check_result(signature, result):
    """Prints a message if the result disagrees with the return type hint, if any."""
    if not _matches(result, signature.return_annotation):
        print(f"Result is '{result}', not of type '{signature.return_annotation}'")


def print_types(func):
    """Prints the type hint and the actual value, with its type, of every argument and of the result."""
    signature = inspect.signature(func)
    params = list(signature.parameters.values())

    @wraps(func)
    def wrapper(*args):
        for param, value in zip(params, args):
            print(f"Formal par '{param.name}':{param.annotation}; actual par '{value}':{type(value)}")
        result = func(*args)
        print(f"Result type {signature.return_annotation}; actual result '{result}':{type(result)}")
        return result

    return wrapper


def type_check(func):
    """Like ``print_types``, but prints only the disagreements between values and type hints."""
    signature = inspect.signature(func)
    params = list(signature.parameters.values())

    @wraps(func)
    def wrapper(*args):
        _check_arguments(params, args)
        result = func(*args)
        _check_result(signature, result)
        return result

    return wrapper


def bb_type_check(max_block):
    """Bounded-blocking type check: a decorator factory.

    Like ``type_check``, but if at least one argument disagrees with its type hint the function
    is blocked: it is not invoked and the call returns ``None``, without checking the result.
    The function can be blocked at most ``max_block`` times; after that it is invoked even with
    wrong arguments, and its result is checked against the return type hint.
    """
    def decorator(func):
        signature = inspect.signature(func)
        params = list(signature.parameters.values())
        remaining_blocks = max_block

        @wraps(func)
        def wrapper(*args):
            nonlocal remaining_blocks
            if _check_arguments(params, args) and remaining_blocks > 0:
                remaining_blocks -= 1
                print(f"Function blocked. Remaining blocks: {remaining_blocks}")
                return None
            result = func(*args)
            _check_result(signature, result)
            return result

        return wrapper

    return decorator
