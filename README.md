# MultiWorldRando

An Archipelago-like multiworld randomizer, currently targeting **Harvest Moon GB
(USA) (CGB)** — `Harvest Moon GB (U) [C][!].gbc`.

## Status

First feature is in: **starting gold randomization (0–500G)**.

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
2. Tick **Randomize starting gold (0–500G)**
3. Click **Generate** — the seed is generated automatically

Output (written next to the ROM):

- `<name> (Seed <SEED>).gbc` — the randomized ROM
- `<name> (Seed <SEED>) Spoiler Log.txt` — what changed

### Headless (automation/testing)

```sh
java -jar MultiWorldRando.jar --headless <rom> [--gold] [--out <dir>]
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

## Layout

```
src/com/multiworldrando/          entry point (GUI + headless)
src/com/multiworldrando/core/     validation, seeds, patches, spoiler log
src/com/multiworldrando/hmgbc/    Harvest Moon GB ROM map + patches
src/com/multiworldrando/ui/       Swing UI
```
