(ns ulisse.writer
  (:require [codox-md.writer :only [write-docs]]
            [codox-md.markdown :only [md]]
            [ulisse.markdown :only [markdown-to-html]])
  (:import [java.lang StringBuffer]))

(defn write-docs
  "Takes raw documentation and turn it into formatted HTML.
   Works as a codox writer.

   Example (in your project.clj):

   ```
   :codox {:include [arianna]
           :writer ulisse.writer/write-docs
           :src-dir-uri \"http://github.com/JulianBirch/arianna/blob/master/\"
           :src-linenum-anchor-prefix \"L\"
   ```"
  [project]
  (with-redefs [codox-md.markdown/md #(markdown-to-html % project)]
    (codox-md.writer/write-docs project)))
