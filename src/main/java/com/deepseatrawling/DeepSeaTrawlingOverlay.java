package com.deepseatrawling;

import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.coords.LocalPoint;

import javax.inject.Inject;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

public class DeepSeaTrawlingOverlay extends Overlay {

    private final Client client;
    private final DeepSeaTrawling plugin;

    private final Map<ShoalData.ShoalSpecies, Color> speciesColors = new EnumMap<>(ShoalData.ShoalSpecies.class);

    @Inject
    private DeepSeaTrawlingOverlay(Client client, DeepSeaTrawling plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);

        speciesColors.put(ShoalData.ShoalSpecies.GIANT_KRILL, new Color(255, 150, 150));
        speciesColors.put(ShoalData.ShoalSpecies.YELLOWFIN, new Color(255, 220, 120));
        speciesColors.put(ShoalData.ShoalSpecies.HADDOCK, new Color(255, 255, 200));
        speciesColors.put(ShoalData.ShoalSpecies.HALIBUT, new Color(200, 255, 200));
        speciesColors.put(ShoalData.ShoalSpecies.BLUEFIN, new Color(120, 180, 255));
        speciesColors.put(ShoalData.ShoalSpecies.MARLIN, new Color(0, 200, 255));
        speciesColors.put(ShoalData.ShoalSpecies.SHIMMERING, new Color(200, 255, 255));
        speciesColors.put(ShoalData.ShoalSpecies.GLISTENING, new Color(220, 200, 255));
        speciesColors.put(ShoalData.ShoalSpecies.VIBRANT, new Color(255, 200, 220));
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        ShoalData shoal = plugin.getNearestShoal();
        if (shoal == null) {
            return null;
        }
        if(!plugin.trackedShoals.contains(shoal.getWorldViewId())) {
            return null;
        }

        GameObject object = shoal.getShoalObject();
        if (object == null)
        {
            LocalPoint localPoint = shoal.getCurrent();
            if (localPoint != null) {
                drawArea(graphics, localPoint, 3, Color.WHITE);
            }
            return null;
        }

        LocalPoint localLocation = object.getLocalLocation();

        ObjectComposition composition = client.getObjectDefinition(object.getId());
        if (composition == null) {
            return null;
        }

        int sizeX = composition.getSizeX();
        int sizeY = composition.getSizeY();

        int size = Math.max(sizeX, sizeY);
        if (size <= 0) {
            size = 1;
        }
        if(plugin.trackedShoals.contains(shoal.getWorldViewId())) {
            Color baseColour = speciesColors.getOrDefault(shoal.getSpecies(), Color.WHITE);

            if (shoal.getSpecies() == ShoalData.ShoalSpecies.SHIMMERING || shoal.getSpecies() == ShoalData.ShoalSpecies.GLISTENING || shoal.getSpecies() == ShoalData.ShoalSpecies.VIBRANT)
            {
                drawPath(graphics, shoal, new Color(0,204,255));
            } else {
                drawPath(graphics, shoal, new Color(0,51,102));
            }
            drawStopSquares(graphics, shoal, size, baseColour);

            drawArea(graphics, localLocation, size, baseColour);

            drawDepthLabel(graphics, shoal, size);
        }

