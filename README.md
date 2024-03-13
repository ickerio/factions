<div align="center">

<img alt="Factions Mod Icon" src="src/main/resources/assets/factions/icon.png">

# Factions Mod

Highly customizable, lightweight and elegant factions mod for the [Fabric loader][fabric] in Minecraft 1.18

[![Release](https://img.shields.io/github/v/release/ickerio/factions?style=for-the-badge&include_prereleases&sort=semver)][github:releases]
[![Available For](https://img.shields.io/badge/dynamic/json?label=Available%20For&style=for-the-badge&color=e64626&query=gameVersion&url=https://api.blueish.dev/api/curseforge/497362)][curseforge]
 [![Downloads](https://img.shields.io/badge/dynamic/json?label=Downloads&style=for-the-badge&color=e64626&query=downloads&url=https://api.blueish.dev/api/curseforge/497362)][curseforge:releases]

</div>

### **ABOUT**

Factions Mod is an ultra lightweight, fast, and elegant solution to factions in modern minecraft. The **server-side** mod expands upon all the classic factions features whilst also focusing on customization and performance. Grow your faction, expand your claims, and storm your enemies for their chunks and loot.

A faction's power cap increases as new members join, expanding their ability to claim more land. For each claim they make, it requires that faction to sustain more power. Dying to other players will temporarily lose faction power and if it drops below the required threshold, all their claims will be vulnerable to being overtaken.

&nbsp;

### **FEATURES**

- 🎯 Fully featured factions mod with over 30 [commands][wiki:commands]
- ✨ Faction ranks, colors, MOTD and descriptions
- 🎉 In faction private chat, global chat and a stylized player list
- ⚡ Extreme performance and reliability
- ⚙️ Advanced [configuration][wiki:config] and customization options
- 🔥 Dynmap and Lucko Perms support out the box
- 🚀 Event driven API for further extensibility 
- 💬 Strong [community][discord] and active developer support

**UPDATE FROM RASUS:**

- Compatible with `Create` and `Create: Big Cannons`
- Has a daily tax system and resource-based economy (Diamond-currency)
- Has a simple relations and war system (if relationship is at minimum - there is a war)


### **WHAT SHOULD RASUS DO IN THE NEXT UPDATES:**
- **Making a simple interface based on double-chest ScreenHandler**

- **Upgrading the war system:**

  - Implementing a "dishonor" variable (Like in `Victoria II`):
    - The more "dishonor" you have - the less it will take to justify war goal on you or for you
    - The more "dishonor" you have - the more it will take you to improve relations with others or others with you
    - If you take too much land in wars - you will gain dishonor
    - The more cost will have your war justification - the more dishonor you gain
    - If you share the land with others or lose war - you will lower your dishonor
    - With each member of your alliance - you will lower your dishonor
  - Vassal-suzerain realization:
    - Free states can join the kingdoms or subjugate another states or join alliances
    - Kingdoms can only subjugate anothers or join the alliances
    - Vassals can't do anything except grant territories to metropoly or declare the war of independence
    - Vassals will have zero dishonor value in any case
    - Vassals will have to pay taxes to their suzerain. Maximum tax from metropoly is configurable.
  - Making the war goal types with different declaring cost multipliers from lowest to highest:
    - Liberation: liberate yourself or another vassal. Modifier - 0.2
    - Vassalization: Make another state (re-)vassalized. Modifier - 0.2
      - Occupied chunks will be granted to the metropoly during war.
      - After victory, it will be transfered to previous leader of this state, but it will be your vassal
      - If the agressor defeats - he will be forced to pay the reparations
    - Puppeting: Same as above, but changing the leader to that one that you have written in war goal.
      - When the war starts - there will be created another puppet faction
      - The newborn puppet faction will have your vassal that you have declared in war goal
      - The newborn puppet faction will have maximum relationship points with you mutually.
      - If agressor fails - he will be forced to pay reparations
    - Integration: The enemy's chunks will be granted to your metropoly
      - If the agressor fails - he will be vassalized to the victor
    - Henocide: The enemy state will be vanished
      - If the agressor fails - he will be puppeted by the victor

&nbsp;

### **GET STARTED**

Factions Mod is very intuitive and works immediately after installation, requiring no additional configuration. However, you can read further about the mod on the [Wiki][wiki]. Our wiki goes in depth about the factions mechanics, its configuration, commands and integrations.

A list of all **commands** is available on our [wiki][wiki:commands]

Have an issue or a suggestion? Join [our discord][discord]

### **License**
[MIT](LICENSE)

[fabric]: https://fabricmc.net/
[curseforge]: https://www.curseforge.com/minecraft/mc-mods/factions-fabric
[curseforge:releases]: https://www.curseforge.com/minecraft/mc-mods/factions-fabric/files
[github:releases]: https://github.com/ickerio/factions/releases
[wiki]: https://github.com/ickerio/factions/wiki
[wiki:config]: https://github.com/ickerio/factions/wiki/Config
[wiki:commands]: https://github.com/ickerio/factions/wiki/Commands
[discord]: https://discord.gg/tHPFegeAY8
