<div align="center">

<img alt="Factions Mod Icon" src="src/main/resources/assets/factions/icon.png" width="128">

# Factions Mod

Highly customizable, lightweight and elegant factions mod for the [Fabric loader][fabric] in Minecraft 1.17

[![Release](https://img.shields.io/github/v/release/ickerio/factions=semver)][releases]

</div>

Factions Mod is an ultra lightweight, fast, and elegant solution to factions in modern minecraft. The **server-side** mod expands upon all the classic factions features whilst also focusing on customizability and performance. Grow your faction, expand your claims, and storm your enemies for their chunks and loot.

A faction's power cap increases as new members join, expanding their ability to claim more land. For each claim they make, it requires that faction to sustain more power. Dying to other players will temporarily lose faction power and if it drops below the required threshold, all their claims will be vulnerable to being overtaken. 

# Getting Started
 - Set up your [fabric server][fabric:install]
 - Download the latest version of Factions in [releases][releases]
 - Drag the Factions jar file into the mods folder
 - Optionally fonfigure `config.json` in the `factions` directory (*coming soon*)

Factions Mod works out of the box and requires no additional configuration.

# Commands

All commands begin with `/factions` (or `/f` shortcut)

### Essentials 

/f create &lt;faction name&gt; *Create a faction*

/f join &lt;faction name&gt; *Joins a faction*

/f leave &lt;faction name&gt; *Leaves a faction*

/f info &lt;faction name&gt; *Basic info about a faction*

/f info *Basic info about the faction you're in*

/f list *Basic info about all factions*

### Faction Management

/f claim *Claims the chunk you're standing in*

/f claim remove *Removes the chunk claim you're standing in*

/f home *Warps to designated faction home*

/f home set *Sets faction home to current position*

/f invite add &lt;player name&gt; *Invite player to join your faction*

/f invite list *Lists all your faction outgoing invites*

/f invite remove &lt;player name&gt; *Removes player invite to your faction*

### Faction Settings

/f modify open [true / false] *Set faction to public or invite only*

/f modify description &lt;faction description&gt; *Set faction description*

/f modify color &lt;color&gt; *Set faction color*


# Upcoming Features

- Fully customisable configuration
- Faction privileges (owner, admin etc)
  - promote, kick, ban commands
- Removing desync when interacting with claimed chunks
- Scoreboard
  - Coloured name tags
  - Customized chat
- Custom language options

Have an issue or a suggestion? Join [our discord](https://discord.gg/tHPFegeAY8)!

# License
[MIT](LICENSE)

[fabric]: https://fabricmc.net/
[fabric:install]: https://fabricmc.net/use/?page=server
[releases]: https://github.com/ickerio/factions/releases