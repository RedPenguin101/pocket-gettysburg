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

