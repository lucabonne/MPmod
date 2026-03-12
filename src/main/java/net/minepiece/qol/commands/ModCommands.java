package net.minepiece.qol.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minepiece.qol.MinepieceQolClient;
import net.minecraft.text.Text;

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
        dispatcher.register(ClientCommandManager.literal("mpdebug")
            .then(ClientCommandManager.literal("copylast")
                .executes(context -> {
                    String lastLine = mod.getDebugLogManager().getLastLine();
                    if (lastLine.isBlank()) {
                        reply(context.getSource(), "No debug line available.");
                        return 0;
                    }
                    mod.copyLastDebugLine();
                    reply(context.getSource(), "Copied last debug line to clipboard.");
                    return 1;
                })));

        dispatcher.register(ClientCommandManager.literal("mpall")
            .then(ClientCommandManager.literal("show")
                .executes(context -> {
                    reply(context.getSource(), mod.setAllFeaturesVisible(true));
                    return 1;
                }))
            .then(ClientCommandManager.literal("hide")
                .executes(context -> {
                    reply(context.getSource(), mod.setAllFeaturesVisible(false));
                    return 1;
                })));

        dispatcher.register(ClientCommandManager.literal("mpjobs")
            .executes(context -> {
                reply(context.getSource(), mod.getJobsTracker().summary());
                return 1;
            })
            .then(ClientCommandManager.literal("init")
                .then(ClientCommandManager.argument("spec", greedyString())
                    .executes(context -> {
                        String result = mod.getJobsTracker().initFromSpec(getString(context, "spec"));
                        reply(context.getSource(), result);
                        return 1;
                    })))
            .then(ClientCommandManager.literal("set")
                .then(ClientCommandManager.argument("job", word())
                    .then(ClientCommandManager.argument("level", integer(0))
                        .then(ClientCommandManager.argument("needed", doubleArg(10.0D, 1_000_000_000.0D))
                            .executes(context -> {
                                String result = mod.getJobsTracker().setJobBaseline(
                                    getString(context, "job"),
                                    getInteger(context, "level"),
                                    getDouble(context, "needed")
                                );
                                reply(context.getSource(), result);
                                return result.startsWith("Set ") ? 1 : 0;
                            })))))
            .then(ClientCommandManager.literal("dailyreset")
                .executes(context -> {
                    reply(context.getSource(), mod.getJobsTracker().resetDailyXp());
                    return 1;
                })
                .then(ClientCommandManager.argument("job", word())
                    .executes(context -> {
                        String result = mod.getJobsTracker().resetDailyXp(getString(context, "job"));
                        reply(context.getSource(), result);
                        return result.startsWith("Reset ") ? 1 : 0;
                    })))
            .then(ClientCommandManager.literal("show")
                .executes(context -> {
                    reply(context.getSource(), mod.setJobsOverviewVisible(true));
                    return 1;
                }))
            .then(ClientCommandManager.literal("hide")
                .executes(context -> {
                    reply(context.getSource(), mod.setJobsOverviewVisible(false));
                    return 1;
                }))
            .then(ClientCommandManager.literal("reset")
                .executes(context -> {
                    mod.getJobsTracker().reset();
                    reply(context.getSource(), "Job progress reset.");
                    return 1;
                }))
            .then(ClientCommandManager.literal("forget")
                .executes(context -> {
                    mod.getJobsTracker().forget();
                    reply(context.getSource(), "All job baselines cleared.");
                    return 1;
                }))
            .then(ClientCommandManager.literal("debug")
                .executes(context -> {
                    reply(context.getSource(), mod.getJobsTracker().debugSummary());
                    return 1;
                })));

        dispatcher.register(ClientCommandManager.literal("mpbosses")
            .executes(context -> {
                for (String line : mod.getBossTracker().commandLines()) {
                    reply(context.getSource(), line);
                }
                return 1;
            })
            .then(ClientCommandManager.literal("clear")
                .executes(context -> {
                    mod.getBossTracker().clear();
                    reply(context.getSource(), "Cleared saved bosses.");
                    return 1;
                })
                .then(ClientCommandManager.argument("index", integer(1, 15))
                    .executes(context -> {
                        int index = getInteger(context, "index");
                        boolean removed = mod.getBossTracker().clearDisplayedIndex(index);
                        reply(context.getSource(), removed
                            ? "Cleared boss #" + index + "."
                            : "No saved boss at index " + index + ".");
                        return removed ? 1 : 0;
                    }))));

        dispatcher.register(ClientCommandManager.literal("mpevents")
            .executes(context -> {
                for (String line : mod.getEventCountdownTracker().infoLines()) {
                    reply(context.getSource(), line);
                }
                return 1;
            })
            .then(ClientCommandManager.literal("info")
                .executes(context -> {
                    for (String line : mod.getEventCountdownTracker().infoLines()) {
                        reply(context.getSource(), line);
                    }
                    return 1;
                }))
            .then(ClientCommandManager.literal("add")
                .then(ClientCommandManager.argument("spec", greedyString())
                    .executes(context -> {
                        EventSpec spec = parseEventSpec(getString(context, "spec"));
                        if (spec == null) {
                            reply(context.getSource(), "Usage: /mpevents add <event name> <HH:mm> (CET)");
                            return 0;
                        }
                        String result = mod.getEventCountdownTracker().addEvent(spec.name(), spec.time());
                        reply(context.getSource(), result);
                        return result.startsWith("Added ") ? 1 : 0;
                    })))
            .then(ClientCommandManager.literal("remove")
                .then(ClientCommandManager.argument("spec", greedyString())
                    .executes(context -> {
                        EventSpec spec = parseEventSpec(getString(context, "spec"));
                        if (spec == null) {
                            reply(context.getSource(), "Usage: /mpevents remove <event name> <HH:mm> (CET)");
                            return 0;
                        }
                        String result = mod.getEventCountdownTracker().removeEvent(spec.name(), spec.time());
                        reply(context.getSource(), result);
                        return result.startsWith("Removed ") ? 1 : 0;
                    }))));

        dispatcher.register(ClientCommandManager.literal("mpmoney")
            .executes(context -> {
                for (String line : mod.getMoneyTracker().commandSummaryLines()) {
                    reply(context.getSource(), line);
                }
                String hint = mod.getMoneyTracker().getBalanceInitializationHint();
                if (!hint.isBlank()) {
                    reply(context.getSource(), hint);
                }
                return 1;
            })
            .then(ClientCommandManager.literal("log")
                .executes(context -> {
                    var lines = mod.getMoneyTracker().recentLogLines(10);
                    if (lines.isEmpty()) {
                        reply(context.getSource(), "No transactions recorded.");
                        return 0;
                    }
                    for (String line : lines) {
                        reply(context.getSource(), line);
                    }
                    return 1;
                }))
            .then(ClientCommandManager.literal("stats")
                .executes(context -> {
                    for (String line : mod.getMoneyTracker().commandSummaryLines()) {
                        reply(context.getSource(), line);
                    }
                    String hint = mod.getMoneyTracker().getBalanceInitializationHint();
                    if (!hint.isBlank()) {
                        reply(context.getSource(), hint);
                    }
                    return 1;
                }))
            .then(ClientCommandManager.literal("export")
                .executes(context -> {
                    var exportPath = mod.getMoneyTracker().exportCsv();
                    reply(context.getSource(), exportPath == null ? "Export failed." : "Exported to " + exportPath);
                    return exportPath == null ? 0 : 1;
                }))
            .then(ClientCommandManager.literal("sync")
                .executes(context -> {
                    mod.getMoneyTracker().sync();
                    mod.savePersistentState();
                    reply(context.getSource(), "Money state synced.");
                    return 1;
                })));
    }

    private static void reply(FabricClientCommandSource source, String message) {
        source.sendFeedback(Text.literal(message));
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
