(ns mtgsmspricer.core-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer [<!!]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check.generators :as gen]
            [mtgsmspricer.core :refer :all]
            [mtgsmspricer.kvstore :as kvstore :refer [put! get!]]))



(deftest words-test
  (testing "that sentences can be split into their constituent words"
    (is (= ["a" "b" "c"] (words "a b c")))
    (is (= [] (words "   ")))
    (is (= [] (words nil)))
    (is (= ["a"] (words "a")))
    (is (= ["a"] (words "a ")))
    (is (= ["a" "b"] (words "a b")))))


(deftest cmd-test
  (testing "that commands can be parsed from text messages"
    (is (= "foo" (cmd "foo")))
    (is (= "foo" (cmd "foo x y")))
    (is (= nil   (cmd nil)))
    (is (= ""    (cmd "")))))


(deftest args-test
  (testing "that arguments can be parsed from text messages"
    (is (= ["x" "y"] (args "foo x y")))
    (is (= ["x"] (args "foo x")))
    (is (= [] (args "foo")))
    (is (= [] (args nil)))))


(deftest parsed-msg-test
  (testing "that text messages can be parsed into cmd/args data structures"
    (is (= {:cmd "foo"
            :args ["x" "y"]}
           (parsed-msg "foo x y")))
    (is (= {:cmd "foo"
            :args ["x"]}
           (parsed-msg "foo x")))
    (is (= {:cmd "foo"
            :args []}
           (parsed-msg "foo")))
    (is (= {:cmd "foo"
            :args ["x" "y" "z" "somereallylongthing"]}
           (parsed-msg "foo x y z somereallylongthing")))))

