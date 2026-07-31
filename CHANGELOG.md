# Changelog

All notable changes to this mod are documented here.

Format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

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

- **`announcer.style` is not implemented.** The config field exists and the datapack templates under `data/factions/factions/announcer/` ship with the mod, but nothing reads them — announcement styling is currently built in code. The field is inert; changing it has no effect. Slated for removal or implementation in 3.1.x.
- **Multi-chunk claims only charge power for one chunk.** `/f claim add <size>` validates power as though claiming a single chunk, so `/f claim add 7` acquires up to 169 chunks for the cost of one. **Pre-existing since 3.0.4** — carried forward unchanged, not introduced here. Worth addressing in a future release if land economy matters on your server.
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
