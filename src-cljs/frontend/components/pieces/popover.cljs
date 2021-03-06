(ns frontend.components.pieces.popover
  (:require [clojure.string :as string]
            [devcards.core :as dc :refer-macros [defcard defcard-doc]]
            [frontend.components.pieces.button :as button]
            [frontend.components.pieces.icon :as icon]
            [frontend.utils :refer-macros [component html]]
            [goog.string :as gstring]
            [om.dom :as om-dom]
            [om.next :as om-next :refer-macros [defui]]))

(defn- with-positioned-element
  "A component responsible for ensuring an element is positioned
   appropriately relative to its anchor element(s).

  children
   The anchor element(s) to position against.

  :placement
   The position of the element relative to the anchor element.
   Can be one of [:top :bottom :left :right].
   (default: :top)

  :element
   The element to position against the anchor."
  [{:keys [placement element]} children]
  (component
   (html
    [:div
     [:.positioned-element
      {:class (name placement)}
      element]
     children])))

(defn- card
  "A popover card component that draws a static popover card that
   can transition itself in and out on visibility change.

  :title (optional)
   The popover title.

  :body
   The content to display in the popover.

  :placement
   The position of the popover relative to the trigger element.
   Can be one of [:top :bottom :left :right].
   (default: :top)

  :visible?
   Popover card transitions in when changed from false to true,
    and transitions out when changed from true to false.
   (default: :false)"
  [{:keys [title body placement visible?]
    :or {placement :top visible? false}}]
  (component
   (js/React.createElement
    js/React.addons.CSSTransitionGroup
    #js {:transitionName "transition"
         :transitionEnterTimeout 200
         :transitionLeaveTimeout 200}
    (when visible?
      (html
       [:.cci-popover
        {:class (name placement)}
        [:.popover-inner
         [:.content
          (when title
            [:.title
             [:span title]])
          [:.body
           body]]]])))))

(defn- handle-document-click [popover event]
  (let [click-target (.-target event)
        popover-node (om-dom/node popover)
        popover-clicked? (.contains popover-node click-target)
        visible? (-> popover om-next/get-state :visible-by-click?)]
    (when (and visible?
               (not popover-clicked?))
      (om-next/update-state! popover assoc :visible-by-click? false))))

