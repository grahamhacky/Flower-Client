package dev.flowerclient.module.impl;

import dev.flowerclient.module.TextHudModule;
import net.minecraft.client.MinecraftClient;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClockModule extends TextHudModule {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("h:mm a");
    public ClockModule() { super("Clock", "Real-world time", false, 4, 188); }
    @Override protected String label() { return ""; }
    @Override protected String value(MinecraftClient mc) { return LocalTime.now().format(FMT); }
}
