# Changelog

All notable changes to this mod are documented here.

Format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [3.4] — 2026-08-20

**Minecraft 26.2 · Fabric Loader 0.19.3 · Fabric API 0.155.2+26.2**

Patch release adding a public permission API for third-party mod integration,
Carry On mod support, and fixing one gameplay-affecting enderchest interaction
bug. **Fully save-compatible with 3.3** — no world data migration, no config
migration. **Requires Fabric Loader 0.19.3 or later** (bumped from 0.18.4).

### Added

- **`io.icker.factions.api.ClaimPermissions` — public permission API.** A stable,
  additive surface for other server-side mods to consult claim rules before
  mutating the world. The full decision engine — bypass level, claim ownership,
  power sufficiency, guest permissions, guest-grant quotas, ally overrides, and
  relationship permissions — is exposed as static methods and used internally by
  `InteractionManager` for every break/place/use event.

  ```java
  ClaimPermissions.canPlaceBlock(ServerPlayer, BlockPos)               // dry-run
  ClaimPermissions.canPlaceBlock(ServerPlayer, BlockPos, ItemStack)    // dry-run, honors restrictedItems
  ClaimPermissions.tryPlaceBlock(ServerPlayer, BlockPos)               // allow + consume guest quota
  ClaimPermissions.tryPlaceBlock(ServerPlayer, BlockPos, ItemStack)    // allow + consume, honors restrictedItems
  ClaimPermissions.check(Player, BlockPos, Level, Permissions,
                         boolean consumeQuota, ItemStack usedItem)     // general predicate
  ClaimPermissions.isRestrictedForGuests(ItemStack)
  ClaimPermissions.isNonMemberInClaim(Player, BlockPos, Level)
  ```

  Consuming variants (`tryPlaceBlock`, `check(..., true, ...)`) decrement guest
  quotas exactly once and warn the player via `InteractionsUtil`. Dry-run
  variants never mutate state. `ItemStack.EMPTY` skips the guest-restricted-item
  check.

- **Carry On mod integration.** With Carry On 2.11.1+ installed, block pickup
  and placement now respect Factions claim rules — non-members cannot carry
  blocks out of a claim they lack `BREAK_BLOCKS` on, and cannot place carried
  blocks into a claim they lack `PLACE_BLOCKS` on. Guest grants are consumed
  exactly once per successful action. Carry On calls `ClaimPermissions` via
  optional reflection, so it also works standalone when Factions is absent.

  **Version pairing required.** Carry On 2.11.1+ fails **closed** (denies every
  carried-block placement) if it detects a Factions build older than 3.4 — the
  `ClaimPermissions` class is missing, so the reflective lookup returns nothing
  and the compat layer conservatively rejects. Keep both jars in lockstep on
  servers that use both mods.

### Changed

- **Minimum Fabric Loader raised to 0.19.3** (from 0.18.4). Required for Carry
  On compatibility on Minecraft 26.2. No other API surfaces changed.
- **`InteractionManager` permission logic extracted into `ClaimPermissions`.**
  Internal refactor — every break/place/use decision that lived inline in
  `InteractionManager.checkPermissions` now delegates to
  `ClaimPermissions.check`. Same inputs produce the same `InteractionResult`;
  no gameplay change.

### Fixed

- **Right-clicking an enderchest without being in a faction was blocked with
  "Cannot use enderchests when not in a faction" instead of opening the personal
  enderchest.** When `safe.enderChest=true` (faction safe replaces the vanilla
  enderchest for members), `FactionsManager.openSafe` unconditionally rejected
  non-faction players — sending the `factions.events.no_enderchests_without_faction`
  fail message and returning `InteractionResult.FAIL`, which the
  `EnderChestBlockMixin` propagated to cancel the vanilla open. Non-faction
  players now fall through with `InteractionResult.PASS` so vanilla
  `EnderChestBlock.useWithoutItem` opens their personal enderchest. Faction
  members continue to open the shared faction safe when the feature is enabled.
  When `safe.enderChest=false` the mixin still short-circuits early, so this
  change has no effect there — it only matters when the feature is turned on.

  The `factions.events.no_enderchests_without_faction` translation key is now
  unreferenced by code but left in `en_us.json` and `ru_ru.json` for downstream
  language packs — **no action required**.

