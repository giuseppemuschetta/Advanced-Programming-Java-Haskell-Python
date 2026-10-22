{-
  Multisets in Haskell - TestMSet
  Version: 2.0
  Author: Giuseppe Muschetta

  Tests the module MultiSet on the auxiliary files of the assignment. Every word of a file is
  replaced by its "ciao" (characters in alphabetical order: the word in lower case with its
  letters sorted), so two words have the same ciao exactly when they are anagrams.

  Run it from this directory with:  runghc TestMSet.hs
-}
module Main (main) where

import Data.Char (toLower)
import Data.List (sort)
import MultiSet

-- | The ciao of a string: same length, all characters in lower case and in alphabetical order.
-- For example, @ciao "Hello"@ is @"ehllo"@.
ciao :: String -> String
ciao = sort . map toLower

-- | Reads a text file and returns the multiset of the ciao of all its words, each with its
-- multiplicity.
readMSet :: FilePath -> IO (MSet String)
readMSet path = do
  contents <- readFile path
  let mset = foldl add empty (map ciao (words contents))
  -- The whole file is consumed before returning, so the file handle is closed at once.
  length (elems mset) `seq` return mset

-- | Writes a multiset to a file, one element per line, in the format "<elem> - <multiplicity>".
writeMSet :: MSet String -> FilePath -> IO ()
writeMSet (MS xs) path = writeFile path (unlines [e ++ " - " ++ show n | (e, n) <- xs])

-- | True if the two multisets have the same elements, regardless of their multiplicities.
-- 'all' and 'elem' come from the Foldable instance, which ignores the multiplicities.
sameElements :: Eq a => MSet a -> MSet a -> Bool
sameElements m1 m2 = all (`elem` m2) m1 && all (`elem` m1) m2

-- | Prints the name of a multiset, its file, its total number of words and of distinct ciao.
describe :: String -> FilePath -> MSet a -> IO ()
describe name path m =
  putStrLn ("  " ++ name ++ "  " ++ pad 26 path ++ pad 6 (show (length (elems m)))
              ++ "words, " ++ show (length m) ++ " distinct ciao")

-- | Pads a string with spaces on the right.
pad :: Int -> String -> String
pad n s = s ++ replicate (n - length s) ' '

-- | Prints a fact, the value computed for it and a comment.
report :: String -> Bool -> String -> String -> IO ()
report fact value ifTrue ifFalse = do
  putStrLn ("  " ++ pad 46 fact ++ show value)
  putStrLn ("    => " ++ (if value then ifTrue else ifFalse))

main :: IO ()
main = do
  let dir = "aux_files/"
      files = [ ("m1", dir ++ "anagram.txt")
              , ("m2", dir ++ "anagram-s1.txt")
              , ("m3", dir ++ "anagram-s2.txt")
              , ("m4", dir ++ "margana2.txt") ]
  [m1, m2, m3, m4] <- mapM (readMSet . snd) files

  putStrLn "Multisets of ciao strings"
  mapM_ (\((name, path), m) -> describe name path m) (zip files [m1, m2, m3, m4])

  putStrLn ""
  putStrLn "Facts"
  report "(i)  m1 == m4" (m1 == m4)
    "m1 and m4 are equal." "m1 and m4 are not equal..."
  report "     m1 and m4 have the same elements" (sameElements m1 m4)
    "...but they have the same elements: the same anagram classes, with different multiplicities."
    "...and they do not have the same elements."
  report "(ii) m1 == union m2 m3" (m1 == union m2 m3)
    "m1 is equal to the union of m2 and m3." "m1 is NOT equal to the union of m2 and m3."
  report "     subeq m2 m1 && subeq m3 m1" (subeq m2 m1 && subeq m3 m1)
    "as expected, m2 and m3 are both contained in m1." "m2 or m3 is not contained in m1."

  writeMSet m1 "anag-out.txt"
  writeMSet m4 "gana-out.txt"
  putStrLn ""
  putStrLn ("Wrote m1 to anag-out.txt (" ++ show (length m1) ++ " lines) and m4 to gana-out.txt ("
              ++ show (length m4) ++ " lines).")