        return null;
    }

    private void drawArea(Graphics2D graphics, LocalPoint centerLP, int sizeTiles, Color baseColour)
    {
        Polygon poly = Perspective.getCanvasTileAreaPoly(client, centerLP, sizeTiles);
        if (poly == null)
        {
            return;
        }

        graphics.setStroke(new BasicStroke(2));
        OverlayUtil.renderPolygon(graphics, poly, baseColour);

        Color fill = new Color(baseColour.getRed(), baseColour.getGreen(), baseColour.getBlue(), 50);
        Composite old = graphics.getComposite();
        graphics.setComposite(AlphaComposite.SrcOver.derive(fill.getAlpha() / 255f));
        graphics.setColor(fill);
        graphics.fill(poly);
        graphics.setComposite(old);
    }

    private void drawPath (Graphics2D path, ShoalData shoal, Color baseColour)
    {
        java.util.List<WorldPoint> points = shoal.getPathPoints();
        if (points.size() < 2) {
            return;
        }

        int plane = shoal.getWorldEntity().getWorldView().getPlane();

        path.setStroke(new BasicStroke(1.5f));
        path.setColor(baseColour);

        int ARROW_EVERY_N_SEGMENTS = 5;
        for (int i = 0; i < points.size() - 1; i++)
        {
            WorldPoint worldPointA = points.get(i);
            WorldPoint worldPointB = points.get(i+1);
            if (worldPointA == null || worldPointB == null) {
                continue;
            }

            LocalPoint localPointA = LocalPoint.fromWorld(client, worldPointA);
            LocalPoint localPointB = LocalPoint.fromWorld(client, worldPointB);
            if (localPointA == null || localPointB == null) {
                continue;
            }

            Point pointA = Perspective.localToCanvas(client, localPointA, plane);
            Point pointB = Perspective.localToCanvas(client, localPointB, plane);
            if (pointA == null || pointB == null)
            {
                continue;
            }

            path.drawLine(pointA.getX(), pointA.getY(), pointB.getX(), pointB.getY());

            if (i % ARROW_EVERY_N_SEGMENTS == 0)
            {
                drawArrow(path, pointA, pointB, baseColour);
            }
        }
    }

    private void drawStopSquares(Graphics2D square, ShoalData shoal, int sizeTiles, Color baseColour)
    {
        Color outline = new Color(baseColour.getRed(), baseColour.getGreen(), baseColour.getBlue());
        Color fill = new Color(baseColour.getRed(), baseColour.getGreen(), baseColour.getBlue(), 50);

        for (WorldPoint worldPoint : shoal.getStopPoints())
        {
            if (worldPoint == null ) {
                continue;
            }

            LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);
            if (localPoint == null) {
                continue;
            }

            if (plugin.localDistanceSq(localPoint, shoal.getCurrent()) < 512 * 512 && !shoal.getWasMoving()) {
                continue;
            }

            Polygon polygon = Perspective.getCanvasTileAreaPoly(client, localPoint, sizeTiles);
            if (polygon == null) {
                continue;
            }

            square.setStroke(new BasicStroke(2));
            OverlayUtil.renderPolygon(square, polygon, outline);

            Composite old = square.getComposite();
            square.setComposite(AlphaComposite.SrcOver.derive(fill.getAlpha() / 255f));
            square.setColor(fill);
            square.fill(polygon);
            square.setComposite(old);
        }
    }

    private void drawArrow(Graphics2D graphics, Point from, Point to, Color colour)
    {
        if (from == null || to == null) {
            return;
        }

        graphics.setColor(colour);
        graphics.setStroke(new BasicStroke(2));

        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double angle = Math.atan2(dy, dx);

        int arrowLength = 10;
        int arrowWidth = 6;

        double leftAngle = angle + Math.toRadians(155);
        double rightAngle = angle - Math.toRadians(155);

        int x1 = to.getX() + (int) (Math.cos(leftAngle) * arrowLength);
        int y1 = to.getY() + (int) (Math.sin(leftAngle) * arrowLength);

        int x2 = to.getX() + (int) (Math.cos(rightAngle) * arrowLength);
        int y2 = to.getY() + (int) (Math.sin(rightAngle) * arrowLength);

        int[] xs = { to.getX(), x1, x2 };
        int[] ys = { to.getY(), y1, y2 };

        graphics.fillPolygon(xs, ys, 3);
    }

    private void drawDepthLabel(Graphics2D graphic, ShoalData shoal, int sizeTiles)
    {
        ShoalData.ShoalDepth depth = shoal.getDepth();
        String text;
        Color textColour;

        switch (depth)
        {
            case SHALLOW:
                text = "Shallow";
                textColour = new Color(0, 200, 0);
                break;
            case MEDIUM:
                text = "Medium";
                textColour = new Color(255, 165, 0);
                break;
            case DEEP:
                text = "Deep";
                textColour = new Color(200, 60, 60);
                break;
            default:
                text = "?";
                textColour = Color.GRAY;
        }

        GameObject object = shoal.getShoalObject();
        if (object == null) {
            return;
        }

        LocalPoint centralPoint = object.getLocalLocation();
        if (centralPoint == null) {
            return;
        }

        Polygon poly = Perspective.getCanvasTilePoly(client, centralPoint, sizeTiles);
        if (poly == null) {
            return;
        }

        Rectangle bounds = poly.getBounds();
        int anchorX = bounds.x + bounds.width / 2;
        int anchorY = bounds.y;

        graphic.setFont(graphic.getFont().deriveFont(Font.BOLD, 16f));
        FontMetrics metrics = graphic.getFontMetrics();
        int width = metrics.stringWidth(text);
        int height = metrics.getHeight();

        int x = anchorX - width / 2;
        int y = anchorY - 8;

        graphic.setColor(new Color(0,0,0,140));
        graphic.fillRoundRect(x - 3, y - height, width + 6, height, 6, 6);

        graphic.setColor((textColour));
        graphic.drawString(text, x, y);

    }

}
