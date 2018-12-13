(ns advent.puzzle12
  (:import java.util.BitSet)
  (:require [clojure.string :as str]))

(set! *unchecked-math* :warn-on-boxed)
(set! *warn-on-reflection* true)

(def neighbors 2)
(def blength 5)
(def padding 3)

(defn read-sample
  []
  (with-open [f (-> "12/sample.txt"
                    clojure.java.io/reader)]
    (vec (line-seq f))))

(defn read-input
  []
  (with-open [f (-> "12/input.txt"
                    clojure.java.io/reader)]
    (vec (line-seq f))))

(defn s->bitset
  [s]
  (let [bs (BitSet. (count s))]
    (doseq [[idx c] (map-indexed vector s)] (when (= \# c) (.set bs idx)))
    bs))

(defn bitset->s
  ([^String s]
   (->> (range (.length s))
        (map (fn [idx] (if (.get s idx) \# \.)))
        (str/join)))
  ([s length]
   (->> (range length)
        (map (fn [idx] (if (.get s idx) \# \.)))
        (str/join))))

(def regex #"initial state: (.+)")

(defn parse-header
  [line]
  (let [s (->> (re-matches regex line)
               second)]
    {:length (count s), :initial-state (s->bitset s)}))

(def regex2 #"^([#.]+) => ([#.])$")

(defn parse-body-line
  [line]
  (let [[_ k v] (re-matches regex2 line)]
    [(-> k
         s->bitset
         .toLongArray
         first) (= "#" v)]))

(defn parse
  [[fst _ & body]]
  (-> (parse-header fst)
      (assoc :lookup (->> body
                          (map parse-body-line)
                          (into {})))))

(defn sub-bitset
  [^BitSet bs from to]
  ;; FIXME: use fast path if from >= 0
  (let [new-bs (BitSet.)]
    (doseq [[new-idx old-idx] (map-indexed vector (range from to))]
      (.set new-bs new-idx (if (neg? old-idx) false (.get bs old-idx))))
    new-bs))

(defn bs->long
  [bs]
  (-> bs
      .toLongArray
      first))

(defn next-gen
  [{:keys [lookup]} [offset bs]]
  (let [new-bs (BitSet.)
        vs (->> (range (- 0 neighbors offset) (+ (.length bs) 2))
                (map (fn [idx]
                       (get lookup
                            (bs->long (sub-bitset bs
                                                  (- idx neighbors)
                                                  (+ idx neighbors 1)))
                            false))))
        [a b] (split-with false? vs)]
    (doseq [[idx v] (map-indexed vector b)] (.set new-bs idx v))
    [(- (count a) neighbors) new-bs]))

(defn bit-seq
  ([^BitSet bs] (bit-seq bs 0))
  ([^BitSet bs ^long idx]
   (lazy-seq
    (let [nxt (.nextSetBit bs idx)]
      (cond (neg? nxt)
            '()
            (= nxt idx)
            (cons nxt (bit-seq bs (inc idx)))
            :else
            (bit-seq bs (inc idx)))))))

(defn calc [^long offset ^BitSet bs]
  (+ (->> bs bit-seq (reduce +)) (* offset (.cardinality bs))))

(defn print-gen
  [[^long num [^long offset bs]]]
  (println (format "%3d" num)
           (str (apply str (repeat (+ ^long padding offset) ".")) (bitset->s bs))
           (calc offset bs)))

(defn generations [ctx] (iterate #(next-gen ctx %) [0 (:initial-state ctx)]))

(defn solution-1 []
  (let [gens (->> (read-input) parse generations)]
    (apply calc (nth gens 20))))

(defn solution-2 []
  #_(let [gens (->> (read-input) parse generations)
          v (apply calc (nth gens 20))]
      (prn v)
      )
  (let [gens (->> (read-input) parse generations)]
    (reduce (fn [[seen? ^long idx] [offset bs]]
              (let [v (calc offset bs)]
                (when (= 0 (mod idx 100))
                  (prn idx v))
                (if (seen? v)
                  (reduced [idx (seen? v)])
                  [(assoc seen? v idx) (inc idx)])))
            [{} 0]
            gens)))
