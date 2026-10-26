# MultiWorldRando

An Archipelago-like multiworld randomizer, currently targeting **Harvest Moon GB
(USA) (CGB)** — `Harvest Moon GB (U) [C][!].gbc`.

## Status

First features are in: **starting gold randomization (0–500G)** and
**grandpa's rainy day money drawer randomization (0–1000G)**.

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
3. Click **Generate** — the seed is generated automatically

Output (written next to the ROM):

- `<name> (Seed <SEED>).gbc` — the randomized ROM
- `<name> (Seed <SEED>) Spoiler Log.txt` — what changed

### Headless (automation/testing)

```sh
java -jar MultiWorldRando.jar --headless <rom> [--gold] [--drawer] [--out <dir>]
```

Prints `SEED=`, `ROM=`, `SPOILER=` on success.

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

## Layout

```
src/com/multiworldrando/          entry point (GUI + headless)
src/com/multiworldrando/core/     validation, seeds, patches, spoiler log
src/com/multiworldrando/hmgbc/    Harvest Moon GB ROM map + patches
src/com/multiworldrando/ui/       Swing UI
```