### Upgrade from 3.3

1. Stop the server
2. **Verify your Fabric Loader is 0.19.3 or later.** The 3.3 minimum was 0.18.4;
   this release bumps that requirement.
3. Replace `factions-mc26.2-3.3.jar` with `factions-mc26.2-3.4.jar`
4. Start the server

No config edits required. No data migration. Downgrading back to 3.3 is safe.
Third-party mods that reflectively call `ClaimPermissions` will need to handle
its absence when downgraded (Carry On does this automatically).

### Testing

`./gradlew build` exits 0.

---

## [3.3]

**Minecraft 26.2 · Fabric Loader 0.18.4 · Fabric API 0.155.2+26.2**

Release adding a full two-phase item-trading GUI on `/f trade`, a per-faction elytra flight rule, and removing the superseded `/f settings radar` command. **Fully save-compatible with 3.2** — no world data migration, no config migration.

### Added

- **`/f trade` now opens a two-phase trading interface.** On accept, the requesting player
  teleports to a configurable facing-west position and the responding player to a
  facing-east position (both configurable via the new `tradeStage` config block). Each
  player gets a private 27-slot **Placement chest** (Phase 1): place items to trade, then
  click the slimeball to confirm. Once both confirm, a read-only **Reveal chest** (Phase 2)
  shows each other's offer: click the slimeball to **Accept**, the ender pearl to
  **Renegotiate** (returns to Phase 1 with items intact), or magma cream to **Decline**.
  When both accept, items swap atomically on the server main thread with a capacity
  pre-flight — if either player's inventory is full the trade is rejected and all items
  return to their owners. Any disconnect, decline, or GUI-close cancels for both players
  and returns items to each owner's inventory (or drops them at the pre-trade position if
  inventory full).
- **`/f rules elytra <enabled|disabled>` disables elytra flight in faction claims.**
  LEADER/OWNER only; defaults to `enabled` (no behavior change on existing worlds). When
  disabled, any player who enters a claim while fall-flying is force-landed via
  `stopFallFlying()` (vanilla fall damage applies). Applies to all visitors including
  allies; `user.bypass=true` admins are exempt.

### Changed

- **`/f trade` on-accept now opens the trading GUI** instead of teleporting both parties
  to the gathering hall. A new `tradeStage` config block (separate from `gather`)
  controls the two facing positions.

### Removed

- **`/f settings radar`** and the `User.radar` persistent field are removed. The Location
  Announcer (added in 3.1) provides the same territory indicator via the actionbar.
  Existing user JSON records containing `"Radar": true/false` are silently ignored by the
  database layer on load — **no action required**.

### Configuration

New `tradeStage` block written to `config/factions.json` on first launch. Existing
settings are preserved.

```json
{
  "tradeStage": {
    "enabled": true,
    "requesterX": 1.050, "requesterY": 160.125, "requesterZ": -27.500,
    "requesterYaw": 89.7, "requesterPitch": 3.2,
    "recipientX": -2.050, "recipientY": 160.125, "recipientZ": -27.482,
    "recipientYaw": -90.9, "recipientPitch": 3.2,
    "level": "minecraft:overworld"
  }
}
```

### Upgrade from 3.2

1. Stop the server
2. Replace `factions-mc26.2-3.2.jar` with `factions-mc26.2-3.3.jar`
3. Start the server

No config edits required. No data migration. Downgrading back to 3.2 is safe.

### Testing

`./gradlew clean build test` exits 0. All previous tests pass, plus N new suites.

## [3.2]

**Minecraft 26.2 · Fabric Loader 0.18.4 · Fabric API 0.155.2+26.2**

Release adding faction gathering hall travel, faction trade meeting requests, and tighter claim
permissions. **Fully save-compatible with 3.1.1** — no world data migration, no config migration.

### Added

- **`/f gather` teleports a player to the faction gathering hall.** Uses the configured hall
  coordinates for quick regrouping.
