# Block Compaction

A Minecraft Fabric mod that brings stonecutter-style block transformations directly to your inventory!

## Features

### 🔄 Inventory Transformations
- **Hover tooltips**: See all available stonecutter transformations for any block in your inventory
- **Scroll to select**: Use your mouse wheel to cycle through transformation options
- **Click to transform**: Left or right-click to pick up items and automatically transform them to your selected type
- **Cross-transformations**: Any blocks from the same family can transform to each other (stairs ↔ walls ↔ slabs, etc.)
- **Ratio display**: Tooltips show conversion ratios (e.g., "(1:2)" means 1 input creates 2 outputs)

### 🧮 Recipe Ratio Tracking
- **Respects recipe ratios**: 1 stone → 2 slabs is honored in both directions
- **Fractional tracking**: Picking up 3 slabs (1.5 base blocks) gives you 1 base block and stores 0.5 for later
- **No item loss**: Partial blocks are tracked until you have enough for a full block
- **Stored amount display**: Tooltips show fractional amounts (e.g., "Stored: 0.50x")

### 📦 Auto-Compaction
- Items picked up from the ground automatically transform into their base block form
- Uses fractional tracking to handle slabs and other partial blocks correctly
- Only converts if you don't have space in existing stacks of that item type
- Example: Pick up 1 stone slab → stores 0.5 base blocks, pick up another → get 1 stone block

### 🔄 Auto-Refill
- Automatically restocks your hotbar when placing blocks
- Triggers when a block count drops to 1 or below
- **Smart priority system**:
  1. First searches for identical blocks in your inventory
  2. Then converts from base blocks (e.g., stone → stone stairs)
  3. Finally converts from other family blocks (e.g., walls → stairs)
- **Ratio-aware refills**: Refills with correct amounts (1 for normal blocks, 2 for slabs, etc.)
- Example: Placing stairs with 1 left + have stone in inventory → auto-converts stone to stairs

### ⌨️ Keybinds
- Press `B` (configurable) to toggle the entire mod on/off
- Press `R` (configurable) to toggle auto-refill on/off
- Shows on-screen notification when toggled
- Both keybinds can be customized in Minecraft's controls menu under "Block Compaction" category

## How It Works

1. **Open your inventory** and hover over any block that has stonecutter recipes
2. **View transformations** in the tooltip - the currently selected one is highlighted with a green arrow (►)
3. **Scroll** to change which transformation is selected
4. **Click** to pick up the stack, and it will transform into the selected block type
5. **Selections persist** - your choice is remembered for each block type

### Examples

**Example 1: Slab to Full Block Conversion**
- You have a stack of 3 stone slabs
- Select "Stone" as the transformation target (ratio shows "2:1")
- Pick up the stack → you get 1 stone block (3 slabs ÷ 2 = 1.5 blocks)
- Tooltip shows "Stored: 0.50x" indicating half a block is saved
- Pick up 1 more slab → you now get 1 more stone block (using the stored 0.5 + new 0.5)

**Example 2: Full Block to Slab Conversion**
- You have 2 stone blocks
- Select "Stone Slab" as the transformation target (ratio shows "1:2")
- Pick up the stack → you get 4 stone slabs (2 blocks × 2 = 4 slabs)

**Example 3: Cross-Family Transformation**
- You have stone stairs
- Available transformations: Stone (1:1), Stone Slabs (1:2), Stone Walls (1:1), Stone Bricks (1:1), etc.
- Select Stone Slabs → pick up 1 stair, get 2 slabs
- Or select Stone Walls → pick up stairs, get walls (1:1 conversion)

**Example 4: Auto-Refill While Building**
- You're placing stone stairs and have 2 left in your hotbar
- Place one → down to 1 stair
- Auto-refill triggers:
  - First checks for more stairs in inventory (finds none)
  - Then checks for stone blocks (finds 64 in inventory)
  - Converts 1 stone → 1 stair, automatically refills hotbar
- You now have 1 stair in hotbar, 63 stone in inventory
- Continue building without manually restocking!

## Technical Details

- **Minecraft Version**: 1.21.10
- **Mod Loader**: Fabric
- **Dependencies**: Fabric API, Fabric Loader 0.17.3+
- **Java Version**: 21

## Recipe System

This mod uses **only stonecutter recipes** for transformations:
- All vanilla stonecutter recipes are supported
- Modded stonecutter recipes work too
- Recipes are loaded automatically when you join a world

## Configuration

No config file needed! The mod works out of the box with sensible defaults.

## Keybinds

- `B` - Toggle entire mod on/off (disables all features)
- `R` - Toggle auto-refill on/off (keeps transformations active)
- Both are configurable in Minecraft's controls menu under "Block Compaction" category

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/)
2. Download [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download this mod
4. Place both JAR files in your `mods` folder
5. Launch Minecraft!

## Building from Source

```bash
./gradlew build
```

The compiled JAR will be in `build/libs/`

## License

This project is licensed under CC0-1.0 - see the LICENSE file for details.

## Author

deepinthewoods

## Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you would like to change.
