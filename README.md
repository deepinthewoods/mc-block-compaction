# Block Compaction

A Minecraft Fabric mod that brings stonecutter-style block transformations directly to your inventory!

## Features

### 🔄 Inventory Transformations
- **Hover tooltips**: See all available stonecutter transformations for any block in your inventory
- **Scroll to select**: Use your mouse wheel to cycle through transformation options
- **Click to transform**: Left or right-click to pick up items and automatically transform them to your selected type
- **Bidirectional**: Transform blocks both ways (e.g., full blocks → stairs, or stairs → full blocks)

### 📦 Auto-Compaction
- Items picked up from the ground automatically transform into their base block form
- Example: Pick up stone stairs from the ground, they become regular stone blocks

### ⌨️ Keybind
- Press `B` (configurable) to toggle the mod on/off
- Shows on-screen notification when toggled

## How It Works

1. **Open your inventory** and hover over any block that has stonecutter recipes
2. **View transformations** in the tooltip - the currently selected one is highlighted with a green arrow (►)
3. **Scroll** to change which transformation is selected
4. **Click** to pick up the stack, and it will transform into the selected block type
5. **Selections persist** - your choice is remembered for each block type

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

- `B` - Toggle Block Compaction on/off (configurable in Minecraft's controls menu under "Block Compaction" category)

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
