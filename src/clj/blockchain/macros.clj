(ns blockchain.macros)


(defmacro load-json [file]
  (slurp file))
