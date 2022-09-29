# Devlog

## 28th and 29th September
### Fixing bug: Units don't have targets
When a unit move to a square adjacent to an enemy unit, the enemy unit should show up as a potential target.
Right now it doesn't.

The code for handling attack options is in the `inputs` namespace, in `add-attack-option`

```clojure
(defn add-attack-option
  "After a move has been done, calculate whether any enemy
   units are in adjacent locations (and so are attackable)"
  [game-state side unit-id unit-loc]
  (let [attacking-unit (get-in game-state [side :units unit-id])
        targets (intersection (occupied-grids game-state (other-side side)) (:viewsheds game-state) (manhattan unit-loc 1))]
    (if (or (empty? targets) (#{:general} (:unit-type attacking-unit)))
      (assoc game-state :attack-option :no-targets)
      (assoc game-state :attack-option [side unit-id unit-loc targets]))))
```

The 'targets' in the let binding are the important thing here.
The binding is calculating the intersection between three sets of coordinates:
1. the 'viewsheds'
2. the squares adjacent to the relevant unit
3. the viewshed at the top level of gamestate

After playing at the repl, the problem is that 'viewsheds' is not being populated:

``` clojure
(def game-state @ui/debug)
(forces/occupied-grids game-state :blue)
;; => #{[13 8] [9 4]}
(u/manhattan [8 4] 1)
;; => #{[8 4] [7 4] [8 3] [8 5] [9 4]}
(:viewsheds game-state)
;; => nil
```

I actually don't recall how viewsheds is supposed to be populated. 
So have to find that out.

...

OK, so `add-attack-option` suggests viewshed is stored at the root of the game state, but I don't think that's actually what happens.
It looks like it's stored as an attribute of the _unit_.
Which does make more sense.

```clojure
(get-in game-state [:red :units "dummy-id" :viewshed])
;; => #{[8 8] [7 6] [8 7] [9 8] etc.
```

So I think just changing `add-attack-option` to look at the unit's viewshed is the fix.

...

Fixed! Easy one.

### Sightings
I think I partially worked on this.
There's an import for a 'sightings' namespace that doesn't exist.

