# Minepiece QoL 2.0.5

This release adds random folder-based picture rotation to the full 2.0.4 mod, retaining its restored source, assets, and Fabric Loader 0.20.1 support.

The 2.0.2 menus, HUD panels, trackers, translation, AH highlights, pet stats, appearance settings, loadouts, and Essentials features are retained.

## Install

Use Minecraft Java **26.2**, Java **25 or newer**, Fabric Loader **0.20.1 or newer**, and Fabric API **0.161.0+26.2**. Replace the older mod in your instance's `mods` folder with `minepieceqol-2.0.5+mc26.2.jar`; install only one version of this mod at a time.

## Random picture rotation

In **Appearance → Pictures**, click **Choose folder**, drop a folder onto the page, or paste its path and click **Import**. Each folder creates one picture panel from its PNG/JPG/JPEG files; subfolders and invalid images are skipped. Images are copied at import time, so import the folder again to include later changes.

The panel chooses a random image every **10 seconds** by default, without repeating the current image when multiple images are available. Change **Interval (s)** directly or use its **− / +** buttons (1–3600 seconds). The interval and image list are saved with your settings and loadouts. Single-image imports, opacity, visibility, and HUD placement still work as before.

## Build and verify

Use JDK 25, Loom 1.17, and the included Gradle 9.5.1 wrapper:

```sh
./gradlew --no-daemon test build
./gradlew --no-daemon runClientGameTest
```

The installable artifact is `build/libs/minepieceqol-2.0.5+mc26.2.jar`. The sources JAR is for developers.

Production source and assets were restored from `minepieceqol-2.0.2+mc26.2-sources.jar`. The client smoke test checks full-mod initialization, restored keybindings, menu rendering, and inventory rendering. Screenshots are saved under `build/run/clientGameTest/screenshots`.

The restored regression suite has 162 passing tests and four failing assertions in GrindingTrackerTest, ProgressHudControllerTest, and UiLoadoutsTest. The same four failures, with identical assertion results, were reproduced using the original 2.0.2 source and Loader 0.19.5. They are retained for visibility; production behavior was preserved. The 2.0.5 package and client smoke test can be run separately with `./gradlew --no-daemon build runClientGameTest -x test`.
