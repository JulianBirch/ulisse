(ns ulisse.core-test
  (:require clojure.test
            ulisse.core
            ulisse.writer
            endophile.core))

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

(defn test2 []
  (-> test-text
      ulisse.writer/markdown-to-html))

(defn test3 []
  (-> test-text
      endophile.core/mp
      endophile.core/to-clj))
