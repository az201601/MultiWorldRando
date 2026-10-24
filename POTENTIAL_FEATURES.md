# MultiWorld Randomizer: Potential Features & Location Checks

This document catalogs potential check locations, item shuffle options, and gameplay features for the **Harvest Moon GBC (US)** module of the standalone single-player MultiWorld Randomizer.

---

## 1. Farmhouse Checks (House Interior)

The farmhouse is the player's starting hub. All interactable objects are routed through the house interaction dispatcher (`Call_022_5880` in Bank `$22` for CGB / `Call_002_4585` in Bank `$02` for DMG) using the object ID stored in `$CB33`.

### 1.1 Grandpa's Drawer *(Implemented)*
* **Location Name**: `Farmhouse - Grandpa's Drawer`
* **Object ID**: `0x09`
* **SRAM Flag**: `$B8FF` (`0x00` uncollected -> `0x01` collected)
* **Vanilla Reward**: 1000G (`$03E8`) added to Gold at `$B8EF`
* **ROM Offsets**: CGB `0x89935` (Bank `$22:5935`), DMG `0x099C8` (Bank `$02:59C8`)
* **Dialogue**: Script `0x09` ("What! There is money in the drawer... Received 1000G!!")
* **Status**: Fully reverse-engineered and ready for custom item grant / remote notification.

### 1.2 Kitchen Cupboard / Pantry Stash
* **Location Name**: `Farmhouse - Kitchen Cupboard`
* **Object IDs**: `0x06`, `0x07`, `0x08` (Cupboard counter tiles next to fireplace)
* **Vanilla Behavior**: Triggers script `0x08` (*"Let me see... Everything is alright!"*)
* **Rando Concept**: Immediate Day 1 free check. A hidden emergency stash, grandma's recipe, or seed packet.
* **Implementation Plan**: Assign an unused SRAM flag (e.g., within `$B8FE`–`$B90F` free bytes) to track first-time opening.

### 1.3 The Fireplace
* **Location Name**: `Farmhouse - Fireplace`
* **Object ID**: `0x02`
* **Vanilla Behavior**: Triggers script `0x07` (*"Confirm the origin of fire! Everything is alright!!"*)
* **Rando Design Options**:
  * **Option A (Day 1 Free Check)**: Inspecting the mantel or chimney brickwork grants a check immediately.
  * **Option B (Gated / Progression Check)**: Requires lighting a fire using **Firewood** (obtained by cutting tree stumps with the **Axe**). Provides natural early-game tool gating.

### 1.4 The Television Set
* **Location Name**: `Farmhouse - Television Broadcast`
* **Object ID**: `0x04`
* **Vanilla Behavior**: Displays weather forecasts and town news. Checks `$B8D1` for catalog shopping.
* **Rando Design Options**:
  * **Option A (Home Shopping / Contest)**: Watching the TV on Day 1 plays a special sweepstakes/broadcast awarding a check.
  * **Option B (Utility Only)**: Leave purely as weather forecasting utility (rain auto-waters crops).

### 1.5 Behind the Clock
* **Location Name**: `Farmhouse - Clock Stash`
* **Object ID**: `0x05`
* **Vanilla Behavior**: Reads RTC registers and displays current in-game time.
* **Rando Concept**: Inspecting the hanging wall clock reveals a hidden note or key tucked behind the pendulum.

### 1.6 The Wall Calendar
* **Location Name**: `Farmhouse - Calendar Note`
* **Object ID**: `0x03`
* **Vanilla Behavior**: Reads day counter `$B885` and displays the current date.
* **Rando Concept**: Inspecting the calendar on Day 1 or on specific festival days awards a check.

### 1.7 Pet Interaction / Companion Stash
* **Location Name**: `Farmhouse - Pet Companion`
* **Object ID**: `0x99`
* **Vanilla Behavior**: Directional barking/meowing and petting animation.
* **Rando Concept**: Petting the dog/cat on Day 1, or awarded when pet affection reaches a target threshold.

