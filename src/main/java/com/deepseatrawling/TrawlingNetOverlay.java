package com.deepseatrawling;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;

public class TrawlingNetOverlay extends Overlay {

    private final Client client;
    private final DeepSeaTrawling plugin;

    @Inject
    private TrawlingNetOverlay(Client client, DeepSeaTrawling plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
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
            if (current <= 0 || current == desired) continue;

            trawlingNetOutline(graphics, plugin.fishQuantity, totalNetSize, plugin.netObjectByIndex[netIndex]);
        }

        return null;

    }

    private void trawlingNetOutline(Graphics2D graphic, int fishQuantity, int totalNetSize, GameObject netObject) {
        Color colour;
        if (fishQuantity >= totalNetSize) {
            colour = Color.RED;
        } else {
            colour = new Color(255, 255, 0, 220);
        }

        if (netObject == null) {
            return;
        }

        Shape convexHull = netObject.getConvexHull();
        if (convexHull == null) {
            return;
        }
        OverlayUtil.renderPolygon(graphic, convexHull, colour);

    }
}