- **`/f trade <faction>` sends a trade meeting request to another faction.** On accept, both
  parties teleport to the gathering hall.
- **`/f claim` is now restricted to LEADER/OWNER ranks.** This tightens claim control from the
  previous COMMANDER+ access.

### Changed

- **`safe.enderChest` now defaults to `false`, restoring personal ender chests by default for new installs.** Previously, the default `true` value redirected ender chest use to the shared faction safe, so every member of a faction opened the same container and players without a faction were blocked from ender chests entirely. The faction safe itself is unchanged and still available through `/f safe`. This only affects fresh installs: existing `config/factions.json` files keep their current `enderChest` value, so servers upgrading this release must change it manually to `"enderChest": false` if they want the new default behavior. Items already stored in a faction safe stay there and remain reachable via `/f safe`; they do not migrate into players' personal ender chests.

## [3.1.1]

**Minecraft 26.2 · Fabric Loader 0.18.4 · Fabric API 0.155.2+26.2**

Patch release covering two tab-completion fixes, one gameplay-affecting economy fix, and a
dead-config cleanup. **Fully save-compatible with 3.1.0** — no world data migration, no config
migration. Existing config files containing `announcer.style` are silently ignored by Gson.

### Fixed

- **`/f tp` and `/f kick` tab-completion showed raw UUIDs instead of player names.** When the
  server's `ProfileResolver` had no cached entry for a factionmate (common on servers that
  disable the Mojang profile cache or restart frequently), the suggestion fallback leaked the
  raw UUID string. `/f kick` now prefers the live player's real name for anyone currently
  online, keeping the `ProfileResolver → UUID` chain only for offline members it legitimately
  needs to target. `/f tp` — which can only ever teleport to online players anyway — now
  suggests **only online factionmates**, matching what its executor can actually resolve. No
  more UUID suggestions in either command.
- **`/f claim add <size>` only required power for a single chunk.** The multi-chunk claim path
  checked affordability as though claiming exactly one chunk, so `/f claim add 7` acquired up
  to 169 chunks (a `13×13` square) for the price of one. Power is now required for **every
  genuinely new chunk** in the requested square; chunks already owned by the acting faction
  are re-claimed harmlessly and do not add cost.

  **⚠️ Gameplay-affecting change.** Factions that mass-claimed cheaply under the old rule will
  find that large claims now cost proportionally more. The `1..7` size cap and the
  foreign-claim exclusivity rules are unchanged. **`/f claim add <size> force` still bypasses
  the power check for leaders holding `factions.claim.add.force`** — that path is
  intentional and unchanged.

### Removed

- **`announcer.style` config field and its unused datapack templates.** Nothing in the mod
  ever read `AnnouncerConfig.STYLE`; the announcer builds its styling in code via
  `AnnouncerManager`. The `data/factions/factions/announcer/*.json` files were reference-only
  and shipped for a styling system that was never implemented. Both the field and the
  templates are gone. Existing configs containing `"style": "default"` are ignored by Gson —
  **no action required**.

### Upgrade from 3.1.0

1. Stop the server
2. Replace `factions-mc26.2-3.1.0.jar` with `factions-mc26.2-3.1.1.jar`
3. Start the server

No config edits required. No data migration. Downgrading back to 3.1.0 is safe.

### Testing

`./gradlew clean build test` exits 0. All 42 tests from 3.1.0 still pass, plus 7 new
arithmetic assertions in the new `ClaimPowerTest` suite (49 total, 9 suites).

---

## [3.1.0]

**Minecraft 26.2 · Fabric Loader 0.18.4 · Fabric API 0.155.2+26.2**

Three new features, a performance pass, and fixes for every issue found in a pre-release audit.
Fully backward compatible with 3.0.4 — **no world data migration required**.

### Added

#### Location Announcer
Displays the territory name on your actionbar when you cross into a new area, Zelda/HFW style.

- Shows the **faction name in bold**, coloured with the faction's colour
- Unclaimed land shows a bold white **Wilderness**
- Quick fade-in — the text steps from dark to full colour over ~0.3s, then holds
- Crossing into a new area **always** announces immediately; the previous announcement is cancelled so it fades out cleanly
- Re-sent once per second for the display window, so it doesn't flicker
- Announces only on genuine area changes — staying inside one territory won't repeat
- Works independently of `/f radar` and `/f autoclaim`

