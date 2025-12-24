package com.deepseatrawling;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.client.config.Config;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;

public class TrawlingNetOverlay extends Overlay {

    private final Client client;
    private final DeepSeaTrawling plugin;
    private final DeepSeaTrawlingConfig config;

    @Inject
    private TrawlingNetOverlay(Client client, DeepSeaTrawling plugin, DeepSeaTrawlingConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.highlightFullNets() && !config.highlightWrongDepthNets()) {
            return null;
        }

        int totalNetSize = 0;
        if (plugin.netList[0] != null)
        {
            totalNetSize += plugin.netList[0].getNetSize();
        }
        if (plugin.netList[1] != null)
        {
            totalNetSize += plugin.netList[1].getNetSize();
        }
        if (plugin.netList[0] == null && plugin.netList[1] == null)
        {
            return null;
        }

        ShoalData shoal = plugin.getNearestShoal();
        if (shoal == null || shoal.getDepth() == ShoalData.ShoalDepth.UNKNOWN)
        {
            return null;
        }

        int desired = ShoalData.ShoalDepth.asInt(shoal.getDepth());
        if (desired < 1)
        {
            return null;
        }

        for (int netIndex = 0; netIndex <= 1; netIndex++)
        {
            GameObject netObj = plugin.netObjectByIndex[netIndex];
            if (netObj == null) continue;

            Net net = plugin.netList[netIndex];
            if (net == null) continue;

            int current = Net.NetDepth.asInt(net.getNetDepth());
            if (current <= 0 || current == desired || plugin.fishQuantity >= totalNetSize) continue;

            trawlingNetOutline(graphics, plugin.fishQuantity, totalNetSize, plugin.netObjectByIndex[netIndex]);
        }

        return null;

    }

    private void trawlingNetOutline(Graphics2D graphic, int fishQuantity, int totalNetSize, GameObject netObject) {
        if (netObject == null) {
            return;
        }

        Shape shape = null;
        switch (config.netHighlightStyle()) {
            case CLICKBOX:
                shape = netObject.getClickbox();
                break;
            case HULL_FILL:
            case OUTLINE:
                shape = netObject.getConvexHull();
                break;
        }
        if (shape == null) {
            return;
        }

        if (fishQuantity >= totalNetSize && config.highlightFullNets()) {
            if (config.netHighlightStyle() == DeepSeaTrawlingConfig.NetHighlightStyle.HULL_FILL) {
                graphic.setColor(new Color(config.netFullHighlightColour().getRed(), config.netFullHighlightColour().getGreen(),config.netFullHighlightColour().getBlue(), 60));
                graphic.fill(shape);
            }
            OverlayUtil.renderPolygon(graphic, shape, config.netFullHighlightColour());
        } else if (config.highlightWrongDepthNets()) {
            if (config.netHighlightStyle() == DeepSeaTrawlingConfig.NetHighlightStyle.HULL_FILL) {
                graphic.setColor(new Color(config.netDepthHighlightColour().getRed(), config.netDepthHighlightColour().getGreen(),config.netDepthHighlightColour().getBlue(), 60));
                graphic.fill(shape);
            }
            OverlayUtil.renderPolygon(graphic, shape, config.netDepthHighlightColour());
        }

    }
}