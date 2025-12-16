package com.deepseatrawling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.WorldEntity;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;


import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

public class ShoalData {

    public enum ShoalSpecies
    {
        GIANT_KRILL (ObjectID.SAILING_SHOAL_CLICKBOX_GIANT_KRILL),
        HADDOCK (ObjectID.SAILING_SHOAL_CLICKBOX_HADDOCK),
        YELLOWFIN (ObjectID.SAILING_SHOAL_CLICKBOX_YELLOWFIN),
        HALIBUT (ObjectID.SAILING_SHOAL_CLICKBOX_HALIBUT),
        BLUEFIN (ObjectID.SAILING_SHOAL_CLICKBOX_BLUEFIN),
        MARLIN (ObjectID.SAILING_SHOAL_CLICKBOX_MARLIN),
        SHIMMERING (ObjectID.SAILING_SHOAL_CLICKBOX_SHIMMERING),
        GLISTENING (ObjectID.SAILING_SHOAL_CLICKBOX_GLISTENING),
        VIBRANT (ObjectID.SAILING_SHOAL_CLICKBOX_VIBRANT);

        private final int objectID;

        public ShoalDepth defaultDepth()
        {
            switch (this)
            {
                case GIANT_KRILL:
                case HADDOCK:
                case SHIMMERING:
                    return ShoalDepth.SHALLOW;
                default:
                    return ShoalDepth.MEDIUM;
            }
        }

        ShoalSpecies(int objectID) {
            this.objectID = objectID;
        }

        public static ShoalSpecies fromGameObjectId(int id)
        {
            for (ShoalSpecies s : values())
            {
                if (s.objectID == id) {
                    return s;
                }
            }
            return null;
        }

    }

    public enum ShoalDepth
    {
        SHALLOW,
        MEDIUM,
        DEEP,
        UNKNOWN;

        public static int asInt(ShoalDepth depth)
        {
            switch(depth) {
                case SHALLOW:
                    return 1;
                case MEDIUM:
                    return 2;
                case DEEP:
                    return 3;
                default:
                    return -1;
            }
        }
    }

    private NPC shoalNpc;

    private ShoalDepth depth;

    private static final String RESOURCE_NAME = "shoals.properties";

    private final WorldEntity worldEntity;
    private final int worldViewId;
    private ShoalSpecies species;

    private GameObject shoalObject;

    //private WorldPoint last;
    private LocalPoint current;
    //private LocalPoint next;
    private boolean wasMoving;

    private List<WorldPoint> pathPoints = new ArrayList<>();
    private List<WorldPoint> stopPoints = new ArrayList<>();

    public ShoalData(int worldViewId, WorldEntity worldEntity) throws IOException {
        this.worldViewId = worldViewId;
        this.worldEntity = worldEntity;
        load();
    }

    public void setSpecies(ShoalSpecies species) {
        this.species = species;
        this.depth = species.defaultDepth();
    }

    public ShoalSpecies getSpecies() {
        return species;
    }

    public ShoalDepth getDepth() {
        return depth;
    }

    public void setDepth(ShoalDepth depth) {
        this.depth = depth;
    }


    public void setShoalObject(GameObject shoalObject) {
        this.shoalObject = shoalObject;
    }

    public GameObject getShoalObject() {
        return shoalObject;
    }

    public WorldEntity getWorldEntity() {
        return worldEntity;
    }

    public int getWorldViewId() {
        return worldViewId;
    }

    public LocalPoint getCurrent() {
        return current;
    }

    public void setCurrent(LocalPoint current) {
        this.current = current;
    }
/*
    public WorldPoint getLast() { return last; }

    public void setNext(LocalPoint next) {
        this.next = next;
    }

    public void setLast(WorldPoint last) {
        this.last = last;
    }

    public void setPathPoints(WorldPoint worldPoint) {
            pathPoints.add(worldPoint);
    }

    public void setStopPoints(WorldPoint worldPoint) {
        if(stopPoints.contains(worldPoint))
        {
            return;
        }
        stopPoints.add(worldPoint);
    }
*/
    public List<WorldPoint> getPathPoints() {
        return pathPoints;
    }

    public List<WorldPoint> getStopPoints() {
        return stopPoints;
    }

    public boolean getWasMoving() {
        return wasMoving;
    }

    public void setWasMoving(boolean wasMoving) { this.wasMoving = wasMoving; }

    public void setShoalNpc(NPC shoalNpc) {
        this.shoalNpc = shoalNpc;
    }

    private void load() throws IOException
    {
        Properties properties = new Properties();
        String idStr;
        int id;
        String value;

        try (InputStream in = getClass().getResourceAsStream("/" + RESOURCE_NAME))
        {
            if (in == null) {
                throw new IOException("Could not find resource: " + RESOURCE_NAME);
            }
            properties.load(in);
        }

        for (String key : properties.stringPropertyNames())
        {
            if (key.startsWith("shoalpath."))
            {
                idStr = key.substring("shoalpath.".length());
                if(Integer.parseInt(idStr) == getWorldViewId()) {
                    value = properties.getProperty(key);
                    this.pathPoints = parsePoints(value);
                }
            }

            if (key.startsWith("shoalstops.")) {
                idStr = key.substring("shoalstops.".length());
                if (Integer.parseInt(idStr) == getWorldViewId()) {
                    value = properties.getProperty(key);
                    this.stopPoints = parsePoints(value);
                }
            }
        }
    }

    private List<WorldPoint> parsePoints(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(value.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::parsePoint)
                .collect(Collectors.toList());
    }

    private WorldPoint parsePoint(String token)
    {
        String[] parts = token.split(", ");
        if(parts.length < 2)
        {
            throw new IllegalArgumentException("Invalid Point: " + token);
        }

        int x = Integer.parseInt(parts[0].trim());
        int y = Integer.parseInt(parts[1].trim());
        int plane = Integer.parseInt(parts[2].trim());

        return new WorldPoint(x, y, plane);
    }

    public void setDepthFromAnimation()
    {
        if (shoalNpc == null)
        {
            this.depth = ShoalDepth.UNKNOWN;
            return;
        }
        int animation = shoalNpc.getAnimation();
        if (animation == -1)
        {
            return;
        }

        switch (animation)
        {
			case AnimationID.DEEP_SEA_TRAWLING_SHOAL_SHALLOW:
                this.depth = ShoalDepth.SHALLOW;
                break;
            case AnimationID.DEEP_SEA_TRAWLING_SHOAL_MID:
                this.depth = ShoalDepth.MEDIUM;
                break;
            case AnimationID.DEEP_SEA_TRAWLING_SHOAL_DEEP:
                this.depth = ShoalDepth.DEEP;
                break;
            default:
                this.depth = ShoalDepth.UNKNOWN;

        }

    }

    public NPC getShoalNpc() {
        return shoalNpc;
    }

}
