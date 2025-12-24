package com.deepseatrawling;

import net.runelite.client.ui.overlay.infobox.InfoBox;

import java.awt.*;
import java.awt.image.BufferedImage;

public class TrawlingNetInfoBox extends InfoBox {
    private final DeepSeaTrawling plugin;
    private final DeepSeaTrawlingConfig config;

    public TrawlingNetInfoBox(BufferedImage image, DeepSeaTrawling plugin, DeepSeaTrawlingConfig config)
    {
        super(image, plugin);
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean render()
    {
        return plugin.fishQuantity > 0;
    }

    @Override
    public String getText()
    {
        // Example: show fish count
        return String.valueOf(plugin.fishQuantity);
    }

    @Override
    public String getTooltip()
    {
        // Example tooltip
        return "Fish in nets: " + plugin.fishQuantity;
    }

    @Override
    public Color getTextColor()
    {
        return config.fishCounterTextColour();
    }

}
