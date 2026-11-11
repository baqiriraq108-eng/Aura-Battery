# UI Refactoring: Permissions Single Row & Claymorphic Color Palette Cards

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed during clarification and guide the architectural implementation:
> - **Palette Color Preview**: Implemented as a tactile dual-tone horizontal clay capsule embedded within the card structure, showcasing each palette's primary accent and secondary container/swatch hues organically without concentric bullseye dots.
> - **Permissions Proportions**: Formatted into a single horizontal row of 3 balanced tiles at `84.dp` height with centered `52.dp` circular badge wells, matching the spacing and depth of the app's top-tier tactile controls.

---

## 1. Overview & Core Concept

This update refactors two core settings areas to enhance tactile consistency, visual balance, and ergonomic clarity:
1. **Permissions Row Simplification**: Completely prunes the legacy purple Autostart shield tile and unifies the 3 primary permissions (*Notification Alerts*, *Battery Optimization*, and *Auto-Start on Boot*) into a single, spacious horizontal row.
2. **Claymorphic Palette Cards**: Redesigns the 3×2 palette selection matrix from flat plinths with concentric circular dots into fully sculpted, beveled 3D tactile clay tiles with dual-tone preview capsules and state-dependent chromatic bevels matching the theme mode buttons.

---

## 2. User Experience & Visual Design

### 2.1 Permissions Section Layout
- **Single Horizontal Row of 3 Tiles**:
  - **Tile 1**: Alert Channel (`ALERT_BELL_CHIME`) on Mustard Ochre pedestal (`#D8AC2C`) with Terracotta dome.
  - **Tile 2**: Unrestricted Battery (`BATTERY_POWER_CORE`) on Terracotta pedestal (`#C86441`) with Slate cylinder and Mustard bolt.
  - **Tile 3**: Auto-Start on System Boot (`SYSTEM_BOOT_IGNITION`) on Emerald Sage pedestal (`#3B7A57`) with Slate core, open power ring, and vertical ignition bar.
- **Proportions**:
  - `height = 84.dp`, `cornerRadius = 20.dp`, `lipDepth = 5.5.dp`.
  - Centered circular pedestal badge canvas sized at `52.dp`.
  - Spaced evenly across `Arrangement.spacedBy(8.dp)` with `Modifier.weight(1f)` for seamless edge-to-edge alignment with the cards above and below.

### 2.2 Color Palette Selector Cards
- **3D Tactile Clay Architecture**:
  - Implements the exact extruded lip, bottom drop shadow, and crisp 1.5.dp inner/outer rim bevels seen on the theme mode toggles (*System*, *Light*, *Dark*).
- **Organic Dual-Tone Clay Capsule (No Center Dots)**:
  - Replaces the concentric circular orb with a horizontal clay pill capsule (`36.dp` width × `14.dp` height, `7.dp` corner radius) divided into two harmonious tactile sections:
    - Left half: Palette primary accent color.
    - Right half: Palette secondary container/swatch color.
    - Includes a subtle clay specular highlight across the top edge and an extruded 3D bottom shadow for realistic tactile depth.
- **Selected vs. Unselected States**:
  - **Selected**:
    - Card face takes on the palette's primary accent face color.
    - 3D bottom lip extrudes in the palette's darkened accent lip shade (`lipDepth = 5.dp`).
    - Outer border uses a gentle high-contrast rim highlight (`Color.White.copy(alpha = 0.38f)`).
    - Label text renders in crisp pure white (`#FFFFFF`) or high-contrast cream.
  - **Unselected**:
    - Card face strictly uses the container track background (`recessedTrackBg` or `cardFace`), matching inactive theme buttons.
    - 3D bottom lip uses `recessedTrackBorder` / `cardLip` (`lipDepth = 3.5.dp`).
    - Border uses the active theme's unified token (`recessedTrackBorder`, `1.5.dp`).
    - Label text displays in legible medium-light gray (`#A6B0AC` in Dark Mode).

---

## 3. Key Product Decisions & Trade-Offs

- **Removing Shield Tile vs. Keeping in Submenu**:
  - *Decision*: Completely remove the purple `SHIELD_AUTOSTART_GUARD` tile from the Permissions card.
  - *Rationale*: The new 4th tile (*Auto-Start on System Boot*) directly covers system boot and manufacturer auto-start settings while toggling `BootReceiver`. Removing the redundant shield eliminates user confusion and allows the 3 essential tiles to span cleanly across a single horizontal row without crowding.
- **Embedded Dual-Tone Capsule vs. Full-Card Color Fill When Unselected**:
  - *Decision*: Keep unselected card background neutral/dark while embedding the dual-tone tactile capsule.
  - *Rationale*: If all 6 unselected cards were fully filled with solid colors, the palette grid would overwhelm the visual hierarchy and destroy the unified dark container aesthetic. The dual-tone clay capsule provides an immediate, elegant color preview while keeping unselected cards calm and cohesive.

---

## 4. Technical Architecture & Component Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                       SettingsScreen.kt                         │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ Color Palette Studio Card                                 │  │
│  │  ┌───────────────────────┐ ┌───────────────────────────┐  │  │
│  │  │ Theme Mode Clay Chips │ │ (System / Light / Dark)   │  │  │
│  │  └───────────────────────┘ └───────────────────────────┘  │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │ 3x2 Clay3DColorMedallion Grid                       │  │  │
│  │  │  - Unselected: Dark card surface + Unified border   │  │  │
│  │  │  - Selected: Palette accent fill + 3D tactile lip   │  │  │
│  │  │  - Dual-tone clay capsule preview (primary+secondary)│  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ Permissions Card (Single Row Layout)                      │  │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐│  │
│  │  │ Alert Channel   │  │ Battery Opt.    │  │ Auto-Start  ││  │
│  │  │ (Bell Chime)    │  │ (Power Core)    │  │ (Ignition)  ││  │
│  │  │ 84dp x 52dp well│  │ 84dp x 52dp well│  │ 84dp x 52dp ││  │
│  │  └─────────────────┘  └─────────────────┘  └─────────────┘│  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Component & Source Modifications:
1. **`app/src/main/java/com/example/ui/Tactile3DComponents.kt`**:
   - Update `Clay3DSculptedPermissionTile` default parameters: adjust height to `84.dp`, canvas to `52.dp`, corner radius to `20.dp`, and lip depth to `5.5.dp`.
   - Redesign `Clay3DColorMedallion`:
     - Remove concentric circular canvas orbs.
     - Implement sculpted dual-tone horizontal clay capsule (`36.dp` × `14.dp`) displaying `primaryOrbColor` on the left and `secondaryRingColor` on the right with tactile bevels and top specular shine.
     - Apply unified claymorphic card styling matching `Tactile3DPebbleCard` with selected palette accent fill vs unselected dark container surface fill.
2. **`app/src/main/java/com/example/SettingsScreen.kt`**:
   - Refactor Permissions card: remove `SHIELD_AUTOSTART_GUARD` tile and the 2×2 grid layout; place the 3 remaining tiles (`ALERT_BELL_CHIME`, `BATTERY_POWER_CORE`, `SYSTEM_BOOT_IGNITION`) into a single `Row` with `horizontalArrangement = Arrangement.spacedBy(8.dp)`.
3. **Verification**:
   - Run `compile_applet` to confirm clean compilation and zero layout regressions.
