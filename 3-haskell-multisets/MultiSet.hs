{-
  Multisets in Haskell - module MultiSet
  Version: 2.0
  Author: Giuseppe Muschetta

  A multiset (or bag) is a collection in which the order of the elements does not count, but
  each element can occur several times. It is represented concretely as a list of pairs
  (element, multiplicity).

  A multiset is WELL-FORMED if every multiplicity is positive and no element appears in two
  pairs. Every operation of this module that returns an MSet returns a well-formed one, and
  the module uses only the Prelude (no Data.MultiSet or similar modules).
-}
module MultiSet
  ( MSet (..)
  , empty
  , add
  , occs
  , elems
  , subeq
  , union
  , mapMSet
  , isWellFormed
  ) where

-- | The concrete representation required by the assignment: a list of (element, multiplicity).
data MSet a = MS [(a, Int)]
  deriving (Show)

-- * Constructors and operations

-- | The empty multiset.
empty :: MSet a
empty = MS []

-- | Adds one occurrence of an element: if the element is already present its multiplicity is
-- increased by one, otherwise it is inserted with multiplicity 1.
add :: Eq a => MSet a -> a -> MSet a
add mset v = addMany mset v 1

-- | Adds k > 0 occurrences of an element (used by 'union' and 'mapMSet').
addMany :: Eq a => MSet a -> a -> Int -> MSet a
addMany (MS xs) v k = MS (insert xs)
  where
    insert [] = [(v, k)]
    insert ((x, n) : rest)
      | x == v = (x, n + k) : rest
      | otherwise = (x, n) : insert rest

-- | The number of occurrences (multiplicity) of an element; 0 if it is not in the multiset.
occs :: Eq a => MSet a -> a -> Int
occs (MS xs) v = maybe 0 id (lookup v xs)

-- | All the elements of the multiset, each repeated as many times as its multiplicity.
-- For example, @elems (MS [('a',3),('b',1)])@ is @"aaab"@.
elems :: MSet a -> [a]
elems (MS xs) = concatMap (\(x, n) -> replicate n x) xs

-- | @subeq m1 m2@ is True if every element of m1 is also an element of m2, with at least the
-- same multiplicity.
subeq :: Eq a => MSet a -> MSet a -> Bool
subeq (MS xs) m2 = all (\(x, n) -> occs m2 x >= n) xs

-- | The multiset with all the elements of both arguments, each with the sum of its
-- multiplicities.
union :: Eq a => MSet a -> MSet a -> MSet a
union (MS xs) m2 = foldl (\acc (x, n) -> addMany acc x n) m2 xs

-- * Class instances

-- | Two multisets are equal if they contain the same elements with the same multiplicities,
-- regardless of the order. Both sides are normalized first, so the comparison is also correct
-- for multisets built directly with the constructor MS.
instance Eq a => Eq (MSet a) where
  m1 == m2 = sameLength && all (`elem` ys) xs
    where
      MS xs = normalize m1
      MS ys = normalize m2
      sameLength = length xs == length ys

-- | Folding applies the function to the elements of the multiset, ignoring the multiplicities:
-- each distinct element is visited once. 'foldr' is a minimal complete definition of Foldable;
-- toList, length, sum, elem, maximum and the others are derived from it.
-- For example, @length (MS [('a',3),('b',1)])@ is 2.
instance Foldable MSet where
  foldr f z (MS xs) = foldr (\(x, _) acc -> f x acc) z xs

-- | Applies a function to every element of a multiset. When the function maps different
-- elements to the same value, their multiplicities are added, so the result is well-formed:
-- @mapMSet (`div` 2) (MS [(2,1),(3,4)])@ is @MS [(1,5)]@.
--
-- Why mapMSet cannot be the fmap of a Functor instance:
-- fmap must have the type (a -> b) -> MSet a -> MSet b for EVERY type b, while mapMSet needs
-- the constraint Eq b. Merging the pairs whose images coincide is the only way to keep the
-- result well-formed, and it requires comparing values of type b; the class Functor offers no
-- way to add that constraint. An fmap that skipped the merge would break the invariant of the
-- type: fmap (const 0) (MS [(1,1),(2,1)]) would be MS [(0,1),(0,1)], with a repeated element.
mapMSet :: Eq b => (a -> b) -> MSet a -> MSet b
mapMSet f (MS xs) = foldl (\acc (x, n) -> addMany acc (f x) n) empty xs

-- * Well-formedness

-- | True if every multiplicity is positive and no element appears in two pairs.
isWellFormed :: Eq a => MSet a -> Bool
isWellFormed (MS xs) = all ((> 0) . snd) xs && distinct (map fst xs)
  where
    distinct [] = True
    distinct (y : ys) = notElem y ys && distinct ys

-- | Rebuilds a multiset in well-formed shape: pairs with the same element are merged and pairs
-- with a non-positive multiplicity are dropped.
normalize :: Eq a => MSet a -> MSet a
normalize (MS xs) = foldl (\acc (x, n) -> addMany acc x n) empty (filter ((> 0) . snd) xs)
