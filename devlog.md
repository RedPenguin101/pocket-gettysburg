# Devlog

## 28th September
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
(get-in game-state [:red :units "id" :viewshed])
;; => #{[8 8] [7 6] [8 7] [9 8] etc.
```

So I think just changing `add-attack-option` to look at the unit's viewshed is the fix.

...

Fixed! Easy one.