Actionbar only. No resource pack, custom font, sounds, bossbar, or title overlay.

#### Guest Break/Place Quotas
Faction leaders can grant a limited number of block breaks and/or places to non-members.

| Command | Description |
|---|---|
| `/f grant <player> break <n>` | Grant `n` block breaks |
| `/f grant <player> place <n>` | Grant `n` block places |
| `/f grant revoke <player>` | Revoke a player's grant |
| `/f grant list` | List all grants for your faction |

- Requires **LEADER** or **OWNER** rank
- Quota is consumed **only** when the action would otherwise be denied — if your faction's `guest_permissions` already allow it, no quota is spent
- One action costs exactly one quota
- A grant is removed automatically once both counters reach zero
- Quotas persist across restarts
- Cannot be granted to a member of your own faction
- Capped by `maxBreak` / `maxPlace`

**Restricted items.** Non-members can never use fire/grief items inside a claim, *regardless of quota or `guest_permissions`*. Default block-list:

```
minecraft:flint_and_steel
minecraft:fire_charge
minecraft:lava_bucket
```

Fully configurable — add `minecraft:tnt` or anything else, or set it to `[]` to disable. Blocked attempts consume **no** quota. **Faction members of every rank, including `GUEST`, are unaffected.**

#### Faction Teleport
Teleport to a factionmate, with consent.

| Command | Run by | Effect |
|---|---|---|
| `/f tp <factionmate>` | requester | Sends a teleport request |
| `/f tp accept` | target | Requester teleports to you |
| `/f tp deny` | target | Rejects the request |

- The request message is **clickable** — the target can click to accept
- Tab-completion only suggests your own factionmates
- Available to **all ranks**, including `GUEST`
- Requests expire after 60s (configurable)
- Cross-dimension teleporting supported
- All conditions are re-validated at accept time, not request time: both players still online, still in the same faction, request unexpired, requester off cooldown and out of combat
- 15s cooldown and combat block mirror `/f home`

### Changed

- **Claim exclusivity is now strict.** A faction can no longer claim a chunk owned by another faction under any circumstances. Admins with `user.bypass` retain the override.
- **Guest grant persistence is now write-throttled.** Previously every quota-consuming block break performed a full synchronous disk write on the main server thread. Writes are now batched (max once per 30s, plus on player disconnect and world save), removing blocking I/O from the block-interaction path.

### Performance

- Removed blocking disk I/O from the guest-quota block-interaction path (~40× fewer writes for a 64-block grant)
- Announcer tick loop no longer allocates a `HashMap` every tick — it now early-returns when idle, eliminating 20 allocations/sec of constant GC pressure
- Movement handler resolves a chunk's owning faction once instead of up to three times
- Explosion handlers read the source entity once per block instead of repeatedly
- `User.get()` reduced from two map probes to one

### Fixed

- **Announcer stopped announcing after repeated crossings.** A per-area cooldown suppressed legitimate re-entry (out → in → out within 10s would silently skip the third announcement). The cooldown was redundant with edge detection and has been removed entirely.
- **Announcer text flickered bold → unbold.** Wilderness faded in bold but rendered its final frame unbold. All announcements are now consistently bold.
- **Bucket use charged guests 2–3× quota.** `onUseBucket` evaluated placement permission up to three times per action, each consuming quota. One bucket placement now costs exactly one quota. Authorisation outcomes are unchanged — only the timing of the deduction.
- **Guest grants leaked on faction disband.** Disbanding a faction left its grants orphaned in memory *and* rewrote them to disk indefinitely. `Faction.remove()` now cleans them up.
- **Exhausted grants could resurrect after a crash.** Grant removal at zero quota now force-persists instead of using the write throttle.
- **Announcer state leaked per player.** Player-keyed announcer maps were never cleared on disconnect. All state is now dropped on logout.
- **Teleport denial message was wrong for the requester.** Both parties received *"Teleport request denied"*; the requester now correctly sees *"X denied your teleport request"*.

