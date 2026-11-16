# Minepiece QoL 2.1.2

Auction house slot colors are much stronger (60% up to solid; they sit behind the item, so it stays readable). Extras has a **Reset safe timers** button that clears every Marineford safe spawn and timer; the safes panel hides until you are back near the safes.

2.1.1: auction house prices are colored in **10% steps**: within ±5% of the average nothing is tinted; cheaper listings go from pale to deep green and pricier ones from pale yellow through orange and red to crimson, with the slot tint getting stronger each step. The tooltip percentage uses the same color.

2.1.0: pictures now **zoom with the mouse wheel** in the HUD editor (10% per notch, around the picture's center) with no fixed size limit: from small up to the size that covers the whole screen, where they snap to center. The **Full screen** toggle still pins a picture to cover the screen.

2.0.6 added a **Marineford safes** HUD panel: the lock countdown of all six safes (purple, green, orange, red, yellow, blue), each in its own color, read from the text above each safe and grouped under a **Spawn** header per server spawn, like the bosses panel. Timers keep counting after you walk away; a safe shows **Ready** when it awaits lockpicking and **Picked** after `Lock picking successful!` until its new timer is seen. `/mpsafes` shows what the mod reads above each safe. Toggle it under Extras, move it in the HUD editor (panel 15; custom pictures now start at 16).

Pictures can now cover the **full screen** (cropped to fit, never stretched) and **fade** to transparent toward the top, bottom, left or right edge over 5–100% of the image.

2.0.5 added random folder-based picture rotation.

The 2.0.2 menus, HUD panels, trackers, translation, AH highlights, pet stats, appearance settings, loadouts, and Essentials features are retained.

## Install

Use Minecraft Java **26.2**, Java **25 or newer**, Fabric Loader **0.20.1 or newer**, and Fabric API **0.161.0+26.2**. Replace the older mod in your instance's `mods` folder with `minepieceqol-2.1.2+mc26.2.jar`; install only one version of this mod at a time.

## Random picture rotation

In **Appearance → Pictures**, click **Choose folder**, drop a folder onto the page, or paste its path and click **Import**. Each folder creates one picture panel from its PNG/JPG/JPEG files; subfolders and invalid images are skipped. Images are copied at import time, so import the folder again to include later changes.

The panel chooses a random image every **10 seconds** by default, without repeating the current image when multiple images are available. Change **Interval (s)** directly or use its **− / +** buttons (1–3600 seconds). The interval and image list are saved with your settings and loadouts. Single-image imports, opacity, visibility, and HUD placement still work as before.

## Build and verify

Use JDK 25, Loom 1.17, and the included Gradle 9.5.1 wrapper:

```sh
./gradlew --no-daemon test build
./gradlew --no-daemon runClientGameTest
```

The installable artifact is `build/libs/minepieceqol-2.1.2+mc26.2.jar`. The sources JAR is for developers.

Production source and assets were restored from `minepieceqol-2.0.2+mc26.2-sources.jar`. The client smoke test checks full-mod initialization, restored keybindings, menu rendering, and inventory rendering. Screenshots are saved under `build/run/clientGameTest/screenshots`.

The regression suite has 178 passing tests. The package and client smoke test can be run separately with `./gradlew --no-daemon build runClientGameTest -x test`.
