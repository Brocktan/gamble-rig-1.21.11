# Fake Dispenser (Fabric, Minecraft 1.21.11, client-side)

Look at a dispenser and press **K** to configure it. Press **J** to toggle fake item rendering on/off (no chat alerts).

## Random item pool
In the dispenser menu, enter comma-separated item IDs in **Random pool**, e.g.
`minecraft:netherite_ingot, minecraft:grass_block`. Each fake item spawned by that dispenser chooses an item from the pool randomly. Leave blank to use the single Item ID field.

This is client-side only. No dispenser auto-return feature is included.

## Build on GitHub
Push this project to a GitHub repository. The included Actions workflow builds the mod; download the `fakedispenser-mod` artifact from the workflow run.
