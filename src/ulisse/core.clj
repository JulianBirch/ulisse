(ns ulisse.core
  (:refer-clojure :exclude [eval cast])
  (:require [spyscope.core]
            [codox-md.writer :only [write-docs]]
            [codox-md.markdown :only [md]]
            [clojure.string :as string]
            [endophile.core]
            [poppea])
  (:import [org.python.util PythonInterpreter]
           [org.python.core PyObject PyString PyDictionary
            PyBoolean PyType]
           [clojure.lang PersistentArrayMap IPersistentMap Keyword]
           [java.lang StringBuffer]
           [clojure.string]))

(defn highlighter []
  (doto (PythonInterpreter.)
    (.exec
"import pygments
import pygments.lexers
import pygments.formatters")))

(defn cast [^PyObject obj class]
  (.__tojava__ obj class))

(defn eval
  ([^PythonInterpreter interpreter function]
     (.eval interpreter function))
  ([^PythonInterpreter interpreter function class]
     (cast (.eval interpreter function) class)))

(defmulti to-jython class)

(defmethod to-jython String [obj] (PyString. obj))
(defmethod to-jython PyObject [obj] obj)
(defmethod to-jython Boolean [obj] (PyBoolean. obj))
(defmethod to-jython IPersistentMap [obj]
  (let [result (PyDictionary.)]
    (doseq [[k v] obj]
      (.__setitem__ result (to-jython k) (to-jython v)))
    result))

(defn parse-args [[args keywords] arg]
  (if (keyword? arg)
    [args (conj keywords (name arg))]
    [(conj args (to-jython arg)) keywords]))

(defn call [^PyObject callable & args]
  (if (empty? args)
    (.__call__ callable)
    (let [[args keywords] (reduce parse-args [[] []] args)
          args (into-array PyObject args)
          keywords (into-array String keywords)]
      (if (empty? keywords)
        (.__call__ callable args)
        (.__call__ callable args keywords)))))

(defn call2 [^PythonInterpreter interpreter name & args]
  (apply call (eval interpreter name) args))

(defn highlight
  ([highlighter code]
     (highlight highlighter code nil))
  ([^PythonInterpreter highlighter code lexer]
     (let [html (call2 highlighter "pygments.formatters.HtmlFormatter"
                       :nowrap true)
           l (if lexer
               (call2 highlighter (str "pygments.lexers." lexer))
               (call2 highlighter "pygments.lexers.guess_lexer" code))]
       (-> highlighter
           (call2 "pygments.highlight" code l html)
           (cast java.lang.String)))))

(def hl (delay (highlighter)))

(def test-text
  "Creates a validator that chains the result of one validator into
   the next.  Validators that are tests, such as `is` will have
   output the same as their input.  Any failed validation or
   sucessful `is-optional` test will terminate the evaluation.  The
   validation error will contain a key `:chain` which contains the
   chain of validation results that led to the error.

   `->>` is a macro that has a number of interpretation rules that
   transform normal clojure syntax into validators.

   * `string?` becomes `(v/is string?)`
   * `inc` becomes `(v/as inc)`
   * `:key` becomes `(v/as-key :key)`
   * `[\"City\" \"Zip\"]` becomes `(v/as-key [\"City\" \"Zip\"])`

   `is` is used if the name of the function ends with a question
   mark or if it's a comparison operator in clojure.core.

   Chaining these together gives you the ability to do things like
   this:

       (v/->> :should-be-even v/required v/number even?)
       ;;; Input should be a map with a key :should-be-even
       ;;; that can be read as a number and that number is even.

       (v/->> :email v/optional v/email?)
       ;;; Input should be an email address or blank.

       (v/->> keys (v/are keyword?))
       ;;; Input should be a map where all the keys are keywords

   Next, if you follow a validator with a map, it will be merged
   into the validator.

       (v/->> :email {:not-found \"xjobcon@phx.cam.ac.uk\"})
       ;;; Provides a default email address
       ;;; see also `as-key` for more details

   Finally, strings are treated as maps with a `:arianna/message` key.
   These are used to provide human readable feedback by using
   stencil/mustache on the validation errors.

       (v/->> :email
              v/required \"You must provide an email.\"
              v/email? \"The input {{value}} doesn't appear to be an email address.\")
   ")

(defn emit-element
  "An alternative emit-element that doesn't cause newlines to be
  inserted around punctuation."
  [yield emit e]
  (if (instance? String e)
    (yield e)
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
              (emit yield c)))
          (yield (str "</" (name (:tag e)) ">")))
        (yield "/>")))))

(poppea/defn-curried emit-element-with-pygments
  [yield e]
  (if (map? e)
    (cond
     (= :pre (:tag e))
     (if-let [{[{[content] :content}] :content} e]
       (do
         (yield "<div class='highlight'><pre>")
         (yield (highlight @hl content "ClojureLexer"))
         (yield "</div></pre>"))
       (emit-element yield emit-element-with-pygments e))
     (= :code (:tag e))
     (if-let [{[content] :content} e]
       (do
         (yield "<code class='highlight'>")
         (yield (clojure.string/trimr (highlight @hl content "ClojureLexer")))
         (yield "</code>"))
       (emit-element yield emit-element-with-pygments e))
     :else
     (emit-element yield emit-element-with-pygments e))
    (emit-element yield emit-element-with-pygments e)))

(defn markdown-to-html [content]
  (let [sb (StringBuffer. 2000)
        yield (fn [^String s] (.append sb s))]
    (->> content
         endophile.core/mp
         endophile.core/to-clj
         (map (emit-element-with-pygments yield))
         dorun)
    (.toString sb)))

(defn write-docs
  "Take raw documentation and turn it into formatted HTML."
  [project]
  (with-redefs [codox-md.markdown/md markdown-to-html]
    (codox-md.writer/write-docs project)))

(defn test2 []
  (-> test-text
      markdown-to-html))

(defn test3 []
  (-> test-text
      endophile.core/mp
      endophile.core/to-clj))