(defui ^:once Popover
  "A popover component that attaches a popover to the specified
   trigger element(s), passed in as children.

  children
   The trigger element(s) to attach the popover to.

  :title (optional)
   The popover title.

  :body
   The content to display in the popover.

  :placement
   The position of the popover relative to the trigger element.
   Can be one of [:top :bottom :left :right].
   (default: :top)

  :trigger-mode
   The type of events on the trigger element that affect the
    visibility of the popover.
   A popover in hover mode will also respond to clicks, but not
    vice versa. This enables touch interactions for a popover in
    hover mode and allows users to pin it in place.
   Can be one of [:click :hover].
   (default: :click)

  :on-show (optional)
   Function to be triggered when popover is shown.
   Can be used to pass in an event-tracking function.

  :on-hide (optional)
   Function to be triggered when popover is hidden.
   Can be used to pass in an event-tracking function."

  Object
  (initLocalState [_]
    {:visible-by-click? false
     :visible-by-hover? false})
  (componentDidMount [this]
    (let [document-click-handler #(handle-document-click this %)]
      (set! (.-documentClickHandler this) document-click-handler)
      (js/document.addEventListener
       "click"
       document-click-handler
       true)))
  (componentDidUpdate [this prev-props prev-state]
    (let [{:keys [on-show on-hide]} (om-next/props this)
          current-state (om-next/get-state this)
          visible? (or (:visible-by-click? current-state)
                       (:visible-by-hover? current-state))
          prev-visible? (or (:visible-by-click? prev-state)
                            (:visible-by-hover? prev-state))
          visibility-changed? (not= visible? prev-visible?)]
      (when visibility-changed?
        (if visible?
          (when on-show (on-show))
          (when on-hide (on-hide))))
      ;; workaround for Safari iOS not firing click events
      ;; reference: https://github.com/kentor/react-click-outside/issues/4#issuecomment-266644870
      (when (exists? js/document.documentElement.ontouchstart)
        (when (not= (:visible-by-click? current-state)
                    (:visible-by-click? prev-state))
          (if visible?
            (set! (.-cursor document.body.style) "pointer")
            (set! (.-cursor document.body.style) nil))))))
  (componentWillUnmount [this]
    (js/document.removeEventListener
     "click"
     (.-documentClickHandler this)
     true))
  (render [this]
    (component
     (let [{:keys [title body placement trigger-mode]
            :or {placement :top trigger-mode :click}}
           (om-next/props this)

           {:keys [visible-by-click? visible-by-hover?]}
           (om-next/get-state this)

           hover-props
           {:on-mouse-enter
            #(om-next/update-state! this assoc :visible-by-hover? true)
            :on-mouse-leave
            #(om-next/update-state! this assoc :visible-by-hover? false)}

           click-props
           {:on-click
            #(om-next/update-state! this update :visible-by-click? not)}]
       (html
        [:div (merge click-props
                     (when (= :hover trigger-mode)
                       hover-props))
         (with-positioned-element
          {:placement placement
           :element (card {:title title
                           :body body
                           :placement placement
                           :visible? (or visible-by-click?
                                         visible-by-hover?)})}
          (om-next/children this))])))))

(def popover (om-next/factory Popover))

(defn tooltip
  "A tooltip component that attaches a tooltip to the specified
   trigger element(s), passed in as children.

  children
   The trigger element(s) to attach the tooltip to.

  :body
   The content to display in the tooltip.

  :placement
   The position of the tooltip relative to the trigger element.
   Can be one of [:top :bottom :left :right].
   (default: :top)

  :on-show (optional)
   Function to be triggered when tooltip is shown.
   Can be used to pass in an event-tracking function.

  :on-hide (optional)
   Function to be triggered when tooltip is hidden.
   Can be used to pass in an event-tracking function."
  [{:keys [body placement on-show on-hide]
    :or {placement :top}}
   children]
  (popover {:title nil
            :body body
            :placement placement
            :on-show on-show
            :on-hide on-hide
            :trigger-mode :hover}
           children))

(dc/do
  (defcard-doc
    "Simple tooltips and more complex popovers for displaying extended
    information for things.

    A simple popup provides extra information or operations on hover, focus
    or click. Compared with tooltips, popovers can provide action elements
    like links and buttons.

    The tooltip shows on mouse enter and hides on mouse leave and doesn’t support
    complex text. The default behavior in hover mode is to respond to clicks as
    well. If you click, it \"pins\" the tooltip. Clicking again or clicking out
    dismisses it, allowing tooltips to work on touch devices as well.")

  (def trigger
    (html
     [:div {:style {:width 32}}
      (icon/settings)]))

  (defn vary-placements
    [component props]
    (for [placement [:top :left :right :bottom]
          :let [body (gstring/format
                          "%s %s"
                          (if-let [trigger-mode (:trigger-mode props)]
                            (-> trigger-mode
                                name
                                string/capitalize)
                            "Hover")
                          (name placement))]]
      (component (assoc props
                        :placement placement
                        :body body)
                 trigger)))

  (defcard tooltip-and-popover-card
    (fn [state]
      (html
       [:div {:style {:display "flex"
                      :justify-content "space-around"
                      :padding-left 150
                      :padding-right 150
                      :padding-top 100
                      :padding-bottom 200}}
        (with-positioned-element
         {:placement :left
          :element (card {:body "Without title"
                          :placement :left
                          :trigger-mode :click
                          :visible? true})}
         trigger)
        (with-positioned-element
         {:placement :top
          :element (card {:title "With title"
                          :body "Content"
                          :placement :top
                          :trigger-mode :click
                          :visible? true})}
         trigger)
        (with-positioned-element
         {:placement :bottom
          :element (card {:title "With multi-line content and multi-line title"
                          :body "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                          :placement :bottom
                          :trigger-mode :click
                          :visible? true})}
         trigger)
        (with-positioned-element
         {:placement :right
          :element (card {:title "With rich content"
                          :body
                          (html
                            [:div
                             [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."]
                             [:p [:a {:href "#"} "Click here!"]]
                             [:p (button/button {:kind :primary} "Do Something")]])
                          :placement :right
                          :trigger-mode :click
                          :visible? true})}
         trigger)])))
  (let [card-style {:style {:display "flex"
                            :justify-content "space-between"
                            :padding 50}}]
    (defcard tooltip
      (fn [state]
        (html
         [:div card-style
          (vary-placements tooltip
                           {:body "Content"})])))

    (defcard popover-click
      (fn [state]
        (html
         [:div card-style
          (vary-placements popover
                           {:title "Title"
                            :body "Content"
                            :trigger-mode :click})])))

    (defcard popover-hover
      (fn [state]
        (html
         [:div card-style
          (vary-placements popover
                           {:title "Title"
                            :body "Content"
                            :trigger-mode :hover})])))

    (defcard popover-callbacks
      (fn [state]
        (html
         [:div card-style
          (popover {:title "Callback fired!"
                    :body "Check your console"
                    :trigger-mode :hover
                    :on-show #(println "popover shown")
                    :on-hide #(println "popover hidden")}
                   trigger)])))))
