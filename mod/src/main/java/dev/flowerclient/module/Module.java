package dev.flowerclient.module;

import net.minecraft.client.MinecraftClient;

public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;

    protected Module(String name, String description, Category category, boolean enabledByDefault) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = enabledByDefault;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean value) {
        if (value == enabled) return;
        enabled = value;
        if (value) onEnable(); else onDisable();
    }

    public void toggle() { setEnabled(!enabled); }

    protected void onEnable() {}
    protected void onDisable() {}
    public void tick(MinecraftClient client) {}
}
