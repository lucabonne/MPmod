package net.minepiece.qol.commands;

import com.mojang.brigadier.CommandDispatcher;
import java.util.Locale;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.minepiece.qol.MinepieceQolClient;
import net.minepiece.qol.config.ConfigManager;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public final class ModCommands {
    private ModCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, MinepieceQolClient mod) {
        dispatcher.register(ClientCommands.literal("mpdebug")
            .then(ClientCommands.literal("copylast")
                .executes(context -> {
                    String lastLine = mod.getDebugLogManager().getLastLine();
                    if (lastLine.isBlank()) {
                        reply(context.getSource(), tr(mod, "cmd.debug.no_line", "No debug line available."));
                        return 0;
                    }
                    mod.copyLastDebugLine();
                    reply(context.getSource(), tr(mod, "cmd.debug.copied", "Copied last debug line to clipboard."));
                    return 1;
                })));

        // Diagnostic: what the safes panel reads from the floating text right now.
        dispatcher.register(ClientCommands.literal("mpsafes").executes(context -> {
            var client = net.minecraft.client.Minecraft.getInstance();
            String[] texts = mod.readSafeTexts(client);
            String spawn = mod.getBossTracker().getCurrentSpawnId();
            reply(context.getSource(), "Current spawn: " + (spawn.isBlank() ? "(unknown)" : spawn));
            for (int i = 0; i < texts.length; i++) {
                var safe = net.minepiece.qol.state.SafeChestTracker.SAFES.get(i);
                reply(context.getSource(), mod.tr("safes." + safe.key()) + ": "
                    + mod.getSafeChests().status(spawn, i, System.currentTimeMillis()) + "  <-  "
                    + (texts[i] == null ? "(no floating text loaded)" : texts[i]));
            }
            return 1;
        }));

        dispatcher.register(ClientCommands.literal("mpsession")
            .executes(context -> {
                for (String line : mod.getSessionLines()) reply(context.getSource(), line);
                return 1;
            })
            .then(ClientCommands.literal("last").executes(context -> {
                for (String line : mod.getLastSessionLines()) reply(context.getSource(), line);
                return 1;
            })));

        dispatcher.register(ClientCommands.literal("mpall")
            .then(ClientCommands.literal("show")
                .executes(context -> {
                    reply(context.getSource(), mod.setAllFeaturesVisible(true));
                    return 1;
                }))
            .then(ClientCommands.literal("hide")
                .executes(context -> {
                    reply(context.getSource(), mod.setAllFeaturesVisible(false));
                    return 1;
                })));

        dispatcher.register(ClientCommands.literal("mpjobs")
            .executes(context -> {
                reply(context.getSource(), mod.getJobsTracker().summary(mod::tr));
                return 1;
            })
            .then(ClientCommands.literal("init")
                .then(ClientCommands.argument("spec", greedyString())
                    .executes(context -> {
                        String result = mod.getJobsTracker().initFromSpec(getString(context, "spec"), mod::tr);
                        reply(context.getSource(), result);
                        return 1;
                    })))
            .then(ClientCommands.literal("set")
                .then(ClientCommands.argument("job", word())
                    .then(ClientCommands.argument("level", integer(0))
                        .then(ClientCommands.argument("needed", doubleArg(10.0D, 1_000_000_000.0D))
                            .executes(context -> {
                                String result = mod.getJobsTracker().setJobBaseline(
                                    getString(context, "job"),
                                    getInteger(context, "level"),
                                    getDouble(context, "needed"),
                                    mod::tr
                                );
                                reply(context.getSource(), result);
                                return 1;
                            })))))
            .then(ClientCommands.literal("dailyreset")
                .executes(context -> {
                    reply(context.getSource(), mod.getJobsTracker().resetDailyXp(mod::tr));
                    return 1;
                })
                .then(ClientCommands.argument("job", word())
                    .executes(context -> {
                        String result = mod.getJobsTracker().resetDailyXp(getString(context, "job"), mod::tr);
                        reply(context.getSource(), result);
                        return result.startsWith(tr(mod, "cmd.jobs.reset_daily_prefix", "Reset ")) ? 1 : 0;
                    })))
            .then(ClientCommands.literal("show")
                .executes(context -> {
                    reply(context.getSource(), mod.setJobsOverviewVisible(true));
                    return 1;
                }))
            .then(ClientCommands.literal("hide")
                .executes(context -> {
                    reply(context.getSource(), mod.setJobsOverviewVisible(false));
                    return 1;
                }))
            .then(ClientCommands.literal("reset")
                .executes(context -> {
                    mod.getJobsTracker().reset();
                    reply(context.getSource(), tr(mod, "cmd.jobs.progress_reset", "Job progress reset."));
                    return 1;
                }))
            .then(ClientCommands.literal("forget")
                .executes(context -> {
                    mod.getJobsTracker().forget();
                    reply(context.getSource(), tr(mod, "cmd.jobs.baselines_cleared", "All job baselines cleared."));
                    return 1;
                }))
            .then(ClientCommands.literal("debug")
                .executes(context -> {
                    reply(context.getSource(), mod.getJobsTracker().debugSummary(mod::tr));
                    return 1;
                })));

        dispatcher.register(ClientCommands.literal("mpbosses")
            .executes(context -> {
                reply(
                    context.getSource(),
                    trf(mod, "cmd.bosses.miniboss_registration", "Miniboss registration: %s", onOff(mod, mod.isBossTrackingEnabled()))
                );
                for (String line : mod.getBossTracker().commandLines(mod::tr)) {
                    reply(context.getSource(), line);
                }
                return 1;
            })
            .then(ClientCommands.literal("on")
                .executes(context -> {
                    reply(context.getSource(), mod.setBossTrackingEnabled(true));
                    return 1;
                }))
            .then(ClientCommands.literal("off")
                .executes(context -> {
                    reply(context.getSource(), mod.setBossTrackingEnabled(false));
                    return 1;
                }))
            .then(ClientCommands.literal("clear")
                .executes(context -> {
                    mod.getBossTracker().clear();
                    reply(context.getSource(), tr(mod, "cmd.bosses.cleared", "Cleared saved bosses."));
                    return 1;
                })
                .then(ClientCommands.argument("index", integer(1, 15))
                    .executes(context -> {
                        int index = getInteger(context, "index");
                        boolean removed = mod.getBossTracker().clearDisplayedIndex(index);
                        reply(context.getSource(), removed
                            ? trf(mod, "cmd.bosses.cleared_index", "Cleared boss #%d.", index)
                            : trf(mod, "cmd.bosses.no_saved_index", "No saved boss at index %d.", index));
                        return removed ? 1 : 0;
                    }))));

        dispatcher.register(ClientCommands.literal("mpevents")
            .executes(context -> {
                for (String line : mod.getEventCountdownTracker().infoLines(mod::tr)) {
                    reply(context.getSource(), line);
                }
                return 1;
            })
            .then(ClientCommands.literal("info")
                .executes(context -> {
                    for (String line : mod.getEventCountdownTracker().infoLines(mod::tr)) {
                        reply(context.getSource(), line);
                    }
                    return 1;
                }))
            .then(ClientCommands.literal("add")
                .then(ClientCommands.argument("spec", greedyString())
                    .executes(context -> {
                        EventSpec spec = parseEventSpec(getString(context, "spec"));
                        if (spec == null) {
                            reply(context.getSource(), tr(mod, "cmd.events.usage_add", "Usage: /mpevents add <event name> <HH:mm> (CET)"));
                            return 0;
                        }
                        String result = mod.getEventCountdownTracker().addEvent(spec.name(), spec.time(), mod::tr);
                        reply(context.getSource(), result);
                        return result.startsWith(tr(mod, "cmd.events.added_prefix", "Added ")) ? 1 : 0;
                    })))
            .then(ClientCommands.literal("remove")
                .then(ClientCommands.argument("spec", greedyString())
                    .executes(context -> {
                        EventSpec spec = parseEventSpec(getString(context, "spec"));
                        if (spec == null) {
                            reply(context.getSource(), tr(mod, "cmd.events.usage_remove", "Usage: /mpevents remove <event name> <HH:mm> (CET)"));
                            return 0;
                        }
                        String result = mod.getEventCountdownTracker().removeEvent(spec.name(), spec.time(), mod::tr);
                        reply(context.getSource(), result);
                        return result.startsWith(tr(mod, "cmd.events.removed_prefix", "Removed ")) ? 1 : 0;
                    }))));

        dispatcher.register(ClientCommands.literal("mpmoney")
            .executes(context -> {
                for (String line : mod.getMoneyTracker().commandSummaryLines(mod::tr)) {
                    reply(context.getSource(), line);
                }
                String hint = mod.getMoneyTracker().getBalanceInitializationHint(mod::tr);
                if (!hint.isBlank()) {
                    reply(context.getSource(), hint);
                }
                return 1;
            })
            .then(ClientCommands.literal("log")
                .executes(context -> {
                    var lines = mod.getMoneyTracker().recentLogLines(10, mod::tr);
                    if (lines.isEmpty()) {
                        reply(context.getSource(), tr(mod, "cmd.money.no_transactions", "No transactions recorded."));
                        return 0;
                    }
                    for (String line : lines) {
                        reply(context.getSource(), line);
                    }
                    return 1;
                }))
            .then(ClientCommands.literal("stats")
                .executes(context -> {
                    for (String line : mod.getMoneyTracker().commandSummaryLines(mod::tr)) {
                        reply(context.getSource(), line);
                    }
                    String hint = mod.getMoneyTracker().getBalanceInitializationHint(mod::tr);
                    if (!hint.isBlank()) {
                        reply(context.getSource(), hint);
                    }
                    return 1;
                }))
            .then(ClientCommands.literal("export")
                .executes(context -> {
                    var exportPath = mod.getMoneyTracker().exportCsv();
                    reply(context.getSource(), exportPath == null
                        ? tr(mod, "cmd.money.export_failed", "Export failed.")
                        : trf(mod, "cmd.money.exported_to", "Exported to %s", exportPath));
                    return exportPath == null ? 0 : 1;
                }))
            .then(ClientCommands.literal("sync")
                .executes(context -> {
                    mod.getMoneyTracker().sync();
                    mod.savePersistentState();
                    reply(context.getSource(), tr(mod, "cmd.money.synced", "Money state synced."));
                    return 1;
                }))
            .then(ClientCommands.literal("reset")
                .executes(context -> {
                    mod.getMoneyTracker().resetAll();
                    mod.savePersistentState();
                    reply(context.getSource(), tr(mod, "cmd.money.reset", "Money counters reset. Run /balance to initialize total."));
                    return 1;
                })));

        dispatcher.register(ClientCommands.literal("talkto")
            .executes(context -> {
                String activeTarget = mod.getTalkTarget();
                if (activeTarget == null || activeTarget.isBlank()) {
                    reply(context.getSource(), tr(mod, "cmd.talk.status_off", "Talk mode is OFF. Use /talkto <name> to enable it."));
                    return 0;
                }
                reply(context.getSource(), trf(
                    mod,
                    "cmd.talk.status_on",
                    "Talk mode is ON for %s. Use /talkoff to disable.",
                    activeTarget
                ));
                return 1;
            })
            .then(ClientCommands.argument("name", word())
                .executes(context -> {
                    String name = getString(context, "name");
                    reply(context.getSource(), mod.setTalkTarget(name));
                    return 1;
                })));

        dispatcher.register(ClientCommands.literal("talkoff")
            .executes(context -> {
                reply(context.getSource(), mod.clearTalkTarget());
                return 1;
            }));
    }

    private static void reply(FabricClientCommandSource source, String message) {
        source.sendFeedback(Component.literal(message));
    }

    private static String tr(MinepieceQolClient mod, String key, String fallback) {
        String value = mod.tr(key);
        if (value == null || value.isBlank() || value.equals(key)) {
            return fallback;
        }
        return value;
    }

    private static String trf(MinepieceQolClient mod, String key, String fallback, Object... args) {
        return String.format(Locale.ROOT, tr(mod, key, fallback), args);
    }

    private static String onOff(MinepieceQolClient mod, boolean enabled) {
        return enabled ? tr(mod, "button.on", "On") : tr(mod, "button.off", "Off");
    }

    private static EventSpec parseEventSpec(String spec) {
        String text = spec == null ? "" : spec.trim();
        int split = text.lastIndexOf(' ');
        if (split <= 0 || split >= text.length() - 1) {
            return null;
        }
        String name = text.substring(0, split).trim();
        String time = text.substring(split + 1).trim();
        if (name.isBlank() || time.isBlank()) {
            return null;
        }
        return new EventSpec(name, time);
    }

    private record EventSpec(String name, String time) {
    }
}
