package com.deepseatrawling;

import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.client.ui.FontManager;
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
    private final DeepSeaTrawlingConfig config;

    @Inject
    private DeepSeaTrawlingOverlay(Client client, DeepSeaTrawling plugin, DeepSeaTrawlingConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {


        if (plugin.netObjectByIndex[0] != null || plugin.netObjectByIndex[1] != null)
        {
            return null;
        }
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
            Color baseColour = plugin.speciesColours.getOrDefault(shoal.getSpecies(), Color.WHITE);

            if (config.pathColourMode() == DeepSeaTrawlingConfig.PathColourMode.SOLID && (shoal.getSpecies() == ShoalData.ShoalSpecies.SHIMMERING || shoal.getSpecies() == ShoalData.ShoalSpecies.GLISTENING || shoal.getSpecies() == ShoalData.ShoalSpecies.VIBRANT))
            {
                drawPath(graphics, shoal, config.specialPathColour());
            } else if (config.pathColourMode() == DeepSeaTrawlingConfig.PathColourMode.SOLID) {
                drawPath(graphics, shoal, config.shoalPathColour());
            } else {
                drawPath(graphics, shoal, Color.WHITE);
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

            if (config.pathColourMode() == DeepSeaTrawlingConfig.PathColourMode.GRADIENT) {
                float t = (points.size() <= 1) ? 0f : (i / (float)(points.size() - 1));
                baseColour = Color.getHSBColor(t, 1.0f, 1.0f);
            }

            path.setColor(baseColour);

            path.drawLine(pointA.getX(), pointA.getY(), pointB.getX(), pointB.getY());

            if (config.showDirectionArrows() && i % ARROW_EVERY_N_SEGMENTS == 0)
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
        if (!config.showShoalDepthText()) {
            return;
        }
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

        graphic.setFont(FontManager.getRunescapeBoldFont().deriveFont( 14f));
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
