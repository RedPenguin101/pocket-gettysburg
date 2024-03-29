# Combat system

Trying to turn this into a semi-realistic combat system

## Infantry vs. Infantry

The basic unit of maneuver is the *regiment*. For infantry, this means a strength of 1000 men (though usually less in practice)

Each of the 1000 men is assumed to have a rifle and ammunition (infinite for now).

When two regiments engage they stand opposed (the assulting side and the defending side) and fire their rifles, and potentially one side charges the other.

A turn length in this game is 10 minutes. In most cases this will be sufficient time for two regiments to fully engage, with one side driving off the other.

### Volley

A combat round is comprised of several volleys, each killing or wounding members of the opposing force. Only unwounded men can fire their rifle, and thus will count towards the volley force.

When a regiment fires a volley, the chance-to-hit an enemy must be calculated. This will depend on the terrain, and whether the opposing unit is dug in.

Say there are two regiments facing off: one is 1000 strong and the other 500. Both are standing on open field. The first fires a volley of 1000 rifle balls. Each ball is aimed at a soldier. There is a 20% chance each ball will hit its target. Each ball is aimed at an individual soldier, randomly chosen, so there is a possibility that several soldiers will be shooting at the same opposing person, and none will be shooting at another.

The algorithm for calculation of hits is as follows

* Each rifle fires a ball at an enemy
* The enemy is chosen randomly from a number between 0 and the number of enemy soldiers: X
* A random number between 0 and 1 is rolled. If the number is less than 0.20, that is a hit.
* The number of unique Xs that were hit is the number of casualties

Under this system, the average hits for different volleys are as follows.

```
Firing men  |   Enemy   |   Casualties
1000        |   1000    |   181 (18%)
500         |   500     |   91  (18%)
1000        |   500     |   165 (33%)
1000        |   250     |   138 (55%)

Force proportion  |  Casualty rate
       1          |        18%
       0.9        |        19%
       0.8        |        22%
       0.7        |        24%
       0.6        |        28%
       0.5        |        32%
       0.4        |        39%
       0.3        |        48%
       0.2        |        63%
       0.1        |        86%
```

The to-hit change is modified by the terrain the receiving unit is standing on. Trees will provide cover.

After each volley, the commander of the regiment will make a call on whether to continue volleying, to charge, or to retreat. The unit could also break and run.

One Volley takes 2 minutes of game time. so there are potentially 5 volleys in a turn, though likely one unit will retreat or charge before that.

## Retreating

After each volley the unit commander will decide whether to retreat or not. The factors that go into this include:

* The number of casualties taken as a proportion of the starting force
* The proportional strength of their unit to the opposing unit: If they are outnumbered they are much more likely to retreat, if they outnumber the opponent they are less likely.
* If they are attacking into unfavorable terrain (i.e. into woods)
* NOT IMPLEMENTED The courage/stubbornous of the commander
* NOT IMPLEMENTED Whether they are being supported by other units

The algorithm is this:

* Take the proportional casualties squared
* Add 1 - proportional strength (a positive proportional strength with therefore reduce retreat chance)
* Modify this by * 1.2 if the enemy terrain is trees
* Modify this by * 0.8 if your terrain is trees

When retreating, units try to move away from the enemy they are facing. If that square is occupied they will randomly pick another unoccupied square

TODO
Charging
Retreating multiple squares, maximising MDist from enemy
Facings and frontage
Morale failing
Formation (road etc.)
