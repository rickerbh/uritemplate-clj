(ns uritemplate-clj.core-test
  (:use clojure.test
        uritemplate-clj.core
        cheshire.core))

(deftest parse-token-test
  (is (= (parse-token "{var}") (->Token  (list (->Variable "var" nil)) nil)))
  (is (= (parse-token "{/var}") (->Token (list (->Variable "var" nil)) "/")))
  (is (= (parse-token "{/var:3,var*}") (->Token (list (->Variable "var" ":3") (->Variable "var" "*")) "/" )))
  (is (= (parse-token "{#var*}") (->Token (list (->Variable "var" "*")) "#")))
  (is (= (parse-token "{+var:7}") (->Token (list (->Variable "var" ":7")) "+"))))

(deftest parse-variable-test
  (is (= (parse-variable "var:5") (->Variable  "var" ":5")))
  (is (= (parse-variable "var*") (->Variable "var" "*")))
  (is (= (parse-variable "var") (->Variable "var" nil ))))

(def spec-examples 
  (cheshire.core/parse-stream (clojure.java.io/reader "test/uritemplate_clj/spec-examples.json")))

(defn- level-test [examples selection]
  (let
      [level (examples selection)
       vars (level "variables")
       testcases (level "testcases")]
    (doall 
     (for [tc testcases] 
       (let [res (uritemplate (first tc) vars)]
         (if (vector? (second tc))
           (is (some #(= res %) (second tc)))
           (is (= res (second tc)))))))))

(deftest level1-test (level-test spec-examples "Level 1 Examples"))
(deftest level2-test (level-test spec-examples "Level 2 Examples"))
(deftest level3-test (level-test spec-examples "Level 3 Examples"))
(deftest level4-test (level-test spec-examples "Level 4 Examples")) 

; test in response of https://github.com/mwkuster/uritemplate-clj/issues/2
(deftest query-param-encoding
  (let
      [temp "{/hello}{?qparam}"
       hello "Hello+ World!"
       qparam "50+%"
       res (uritemplate temp {"hello" hello, "qparam" qparam})]
    (is (= res "/Hello+%20World%21?qparam=50%2B%25"))))
    

(def extended-tests  (cheshire.core/parse-stream (clojure.java.io/reader "test/uritemplate_clj/extended-tests.json")))

(deftest extended-test (level-test extended-tests "Additional Examples 1"))
(deftest extended2-test (level-test extended-tests "Additional Examples 2"))
(deftest extended3-test (level-test extended-tests "Additional Examples 3: Empty Variables"))

(deftest additional-test
  (let
      [template1 "abc{/type}{/agent*}{/year}{/natural_identifier,version,language}"
       template2 "abc{/type}/{agent*}{/year}{/natural_identifier,version,language}"
       values1 {"type" "dir", 
               "agent"  ["ep" "consil"], 
               "year"  "2003",
               "natural_identifier" "98",
               "version" "R3",
               "language" "SPA"}
       values2 {"type" "dir", 
               "agent"  ["ep" "consil"], 
               "year"  "2003",
               "natural_identifier" "98"}
       values3 {"type" "dir", 
                "year"  "2003",
                "natural_identifier" "98",
                "version" "R3",
                "language" "SPA"}
       values4 {"type" "dir", 
                "agent"  ["ep" "consil"], 
                "year"  "2003",
                "natural_identifier" "98",
                "language" "SPA"}]
    (is (= (uritemplate template1 values1) "abc/dir/ep/consil/2003/98/R3/SPA"))
    (is (= (uritemplate template1 values2) "abc/dir/ep/consil/2003/98"))
    (is (= (uritemplate template1 values3) "abc/dir/2003/98/R3/SPA"))
    (is (= (uritemplate template1 values4) "abc/dir/ep/consil/2003/98/SPA"))
    (is (= (uritemplate template2 values4) "abc/dir/ep,consil/2003/98/SPA"))))

(deftest ambiguous-template-level3-test
  (let
      [ambiguous-template "/foo{/ba,bar}{/baz,bay}"
       values1 {"ba" "x", "bar" "y", "baz" "z"}
       values2 {"ba" "x", "baz" "y", "bay" "z"}]
    (is (= (uritemplate ambiguous-template values1) (uritemplate ambiguous-template values2)))))

(deftest ambiguous-template-level4-test
  (let
      [ambiguous-template "/foo{/ba*}{/baz,bay}"
       values1 {"ba" '("x" "y"), "baz" "z"}
       values2 {"ba" "x", "baz" "y", "bay" "z"}
       ambiguous-template-level1 "/foo{hello}{world}"]
    (is (= (uritemplate ambiguous-template values1) (uritemplate ambiguous-template values2)))
    (is (= (uritemplate ambiguous-template-level1 {"hello" "hello", "world" "world"}) "/foohelloworld"))))

(deftest uritemplate-with-uuid
  (is (= "http://localhost:3000/foo/36cf2ce0-5ee0-4b6b-817d-b094fe94a9e1"
         (uritemplate "http://localhost:3000/foo/{id}"
                      {"id" (java.util.UUID/fromString "36cf2ce0-5ee0-4b6b-817d-b094fe94a9e1")}))))
