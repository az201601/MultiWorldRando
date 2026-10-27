# MultiWorldRando

An Archipelago-like multiworld randomizer, currently targeting **Harvest Moon GB
(USA) (CGB)** — `Harvest Moon GB (U) [C][!].gbc`.

## Status

Features so far: **starting gold randomization (0–500G)**, **grandpa's rainy
day money drawer randomization (0–1000G)**, **optional user-entered seed**,
**seed shop slot shuffling (what each slot sells)**, and **seed season
shuffling (which season sells which crops)**.

## Requirements

- Any **Java 8+** JRE to run the randomizer
- To build: a Java compiler that can target Java 8 (the included `build.sh`
  uses the ECJ batch compiler; set `ECJ_JAR` to your copy)

## Build

```sh
./build.sh        # produces MultiWorldRando.jar (Java 8 bytecode)
```

## Usage

### GUI

```sh
java -jar MultiWorldRando.jar
```

1. **Browse…** to choose your Harvest Moon GB ROM (it is validated — wrong
   files are rejected with an explanation)
2. Tick the options you want:
   - **Randomize starting gold (0–500G)**
   - **Randomize grandpa's rainy day money drawer (0–1000G)**
   - **Shuffle seed shop slots (what each slot sells)**
   - **Shuffle seed seasons (which season sells which crops)**
3. Optionally type a **Seed** (1–16 hex digits) to generate exactly that
   seed — leave it blank to generate a random one
4. Click **Generate**

Output (written next to the ROM):

- `<name> (Seed <SEED>).gbc` — the randomized ROM
- `<name> (Seed <SEED>) Spoiler Log.txt` — what changed

### Headless (automation/testing)

```sh
java -jar MultiWorldRando.jar --headless <rom> [--gold] [--drawer] [--seedshop] [--seedseasons] [--seed <hex>] [--out <dir>]
```

Prints `SEED=`, `ROM=`, `SPOILER=` on success. With `--seed` the run is
reproducible; without it the seed is generated automatically.

## How the starting gold patch works

Reverse-engineered facts (Harvest Moon GB (USA)):

- Money is a 3-byte little-endian value in battery SRAM at `$B8EF–$B8F1`
  (cap 99,999G)
- Vanilla starting gold is **500G**, written with immediate operands by the
  new-game initializer at CPU `$52B9`, mirrored in ROM banks `$1D` and `$3D`
  (file offsets `0x0752C3` and `0x0F527E`)
- The patch replaces the two immediate bytes per mirror — a pure data patch
- Verified end-to-end in an emulator: new game on the patched ROM starts with
  the seeded amount instead of 500G

### Grandpa's rainy day money drawer

- Interacting with object `$09` (the drawer) runs a one-time grant of
  **1000G**, gated by SRAM flag `$B8FF` ("empty" message on revisit)
- The handler lives at CPU `$59C8` in banks `$02`/`$22` and adds the amount
  via the same `Call_000_0cbb`/`Call_000_1056` money machinery
- The patch replaces the `ld bc, $03e8` immediate (file offsets `0x99D6`
  and `0x89943`) — again a pure data patch
- The drawer dialog (message `$09`) is rewritten at generation time to
  match the roll, using the game's glyph encoding:
  - **0G** → *"Empty! Grandpa's rainy day fund was all sunshine."*
  - **1–499G** → *"Rainy day fund? More like a light drizzle."* followed by
    `Received XG!!` with the actual amount
  - **500–1000G** → the vanilla dialog, with the `Received XG!!` line
    updated to the actual amount

### Seed shop slot shuffle

The seed shop buy menu has **9 slots** (Turnip, Potato, Tomato, Corn,
Eggplant, Peanut, Carrot, Broccoli and Grass seeds), each showing a name and
price tag ("Turnip Seeds / 1 bag is 120G.").

Reverse-engineered data (Harvest Moon GB (USA)):

- A 9-record per-slot item table lives at file offsets `0x02495D` (bank `$09`)
  and `0x0A4940` (bank `$29` mirror). Each 9-byte record carries the item's
  identity: stock flag, season availability, growth parameter, the pointer to
  its purchase data (price charged, item granted, "Those are … Seeds."
  description) and its inventory RAM slot, plus the (position-linked) pointer
  to the slot's display text
