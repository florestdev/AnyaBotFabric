# AnyaBot for Fabric

**Version:** 1.0
**Minecraft:** 1.21.x
**Platform:** Fabric

---

## Overview

AnyaBot is an interactive AI-powered companion mod for Minecraft Fabric. Players can spawn a special entity called **Anya** who can follow them, respond to commands, play mini-games, and even create a unique in-game 'baby' villager.

This mod is designed to enhance immersion, particularly for Russian-speaking players, but supports basic English commands as well.

---

## Features

* **Summon Anya:** Spawn the Anya entity via `/summon` or by interacting with the spawn egg.
* **Follow Player:** Anya can follow a specific player when enabled.
* **Interactive Commands:** Players can trigger actions with natural language phrases like:

  * `Anya, come on!` – summon and follow the player
  * `Anya, go play` or `Anya, давай поиграем` – Anya creates a 'baby' villager with heart particles
* **AI Response System:** Anya reacts to actions such as getting hit or being spoken to.
* **Sleep Behavior:** Anya can go to bed when the player is on a bed, sleep through the night, and wake up at daytime.
* **Custom Name & Skin:** Anya has a unique texture and name (`AnyaChan`) with a fixed UUID.
* **Micro Villager Spawn:** Performing specific interactions causes a tiny villager to appear, representing the player-Anya interaction.

---

## Installation

1. Install **Fabric Loader** for Minecraft 1.21.x.
2. Install **FabricAPI** (mod) for Minecraft 1.21.x
3. Place the `anyabotfabric.jar` into the `mods` folder.
4. Start the Minecraft client or server.

---

## Notes

* Requires Fabric API.
* Primarily intended for immersive single-player or small server gameplay.
* Supports both Russian and English command inputs for core actions.
* Players can manage which Anya entity is associated with them.
* If you have some problems, please write your error (or server's crash log) in Github Issues.

---

## Example Commands / Phrases

* `Anya, come on!` – summon Anya and start following.
* `Anya, go play` – triggers the creation of a shared 'baby' villager.

---

**Author:** FlorestDev
**Modrinth**: https://modrinth.com/mod/anyabotfabric
