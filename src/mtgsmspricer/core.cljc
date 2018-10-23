(ns mtgsmspricer.core
  (:require [clojure.string :as string]
            [clojure.core.async :as async :refer [go chan <! >!]]
            #?(:clj [clj-http.client :as client]
               :cljs [cljs-http.client :as jsclient])
            [clojure.data.json :as json]
            [mtgsmspricer.kvstore :as kvstore
             :refer [put! get! list! remove!]]))

;; words -- a helper function that splits a string into a list
;; delimited by spaces
(defn words [msg]
  (if msg
      (string/split msg #" ")
      []))

;; cmd - returns the first word in a text message.
(defn cmd [msg]
  (first
    (words msg)))



;; args -- returns the list of words following
;; the command in a text message.
(defn args [msg]
  (rest
    (words msg)))


;; parsed-msg -- returns a map with keys for the
;; :cmd and :args parsed from the msg.
(defn parsed-msg [msg]
  (zipmap [:cmd, :args]
    (list (cmd msg) (args msg))))

;; help -- provides info about available commands and their proper
;; usage
(defn help [& pmsg]
  (list
    []
    (let [arg (first (:args (first pmsg)))]
      (cond
        (= arg "price") "Syntax: \"price [card name]\", where [card name] is the name of any card (or a close approximation). Returns the price of the first available printing of the card."
        (= arg "sum") "Syntax: \"sum [list of card names]\", where [list of card names] is a list of card names in the format \"X card1 Y card2 Z card3 ...\" where X, Y, and Z are the quantities of card1, card2, and card3 respectively. Returns the sum of the prices of all cards in the list."
        (= arg "diff") "Syntax: \"diff [list of card names] & [list of card names]\", where [list of card names] is a list of card names in the format \"X card1 Y card2 Z card3 ...\" where X, Y, and Z, are the quantities of card1, card2, and card2 respectively. Returns the difference between the sum of the prices of the first list and the sum of the prices of the second list."
        :else "Potential commands are:

        * \"price [card name]\"
        * \"sum [list of card names]\"
        * \"diff [list of card names] & [list of card names]\"

        For detailed command info, enter \"help [command name]\""))))


;; Asgn 2.
;;
;; @Todo: Create a function called action-send-msg that takes
;; a destination for the msg in a parameter called `to`
;; and the message in a parameter called `msg` and returns
;; a map with the keys :to and :msg bound to each parameter.
;; The map should also have the key :action bound to the value
;; :send.
;;
(defn action-send-msg [to msg]
  (zipmap [:to :msg :action] [to msg :send]))

;; Asgn 2.
;;
;; @Todo: Create a function called action-send-msgs that takes
;; takes a list of people to receive a message in a `people`
;; parameter and a message to send them in a `msg` parmaeter
;; and returns a list produced by invoking the above `action-send-msg`
;; function on each person in the people list.
;;
;; java-like pseudo code:
;;
;; output = new list
;; for person in people:
;;   output.add( action-send-msg(person, msg) )
;; return output
;;
(defn action-send-msgs [people msg]
  (map #(action-send-msg % msg) people))

;; Asgn 2.
;;
;; @Todo: Create a function called action-insert that takes
;; a list of keys in a `ks` parameter, a value to bind to that
;; key path to in a `v` parameter, and returns a map with
;; the key :ks bound to the `ks` parameter value and the key :v
;; vound to the `v` parameter value.)
;; The map should also have the key :action bound to the value
;; :assoc-in.
;;
(defn action-insert [ks v]
  (zipmap [:action :ks :v] [:assoc-in ks v]))

;; Asgn 2.
;;
;; @Todo: Create a function called action-inserts that takes:
;; 1. a key prefix (e.g., [:a :b])
;; 2. a list of suffixes for the key (e.g., [:c :d])
;; 3. a value to bind
;;
;; and calls (action-insert combined-key value) for each possible
;; combined-key that can be produced by appending one of the suffixes
;; to the prefix.
;;
;; In other words, this invocation:
;;
;; (action-inserts [:foo :bar] [:a :b :c] 32)
;;
;; would be equivalent to this:
;;
;; [(action-insert [:foo :bar :a] 32)
;;  (action-insert [:foo :bar :b] 32)
;;  (action-insert [:foo :bar :c] 32)]
;;
(defn action-inserts [prefix ks v]
  (map #(action-insert (conj prefix %) v) ks))

;; Asgn 2.
;;
;; @Todo: Create a function called action-remove that takes
;; a list of keys in a `ks` parameter and returns a map with
;; the key :ks bound to the `ks` parameter value.
;; The map should also have the key :action bound to the value
;; :dissoc-in.
;;
(defn action-remove [ks]
  (zipmap [:action :ks] [:dissoc-in ks]))

;; parse-response -- parses the JSON response from the Scryfall
;; API into a clojure map object
(defn parse-response [response]
  (json/read-str (:body response) :key-fn keyword))

;; http-wrap -- wraps the http requests in a reader conditional
;; so that it works with the java backend and the node backend
(defn http-wrap [uri params]
  #?(:clj (client/get uri params)
     :cljs (go
            (let [response (<! (jsclient/get uri params))]
              response))))


;; set-traverse -- recursively traverses a given list of
;; card printings in JSON format and finds the first one that
;; has a USD price listed
(defn set-traverse [set_list]
  (if (empty? set_list)
    nil
    (if (contains? (first set_list) :usd)
      (first set_list)
      (set-traverse (rest set_list)))))

;; grab-proper-printing -- finds an appropriate printing
;; of a card from Scryfall (i.e. one that has a USD price listed)
;; Takes a parsed card name as input, and returns a map of the
;; JSON object of the printing it finds
(defn grab-proper-printing [name]
  (try
    ;; first find the list of all printings of the card
    (let [prints_uri
          (:prints_search_uri
            (parse-response
              (http-wrap "https://api.scryfall.com/cards/named"
                {:query-params {:fuzzy [name]}})))]
      ;; then find one with a valid USD price listed
      (let [data_list
            (:data
               (parse-response
                 (http-wrap prints_uri {:debug false})))]
        (set-traverse data_list)))
    (catch Exception e nil)))

;; format-output -- given a card object as a parsed JSON, extracts
;; and formats the appropriate information to print for a price command
(defn format-price-output [card-obj]
  (str (:name card-obj) " (" (string/upper-case (:set card-obj)) "): $" (:usd card-obj)))

;; price-fetch -- given a set of arguments that form
;; a card name or roughly form a card name, responds
;; with the price of the first available printing
;; of the card on Scryfall that has a price in USD
;; and the set code for the set that it is from
(defn price-fetch [{:keys [args user-id]}]
  (list
   []
   (let [printing
         (grab-proper-printing
           (string/join " " args))]
    (if (nil? printing)
      "Error: unknown card name"
      (format-price-output printing)))))

;; find-card-name -- given a list of cards and associated
;; quantities, finds the first card name in the list
(defn find-card-name [list]
  (if (or (empty? list) (number? (read-string (first list))))
    '()
    (cons (first list) (find-card-name (rest list)))))

;; find-rest-cards -- given a list of cards and associated
;; quantities, finds the next card in the list that includes
;; a quantity as well as the rest of the list
(defn find-rest-cards [list]
  (if (or (empty? list) (number? (read-string (first list))))
    list
    (find-rest-cards (rest list))))

;; sum-list-parse -- recursively parses input to sum-fetch
(defn sum-list-parse [list]
  (if (empty? list)
    0
    (if (or (empty? (rest list)) (not (number? (read-string (first list)))))
      "Error: invalid list syntax or card name"
      ;; if the list is not empty, sum the product of the
      ;; current quantity and price with the rest of the
      ;; products of quantity and price
      (let [card (find-card-name (rest list))]
        (if (empty? card)
          "Error: invalid list syntax or card name"
          ;; call the Scryfall API to find the price of
          ;; a particular printing of the card
          (let [printing (grab-proper-printing (string/join " " card))]
            (if (nil? printing)
              "Error: invalid list syntax or card name"
              ;; build a recursive sum
              (+
                ;; multiply the price by the desired quantity
                (*
                 (read-string (first list))
                 (read-string (:usd printing)))
                ;; sum the rest of the cards
                (sum-list-parse (find-rest-cards (rest list)))))))))))

;; sum-fetch -- given a set of arguments that form
;; a list of numbers and associated card names, responds
;; with the sum of the prices of the cards listed
(defn sum-fetch [{:keys [args user-id]}]
  (list
   []
   (if (empty? args)
     "Error: invalid list syntax"
     (let [result (sum-list-parse args)]
       (if (string? result)
         result
         (str "Total price is: $" (format "%.2f" result)))))))

;; abs -- helper function to find absolute value of a number
(defn abs [n] (if (neg? n) (- n) n))

;; list-split -- helper function to split a string into a vector
;; of the components delimited by whitespace and then turn it
;; into a list
(defn list-split [str]
  (apply list (string/split str #" ")))

;; diff-parse -- parses the input to the diff command. Joins the
;; arguments and splits the string into a list of 2 card list strings
;; based on the & token.
(defn diff-parse [arglist]
  (map
    #(string/trim %)
     (apply list
       (string/split (string/join " " arglist) #"&"))))

;; diff-format -- helper function to properly format the result
;; of the diff command.
(defn diff-format [result]
  (if (neg? result)
    (str "Price difference is -$" (format "%.2f" (abs result)))
    (str "Price difference is $" (format "%.2f" result))))

;; diff-fetch -- given a set of arguments that form
;; two lists of numbers and associated card names, responds
;; with the difference between the two sums of the card list prices.
(defn diff-fetch [{:keys [args user-id]}]
  (list
   []
   (let [parsed-lists (diff-parse args)]
    (if (= (count parsed-lists) 2)
      (let [res1 (sum-list-parse (list-split (first parsed-lists)))
            res2 (sum-list-parse (list-split (second parsed-lists)))]
        (if (and (number? res1) (number? res2))
          (diff-format (- res1 res2))
          "Error: invalid list syntax or card name"))
      "Error: invalid diff syntax"))))


;; Don't edit!
(defn stateless [f]
  (fn [_ & args]
    [[] (apply f args)]))


(def routes {"default"  (stateless (fn [& args] "Unknown command. Text \"help\" for commands."))
             "help"     #(help %)
             "price"    #(price-fetch %)
             "sum"      #(sum-fetch %)
             "diff"     #(diff-fetch %)})

;; Don't edit!
(defn experts-on-topic-query [state-mgr pmsg]
  (let [[topic]  (:args pmsg)]
    (list! state-mgr [:expert topic])))


;; Don't edit!
(defn conversations-for-user-query [state-mgr pmsg]
  (let [user-id (:user-id pmsg)]
    (get! state-mgr [:conversations user-id])))


;; Don't edit!
(def queries
  {"expert" experts-on-topic-query
   "ask"    experts-on-topic-query
   "answer" conversations-for-user-query})


;; Don't edit!
(defn read-state [state-mgr pmsg]
  (go
    (if-let [qfn (get queries (:cmd pmsg))]
      (<! (qfn state-mgr pmsg))
      {})))



;; Asgn 1.
;;
;; @Todo: This function should return a function (<== pay attention to the
;; return type) that takes a parsed message as input and returns the
;; function in the `routes` map that is associated with a key matching
;; the `:cmd` in the parsed message. The returned function would return
;; `welcome` if invoked with `{:cmd "welcome"}`.
;;
;; Example:
;;
;; (let [msg {:cmd "welcome" :args ["bob"]}]
;;   (((create-router {"welcome" welcome}) msg) msg) => "Welcome bob"
;;
;; If there isn't a function in the routes map that is mapped to a
;; corresponding key for the command, you should return the function
;; mapped to the key "default".
;;
;; See the create-router-test in test/mtgsmspricer/core_test.clj for the
;; complete specification.
;;
(defn create-router [routes]
  (fn [parsed-msg]
    (if (contains? routes (get parsed-msg :cmd))
      (get routes (get parsed-msg :cmd))
      (get routes "default"))))

;; Don't edit!
(defn output [o]
  (second o))


;; Don't edit!
(defn actions [o]
  (first o))


;; Don't edit!
(defn invoke [{:keys [effect-handlers] :as system} e]
  (go
    (println "    Invoke:" e)
    (if-let [action (get effect-handlers (:action e))]
      (do
        (println "    Invoking:" action "with" e)
        (<! (action system e))))))


;; Don't edit!
(defn process-actions [system actions]
  (go
    (println "  Processing actions:" actions)
    (let [results (atom [])]
      (doseq [action actions]
        (let [result (<! (invoke system action))]
          (swap! results conj result)))
      @results)))


;; Don't edit!
(defn handle-message
  "
    This function orchestrates the processing of incoming messages
    and glues all of the pieces of the processing pipeline together.

    The basic flow to handle a message is as follows:

    1. Create the router that will be used later to find the
       function to handle the message
    2. Parse the message
    3. Load any saved state that is going to be needed to process
       the message (e.g., querying the list of experts, etc.)
    4. Find the function that can handle the message
    5. Call the handler function with the state from #3 and
       the message
    6. Run the different actions that the handler returned...these actions
       will be bound to different implementations depending on the environemnt
       (e.g., in test, the actions aren't going to send real text messages)
    7. Return the string response to the message

  "
  [{:keys [state-mgr] :as system} src msg]
  (go
    (println "=========================================")
    (println "  Processing:\"" msg "\" from" src)
    (let [rtr    (create-router routes)
          _      (println "  Router:" rtr)
          pmsg   (assoc (parsed-msg msg) :user-id src)
          _      (println "  Parsed msg:" pmsg)
          state  (<! (read-state state-mgr pmsg))
          _      (println "  Read state:" state)
          hdlr   (rtr pmsg)
          _      (println "  Hdlr:" hdlr)
          [as o] (hdlr pmsg)
          _      (println "  Hdlr result:" [as o])
          arslt  (<! (process-actions system as))
          _      (println "  Action results:" arslt)]
      (println "=========================================")
      o)))
