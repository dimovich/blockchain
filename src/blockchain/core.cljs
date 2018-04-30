(ns blockchain.core
  (:require
   [reagent.core :as r]
   [dommy.core :refer [sel1]]
   [cljs-web3.core :as web3-core]
   [cljs-web3.eth :as web3-eth])

  (:require-macros
   [blockchain.macros :refer [load-json]]))



(defonce abi (->> (load-json "abi.json")
                  (.parse js/JSON)))

(defonce address "0x63a6b245ae31aa77f6173c5300d0fde97ad17585")

(defonce web3* (atom nil))


(defn get-web3 []
  (let [web3-inst (goog.object/get js/window "Web3")
        web3 (goog.object/get js/window "web3")]
    (when (and web3-inst web3)
      (web3-inst. (web3-core/current-provider web3)))))



(defn init [state]
  (when-let [web3 @web3*]
    (let [info-keys [:name :website :logo]
          contract (web3-eth/contract-at web3 abi address)
          get-vote-count (fn [props]
                           (doseq [prop props]
                             (web3-eth/contract-call
                              contract :get-proposal-vote-count
                              prop #(swap! state assoc-in [:proposals prop] %2))))]
    
      (swap! state assoc :contract contract)

      (doseq [k info-keys]
        (web3-eth/contract-call
         contract k #(swap! state assoc k %2)))

      (web3-eth/contract-call contract :get-proposals
                              #(let [props (js->clj %2)]
                                 (get-vote-count props))))))



(defn voting [state]
  (r/with-let [to-utf8 (goog.object/get @web3* "toUtf8")]
    (let [{:keys [logo name website proposals contract]} @state]
      [:div
       [:img {:src logo}]
       [:h1 name]
   
       [:ul
        (doall
         (for [[prop votes] proposals]
           ^{:key prop}
           [:li
            [:em {:style {:margin "1em"}}
             (.toString votes 10)]
        
            [:button {:on-click #(web3-eth/contract-call
                                  contract :vote prop identity)}
             (to-utf8 prop)]]))]])))



(defn reload []
  (let [state (r/atom nil)]
    (init state)
    (r/render [voting state] (sel1 :#app))))



;; check if MetaMask web3 is present and save instance
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

