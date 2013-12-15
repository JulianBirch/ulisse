(ns ulisse.writer
  (:require [spyscope.core]
            [codox-md.writer :only [write-docs]]
            [codox-md.markdown :only [md]]
            [clojure.string :as string]
            [endophile.core]
            [poppea]
            [ulisse.jython :refer [__ _> ->PythonInterpreter
                                  py-cast]])
  (:import [java.lang StringBuffer]))

(defn highlighter []
  (doto (->PythonInterpreter)
    (.exec
"import pygments
import pygments.lexers
import pygments.formatters")))

(defn highlight
  ([highlighter code]
     (highlight highlighter code nil))
  ([highlighter code lexer]
     (let [html (_> highlighter
                         "pygments.formatters.HtmlFormatter"
                         :nowrap true)
           l (if lexer
               (_> highlighter (str "pygments.lexers." lexer))
               (_> highlighter "pygments.lexers.guess_lexer" code))]
       (-> highlighter
           (_> "pygments.highlight" code l html)
           (py-cast java.lang.String)))))

(def hl (delay (highlighter)))

;;; Ripped from juxt/juxtweb
(defn emit-element
  "An alternative emit-element that doesn't cause newlines to be
  inserted around punctuation."
  [yield emit e]
  (if (instance? String e)
    (yield+ e)
    (do
      (yield (str "<" (name (:tag e))))
      (when (:attrs e)
        (doseq [attr (:attrs e)]
          (yield (str " " (name (key attr)) "='" (val attr)"'"))))
      (if (:content e)
        (do
          (yield ">")
          (if (instance? String (:content e))
            (yield (:content e))
            (doseq [c (:content e)]
              (emit c)))
          (yield (str "</" (name (:tag e)) ">")))
        (yield "/>")))))

(defn highlight-clojure [content]
  (highlight @hl content "ClojureLexer"))

;;; Ripped and adapted from juxtweb
(defn yield-code [yield [start end] content]
  (when content
    (yield start)
    (yield (clojure.string/trimr (highlight-clojure content)))
    (yield end)
    true))

(def code-wrap
  {:pre ["<div class='highlight'><pre>" "</pre></div>"]
   :code ["<code class='highlight'>" "</code>"]})

(def content-accessor
  {:code (comp first :content)
   :pre (comp first :content first :content)})

(poppea/defn-curried emit-element-with-pygments
  [yield code-wrap e]
  (or (if-let [tag (#{:pre :code} (:tag e))]
        (yield-code yield
                    (tag code-wrap)
                    ((tag content-accessor) e)))
      (emit-element yield
                    (emit-element-with-pygments yield code-wrap)
                    e)))

(defn endophile-clj-to-html [user-code-wrap clj]
  (let [code-wrap (merge code-wrap user-code-wrap)
        sb (StringBuffer. 2000)
        yield (fn [^String s] (.append sb s))]
    (dorun (map (emit-element-with-pygments yield code-wrap) clj))
    (.toString sb)))

(defn markdown-to-html
  ([content code-wrap]
     (->> content
          endophile.core/mp
          endophile.core/to-clj
          (endophile-clj-to-html code-wrap)))
  ([content]
     (markdown-to-html content {})))
