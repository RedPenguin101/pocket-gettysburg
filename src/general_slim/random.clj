(ns general-slim.random)

(defn rng-int!
  "Generates a lazy sequence of random numbers between 0 and
   the provided number (not inclusive)"
  [n]
  (lazy-seq (cons (rand-int n) (rng-int! n))))

(defn rand-bool! [] (zero? (rand-int 2)))
