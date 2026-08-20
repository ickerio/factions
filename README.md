<h1 align="center">Factions</h1>

<p align="center">
  <strong>Server-side factions for Minecraft Fabric 26.2.</strong><br>
  Ultralight, dependency-free, event-driven. Claims, power economy, GUI trading, guest quotas, gathering hall, permission API, and a public <code>ClaimPermissions</code> surface for third-party mod integration.
</p>

<p align="center">
  <a href="https://github.com/joshes14/factions/releases/latest"><img alt="Release" src="https://img.shields.io/github/v/release/joshes14/factions?display_name=tag&sort=semver"></a>
  <a href="https://github.com/joshes14/factions/actions/workflows/build.yml"><img alt="Build" src="https://github.com/joshes14/factions/actions/workflows/build.yml/badge.svg"></a>
  <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-26.2-brightgreen">
  <img alt="Fabric" src="https://img.shields.io/badge/Loader-Fabric%200.19.3%2B-9146FF">
  <img alt="Java" src="https://img.shields.io/badge/Java-25-orange">
  <a href="LICENSE"><img alt="License" src="https://img.shields.io/badge/License-MIT-lightgrey"></a>
</p>

---

## Table of Contents

- [At a glance](#at-a-glance)
- [Feature matrix](#feature-matrix)
- [Compatibility](#compatibility)
- [Install](#install)
- [Commands reference](#commands-reference)
- [Configuration reference](#configuration-reference)
- [Public API](#public-api)
- [Architecture](#architecture)
- [Security trade-offs and limitations](#security-trade-offs-and-limitations)
- [Build from source](#build-from-source)
- [Troubleshooting](#troubleshooting)
- [Changelog](#changelog)
- [Credits and upstream](#credits-and-upstream)
- [License](#license)

---

## At a glance

Factions is a **server-side-only** Fabric mod. Nothing to install on the client — one jar in the server's `mods/` directory adds 25 commands, 12 configurable subsystems, sgui-based inventory UIs (admin panel, info screens, two-phase trade GUI), and a stable Java API for other server mods to consult claim rules before they mutate the world. Persistence is a small JSON store under `<world>/factions/` (no external database). Optional integrations with Dynmap, BlueMap, squaremap, PlaceholderAPI, LuckPerms, and StyledChat are auto-detected at load time — none are required.

Power economy holds the whole design together. A faction can claim `claimCount * CLAIM_WEIGHT ≤ power` chunks; die too much and your claims lose protection until you regain power. That single invariant is enforced by [`ClaimPermissions.check`](src/main/java/io/icker/factions/api/ClaimPermissions.java:64) for every break, place, and use event.

## Feature matrix

Every feature entry links directly to the code that implements it.

| Area | Feature | Config toggle | Implementation |
|---|---|---|---|
| Factions | Create / disband / invite / join / leave | `maxFactionSize` | [`CreateCommand`](src/main/java/io/icker/factions/command/CreateCommand.java), [`DisbandCommand`](src/main/java/io/icker/factions/command/DisbandCommand.java), [`InviteCommand`](src/main/java/io/icker/factions/command/InviteCommand.java), [`JoinCommand`](src/main/java/io/icker/factions/command/JoinCommand.java), [`LeaveCommand`](src/main/java/io/icker/factions/command/LeaveCommand.java) |
| Factions | Rank hierarchy (OWNER > LEADER > COMMANDER > MEMBER > GUEST) | — | [`User.Rank`](src/main/java/io/icker/factions/api/persistents/User.java:29), [`RankCommand`](src/main/java/io/icker/factions/command/RankCommand.java) |
| Factions | Kick / member listing | — | [`KickCommand`](src/main/java/io/icker/factions/command/KickCommand.java), [`MemberCommand`](src/main/java/io/icker/factions/command/MemberCommand.java) |
| Claims | Claim / autoclaim / multi-chunk claim | `power.*`, `claimProtections` | [`ClaimCommand`](src/main/java/io/icker/factions/command/ClaimCommand.java), [`Claim`](src/main/java/io/icker/factions/api/persistents/Claim.java) |
| Claims | Strict claim exclusivity (no overtake) | — | [`ClaimCommand`](src/main/java/io/icker/factions/command/ClaimCommand.java) |
| Claims | Access-level enforcement per chunk | — | [`InteractionManager`](src/main/java/io/icker/factions/core/InteractionManager.java), [`ClaimPermissions`](src/main/java/io/icker/factions/api/ClaimPermissions.java) |
| Claims | ASCII map | — | [`MapCommand`](src/main/java/io/icker/factions/command/MapCommand.java) |
| Movement | `/f home` (per-faction home + cooldown + combat block) | `home.*` | [`HomeCommand`](src/main/java/io/icker/factions/command/HomeCommand.java), [`Home`](src/main/java/io/icker/factions/api/persistents/Home.java) |
| Movement | `/f tp <factionmate>` with consent | `teleport.*` | [`TeleportCommand`](src/main/java/io/icker/factions/command/TeleportCommand.java), [`TeleportRequestManager`](src/main/java/io/icker/factions/core/TeleportRequestManager.java) |
| Movement | `/f gather` — teleport to gathering hall | `gather.*` | [`GatherCommand`](src/main/java/io/icker/factions/command/GatherCommand.java) |
| Movement | `/f trade <faction>` — two-phase item trading GUI | `tradeStage.*` | [`TradeCommand`](src/main/java/io/icker/factions/command/TradeCommand.java), [`TradeSession`](src/main/java/io/icker/factions/core/TradeSession.java), [`TradePlacementGui`](src/main/java/io/icker/factions/ui/TradePlacementGui.java), [`TradeRevealGui`](src/main/java/io/icker/factions/ui/TradeRevealGui.java) |
| Diplomacy | Ally / neutral / enemy relationships + permission grants | `relationships.*` | [`DeclareCommand`](src/main/java/io/icker/factions/command/DeclareCommand.java), [`PermissionCommand`](src/main/java/io/icker/factions/command/PermissionCommand.java), [`Relationship`](src/main/java/io/icker/factions/api/persistents/Relationship.java) |
| Guests | Break / place quotas for non-members | `guestGrant.*` | [`GrantCommand`](src/main/java/io/icker/factions/command/GrantCommand.java), [`GuestGrant`](src/main/java/io/icker/factions/api/persistents/GuestGrant.java) |
| Guests | Restricted-item blocklist (fire, lava, etc.) | `guestGrant.restrictedItems` | [`ClaimPermissions.isRestrictedForGuests`](src/main/java/io/icker/factions/api/ClaimPermissions.java:146) |
| Storage | Shared faction safe (double chest) | `safe.*` | [`SafeCommand`](src/main/java/io/icker/factions/command/SafeCommand.java), [`EnderChestBlockMixin`](src/main/java/io/icker/factions/mixin/EnderChestBlockMixin.java) |
| Chat | Focus / faction / global chat modes + tab menu | `display.*` | [`ChatManager`](src/main/java/io/icker/factions/core/ChatManager.java), [`SettingsCommand`](src/main/java/io/icker/factions/command/SettingsCommand.java) |
| UX | Location Announcer (actionbar territory name) | `announcer.*` | [`AnnouncerManager`](src/main/java/io/icker/factions/core/AnnouncerManager.java) |
| Rules | Per-faction gameplay rules (e.g. elytra flight) | `/f rules` | [`RulesCommand`](src/main/java/io/icker/factions/command/RulesCommand.java) |
| Admin | Admin GUI, bypass, spoof-user | `requiredBypassLevel` | [`AdminCommand`](src/main/java/io/icker/factions/command/AdminCommand.java), [`AdminGui`](src/main/java/io/icker/factions/ui/AdminGui.java) |
| Integrations | Dynmap / BlueMap / squaremap claim overlays | auto-detected | [`DynmapWrapper`](src/main/java/io/icker/factions/util/DynmapWrapper.java), [`BlueMapWrapper`](src/main/java/io/icker/factions/util/BlueMapWrapper.java), [`SquareMapWrapper`](src/main/java/io/icker/factions/util/SquareMapWrapper.java) |
| Integrations | Xaero's Minimap claim sync | `/f xaerosync` | [`XaeroSyncCommand`](src/main/java/io/icker/factions/command/XaeroSyncCommand.java), [`ClaimSyncSender`](src/main/java/io/icker/factions/core/ClaimSyncSender.java) |
| Integrations | PlaceholderAPI expansion | auto-detected | [`PlaceholdersWrapper`](src/main/java/io/icker/factions/util/PlaceholdersWrapper.java) |
| Integrations | LuckPerms fabric-permissions-api hooks | — | permission checks throughout |
| Integrations | StyledChat compat | auto-detected | [`StyledChatCompatibility`](src/main/java/io/icker/factions/util/StyledChatCompatibility.java) |
| API (3.4+) | `ClaimPermissions` — public permission surface | — | [`ClaimPermissions`](src/main/java/io/icker/factions/api/ClaimPermissions.java) |
| API | Event bus — faction, claim, player, relationship, misc | — | [`FactionEvents`](src/main/java/io/icker/factions/api/events/FactionEvents.java), [`ClaimEvents`](src/main/java/io/icker/factions/api/events/ClaimEvents.java), [`PlayerEvents`](src/main/java/io/icker/factions/api/events/PlayerEvents.java), [`RelationshipEvents`](src/main/java/io/icker/factions/api/events/RelationshipEvents.java), [`MiscEvents`](src/main/java/io/icker/factions/api/events/MiscEvents.java) |

## Compatibility

| Dependency | Version | Interaction |
|---|---|---|
| Minecraft | `~26.2` | required |
| Fabric Loader | `>=0.19.3` | required (raised in 3.4 for Carry On compat) |
| Fabric API | `0.155.2+26.2` | required |
| Java | `>=25` | required |
| fabric-permissions-api-v0 | any | suggested (LuckPerms hooks) |
| Dynmap | `>=3.0` | optional, auto-detected |
| BlueMap | `>=2.0` | optional, auto-detected |
| squaremap | `>=1.0` | optional, auto-detected |
| PlaceholderAPI | `3.1.0-beta.1+26.2` | optional, auto-detected |
| StyledChat | any | optional, auto-detected |
| Carry On | `>=2.11.1` | optional, calls `ClaimPermissions` reflectively |

> [!NOTE]
> This mod is **server-side only**. Clients need Fabric API and Fabric Loader to connect to a Fabric server (as they would for any Fabric mod), but do not need Factions itself installed.

## Install

### Server (production)

1. Confirm server is Fabric 0.19.3+ on Minecraft 26.2 with Fabric API `0.155.2+26.2` installed.
2. Drop `factions-mc26.2-3.4.jar` from the [latest release](https://github.com/joshes14/factions/releases/latest) into `mods/`.
3. Start the server. First-line confirmation in `logs/latest.log`:
   ```
   [INFO] [Factions] Initialized Factions Mod
   ```
4. A `config/factions.json` is written on first launch. See [Configuration reference](#configuration-reference) for defaults.
5. Player data appears under `<world>/factions/` (JSON files, one per persistent entity type).

### Client

No client install required. Vanilla Fabric clients connect fine.

### Development (singleplayer / test server)

`./gradlew runServer` — launches an integrated dev server at `run/`. All Factions commands available immediately. Loom config in `build.gradle` splits client/server sourcesets and uses `log4j-dev.xml` for dev-only logging.

## Commands reference

Every command is registered under both `/factions ...` and `/f ...`. Twenty-five subcommands.

| Command | Rank required | Effect |
|---|---|---|
| `/f create <name>` | any | Create a new faction; caller becomes OWNER |
| `/f disband` | OWNER | Disband the faction, clear all claims and grants |
| `/f invite <player>` | LEADER+ | Invite a player to your faction |
| `/f join <faction>` | any | Join an open faction or accept an invite |
| `/f leave` | any (not OWNER) | Leave your faction |
| `/f kick <player>` | LEADER+ | Kick a member (falls back to UUID lookup for offline targets) |
| `/f rank <player> <rank>` | OWNER | Promote/demote a member |
| `/f info [faction]` | any | Show faction stats, members, MOTD |
| `/f list` | any | Paginated list of all factions |
| `/f member` | any | Show your faction's roster |
| `/f map` | any | ASCII map of nearby chunks |
| `/f modify <name\|desc\|motd\|color\|open>` | LEADER+ | Edit faction metadata |
| `/f claim [add [size] [force]\|remove\|list\|auto]` | LEADER+ | Claim / unclaim chunks; multi-chunk claims charge power for every genuinely new chunk (fixed in 3.1.1) |
| `/f home [set]` | any / LEADER+ set | Teleport to faction home; combat-locked, cooldown-gated |
| `/f gather` | any | Teleport to the configured gathering hall (3.2) |
| `/f tp <player\|accept\|deny>` | any | Consent-based teleport to a factionmate (3.1) |
| `/f trade <faction\|accept\|deny>` | LEADER+ | Two-phase trading GUI with another faction (3.3) |
| `/f safe` | any | Open the shared faction safe (double chest) |
| `/f declare <faction> <ally\|enemy\|neutral>` | LEADER+ | Set relationship |
| `/f permission <faction> <perm> <allow\|deny>` | LEADER+ | Grant a permission to another faction |
| `/f rules <rule> <enabled\|disabled>` | LEADER+ | Toggle per-faction rules (e.g. `elytra`, `mobGriefing`) |
| `/f grant <player> <break\|place> <n>` | LEADER+ | Grant a guest break/place quota (3.1) |
| `/f grant revoke <player>` | LEADER+ | Revoke a grant |
| `/f grant list` | LEADER+ | List active grants |
| `/f settings` | any | Personal settings (chat mode, sound mode, autoclaim) |
| `/f admin` | op / `factions.admin.gui` | Open the admin GUI |
| `/f xaerosync` | any | Force-resync Xaero's Minimap claim overlay |

Tab completion uses live player names where possible and falls back to the profile cache for offline targets. `/f tp` and `/f kick` no longer suggest raw UUIDs (fixed in 3.1.1).

## Configuration reference

Written to `config/factions.json` on first launch. Missing keys use defaults; extra keys are ignored. Full definition: [`Config.java`](src/main/java/io/icker/factions/config/Config.java).

**Root**

| Field | Default | Purpose |
|---|---|---|
| `version` | `3` | Config schema version; mismatch logs a warning but does not abort |
| `gui` | `true` | Enable sgui-based UIs (admin, info, modify, trade) |
| `blockTNT` | `false` | Cancel TNT explosions inside claims regardless of other rules |
| `maxFactionSize` | `-1` | Member cap (`-1` = unlimited) |
| `friendlyFire` | `false` | Allow factionmates to damage each other |
| `requiredBypassLevel` | `2` | OP level required to use `/f admin bypass` |
| `claimProtections` | `true` | Master switch for claim-permission enforcement |
| `language` | `"en_us"` | Server default lang; individual clients override |

**Power** ([`PowerConfig`](src/main/java/io/icker/factions/config/PowerConfig.java))

Governs the claim economy: how much power a member starts with, how death costs, and how much power a claim consumes.

**Guest grant** ([`GuestGrantConfig`](src/main/java/io/icker/factions/config/GuestGrantConfig.java))

| Field | Default | Purpose |
|---|---|---|
| `maxBreak` | `64` | Cap on a single break grant |
| `maxPlace` | `64` | Cap on a single place grant |
| `restrictedItems` | `[flint_and_steel, fire_charge, lava_bucket]` | Items non-members can never use in claims, regardless of grant |

**Announcer** ([`AnnouncerConfig`](src/main/java/io/icker/factions/config/AnnouncerConfig.java))

| Field | Default | Purpose |
|---|---|---|
| `enabled` | `true` | Show faction name on actionbar when crossing territory borders |
| `displaySeconds` | `5` | Hold time for the announcement text |

The `style` field was removed in 3.1.1 — existing configs containing `"style"` are silently ignored.

**Safe** ([`SafeConfig`](src/main/java/io/icker/factions/config/SafeConfig.java))

| Field | Default | Purpose |
|---|---|---|
| `enderChest` | `false` | When `true`, replaces the vanilla ender chest with the shared faction safe for members. Default changed to `false` in 3.2 |

> [!WARNING]
> Enabling `safe.enderChest` means every faction member opens the **same** container from any ender chest. As of 3.4, non-faction players fall through to their personal ender chests correctly. Faction members do not.

**Home** ([`HomeConfig`](src/main/java/io/icker/factions/config/HomeConfig.java))

Cooldown + combat-block config for `/f home`.

**Teleport** ([`TeleportConfig`](src/main/java/io/icker/factions/config/TeleportConfig.java))

| Field | Default | Purpose |
|---|---|---|
| `enabled` | `true` | Enable `/f tp` |
| `cooldownSeconds` | `15` | Cooldown between successful teleports (mirrors `/f home`) |
| `requestExpirySeconds` | `60` | Pending request timeout |
| `damageCooldown` | `100` | Combat-block window (ticks) after taking damage |

**Gather** ([`GatherConfig`](src/main/java/io/icker/factions/config/GatherConfig.java))

Coordinates + dimension for the `/f gather` hall.

**Trade stage** ([`TradeStageConfig`](src/main/java/io/icker/factions/config/TradeStageConfig.java))

Coordinates for the two facing positions where `/f trade` deposits both parties. See CHANGELOG 3.3 for the full schema.

**Display** ([`Config.DisplayConfig`](src/main/java/io/icker/factions/config/Config.java:136))

| Field | Default | Purpose |
|---|---|---|
| `factionNameMaxLength` | `-1` | Cap on `/f create` name length (`-1` = unlimited) |
| `changeChat` | `true` | Modify chat format with faction prefix |
| `tabMenu` | `true` | Replace vanilla tab list with grouped faction view |
| `nameBlackList` | `[wilderness, factionless, без фракции]` | Disallowed faction names |
| `powerMessage` | `true` | Send power-change notifications |

**Relationships** ([`Config.RelationshipConfig`](src/main/java/io/icker/factions/config/Config.java:154))

| Field | Default | Purpose |
|---|---|---|
| `allyOverridesPermissions` | `true` | Allies inherit member-level access on member-tier claims |
| `defaultGuestPermissions` | `[USE_BLOCKS, USE_ENTITIES]` | Baseline guest access on newly created factions |

## Public API

The `io.icker.factions.api` package is the stable, additive surface for third-party server mods. Depending on it does not require any Factions-side wiring — call it reflectively if you want soft-compat.

### `ClaimPermissions` (added in 3.4)

Consult the same decision engine `InteractionManager` uses internally, without duplicating the bypass/ownership/power/quota/ally logic.

```java
import io.icker.factions.api.ClaimPermissions;
import io.icker.factions.api.persistents.Relationship.Permissions;

// Dry-run: "will this succeed?" — never mutates state.
ClaimPermissions.canPlaceBlock(serverPlayer, pos);
ClaimPermissions.canPlaceBlock(serverPlayer, pos, itemStack);    // honors restrictedItems

// Attempt: allow AND consume a guest quota if that was the mechanism that permitted it.
ClaimPermissions.tryPlaceBlock(serverPlayer, pos);
ClaimPermissions.tryPlaceBlock(serverPlayer, pos, itemStack);

// General predicate.
InteractionResult r = ClaimPermissions.check(
    player, pos, level, Permissions.BREAK_BLOCKS,
    /* consumeQuota */ true,
    itemUsed
);

// Utility checks.
ClaimPermissions.isRestrictedForGuests(itemStack);
ClaimPermissions.isNonMemberInClaim(player, pos, level);
```

- **Dry-run** variants never mutate state.
- **Consuming** variants decrement guest quotas exactly once and warn via `InteractionsUtil`.
- `ItemStack.EMPTY` skips the restricted-item check.
- Full source: [`ClaimPermissions.java`](src/main/java/io/icker/factions/api/ClaimPermissions.java).

### Event bus

Register listeners via the `EVENT.register(...)` pattern on any of:

- [`FactionEvents`](src/main/java/io/icker/factions/api/events/FactionEvents.java) — CREATE, DISBAND, MODIFY, POWER_CHANGE, MEMBER_JOIN, MEMBER_LEAVE
- [`ClaimEvents`](src/main/java/io/icker/factions/api/events/ClaimEvents.java) — CLAIM, UNCLAIM
- [`PlayerEvents`](src/main/java/io/icker/factions/api/events/PlayerEvents.java) — CHAT, JOIN, LEAVE, KILL
- [`RelationshipEvents`](src/main/java/io/icker/factions/api/events/RelationshipEvents.java) — DECLARE
- [`MiscEvents`](src/main/java/io/icker/factions/api/events/MiscEvents.java) — server lifecycle

## Architecture

```
Player action (break / place / use / attack / chat / cmd)
        │
        ▼
┌────────────────────────────────────────────────┐
│  Mixin layer (10 mixins)                        │
│  - EnderChestBlockMixin / BaseContainerBEMixin  │
│  - ServerExplosionMixin / CombatTrackerAccessor │
│  - ServerGamePacketListenerImplMixin (movement) │
│  - ServerPlayerGameModeMixin (break events)     │
│  - PlayerListMixin (tab menu)                   │
└────────────────────────────────────────────────┘
        │
        ▼
┌────────────────────────────────────────────────┐
│  Core managers                                  │
│  InteractionManager ─ break/place/use routing   │
│  FactionsManager    ─ safe container lifecycle  │
│  ChatManager        ─ chat modes + tab menu     │
│  AnnouncerManager   ─ actionbar territory name  │
│  TeleportRequest*   ─ /f tp state machine       │
│  TradeSession       ─ /f trade phase FSM        │
│  ServerManager      ─ save/tick hooks           │
└────────────────────────────────────────────────┘
        │
        ▼
┌────────────────────────────────────────────────┐
│  API layer                                      │
│  ClaimPermissions.check() ─ single source of    │
│  truth: bypass? ownership? power sufficient?    │
│  guest permissions? guest grant? ally override? │
│  relationship permission?                       │
└────────────────────────────────────────────────┘
        │
        ▼
┌────────────────────────────────────────────────┐
│  Persistents (JSON store)                       │
│  Faction · User · Claim · Home · Relationship   │
│  GuestGrant · FactionClaimCounts                │
│  Database.load / Database.save (throttled)      │
└────────────────────────────────────────────────┘
        │
        ▼
   <world>/factions/*.json  +  Fabric event bus
```

### Why the mixin layer?

Fabric events cover many of the hooks Factions needs (block break, entity interact, chat, player join) but not all (ender chest replacement, container access from adjacent chunks, explosion attribution). Mixins fill exactly those gaps — every mixin is documented at the top of its file with the reason it exists.

### Why is `ClaimPermissions` a separate class from `InteractionManager`?

Before 3.4, the entire permission decision tree lived inline inside `InteractionManager.checkPermissions`. Third-party mods (Carry On being the immediate motivator) needed to reproduce that logic for compatibility. Extracting it into a stable, additive API class means:

1. Other mods consult the exact same decision engine — no drift.
2. Refactoring the internal check flow does not break external callers as long as `ClaimPermissions.check` preserves its input/output contract.
3. Callers can dry-run (`canPlaceBlock`) or commit-and-consume (`tryPlaceBlock`) — a distinction that matters for guest quotas.

### Why JSON persistence?

Factions has always aimed for zero-setup deployment. A SQLite dependency triggers native-lib version pain across mod platforms; a Postgres dependency is absurd for a per-world data store. JSON files under `<world>/factions/` are trivially readable, trivially backupable, and cheap to load at server start. Write throttling ([`GuestGrant.saveThrottled`](src/main/java/io/icker/factions/api/persistents/GuestGrant.java:92)) keeps the block-interaction path allocation-free.

## Security trade-offs and limitations

Documented deliberately. None are regressions.

> [!WARNING]
> **Overclaimed factions lose all protection.** When a faction's `claimCount * CLAIM_WEIGHT` exceeds its current power, every interaction inside its claims — including chest access, sign edits, and entity attacks — passes without a permission check. This is intentional gameplay design (it creates the raid window), but it means an overclaimed faction cannot rely on Factions to guard sensitive containers.

> [!WARNING]
> **`/f tp accept` checks combat status on the requester, not the target.** A player under attack can summon a factionmate as reinforcement. This is an inherent consequence of the consent-based teleport design. If your server bans combat log or reinforcement, disable `/f tp` via `teleport.enabled = false`.

> [!WARNING]
> **Guest-quota grants only resolve online players.** `/f grant <player>` and `/f tp` look up targets by name among currently-online players. Offline targeting requires a UUID and is not exposed via command.

> [!NOTE]
> **Teleport request stomping.** Only one pending `/f tp` request is tracked per target. A same-faction griefer can repeatedly overwrite a teammate's legitimate request, delaying accepts. Kick the griefer.

> [!NOTE]
> **Explosions consume guest quota per block.** A guest igniting TNT can spend their entire break quota in one blast. Considered correct (each destroyed block is one break). Raise `guestGrant.maxBreak` if you want more tolerant defaults.

> [!NOTE]
> **`/f xaerosync` is ungated.** No permission requirement, no cooldown, rebuilds the full claim payload per invocation. Cheap enough on small servers; consider gating via LuckPerms on larger deployments.

> [!NOTE]
> **Admin GUI permission split.** Opening the admin GUI requires `factions.admin.gui`, but its spoof/bypass buttons do not re-check `factions.admin.spoof` / `factions.admin.bypass`. Only relevant if you configure those permissions separately.

## Build from source

```bash
git clone https://github.com/joshes14/factions.git
cd factions
chmod +x ./gradlew
./gradlew clean build
```

Output: `build/libs/factions-mc26.2-3.4.jar` (main) and `build/libs/factions-mc26.2-3.4-sources.jar` (sources).

Tests: `./gradlew test` (JUnit 5, no external dependencies).

Dev server: `./gradlew runServer`.

## Troubleshooting

### Symptom: `Initialized Factions Mod` missing from `logs/latest.log`

Mod jar not picked up. Check:
- Server is Fabric (not Forge / Paper).
- Loader is `>= 0.19.3` — check `logs/latest.log` for the loader banner.
- File is in `mods/`, not `plugins/` or `datapacks/`.

### Symptom: `Config file incompatible (requires version 3)`

Your `config/factions.json` has a lower `version` field than this build expects. Back up the file, delete it, restart the server, then merge your customizations back into the freshly generated config.

### Symptom: `/f trade` teleports both parties but no GUI appears

Check `tradeStage.enabled` is `true` and the configured coordinates are reachable in the configured level. If both are set, the sgui integration is present at load time (the mod bundles sgui internally via Gradle `include`, so this should never fail on a supported Fabric version).

### Symptom: Non-member cannot open their personal ender chest

Fixed in 3.4. Confirm the running jar is `3.4` (jar filename or `logs/latest.log` version line). Downgrade will restore the bug.

### Symptom: Guest quota drains without a break happening

Fixed in 3.1.0 for bucket use (used to charge 2–3× per action). If still observed on 3.1.0+, the block is likely being broken by an explosion — one destroyed block = one break, by design.

### Symptom: `/f tp` suggestions show raw UUIDs

Fixed in 3.1.1. Confirm the running jar is `>= 3.1.1`.

## Changelog

Full history including pre-release notes: [`CHANGELOG.md`](CHANGELOG.md).

**3.4** — `ClaimPermissions` public API for third-party mod integration, Carry On mod compat, ender chest fallback fix for non-faction players. Fabric Loader minimum raised to 0.19.3.

**3.3** — Two-phase trade GUI on `/f trade`, per-faction elytra flight rule, removed `/f settings radar`.

**3.2** — `/f gather` (gathering hall teleport), `/f trade <faction>` (trade requests), `/f claim` restricted to LEADER+, `safe.enderChest` defaults to `false`.

**3.1.1** — `/f tp` and `/f kick` tab-completion name-based, multi-chunk claim power fix, dead `announcer.style` removed.

**3.1.0** — Location Announcer, guest break/place quotas, `/f tp` consent teleport, strict claim exclusivity, performance pass.

## Credits and upstream

Downstream fork of [`ickerio/factions`](https://github.com/ickerio/factions) (MIT). Original authors: [ickerio](https://github.com/ickerio), [BlueZeeKing](https://github.com/BlueZeeKing), [PyPylia](https://github.com/PyPylia). This fork is maintained by [joshes14](https://github.com/joshes14) and tuned for the `yayserver2` deployment.

Both the upstream copyright (2023) and the downstream fork copyright (2026) are preserved in [`LICENSE`](LICENSE).

## License

[MIT](LICENSE) — permissive, requires attribution. See the LICENSE file for the full text and copyright chain.
