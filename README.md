# AnyabotFabric

**AnyabotFabric** is a Minecraft Fabric mod that adds AI-powered companions to your world.
Now with **three unique companions**: **Anya**, **Kira**, and **Masha**. Each has her own personality, skin, and AI behavior.

---

## Features

### 1. Three AI Companions

* **Anya** – The original companion.
* **Kira** – New companion with her own skin and AI prompt.
* **Masha** – Another new companion with unique AI and appearance.

### 2. Follows Players

* Companions can follow players around.
* Controlled by a config option: `follow_player`.

### 3. Chat Interaction

* Speak to them in chat, and they will respond:

  * `"anya, come on"` → Anya responds
  * `"kira, come on"` → Kira responds
  * `"masha, come on"` → Masha responds
* AI responses powered by **SambaNova** or **Ollama**, configurable per companion.

### 4. Build Structures

* Companions can create Minecraft structures from NBT/SNBT.
* Chat commands:

  * `"create <idea>"` – Generates a structure based on the idea.
  * `"build <url>"` – Builds a structure from a schematic URL.

### 5. Heart Effects & Little Villagers

* Interactive commands trigger heart particles.
* A baby villager spawns as a "shared creation" with the player.
* Works for all three companions.

### 6. Sleep Mechanic

* Companions will detect beds and attempt to sleep.
* Warn players when they want to sleep.
* Automatically wake up when the night ends.

### 7. Configurable AI Prompts

* Each companion has a unique AI prompt stored in `config/anya_config.json`:

```json
{
  "model_anya": "DeepSeek-V3-0324",
  "model_kira": "DeepSeek-V3-0324",
  "model_masha": "DeepSeek-V3-0324",
  "temperature": 0.7,
  "follow_player": true
}
```

### 8. Fully Server-Compatible

* Works on Fabric servers.
* Server tracks each companion individually.
* No client-side mods required for basic AI interaction, but textures and rendering need a client install.

---

## Installation

1. Place the mod JAR in your `mods` folder.
2. You must download the FabricAPI mod from Modrinth, etc.
3. Ensure Fabric Loader is installed (1.21.x supported).
4. Optionally edit `config/anya_config.json` for AI and follow settings.
5. Launch Minecraft (client or server).

---

## Usage

* Spawn a companion near you using chat: `"anya, come on"`, `"kira, come on"`, `"masha, come on"`.
* Give commands for AI interaction, structure building, and more.
* Watch them follow you, chat, and build in-game!

---
Author: FlorestDev
**AnyabotFabric** turns Minecraft into a world with three personalized, AI-powered companions, ready to chat, build, and interact with you.
**Modrinth**: https://modrinth.com/mod/anyabotfabric
