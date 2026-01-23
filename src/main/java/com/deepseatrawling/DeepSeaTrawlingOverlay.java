package com.deepseatrawling;

import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.client.Notifier;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.coords.LocalPoint;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.util.Timer;


public class DeepSeaTrawlingOverlay extends Overlay {

    private final Client client;
    private final DeepSeaTrawling plugin;
    private static final int SHOAL_DEFAULT_SIZE = 6;

    @Inject
    DeepSeaTrawlingConfig config;

    @Inject
    Notifier notifier;
    private boolean notifiedShoalMoving = false;

    @Inject
    private DeepSeaTrawlingOverlay(Client client, DeepSeaTrawling plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {

        if (!plugin.boats.containsKey(client.getLocalPlayer().getWorldView().getId()))
        {
            return null;
        }
        ShoalData shoal = plugin.getNearestShoal();
        if (shoal == null) {
            return null;
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
            drawStopSquares(graphics, shoal, baseColour);

            GameObject object = shoal.getShoalObject();
            if (object == null)
            {
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

            drawArea(graphics, localLocation, size, baseColour);

            drawDepthLabel(graphics, shoal, size);

            if (!shoal.hasActiveStopTimer() && !notifiedShoalMoving && config.notifyShoalMoving()) {
                notifier.notify("Shoal is on the move! Set sail!");
                notifiedShoalMoving = true;
            } else if (shoal.hasActiveStopTimer()){
                notifiedShoalMoving = false;
            }
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
        if (config.diagonalSmoothing())
        {
            points = smoothDiagonals(points);
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

    private void drawStopSquares(Graphics2D square, ShoalData shoal, Color baseColour)
    {
        Color outline = new Color(baseColour.getRed(), baseColour.getGreen(), baseColour.getBlue());
        Color fill = new Color(baseColour.getRed(), baseColour.getGreen(), baseColour.getBlue(), 50);

        WorldPoint shoalWP = shoal.getCurrentWorldPoint();

        for (WorldPoint worldPoint : shoal.getStopPoints())
        {
            if (worldPoint == null ) {
                continue;
            }

            if (shoalWP != null && worldPoint.distanceTo(shoalWP) < 2) {
                continue;
            }

            LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);
            if (localPoint == null) continue;

            Polygon polygon = Perspective.getCanvasTileAreaPoly(client, localPoint, SHOAL_DEFAULT_SIZE);
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
        if (!config.showShoalDepthText() && config.depthTimer() == TimerType.NONE) {
            return;
        }
        ShoalData.ShoalDepth depth = shoal.getDepth();
        String depthText;
        Color textColour;
        if (depth == null) return;

        switch (depth)
        {
            case SHALLOW:
                depthText = "Shallow";
                textColour = new Color(0, 200, 0);
                break;
            case MEDIUM:
                depthText = "Medium";
                textColour = new Color(255, 165, 0);
                break;
            case DEEP:
                depthText = "Deep";
                textColour = new Color(200, 60, 60);
                break;
            default:
                depthText = "UNKNOWN";
                textColour = Color.GRAY;
        }

        GameObject object = shoal.getShoalObject();
        if (object == null) {
            return;
        }

        LocalPoint centralPoint = object.getLocalLocation();

        Polygon poly = Perspective.getCanvasTilePoly(client, centralPoint, sizeTiles);
        if (poly == null) {
            return;
        }

        Rectangle bounds = poly.getBounds();
        int anchorX = bounds.x + bounds.width / 2;
        int anchorY = bounds.y;

        graphic.setFont(FontManager.getRunescapeBoldFont().deriveFont( 14f));
        FontMetrics metrics = graphic.getFontMetrics();

        java.util.List<String> lines = new ArrayList<>();

        if (config.showShoalDepthText()) {
            lines.add(depthText);
        }

        int nowTick = client.getTickCount();
        if (!shoal.isWasMoving() && shoal.hasActiveStopTimer() && config.depthTimer() != TimerType.NONE)
        {
            int tDepth = shoal.getTicksUntilDepthChange(nowTick);
            int tMove  = shoal.getTicksUntilMove(nowTick);

            // Show "until net change" only before halfway point
            if (tDepth > 0)
            {
                if (config.depthTimer() == TimerType.SECONDS) {
                    lines.add("Net change: " + formatTicks(tDepth));
                } else {
                    lines.add("Net change: " + tDepth);
                }
            }
            // Always show "until move" while stopped (until it hits 0)
            if (tMove > 0)
            {
                if (config.depthTimer() == TimerType.SECONDS) {
                    lines.add("Moves: " + formatTicks(tMove));
                } else {
                    lines.add("Moves: " + tMove);
                }
            }
        }
        if (lines.isEmpty()) {
            return;
        }

        int lineHeight = metrics.getHeight();
        int maxWidth = 0;

        for (String str : lines) {
            maxWidth = Math.max(maxWidth, metrics.stringWidth(str));
        }

        int x = anchorX - maxWidth / 2;
        int y = anchorY - 8;

        int boxHeight = lineHeight * lines.size();
        graphic.setColor(new Color(0,0,0,140));
        graphic.fillRoundRect(x - 3, y - boxHeight, maxWidth + 6, boxHeight, 6, 6);

        // Draw lines top -> bottom
        for (int i = 0; i < lines.size(); i++)
        {
            String l = lines.get(i);
            int lx = anchorX - (metrics.stringWidth(l) / 2);
            int ly = y - (lineHeight * (lines.size() - 1 - i)); // stack upward

            // color: depth line uses depth colour; timers use white
            graphic.setColor(i == 0 ? textColour : Color.WHITE);
            graphic.drawString(l, lx, ly);
        }

    }

    private static String formatTicks(int ticks)
    {
        if (ticks < 0) return "";
        int totalSeconds = (int) Math.ceil(ticks * 0.6);
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return (m > 0) ? String.format("%d:%02d", m, s) : String.format("%ds", s);
    }

    private static List<WorldPoint> smoothDiagonals(List<WorldPoint> in) {
        if (in == null || in.size() < 3) return in;

        java.util.List<WorldPoint> out = new ArrayList<>();
        out.add(in.get(0));

        int i = 1;
        while (i < in.size() - 1)
        {
            WorldPoint a = out.get(out.size() - 1);
            WorldPoint b = in.get(i);
            WorldPoint c = in.get(i + 1);

            int abx = b.getX() - a.getX();
            int aby = b.getY() - a.getY();
            int bcx = c.getX() - b.getX();
            int bcy = c.getY() - b.getY();

            // A->B and B->C are cardinal steps (N/E/S/W)
            boolean abCardinal = (Math.abs(abx) + Math.abs(aby)) == 1;
            boolean bcCardinal = (Math.abs(bcx) + Math.abs(bcy)) == 1;

            // A->C is a diagonal neighbour (one tile diagonally)
            int acx = c.getX() - a.getX();
            int acy = c.getY() - a.getY();
            boolean acDiagonal = Math.abs(acx) == 1 && Math.abs(acy) == 1;

            // Ensure it’s a "turn" not a straight line
            boolean turnsCorner = (abx != bcx) || (aby != bcy);

            if (abCardinal && bcCardinal && acDiagonal && turnsCorner)
            {
                // Drop the corner point b (replace stair with diagonal)
                i++;           // skip b
                out.add(c);    // add c directly
                i++;           // move past c
                continue;

            }

            out.add(b);
            i++;
        }

        // Add the last point if we didn't already
        WorldPoint lastIn = in.get(in.size() - 1);
        if (out.get(out.size() - 1) != lastIn)
        {
            out.add(lastIn);
        }

        return out;
    }
}
