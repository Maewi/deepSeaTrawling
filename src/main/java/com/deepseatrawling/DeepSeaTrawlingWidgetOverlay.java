package com.deepseatrawling;

import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.api.WorldEntity;

import javax.inject.Inject;
import java.awt.*;

public class DeepSeaTrawlingWidgetOverlay extends Overlay {

    @Inject
    private Notifier notifier;
    private boolean notifiedDepthChange = false;

    private static final int SAILING_SIDEPANEL_GROUP = 937;
    private static final int FACILITIES_CONTENT_CLICKLAYER_CHILD = 25;

    private static final int SKIFF_DOWN_INDEX = 29;
    private static final int SKIFF_UP_INDEX = 30;
    private static final int STARBOARD_DOWN_INDEX = 41;
    private static final int STARBOARD_UP_INDEX = 42;
    private static final int PORT_DOWN_INDEX = 45;
    private static final int PORT_UP_INDEX = 46;

    private static final int SKIFF_WORLDVIEW_ID = 2;
    private static final int SLOOP_WORLDVIEW_ID = 3;

    private final Client client;
    private final DeepSeaTrawling plugin;
    private final DeepSeaTrawlingConfig config;

    enum Direction {
        UP,
        DOWN
    }

    @Inject
    public DeepSeaTrawlingWidgetOverlay(Client client, DeepSeaTrawling plugin, DeepSeaTrawlingConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (plugin.boats.get(client.getLocalPlayer().getWorldView().getId()) != null) {
            int playerBoat = plugin.boats.get(client.getLocalPlayer().getWorldView().getId());
            if (config.showNetDepthText()) {
                if (playerBoat == SKIFF_WORLDVIEW_ID) {
                    drawNetDepthLetter(graphics, 0);
                } else if (playerBoat == SLOOP_WORLDVIEW_ID) {
                    drawNetDepthLetter(graphics, 0);
                    drawNetDepthLetter(graphics, 1);
                }
        }
    }
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
                   notifiedDepthChange = false;
                   continue;
               }

               if (!notifiedDepthChange && config.notifyDepthChange()) {
                   notifier.notify("Shoal Depth Changed! Change net depth!");
                   notifiedDepthChange = true;
               }
               if (config.showNetWidgetHint()) {
                   Direction direction = current < desired ? Direction.DOWN : Direction.UP;
                   highlightNetButton(graphics, netIndex, direction);
               }
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

        if (plugin.boats.get(client.getLocalPlayer().getWorldView().getId()) == null) {
            return;
        }
        int shipType = plugin.boats.get(client.getLocalPlayer().getWorldView().getId());

        int childId = -1;
        if (netIndex == 0) {
            if (shipType == SKIFF_WORLDVIEW_ID) {
                childId = (direction == Direction.DOWN ? SKIFF_DOWN_INDEX : SKIFF_UP_INDEX);
            } else if (shipType == SLOOP_WORLDVIEW_ID){
                childId = (direction == Direction.DOWN ? STARBOARD_DOWN_INDEX : STARBOARD_UP_INDEX);
            }
        } else if (netIndex == 1) {
            childId = direction == Direction.DOWN ? PORT_DOWN_INDEX : PORT_UP_INDEX;
        } else {
            return;
        }

        Widget viewport = client.getWidget(161, 73);
        if (viewport == null) {
            viewport = client.getWidget(164, 72);
            if (viewport == null) return;
        }
        Rectangle canvas = viewport.getBounds();

        Widget button = parent.getChild(childId);
        if (button == null || button.isHidden() || hidden) return;

        Rectangle bounds = button.getBounds();
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) return;
        if (!canvas.intersects(button.getBounds())) return;

        g.setColor(new Color(config.uiHighlightColour().getRed(), config.uiHighlightColour().getGreen(), config.uiHighlightColour().getBlue(), 120));
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);

        g.setColor(new Color(config.uiHighlightColour().getRed(), config.uiHighlightColour().getGreen(), config.uiHighlightColour().getBlue(), 220));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);


    }

    private void drawNetDepthLetter(Graphics2D graphics, int netIndex) {
        int downId = -1;

        Widget parent = client.getWidget(SAILING_SIDEPANEL_GROUP , FACILITIES_CONTENT_CLICKLAYER_CHILD);
        if (parent == null) return;
        boolean hidden = false;
        for (Widget widgetParent = parent; widgetParent != null; widgetParent = widgetParent.getParent())
        {
            if (widgetParent.isHidden()) {
                hidden = true;
            }
        }

        if (plugin.boats.get(client.getLocalPlayer().getWorldView().getId()) == null) {
            return;
        }
        int shipType = plugin.boats.get(client.getLocalPlayer().getWorldView().getId());


        if (shipType == SLOOP_WORLDVIEW_ID) {
            downId = (netIndex == 0) ? STARBOARD_DOWN_INDEX : PORT_DOWN_INDEX;
        } else if (shipType == SKIFF_WORLDVIEW_ID) {
            downId = SKIFF_DOWN_INDEX;
        }
        if (downId < 0) { return; }

        Widget down = parent.getChild(downId);

        Widget viewport = client.getWidget(161, 73);
        if (viewport == null) {
            viewport = client.getWidget(164, 72);
            if (viewport == null) return;
        }
        Rectangle canvas = viewport.getBounds();

        if (down == null || down.isHidden())
        {
            return;
        }

        Rectangle downButton = down.getBounds();

        if (downButton == null || downButton.width <= 0 || downButton.height <= 0)
        {
            return;
        }
        if (!canvas.intersects(downButton)) return;

        Net net = plugin.netList[netIndex];
        if (net == null)
        {
            return;
        }

        String letter = depthLetter(net.getNetDepth());

        int targetX = downButton.x - 39;                  // left of the down button (magic number)
        int targetY = downButton.y;                       // halfway towards the up button (if dy=0, stays on down)

        // Draw the label with a background pill
        graphics.setFont(FontManager.getRunescapeBoldFont().deriveFont( 14f));
        FontMetrics fm = graphics.getFontMetrics();

        int textW = fm.stringWidth(letter);
        int textH = fm.getAscent();

        int x = targetX - textW / 2 + downButton.width / 2;  // center-ish relative to down button width
        int y = targetY + downButton.height / 2 + textH / 2 - 2;

        int padX = 6;
        int padY = 4;

        int bgW = textW + padX * 2;
        int bgH = fm.getHeight() + padY * 2;

        int bgX = x - padX;
        int bgY = y - fm.getAscent() - padY;

        graphics.setColor(new Color(0, 0, 0, 160));
        graphics.fillRoundRect(bgX, bgY, bgW, bgH, 10, 10);

        graphics.setColor(colorForDepth(net.getNetDepth()));
        graphics.drawString(letter, x, y);
    }

    private static String depthLetter(Net.NetDepth d)
    {
        switch (d)
        {
            case RAISED:  return "R";
            case SHALLOW: return "S";
            case MEDIUM:  return "M";
            case DEEP:    return "D";
            default:      return "?";
        }
    }

    private static Color colorForDepth(Net.NetDepth d)
    {
        switch (d)
        {
            case RAISED:  return new Color(200, 200, 200);
            case SHALLOW: return new Color(80, 255, 80);
            case MEDIUM:  return new Color(255, 200, 80);
            case DEEP:    return new Color(255, 80, 80);
            default:      return Color.WHITE;
        }
    }
}