### 1.8 Upgraded Farmhouse Extensions
When the Carpenter completes house expansions:
* **Tool Storage Cabinet / Refrigerator (`Object 0x0B`)**: Unlocked with House Upgrade 1 (250 Wood + 2,500G).
* **Bathroom / Restroom**: Unlocked with House Upgrade 2 (500 Wood + 5,000G).

---

## 2. Tool Shed Checks (Tools & Shelves)

The shed right outside the farmhouse contains dedicated shelf slots for tools. The presence of tools is recorded in battery-backed SRAM (`$B8A0`–`$B8BF`):

* `$B8A0`: Sickle
* `$B8A1`: Hoe
* `$B8A2`: Hammer
* `$B8A3`: Axe
* `$B8AE`: Watering Can
* `$B8A7`: Super Sickle
* `$B8A8`: Super Hoe
* `$B8A9`: Super Hammer
* `$B8AA`: Super Axe
* `$B8AF`: Sprinkler
* `$B8AC`: Milker
* `$B8AD`: Brush
* `$B8B3`: Cow Bell
* `$B8BD`: Pickaxe
* `$B8BE`: Umbrella
* `$B8BF`: Fishing Rod

### Rando Features
* **Starting Tool Shuffle (Optional Toggle)**:
  * When disabled: The 5 basic farming tools (Hoe, Sickle, Axe, Hammer, Watering Can) are present from Day 1.
  * When enabled: The 5 basic shelves become check locations (`Shed - Shelf 1` through `Shed - Shelf 5`), and tools are scattered into the multiworld pool.
* **Acquired Tool Shelves**:
  * Shelf checks awarded upon unlocking/purchasing specialty tools.

---

## 3. Farmstead & Agricultural Milestones

Rewarding core farming gameplay without excessive grind:

### 3.1 Field Tilling & Buried Treasure
* **Location Name**: `Farm - Buried Field Treasure`
* **Vanilla Behavior**: Tilling field soil with the Hoe has a random chance to uncover a Power Berry or coins.
* **Rando Concept**: The first buried treasure dug up in the field is guaranteed to grant a randomized multiworld check.

### 3.2 Clearing Farm Debris (Tool Gating)
* **Location Name**: `Farm - Clear First Tree Stump` (Requires **Axe** or Super Axe).
* **Location Name**: `Farm - Smash First Boulder` (Requires **Hammer** or Super Hammer).
* **Location Name**: `Farm - Clear Overgrown Weeds` (Requires **Sickle** or hands).

### 3.3 The Farm Well
* **Location Name**: `Farm - Water Well`
* **Vanilla Behavior**: Refills the Watering Can.
* **Rando Concept**: Inspecting the well depths or tossing an item into the well.

### 3.4 Shipping Bin Milestones
* `Shipping - First Crop Shipped`: Shipped any vegetable (Turnip, Potato, Tomato, Corn).
* `Shipping - 1,000G Revenue`: Shipped 1,000G worth of produce/goods.
* `Shipping - 5,000G Revenue`: Shipped 5,000G worth of produce/goods.

---

## 4. Livestock & Animal Husbandry Milestones

### 4.1 Chicken Coop
* `Coop - First Chicken Acquired`: Purchasing or hatching a chick.
* `Coop - First Egg Harvested`: Daily egg collection from an adult chicken.
* `Coop - Incubator Hatch`: Successfully hatching an egg in the incubator.

### 4.2 Cow Barn
* `Barn - First Cow Acquired`: Purchasing a cow from the Animal Ranch.
* `Barn - First Milk Harvested`: Using the **Milker** on an adult cow.
* `Barn - Miracle Potion Pregnancy`: Successfully breeding a cow.
* `Barn - Golden / Large Milk`: Reaching maximum affection/hearts with a cow.

---

## 5. Mountain, Wilderness & Secret Areas

### 5.1 Harvest Goddess Spring
* **Location Name**: `Mountain - Harvest Goddess Offering`
* **Vanilla Behavior**: Throwing a high-quality crop or vegetable into the spring summons the Harvest Goddess to reward a Power Berry.
* **Rando Concept**: Prime progression check—tossing an offering into the spring grants a multiworld item.

