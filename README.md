# Minepiece QoL

Client-side mod for Minecraft 1.21.11, by SiickLukee.

## Customize your UI

Open the mod menu with `.` and select **Appearance**. The main **Panel colors** list has two pages covering all eleven HUD panels (including combined player/fruit/weapon XP, Grinding, and Cooking) plus the chat indicator. Use the arrows beside the heading to switch pages. Each panel row has a six-digit HEX field (optional `#`), a **Random** button, and a live color swatch. Complete valid colors save immediately; incomplete edits keep the previous color. Existing named colors keep their previous appearance. The **HUD opacity & text colors** button opens Appearance directly. **HUD opacity** controls panel backgrounds/borders, and **Text** controls text color; choosing a custom text color also overrides HUD status-row and heading colors. Restoring the default text color (`EAF1FF`) restores automatic status colors.

- **Appearance:** set six-digit hex colors for the accent, background, border, and text. Adjust menu/HUD opacity and border thickness. Toggle compact HUD spacing, headings, decorations, text shadows, and rarity icons.
- **Loadouts:** apply Farming, Fighting, or Grinding presets. Save the current setup with **Save new**. To edit or rename a saved setup, apply it, make changes, enter its name, and press **Update**. The first preset preserves your original arrangement automatically. Use **Remove** to delete a saved setup.
- **Pictures:** drop PNG/JPG files onto this page, or paste a local file path and press **Import**. Up to eight pictures can be displayed per setup. Images are copied locally into `config/minepiece-qol/images`; nothing is uploaded. Removing a picture from a setup keeps its imported file available to other saved loadouts.
- **HUD editor:** drag panels or pictures to move them, scroll to resize, and press `H` to hide/show the selected element. Close the editor to return to the menu you opened it from.

Press `\` while playing to open quick loadout selection. This key can be changed in Minecraft's Controls settings. Loadouts save panel positions, sizes, colors, visibility, overall appearance, and pictures; changing loadouts does not reset XP or money tracking. Saved positions adapt to changes in screen size.

Epic and Mythic use the mod's existing icon assets. Primordial uses a new geometric badge and is recognized when the item tooltip contains a readable Primordial label. Resource-pack-only Primordial glyphs need their actual text marker to be identified.

## Player XP and grinding

Player XP is saved locally for each server/account. Open `/profile` once to learn the current level's XP requirement. Subsequent combat XP increases from weapon/fruit item data update the player estimate without waiting for the top XP bar. Shared fruit/weapon gains are counted once. Noncombat item upgrades are excluded using the danger reward signal; derived player XP remains marked `~` because it is an estimate. When the level changes, the HUD requests `/profile` again to learn the new requirement.

The grinding box shows XP gained alongside money earned, using the same session reset and daily persistence rules. Whenever the server XP bar changes, its level and percentage reconcile the player estimate and outstanding grinding XP, including booster gains. Between visible readings, item XP continues updating the estimate. Corrections are approximate because the server percentage is rounded; they do not restart the grinding timer.

Grinding rates use recent combat rewards, with smoothing and a maximum refresh frequency of once every five seconds. Rates stay still between rewards. The timer pauses one minute after the last XP gain. Totals and rates reset at midnight in the computer's local timezone, and same-day totals survive reconnects.

**Item icon grids** enables the shared tooltip/HUD component renderer. It displays registered item sprites, custom inventory item models, and rarity badges in an aligned grid. Recipe hover grids use the chosen dish quantity; known ingredients use their actual inventory icons. Original lore is retained for parsing, with only the visual rows replaced. Vanilla tooltips that already have their own data component (such as bundles) are left alone.

## Chat channel indicator

While chat is open, a compact badge above the input shows Public, Party, or Island and the last confirmed public-language flag. It follows the server confirmations for `/is chat`, `/p chat`, and `/lang`, not the command text alone. Language changes retain the current sending channel. On reconnect, the badge shows `Chat ?` with a neutral icon until the server confirms the channel/language again. Supported flags include English, Italian, French, German, Spanish, Portuguese, Brazilian, Polish, Indonesian, Turkish, Russian, and Dutch; other channel languages show their name with a neutral icon. The badge follows the UI background, text, border, and opacity settings and disappears when chat closes.