### Configuration

Three new blocks are written to `config/factions.json` on first launch. **Existing settings are preserved.**

```json
{
  "announcer": {
    "enabled": true,
    "displaySeconds": 5,
    "style": "default"
  },
  "guestGrant": {
    "maxBreak": 64,
    "maxPlace": 64,
    "restrictedItems": [
      "minecraft:flint_and_steel",
      "minecraft:fire_charge",
      "minecraft:lava_bucket"
    ]
  },
  "teleport": {
    "enabled": true,
    "cooldownSeconds": 15,
    "requestExpirySeconds": 60,
    "damageCooldown": 100
  }
}
```

**Permissions:** `factions.tp` (default level 0 — available to all faction members).

### Upgrade from 3.0.4

1. Stop the server
2. **Back up your world** (specifically the `factions/` data directory)
3. Replace `factions-mc26.2-3.0.4.jar` with `factions-mc26.2-3.1.0.jar`
4. Start the server

No config edits required. No data migration. Downgrading back to 3.0.4 is safe — the new `guestgrant` data file is simply ignored, and unknown config keys are skipped.

### Known issues and limitations

Documented deliberately. None are regressions from 3.0.4 unless noted.

- **`announcer.style` is not implemented.** ~~The config field exists and the datapack templates under `data/factions/factions/announcer/` ship with the mod, but nothing reads them — announcement styling is currently built in code. The field is inert; changing it has no effect. Slated for removal or implementation in 3.1.x.~~ **Fixed in 3.1.1** — the field and templates have been removed.
- **Multi-chunk claims only charge power for one chunk.** ~~`/f claim add <size>` validates power as though claiming a single chunk, so `/f claim add 7` acquires up to 169 chunks for the cost of one. **Pre-existing since 3.0.4** — carried forward unchanged, not introduced here. Worth addressing in a future release if land economy matters on your server.~~ **Fixed in 3.1.1** — power is now required for every genuinely new chunk in the requested square.
- **Overclaimed factions lose all protection.** When a faction's claim count exceeds its power, every interaction — including chest access — is permitted. Pre-existing, believed intentional.
- **Teleport can be used as combat reinforcement.** `/f tp accept` checks combat status on the requester but not the target, so a player under attack can summon a teammate. An inherent consequence of the consent-based design.
- **Teleport requests can be stomped.** Only one pending request is tracked per target, so a same-faction griefer can repeatedly overwrite a teammate's legitimate request.
- **Guest grants only resolve online players.** `/f guest grant` and `/f tp` look up targets by name among online players; offline players cannot be targeted.
- **Explosions consume quota per block.** A guest igniting TNT can spend their entire break quota in one blast. Considered correct behaviour (each destroyed block is one break) but worth knowing.
- **`/f xaerosync` is ungated.** No permission requirement or cooldown; rebuilds the full claim payload per invocation. Pre-existing.
- **Admin GUI permission split.** Opening the admin GUI requires `factions.admin.gui`, but its spoof/bypass buttons don't re-check `factions.admin.spoof` / `factions.admin.bypass`. Only relevant if those permissions are configured separately. Pre-existing.

### Testing

42 automated tests across 8 suites, all passing:

| Suite | Tests |
|---|---|
| `TeleportRequestManagerTest` | 15 |
| `AnnouncerManagerTest` | 6 |
| `GuestGrantTest` | 6 |
| `RestrictedItemsTest` | 6 |
| `ClaimExclusivityTest` | 4 |
| `FactionClaimCountsTest` | 3 |
| `ClaimSyncPayloadTest` | 1 |
| `WorldUtilsTest` | 1 |

`./gradlew clean build test` exits 0.

### Compatibility

No changes to `io.icker.factions.api.*` event signatures. No new mixins. No persisted schema changes to existing entities — `GuestGrant` is a new, additive store. Dynmap, BlueMap, squaremap, PlaceholderAPI, LuckPerms, StyledChat, and sgui integrations are untouched.

---

## [3.0.4] and earlier

See the git history prior to this changelog.