Anyway, the idea is that each unit has its own record of what it has seen and when.
So if a unit sees an enemy (meaning it's come into their field of view), and the enemy _leaves_ it's field of view, the unit 'remembers' where it last saw the unit and when.

On the UI, the 'shadow' of the unit would be displayed, along with how recently it had been seen.

I think the way to do it is like this:

At the end of each tick, a unit 'scans' it's surroundings for any units and calculates the 'units in view'.
(This is just an intersection of the unit's viewshed and occupied squares.)

It stores this 'intel' record, and if, on the subsequent tick, the unit is still in view, then it updates the record.
If it's _not_ still in view, then it 'ages' the record.
What age is, in this case, I'm not completely sure yet.

```clojure
;; intel
{"unit-id" {:location [1 2] :age 10}
 "unit-id" {:location [2 2] :age 0}}
```

So specifically:

```
get-units-in-fov-of unit game-state :: [unit]
update-intel unit [unit] :: unit 
```

The first thing I think I need is a function which gets all units in a set of coordinates.
I already have a `unit-at-location`, but I think I need one which is more general.
Maybe even replace that `unit-at-location` altogether.

It's pretty simple I think.
I took the opportunity to do some fn-speccing too.
(Also, some tests)

```clojure
(defn units-at-locations [game-state locations]
  (select-keys (units-by-location game-state) locations))

(spec/fdef units-at-locations
  :args (spec/cat :game-state map?
                  :locations (spec/coll-of :general-slim.specs/coord))
  :ret map?)
```

In a new namespace `intel` I made

```
units-in-fov :: game-state, unit -> [unit]
age-intel :: intel -> intel
update-intelligence :: old-intel, [unit], new-intel
update-unit-intel :: game-state, unit-id -> game-state
```

Only the last of these is public.
The 'intel' spec is new:

```clojure
(s/def ::intel-report
  (s/keys :req-un [::id ::position ::age ::side]))

(spec/nilable (spec/map-of :general-slim.specs/id :general-slim.specs/intel-report))
```

Now, where do I put this in the actual game?
Long time since I looked at this, I assume there's a loop that iterates through every unit...

...

So top level tick looks like this:

```clojure
(defn tick [game-state]
  (if (or (:current-order game-state) (not-empty (:order-queue game-state)))
    (update (inputs/handle-input game-state) :ticks (fnil inc 0))
    (update game-state :ticks (fnil inc 0))))

(defn handle-input [game-state]
  (cond (:current-order game-state)
        (let [[order-type side unit target] (:current-order game-state)]
          (case order-type
            :move (execute-move-order game-state order-type side unit target)
            :retreat (execute-move-order game-state order-type side unit target)
            :end-turn (end-turn game-state side)
            :attack (execute-attack-order game-state side unit target)))
        (not-empty (:order-queue game-state))
        (-> game-state
            (assoc :current-order (first (:order-queue game-state)))
            (update :order-queue rest))

        :else (do (println "Erroneous input handle")
                  game-state)))
```

The 'current-order' and 'order-queue' ... I don't remember what they are.
I should spec that.
According to the `let`, an order is a tuple of `type`, `side`, `id`, `target`.
Type can be move, retreat, end-turn, or attack.
`target` has different meanings in different contexts, but I think it's always a sequence of coords. 

Anyway, I think the right place to put the intel updates is in `execute-move-order` - it already has an `update-viewshed` call in it.

```clojure
(-> game-state
    (assoc-in [side :units unit-id :position] (first route))
    (update-in [side :units unit-id :move-points] - move-cost)
    (vs/update-viewshed unit-id)
    (intel/update-unit-intel unit-id) ;; new
    (update-move-order))
```

There's a major problem with this though, and that is that unit intel is only updated when the unit moves _not when other units move_.
So if another unit moves out of view, the intel won't be displayed.
I think this will be OK for now, but lets see.

... 

This didn't cause any game crashes, which is good.
I want to put the 'intel' in the debug window.
This was trivial, putting a new item in `draw-debug-window` in the ui ns.

The problem with this implementation is that no units start with any intel.
I think I can fix this in the setup function maybe?
OK, not in there.
There must be a place where viewsheds are added for the first time, that's probably the right way to do it.

OK, so there is 

```clojure
(def game-state
  (-> (load-scenario "aw_ft1")
      (assoc :camera [0 0])
      (vs/add-viewshed-to-units :red)
      (vs/add-viewshed-to-units :blue)))
```

But this doesn't work great with how intel updating works.
I added a utility function in intel 

```clojure
(defn update-all-unit-intel [game-state]
  (reduce update-unit-intel
          game-state
          (map :id (forces/units game-state))))
```

Then just added it to the game-state def.
That worked fine.

Next to work on the UI implementation.
I think it's a new UI layer.
One of the interesting things here is that you can have different units with different intelligence.
So unit A could've seen enemy X 2 turns ago at [1 1], but unit B could've seen enemy X _1_ turn ago at [2 2].

Ultimately, I want friendly FOW in here, which implies that the intel you see should only be the _generals_ intel (including reports from your subordinates.)
But for now I'll just assume that 'you' know as soon as your subordinates do, and you filter the latest.

I think the implementation of updating info when moving will cause some issues here, because age is relative to how recently a unit moved. So if a unit hasn't moved, it won't update the intel, or the _age_ of the intel.
There are two things I can think of here, not mutually exclusive:

* Call update-intel on tick rather than on move. This would also solve the problem I mention above about intel not updating on enemy moves. It's probably quite expensive though.
* store the _turn_ (or tick?) on which the sighting was made, and calculate the age dynamically from that.

I did the first, which is pretty simple: just add `intel/update-all-unit-intel` into the tick fn.
But then it has this age counter which incremnts the age every tick (30 times per second), which I don't love.

If I put the tick in the intel report _once_, instead of putting age and then incrementing, what does that do?
It makes it harder to see if something is current I think?
Does that matter?

So I've got

```clojure
(defn- update-intelligence [old-intel units]
  (merge
   (when old-intel (age-intel old-intel))
   (update-vals (->> units
                     (map #(vector (:id %) (select-keys % [:position :id :side])))
                     (into {}))
                #(assoc % :age 0))))
```

If I put 'tick' in there instead of age, and removed the age-intel, the merge would still take care of overwrites.
But tick is advancing all the time, so you can't tell if the unit is still in view or not.
I could add a 'in-fov' key to the new intel, and then replace 'age-intel' with 'remove-current' or something. I think that might work.

```clojure
(defn- update-intelligence [old-intel units sight-time]
  (merge
   (when old-intel (update-vals old-intel #(assoc % :is-current false)))
   (update-vals (->> units
                     (map #(vector (:id %) (select-keys % [:position :id :side])))
                     (into {}))
                #(assoc % :sight-time sight-time
                        :is-current true))))
```

This has the consequence that _current_ sightings will have their 'sight-time' updated every tick, but I think that's OK.


