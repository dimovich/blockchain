(ns blockchain.core
  (:require
   [reagent.core :as r]
   [dommy.core :refer [sel1]]
   [cljs-web3.core :as web3-core]
   [cljs-web3.eth :as web3-eth])

  (:require-macros
   [blockchain.macros :refer [load-json]]))



(def abi (->> (load-json "abi.json")
              (.parse js/JSON)))

(def address "0x63a6b245ae31aa77f6173c5300d0fde97ad17585")

(def web3* (atom nil))


(defn get-web3 []
  (let [web3-inst (goog.object/get js/window "Web3")
        web3 (goog.object/get js/window "web3")]
    (when (and web3-inst web3)
      (web3-inst. (web3-core/current-provider web3)))))



(defn init [state]
  (let [contract (web3-eth/contract-at @web3* abi address)
        get-vote-count (fn [props]
                         (doseq [prop props]
                           (.. contract -getProposalVoteCount
                               (call prop #(swap! state assoc-in [:proposals prop] %2)))))]
    
    (swap! state assoc :contract contract)
    (doto contract
      (.. -name    (call #(swap! state assoc :name %2)))
      (.. -website (call #(swap! state assoc :website %2)))
      (.. -logo    (call #(swap! state assoc :logo %2)))
      (.. -getProposals (call #(let [props (js->clj %2)]
                                 (get-vote-count props)))))))



(defn voting [state]
  (let [{:keys [logo name website proposals contract]} @state]
    [:div
     [:img {:src logo}]
     [:h1 name]
     [:h2 website]
   
     [:ul
      (doall
       (for [[prop votes] proposals]
         ^{:key prop}
         [:li
          [:em {:style {:margin "1em"}}
           (.toString votes 10)]
        
          [:button {:on-click #(.. contract (vote prop identity))}
           (.toUtf8 @web3* prop)]]))]]))


(defn reload []
  (let [state (r/atom nil)]
    (init state)
    (r/render [voting state] (sel1 :#app))))


;; check if web3 is present and save instance
(defn checker [t]
  (let [web3 (get-web3)]
    (if (and web3 (web3-eth/default-account web3))
      (do
        (reset! web3* web3)
        (reload))
      (when (pos? t)
        (js/setTimeout #(checker (dec t)) 300)))))



(defn ^:export main []
  (checker 20))