- The 9 display texts are fixed 32-byte rows at file offset `0x06F773`
  (bank `$1B`) with a mirror at `0x0EF773` (bank `$3B`); both copies are
  patched in sync
- The patch permutes the item-identity fields across the 9 slots and moves the
  display rows with the same permutation — every slot consistently shows and
  sells its new item, including the price tag, the description dialog and the
  price actually charged. Both table copies are patched in sync
- The spoiler log lists what each slot sells and what it sold in the vanilla
  game

### Seed season shuffle

The shop only offers seeds whose **season availability bitmask** matches the
current season (record byte +2: `01` spring, `02` summer, `04` fall, `08`
winter). Vanilla sells {Turnip, Potato} in spring, {Tomato, Corn} in summer,
{Eggplant, Peanut} in fall and {Carrot, Broccoli} in winter; **Grass Seeds**
keeps `07` (spring|summer|fall) year-round within those seasons.

- The patch permutes the 8 crop masks among the 8 crop records — exactly two
  crops per season, as vanilla (the shop shelf never asks for more than three
  bags at once) — and leaves Grass Seeds' `07` untouched
- Works standalone or combined with the slot shuffle; the two shuffles use
  separate RNG streams, so with both enabled the seed entry reproduces
  everything
- Tomato Seeds' description ("…during Summer,") is rewritten to the crop's
  new season — the only season word in any seed description
- The spoiler log lists what each season sells now versus vanilla

#### Crop survival in the shuffled season

In the vanilla game a crop planted outside its season vanishes the next game
day when you go to check on it (the game's notion of a crop dying): the
nightly growth walk and the map-load walk each hardcode every crop's valid
season as a `cp` immediate against the current season (`ld a,[$B884]; cp n`)
and kill the crop (writing the tilled tile back) on a mismatch. Those checks
lived in banks $02/$22 and knew nothing about the shop table - so the shop
could sell a crop in a new season while the field still enforced the vanilla
one.

The patch realigns all of them: both walks, both bank copies, for each of the
eight crops, the `cp` immediate is rewritten to the crop's shuffled season.
Grass is untouched and keeps its vanilla "grows in spring/summer/fall, dies in
winter" rule. The expected behaviour after the shuffle:

- A crop planted in the season its seed is sold in survives and grows, and
  still dies when that season ends (vanilla behaviour)
- A seed bought in one season and planted in a different one still dies the
  next day (vanilla behaviour, kept intact)

#### Crop graphics in the shuffled season

Crop stage tiles expand to shared BG tiles whose pixel art lives in the
seasonal tile chunks (bank $0B for the DMG load path, bank $2B for the CGB
path - one pair of chunks per season). Vanilla only ever shows each crop in
its own season, so a shared tile slot holds "the art of whichever crop grows
this season" - and is blank for the Tomato/Corn/Broccoli fruiting stages
outside summer/winter. Planted in a shuffled season, a crop would otherwise
grow up as a different crop's art (or disappear entirely).

The generator realigns the art per seed: for each season, each crop-art tile
slot is rewritten to the art of the crop actually assigned to that season
(taken from that crop's vanilla chunk, in both art sets). When two same-family
crops land in one season and would share a slot, the losing crop's metatile
rows are repointed to spare tile slots. Crops only ever render in their
assigned season (the field walks clear them everywhere else), so each crop's
art is only needed in its own season's chunks - and when the shuffle is off
the whole fix is a no-op and the ROM is byte-identical to vanilla.

Verified in-game: an out-of-vanilla-season crop planted on a shuffled seed
renders pixel-identical to the same crop in its vanilla season, and the
non-crop screens are unchanged (frame-by-frame A/B against vanilla).

## Layout

```
src/com/multiworldrando/          entry point (GUI + headless)
src/com/multiworldrando/core/     validation, seeds, patches, spoiler log
src/com/multiworldrando/hmgbc/    Harvest Moon GB ROM map + patches
src/com/multiworldrando/ui/       Swing UI
```
