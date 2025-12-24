package com.deepseatrawling;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.*;

@ConfigGroup("deepseatrawling")
public interface DeepSeaTrawlingConfig extends Config
{
    // --------------- Sections ------------------------
    @ConfigSection(
            name = "shoals",
            description = "Shoal rendering and prediction settings",
            position = 0
    )
    String shoalsSection = "Shoals Section";

    @ConfigSection(
            name = "nets",
            description = "Net overlays + UI highlights",
            position = 1
    )
    String netsSection = "Nets Section";

    @ConfigSection(
            name = "notifications",
            description = "Desktop notifications",
            position = 2
    )
    String notifSection = "Notification Section";

    @ConfigSection(
            name = "colours",
            description = "Custom colours for overlays",
            position = 3
    )
    String coloursSection = "Colours Section";


    // --------------- Shoals Section ------------------
    @ConfigItem(
            keyName = "showGiantKrill",
            name = "Show Giant Krill Shoals",
            description = "Highlight Giant Krill Shoals",
            position = 0,
            section = shoalsSection
    )
    default boolean showGiantKrill()
    {
        return true;
    }


    @ConfigItem(
			keyName = "showHaddock",
			name = "Show Haddock Shoals",
			description = "Highlight Haddock Shoals",
            position = 1,
            section = shoalsSection
	)
	default boolean showHaddock()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showYellowfin",
			name = "Show Yellowfin Shoals",
			description = "Highlight Yellowfin Shoals",
            position = 2,
            section = shoalsSection
	)
	default boolean showYellowfin()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showHalibut",
			name = "Show Halibut Shoals",
			description = "Highlight Halibut Shoals",
            position = 3,
            section = shoalsSection
	)
	default boolean showHalibut()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showBluefin",
			name = "Show Bluefin Shoals",
			description = "Highlight Bluefin Shoals",
            position = 4,
            section = shoalsSection
	)
	default boolean showBluefin()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showMarlin",
			name = "Show Marlin Shoals",
			description = "Highlight Marlin Shoals",
            position = 5,
            section = shoalsSection
	)
	default boolean showMarlin()
	{
		return true;
	}

    @ConfigItem(
            keyName = "showDirectionArrows",
            name = "Show direction arrows",
            description = "Draw arrowheads along the path line",
            position = 10,
            section = shoalsSection
    )
    default boolean showDirectionArrows() { return true; }

    @ConfigItem(
            keyName = "diagonalSmoothing",
            name = "Smooth diagonal path prediction",
            description = "Reduces zig-zag in diagonal segments (higher = smoother, but less 'grid accurate')",
            position = 12,
            section = shoalsSection
    )
    default int diagonalSmoothing() { return 2; } // 0..5 is a good range

    // ------------------------ Path Colour Mode ---------------------
    enum PathColourMode { SOLID, GRADIENT }

    @ConfigItem(
            keyName = "pathColourMode",
            name = "Path colour mode",
            description = "Solid uses one colour; Gradient cycles colours along the route to help follow overlaps",
            position = 13,
            section = shoalsSection
    )
    default PathColourMode pathColourMode() { return PathColourMode.SOLID; }

    // -------------------- Nets Section -----------------------------------
    enum NetHighlightStyle
    {
        OUTLINE,        // convex hull / polygon outline
        HULL_FILL,      // filled convex hull
        CLICKBOX        // use getClickbox() if available
    }

    @ConfigItem(
            keyName = "highlightWrongDepthNets",
            name = "Highlight nets at wrong depth",
            description = "Highlights trawling nets when they don't match the shoal depth",
            position = 0,
            section = netsSection
    )
    default boolean highlightWrongDepthNets() { return true; }

    @ConfigItem(
            keyName = "highlightFullNets",
            name = "Highlight nets when full",
            description = "Highlights trawling nets when they're full'",
            position = 1,
            section = netsSection
    )
    default boolean highlightFullNets() { return true; }

    @ConfigItem(
            keyName = "netHighlightStyle",
            name = "Net highlight style",
            description = "How the net should be highlighted when it needs adjustment",
            position = 2,
            section = netsSection
    )
    default NetHighlightStyle netHighlightStyle() { return NetHighlightStyle.OUTLINE; }

    @ConfigItem(
            keyName = "showNetDepthText",
            name = "Show net depth text (R/S/M/D)",
            description = "Draws the current depth setting of each net on-screen (scene or widget overlay)",
            position = 3,
            section = netsSection
    )
    default boolean showNetDepthText() { return true; }

    @ConfigItem(
            keyName = "showNetWidgetHint",
            name = "Highlight net up/down UI buttons",
            description = "Highlights the sailing sidepanel buttons that should be pressed",
            position = 4,
            section = netsSection
    )
    default boolean showNetWidgetHint() { return true; }

    @ConfigItem(
            keyName = "showShoalDepthText",
            name = "Show shoal depth text (Shallow/Medium/Deep)",
            description = "Draws the current depth setting of nearest shoal on screen",
            position = 5,
            section = netsSection
    )
    default boolean showShoalDepthText() { return true; }

    // ---------- Notifications ----------
    @ConfigItem(
            keyName = "notifyNetFull",
            name = "Notify when net is full",
            description = "Shows a RuneLite notification when your nets are full",
            position = 0,
            section = notifSection
    )
    default boolean notifyNetFull() { return true; }

    @ConfigItem(
            keyName = "notifyDepthChange",
            name = "Notify when depth change (on screen)",
            description = "Shows a RuneLite notification when the rendered shoal's depth changes",
            position = 1,
            section = notifSection
    )
    default boolean notifyDepthChange() { return true; }

    // -------------- Colours ---------------------
    @ConfigItem(
            keyName = "fishCounterTextColour",
            name = "Fish counter text colour",
            description = "Colour of the fish quantity text",
            position = 0,
            section = coloursSection
    )
    default Color fishCounterTextColour() { return Color.WHITE; }

    @ConfigItem(
            keyName = "uiHighlightColour",
            name = "Highlight UI Button Colour",
            description = "Colour UI Button to improve readability",
            position = 1,
            section = coloursSection
    )
    default Color uiHighlightColour() { return new Color(255, 255, 0); }

    @ConfigItem(
            keyName = "netDepthHighlightColour",
            name = "Wrong Net Depth highlight colour",
            description = "Colour used to highlight the net when it's at the wrong depth",
            position = 2,
            section = coloursSection
    )
    default Color netDepthHighlightColour() { return new Color(255, 255, 0, 220); }

    @ConfigItem(
            keyName = "netFullHighlightColour",
            name = "Net Full highlight colour",
            description = "Colour used to highlight the net when it's full",
            position = 3,
            section = coloursSection
    )
    default Color netFullHighlightColour() { return Color.RED; }

    @ConfigItem(
            keyName = "shoalPathColour",
            name = "Regular Shoal path solid colour",
            description = "Solid path line colour (used when Path colour mode = SOLID) and NOT special shoal",
            position = 3,
            section = coloursSection
    )
    default Color shoalPathColour() { return new Color(0,51,102); }

    @ConfigItem(
            keyName = "specialPathColour",
            name = "Special Shoal path solid colour",
            description = "Solid path line colour (used when path colour mode = SOLID) and IS special shoal",
            position = 4,
            section = coloursSection
    )
    default Color specialPathColour() { return new Color(0,204,255); }

    @ConfigItem(
            keyName = "giantKrillColour",
            name = "Giant Krill Colour",
            description = "Colour of Giant Krill Shoals",
            position = 5,
            section = coloursSection
    )
    default Color giantKrillColour() { return new Color(255, 150, 150); }

    @ConfigItem(
            keyName = "haddockColour",
            name = "Haddock Colour",
            description = "Colour of Haddock Shoals",
            position = 6,
            section = coloursSection
    )
    default Color haddockColour() { return new Color(255, 255, 200); }

    @ConfigItem(
            keyName = "shimmeringColour",
            name = "Shimmering Colour",
            description = "Colour of Shimmering Shoals",
            position = 7,
            section = coloursSection
    )
    default Color shimmeringColour() { return new Color(200, 255, 255); }

    @ConfigItem(
            keyName = "yellowfinColour",
            name = "Yellowfin Colour",
            description = "Colour of Yellowfin Shoals",
            position = 8,
            section = coloursSection
    )
    default Color yellowfinColour() { return new Color(255, 220, 120); }

    @ConfigItem(
            keyName = "halibutColour",
            name = "Halibut Colour",
            description = "Colour of Halibut Shoals",
            position = 9,
            section = coloursSection
    )
    default Color halibutColour() { return new Color(200, 255, 200); }

    @ConfigItem(
            keyName = "glisteningColour",
            name = "Glistening Colour",
            description = "Colour of Glistening Shoals",
            position = 10,
            section = coloursSection
    )
    default Color glisteningColour() { return new Color(220, 200, 255); }

    @ConfigItem(
            keyName = "bluefinColour",
            name = "Bluefin Colour",
            description = "Colour of Bluefin Shoals",
            position = 11,
            section = coloursSection
    )
    default Color bluefinColour() { return new Color(120, 180, 255); }

    @ConfigItem(
            keyName = "marlinColour",
            name = "Marlin Colour",
            description = "Colour of Marlin Shoals",
            position = 12,
            section = coloursSection
    )
    default Color marlinColour() { return new Color(0, 200, 255); }

    @ConfigItem(
            keyName = "vibrantColour",
            name = "Vibrant Colour",
            description = "Colour of Vibrant Shoals",
            position = 13,
            section = coloursSection
    )
    default Color vibrantColour() { return new Color(255, 200, 220); }

}
