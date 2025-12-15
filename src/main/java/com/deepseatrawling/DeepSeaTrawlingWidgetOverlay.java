package com.deepseatrawling;

import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

public class DeepSeaTrawlingWidgetOverlay extends Overlay {

    private static final int SAILING_SIDEPANEL_GROUP = 937;
    private static final int FACILITIES_CONTENT_CLICKLAYER_CHILD = 25;

    private static final int SKIFF_DOWN_INDEX = 29;
    private static final int SKIFF_UP_INDEX = 30;
    private static final int STARBOARD_DOWN_INDEX = 41;
    private static final int STARBOARD_UP_INDEX = 42;
    private static final int PORT_DOWN_INDEX = 45;
    private static final int PORT_UP_INDEX = 46;

    private final Client client;
    private final DeepSeaTrawling plugin;

    enum Direction {
        UP,
        DOWN
    }

    @Inject
    public DeepSeaTrawlingWidgetOverlay(Client client, DeepSeaTrawling plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
       ShoalData shoal = plugin.getNearestShoal();
       if (shoal != null && shoal.getDepth() != ShoalData.ShoalDepth.UNKNOWN)
       {
           int desired = ShoalData.ShoalDepth.asInt(shoal.getDepth());
           if (desired < 1) {
               return null;
           }

           for (int netIndex = 0; netIndex < 2; netIndex++)
           {
               int current = Net.NetDepth.asInt(plugin.netList[netIndex].getNetDepth());
               if (current <= 0 || current == desired) {
                   continue;
               }

               Direction direction = current < desired ? Direction.DOWN : Direction.UP;
               highlightNetButton(graphics, netIndex, direction);
           }
       }
        return null;
    }

    private void highlightNetButton(Graphics2D g, int netIndex, Direction direction)
    {
        Widget parent = client.getWidget(SAILING_SIDEPANEL_GROUP , FACILITIES_CONTENT_CLICKLAYER_CHILD);
        if (parent == null) return;
        boolean hidden = false;
        for (Widget widgetParent = parent; widgetParent != null; widgetParent = widgetParent.getParent())
        {
            if (widgetParent.isHidden()) {
                hidden = true;
            }
        }

        int childId;
        if (netIndex == 0)
            childId = direction == Direction.DOWN ? STARBOARD_DOWN_INDEX : STARBOARD_UP_INDEX;
        else if (netIndex == 1)
            childId = direction == Direction.DOWN ? PORT_DOWN_INDEX : PORT_UP_INDEX;
        else
            return;

        Widget button = parent.getChild(childId);
        if (button == null || button.isHidden() || button.getBounds().width <=0 || button.getBounds().height <= 0 || hidden || !client.getWidget(161,73).getBounds().intersects(button.getBounds())) return;

        Rectangle bounds = button.getBounds();
        if (bounds == null) return;

        g.setColor(new Color(255, 255, 0, 120));
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);

        g.setColor(new Color(255, 255, 0, 220));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
    }

}
