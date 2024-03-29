# General Slim

A slimmed down version of general intended to operate at a small scale, similar to Advance Wars, aimed at testing out some of the ideas I have for a larger scale version of this game. The intention is to build effectively a limited Advance wars-system clone, and then evolve some of the ideas. Expected features of the clone product are as follows, with differences from advance wars in italics

* A small grid/tile based map
* Multiple unit types with movement rates and attack characteristics
* Terrain types affecting movement
* Direct orders
* No command structure
* Turn based
* Fog of war
* AI enemy commander
* Roads, rivers, cities
* _No base building_
* _No air or sea_

## Controls
* arrow keys/wasd: move cursor
* space: Action
* q: cancel action
* e: end turn
* g: debug menu

## DONE

* DONE Onscreen General
* DONE LOS increased on hills
* DONE Generate IDs, display names 
* DONE Short names to display on unit counters
* DONE proper algorithm for viewsheds
* DONE Bend roads properly
* DONE Improve road drawings: tiles depend on surrounding
* DONE Bug: If a general tries to attack, it throws (make it so a general doesn't have any attack options)
* DONE Set up proper dev/repl space
* DONE Add individual viewsheds to units
* DONE Add turn number to ui

## DOING

* **DONE** BUG: Units don't have available targets when next to another unit

### Enemy Intel

* Side has 'sightings'. Each sighting is an 
* When your unit loses sight of an enemy unit, an intel counter is created and placed at the location the enemy unit was last seen
* The sighting tracks how many turns ago since the sighting was made
* When that location comes into visibility again, the counter is replaced by the actual unit (even if they are not in the same location)
* Move intel counters: If displaying an intel card instead of a unit, A general should be able to move that intel card, changing his estimation of where the unit is

## TODO

### Small

* retreat improve: a retreating unit should prefer to escape into forests or hills if they are available
* QOL: When turn ends, cursor is moved to the nearest unit (Camera moves too)

### Medium

* Put in actual time, display in top right
* when in attack mode, menu should show the attributes of the two sides are
* Replace routing path with arrow, extend to attack with BANG! 
* Win condition: no enemy units
* Lose condition: no remaining units
* Infantry Charging
* Rivers
* enemy unit info - attack range on button hold
* Sprite for general
* Sprite for cav
* Sprite for art
* Refactor units to remove side as a top level key?

### Large

#### Morale

* Units have morale score that causes them to route if they fail a roll during a battle. A routed unit disappers from the map.
* Unit morale recovers over time where they are not engaged
* Morale increases faster when units aren't moving
* Morale recovery is nil if the unit has no food
* Morale is damaged by not having food or ammunition

#### Reframe all actions in terms of time taken

* Decide on turn length and square size
* Each unit has a mph rating for each terrain type
* Movement radius' are defined in terms of that. So if movement rate on road is 3mph, a unit can move 1.5 miles in a turn
* Other actions can be framed in that, like formup time
* A units movement pool then gets framed in minutes remaining in a turn, and the actions they can perform are dependent on that.


#### Pre-battle stuff
#### Cavalry combat system
#### Artillery combat and indirect fire
#### Facing and flanking
#### Supplies / Wagons

* Units have supplies of food and ammunition
* Units have hunger, affects fighting and marching speed
* Units without ammunition have a big penalty
* Supply screen at edge of square
* Wagons made available each turn
* You can order wagons to units
* Wagons can be interdicted/captured by enemy

#### Message system

* Units can send messages to eachother
* Unit can send a combat report to their commander, reporting the result of an engagement
* Unit can send a status report to their commander, detailing the strength and condition of their unit
* Messengers take time to move between units (following map)
* Messengers are displayed on screen
* Messengers can be intercepted by enemy, meaning the message will never reach its destination

#### Unit Status: Forming up

* Additional menu option: Form up. A unit that isn't formed up is much less combat effective
* Indicate formed up on unit counter somehow
* A unit can unform, which puts them back in to movement status
* Forming up takes time, reduces action pool
* A unit's movement highlight has a distinguishing tint: white for where they can move and still form up and attack, red for where they can move, but will not be able to form up and attack afterwards
* Moving while formed up is possible, but much slower

#### Unit differentiation

* Units can have modifiers to hit rates
* Units can have modifiers to move speeds
* Units can have modifiers to morale recovery

#### Commanders

* Each regiment has a commander
* Commanders can be 'stubborn', and be less likely to order a retreat
* Commanders can be 'inspiring' and make their unit less likely to route in combat, but which means they are more likely to be killed in combat. A unit with no commander can't retreat and is much more likely to route
* Commanders can be 'caring', which improves morale recovery at the cost of movement rate
* Commanders can be 'taskmasters', which improves movement rate but reduces morale recovery

### Huge

#### Enemy AI

#### Rework order system to be more indirect

* General can order units to move to any location on the map
* General can provide general orders:
** Attack any enemy in the vicinity
** Hold the location
** Report on enemy movements but avoid combat
* Unit will be responsible for creating and execting its own order on each turn

### Maybes

* Friendly FOW
* Variable FOV: units that come into LOS within 3 distance are directly in sight. Units that come into LOS within 4 distance generate a sighting, but are not directly visible
* Chance of a false sighting
* Unit status: Dugin / Encamped
* Unit status: Organizing

## Old Done

### Iteration goals

* Moveable units
* Combat
* Terrain
* AI

### Iteration 1: A map with movable units

* DONE A small ~10x10 map with no terrain features 
* DONE Two opposing forces of two infantry units each  
* DONE Units can be issued orders to move 1 square  
* DONE No AI, player plays both sides  
* DONE End turn on "c"  
* DONE UI for displaying field and issuing orders  
* DONE Cursor  
* DONE Select units  
* DONE cursor doesn't go OOB  
* DONE error handling for movement~
* DONE Highlight moveable area  

* DONE Opacity for select  
* DONE Iteration 1a: units can't move twice in a turn  
* DONE ending turn refreshes sides move-points  
* DONE Iteration 1b: box display of cursor coord, whose turn  
* DONE Status box moves if cursor is over it  

### Iteration 2: Combat

* DONE units have HP  
* DONE Units name and HP displayed in status box  
* DONE Units are destroyed and removed from the map when their HP reaches zero  
* DONE Units can attack one another  
* DONE Units have attack and defense power, which impacts the HP they lose in combat  
* DONE Attack power depends on strength  

* DONE Unit identfiers on tile  
* DONE A second, Cavalry unit type is added  
* DONE Cavalry can move 2 spaces  
* DONE move points refesh from max move points  
* DONE highlight shows manhattan distance based on move points 
* DONE Fix routing so you can't do that loop thing   Hacked!
* DONE can move 2 squares at once  
* DONE Cavalry have different attack characteristics  
* DONE Attack/Def chars in menu  

### Iteration 3: Terrain and features

* DONE Map has forests
* DONE and lower movement rate
* DONE Display HP on unit tile, get rid of status box
* DONE Debug box
* DONE Have moving into forests decrease movement rate accordingly
* DONE Top left turn indicator
* DONE increased defence 
* DONE Map has mountains, impassible by cavalry
* DONE Map has roads, and units have increased range on roads
* DONE Roads draw based on direction properly
* DONE forked roads and crossroads
* DONE (but broke attacking) Fix units moving though other units
* DONE Fix attack / Attack after move
** DONE AW style wait menu after move
** DONE add attack option if enemy unit in adjacent
* DONE variable size levels

* DONE Move non-quil specific handlers to game NS
* DONE Scalable tile size
* Separate order handling into own NS?
* DONE Move debug stuff to game NS
* DONE refactor debug text stuff
* DONE Cursor to target on attack
* DONE add wasd support
* DONE see move range on clicking enemy unit
* DONE Bug: units can't _not_ move and then attack
* DONE Bug: unit is still selected when finished move and no attack option
* DONE BUG selecting no unit throws
* DONE Better combat system
* DONE In battles, attackers losses are modified by the terrain they're on (think this is why my losses aren't same as AW).
* DONE Bug: in battle, attackers losses are not impacted by defenders hp
* DONE REPLICATE FIRST AW LEVEL
* DONE Change order system to a sort of queue
** DONE Issue move order, target square has a 'shadow' of unit on it but unit doesn't move
** DONE Can issue attack order (or wait) from shadow. Attack order gets queued behind the move order
** DONE Once attack/wait commmand is issued _then_ the unit moves and attacks
* DONE BUG: not moving costs a movement point (Maybe just don't send an empty move order?)
* DONE BUG: end highlight / select on end turn
* DONE BUG: Units can move after attacking
* DONE BUG: Can end turn in menu mode
* DONE BUG: Roads not scaling
* DONE Sprites: Units, Field, Mountain, Trees 
* DONE: cancel out of order mid move
* DONE: Artillery
* DONE: Map builder stuff
* DONE: level persistence

* DONE Scenario namespace
* DONE Persist unit tables
* DONE Persist scenarios
* DONE Make units a bit transparent so you can see terrain underneath
* DONE Move units a bit so they're not blocking text
* DONE BUG Dead units try to withdraw
* DONE BUG Retreating costs movement points
* DONEBUG Can't multidirection attack
* DONE Improve Infantry combat system
** DONE Infantry on infantry Volley
** DONE Terrain modifers
** DONE Retreat mechanics
** DONE Disengagement (non-retreaters get a free shot)
** DONE Actual retreat, move on retreat
* DONE FOV
* DONE BUG: Units can't see themselves
* DONE BUG: Can attack a unit you can't see
* DONE Tests!
** DONE Field
** DONE Combat
* DONE Get rid of unit strength indicators
* DONE Hover menu for units
* DONE Better sprites
* DONE Scrolling camera: display only 15x15 map and scroll around to see more
* DONE BUG: Unit can attack twice in one turn
* DONE Unit's can't move twice in a given turn