(deftest help-test
  (testing "that help messages are correctly formatted"
    (is (= '([] "Syntax: \"price [card name]\", where [card name] is the name of any card (or a close approximation). Returns the price of the first available printing of the card.") (help {:cmd "help" :args ["price"]})))
    (is (= '([] "Syntax: \"sum [list of card names]\", where [list of card names] is a list of card names in the format \"X card1 Y card2 Z card3 ...\" where X, Y, and Z are the quantities of card1, card2, and card3 respectively. Returns the sum of the prices of all cards in the list.") (help {:cmd "help" :args ["sum"]})))
    (is (= '([] "Syntax: \"diff [list of card names] & [list of card names]\", where [list of card names] is a list of card names in the format \"X card1 Y card2 Z card3 ...\" where X, Y, and Z, are the quantities of card1, card2, and card2 respectively. Returns the difference between the sum of the prices of the first list and the sum of the prices of the second list.") (help {:cmd "help" :args ["diff"]})))))

(deftest create-router-test
  (testing "correct creation of a function to lookup a handler for a parsed message"
    (let [router (create-router {"hello" #(str (:cmd %) " " "test")
                                 "argc"  #(count (:args %))
                                 "echo"  identity
                                 "default" (fn [& a] "No!")})
          msg1   {:cmd "hello"}
          msg2   {:cmd "argc" :args [1 2 3]}
          msg3   {:cmd "echo" :args ["a" "z"]}
          msg4   {:cmd "echo2" :args ["a" "z"]}]
      (is (= "hello test" ((router msg1) msg1)))
      (is (= "No!" ((router msg4) msg4)))
      (is (= 3 ((router msg2) msg2)))
      (is (= msg3 ((router msg3) msg3))))))


(deftest action-send-msg-test
  (testing "That action send msg returns a correctly formatted map"
    (is (= :send
           (:action (action-send-msg :bob "foo"))))
    (is (= :bob
           (:to (action-send-msg :bob "foo"))))
    (is (= "foo"
           (:msg (action-send-msg [:a :b] "foo"))))))


(deftest action-send-msgs-test
  (testing "That action send msgs generates a list of sends"
    (let [a (action-send-msg [:a :f :b] 1)
          b (action-send-msg [:a :f :d] 1)
          c (action-send-msg [:a :f :e] 1)
          d (action-send-msg [:a :f :c] 1)]
      (is (= [a b c d]
             (action-send-msgs [[:a :f :b]
                                [:a :f :d]
                                [:a :f :e]
                                [:a :f :c]]
                              1))))))

(deftest action-insert-test
  (testing "That action insert returns a correctly formatted map"
    (is (= #{:action :ks :v}
           (into #{}(keys (action-insert [:a :b] {:foo 1})))))
    (is (= #{:assoc-in [:a :b] {:foo 1}}
           (into #{}(vals (action-insert [:a :b] {:foo 1})))))
    (is (= :assoc-in
           (:action (action-insert [:a :b] {:foo 1}))))
    (is (= {:foo 1}
           (:v (action-insert [:a :b] {:foo 1}))))
    (is (= [:a :b]
           (:ks (action-insert [:a :b] {:foo 1}))))))


(deftest action-remove-test
  (testing "That action remove returns a correctly formatted map"
    (is (= #{:action :ks}
         (into #{} (keys (action-remove [:a :b])))))
    (is (= #{:dissoc-in [:a :b]}
          (into #{}(vals (action-remove [:a :b])))))
    (is (= :dissoc-in
           (:action (action-remove [:a :b]))))
    (is (= [:a :b]
           (:ks (action-remove [:a :b]))))))


(deftest action-inserts-test
  (testing "That action inserts generates a list of inserts"
    (let [a (action-insert [:a :f :b] 1)
          b (action-insert [:a :f :d] 1)
          c (action-insert [:a :f :e] 1)
          d (action-insert [:a :f :c] 1)]
      (is (= [a b c d]
             (action-inserts [:a :f] [:b :d :e :c] 1))))))


(defn action-send [system {:keys [to msg]}]
  (put! (:state-mgr system) [:msgs to] msg))

(defn pending-send-msgs [system to]
  (get! (:state-mgr system) [:msgs to]))

(def send-action-handlers
  {:send action-send})

(deftest price-test
  (testing "the price command"
    (is (= "Error: unknown card name" (second (price-fetch {:cmd "price", :args '(), :user-id nil}))))
    (is (= "Error: unknown card name" (second (price-fetch {:cmd "price", :args '("not" "a" "card" "name"), :user-id nil}))))
    (is (= "Shock (10E): $0.13" (second (price-fetch {:cmd "price", :args '("shock"), :user-id nil}))))
    (is (= "Storm Crow (6ED): $0.19" (second (price-fetch {:cmd "price", :args '("storm" "crow"), :user-id nil}))))))

(deftest sum-test
  (testing "the sum command"
    (is (= "Error: invalid list syntax" (second (sum-fetch {:cmd "sum", :args '(), :user-id nil}))))
    (is (= "Error: invalid list syntax or card name" (second (sum-fetch {:cmd "sum", :args '("not" "a" "card" "name"), :user-id nil}))))
    (is (= "Error: invalid list syntax or card name" (second (sum-fetch {:cmd "sum", :args '("1"), :user-id nil}))))
    (is (= "Error: invalid list syntax or card name" (second (sum-fetch {:cmd "sum", :args '("1" "1"), :user-id nil}))))
    (is (= "Error: invalid list syntax or card name" (second (sum-fetch {:cmd "sum", :args '("1" "not" "a" "card" "name"), :user-id nil}))))
    (is (= "Error: invalid list syntax or card name" (second (sum-fetch {:cmd "sum", :args '("not" "a" "card" "name"), :user-id nil}))))
    (is (= "Error: invalid list syntax or card name" (second (sum-fetch {:cmd "sum", :args '("storm" "crow"), :user-id nil}))))
    (is (= "Error: invalid list syntax or card name" (second (sum-fetch {:cmd "sum", :args '("shock"), :user-id nil}))))
    (is (= "Error: invalid list syntax or card name" (second (sum-fetch {:cmd "sum", :args '("1" "storm" "crow" "2"), :user-id nil}))))
    (is (= "Total price: $0.26" (second (sum-fetch {:cmd "sum", :args '("2" "shock"), :user-id nil}))))
    (is (= "Total price: $0.38" (second (sum-fetch {:cmd "sum", :args '("2" "storm" "crow"), :user-id nil}))))
    (is (= "Total price: $0.64" (second (sum-fetch {:cmd "sum", :args '("2" "shock" "2" "storm" "crow"), :user-id nil}))))
    (is (= "Total price: $3.20" (second (sum-fetch {:cmd "sum", :args '("10" "shock" "10" "storm" "crow"), :user-id nil}))))))


(deftest diff-test
  (testing "the diff command"
    (is (= "Error: invalid diff syntax" (second (diff-fetch {:cmd "diff", :args '(), :user-id nil}))))
    (is (= "Error: invalid diff syntax" (second (diff-fetch {:cmd "diff", :args '("1"), :user-id nil}))))
    (is (= "Error: invalid diff syntax" (second (diff-fetch {:cmd "diff", :args '("storm" "crow"), :user-id nil}))))
    (is (= "Error: invalid diff syntax" (second (diff-fetch {:cmd "diff", :args '("1" "storm" "crow"), :user-id nil}))))
    (is (= "Error: invalid diff syntax" (second (diff-fetch {:cmd "diff", :args '("2" "storm" "crow" "2" "shock"), :user-id nil}))))
    (is (= "Error: invalid diff syntax" (second (diff-fetch {:cmd "diff", :args '("2" "storm" "crow" "&"), :user-id nil}))))
    (is (= "Error: invalid diff syntax" (second (diff-fetch {:cmd "diff", :args '("2" "storm" "crow" "&" "2" "shock" "&" "2" "ponder"), :user-id nil}))))
    (is (= "Error: invalid list syntax or card name" (second (diff-fetch {:cmd "diff", :args '("2" "storm" "crow" "&" "2"), :user-id nil}))))
    (is (= "Error: invalid list syntax or card name" (second (diff-fetch {:cmd "diff", :args '("2" "storm" "crow" "&" "2" "not" "a" "card"), :user-id nil}))))
    (is (= "Price difference: $0.00" (second (diff-fetch {:cmd "diff", :args '("2" "storm" "crow" "&" "2" "storm" "crow"), :user-id nil}))))
    (is (= "Price difference: $0.12" (second (diff-fetch {:cmd "diff", :args '("2" "storm" "crow" "&" "2" "shock"), :user-id nil}))))
    (is (= "Price difference: -$0.12" (second (diff-fetch {:cmd "diff", :args '("2" "shock" "&" "2" "storm" "crow"), :user-id nil}))))))
