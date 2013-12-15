(ns ulisse.jython
  (:refer-clojure :exclude [eval cast])
  (:import [org.python.util PythonInterpreter]
           [org.python.core PyObject PyString PyDictionary
            PyBoolean PyType]
           [clojure.lang PersistentArrayMap IPersistentMap Keyword]
           [java.lang StringBuffer]
           [clojure.string]))

(defn ->PythonInterpreter [] (PythonInterpreter.))

(defn py-cast [^PyObject obj class]
  (.__tojava__ obj class))

(defn _e
  ([^PythonInterpreter interpreter function]
     (.eval interpreter function))
  ([^PythonInterpreter interpreter function class]
     (py-cast (.eval interpreter function) class)))

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

(defn __ [^PyObject callable & args]
  (if (empty? args)
    (.__call__ callable)
    (let [[args keywords] (reduce parse-args [[] []] args)
          args (into-array PyObject args)
          keywords (into-array String keywords)]
      (if (empty? keywords)
        (.__call__ callable args)
        (.__call__ callable args keywords)))))

(defn _> [^PythonInterpreter interpreter name & args]
  (apply __ (_e interpreter name) args))