### 5.2 Harvest Sprites' Cave
* **Location Name**: `Mountain - Harvest Sprites Cave`
* **Vanilla Behavior**: Cave dwellers who assist with farm chores when befriended.
* **Rando Concept**: Speaking to or feeding the Harvest Sprites awards a check.

### 5.3 The Earthquake Secret Chamber
* **Location Name**: `Mountain - Secret Cave Passage`
* **Vanilla Behavior**: An earthquake opens a sealed passageway in the mountain.
* **Rando Concept**: Accessing the revealed chamber or underground spring.

### 5.4 Mountain Hot Spring
* **Location Name**: `Mountain - Hot Spring Soak`
* **Rando Concept**: Soaking in the hot spring to recover stamina grants a stamina/berry check.

### 5.5 The Sacred Tree / Mountain Woodsman
* **Location Name**: `Mountain - Woodsman's Tree`
* **Rando Concept**: Speaking to the mountain woodsman or inspecting the sacred hollow tree.

---

## 6. Town Upgrades & The 10 Power Berries

### 6.1 Tool Shop Upgrades
* Exchanging tools for upgraded models (Super Hoe, Super Sickle, Super Hammer, Super Axe).
* Purchasing specialty tools (Sprinkler, Brush, Milker, Cow Bell).

### 6.2 Carpenter Extensions
* `Carpenter - House Upgrade 1` (250 Wood + 2,500G).
* `Carpenter - House Upgrade 2` (500 Wood + 5,000G).

### 6.3 The 10 Power Berries
Vanilla *Harvest Moon GBC* contains 10 Power Berries, each permanently expanding max stamina. In the randomizer, each Power Berry source serves as a distinct location check:
1. Field Soil Digging.
2. Harvest Goddess Spring Offering.
3. Waterfall / Secret Mountain Cave.
4. Tool Shop / Flower Shop Special Purchase.
5. Mountain Fisherman Pond Event.
6. Festival Trade / Mini-game Prize.
7. Hurricane Debris Cleanup.
8. Basil / Plant Botanist Reward.
9. Egg / Animal Exhibition Reward.
10. End of Year Evaluation Milestone.

---

## 7. Technical Implementation Architecture

### 7.1 Cross-Game Item Delivery & Notifications
* When an item for Game B is found while playing Harvest Moon GBC:
  * The in-game text window displays: `"Found [Item Name] for [Player/Game]!"`
  * The central Java client's memory watcher detects the SRAM flag change and logs the item to the session store.
* When Harvest Moon GBC receives an item discovered in another game:
  * The central Java client detects an active HM GBC session.
  * The client triggers an in-game delivery event (or mailbox/drawer delivery) with an `"Obtained [Item Name]!"` pop-up text box.

### 7.2 Memory Watcher Flags Table
| Location | SRAM Address | Uncollected | Collected | Notes |
| :--- | :---: | :---: | :---: | :--- |
| `Farmhouse - Grandpa's Drawer` | `$B8FF` | `0x00` | `0x01` | Dedicated vanilla byte |
| `Farmhouse - Kitchen Cupboard` | TBD (e.g. `$B8FD`) | `0x00` | `0x01` | Custom allocated flag |
| `Farmhouse - Fireplace` | TBD (e.g. `$B8FC`) | `0x00` | `0x01` | Custom allocated flag |
| `Shed - Sickle Shelf` | `$B8A0` | `0x00` | `0x01` | Vanilla tool inventory byte |
| `Shed - Hoe Shelf` | `$B8A1` | `0x00` | `0x01` | Vanilla tool inventory byte |
| `Shed - Hammer Shelf` | `$B8A2` | `0x00` | `0x01` | Vanilla tool inventory byte |
| `Shed - Axe Shelf` | `$B8A3` | `0x00` | `0x01` | Vanilla tool inventory byte |
| `Shed - Watering Can Shelf`| `$B8AE` | `0x00` | `0x01` | Vanilla tool inventory byte |
