package com.deepseatrawling;

import java.util.*;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.WorldEntity;
import net.runelite.api.coords.WorldPoint;

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
			if (depth == null) {
				return -1;
			}
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

    public static Map<ShoalSpecies, Integer> shoalTimers = Map.of(
            ShoalSpecies.GIANT_KRILL, 150,
            ShoalSpecies.HADDOCK, 120,
            ShoalSpecies.YELLOWFIN, 100,
            ShoalSpecies.HALIBUT, 80,
            ShoalSpecies.BLUEFIN, 70,
            ShoalSpecies.MARLIN, 50
    );

    private int stopStartTick = -1;
    private int stopDurationTicks = 0; // set when stop begins

    @Setter
    @Getter
    private NPC shoalNpc;

    @Getter
    @Setter
    private ShoalDepth depth;

    @Getter
    private final WorldEntity worldEntity;

    @Getter
    private final int worldViewId;

    @Setter
    @Getter
    private ShoalSpecies species;

    @Setter
    @Getter
    private GameObject shoalObject;

    @Getter
    @Setter
    private WorldPoint last;

    @Getter
    @Setter
    private WorldPoint currentWorldPoint;

    @Setter
    @Getter
    private boolean wasMoving;

    @Getter
    @Setter
    private int movingStreak = 0;

    @Getter
    @Setter
    private int stoppedStreak = 0;

    /*
    public void setNext(LocalPoint next) {
        this.next = next;
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
    @Getter
    private final List<WorldPoint> pathPoints;
    @Getter
    private final List<WorldPoint> stopPoints;

    public ShoalData(int worldViewId, WorldEntity worldEntity, ShoalRoute route) {
        this.worldViewId = worldViewId;
        this.worldEntity = worldEntity;
        this.pathPoints = route.getPathPoints();
        this.stopPoints = route.getStopPoints();
    }

    public void setDepthFromAnimation(int currentTick)
    {
        if (shoalNpc == null)
        {
            if (getTicksUntilMove(currentTick) > 0 && getTicksUntilDepthChange(currentTick) > 0) {
                this.depth = ShoalDepth.UNKNOWN;
            } else {
                this.depth = this.species.defaultDepth();
            }
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

    public void beginStopTimer(int currentTick, int durationTicks)
    {
        this.stopStartTick = currentTick;
        this.stopDurationTicks = durationTicks;
    }

    public void clearStopTimer()
    {
        this.stopStartTick = -1;
        this.stopDurationTicks = 0;
    }

    public boolean hasActiveStopTimer()
    {
        return stopStartTick >= 0 && stopDurationTicks > 0;
    }

    public int getTicksUntilMove(int currentTick)
    {
        if (!hasActiveStopTimer()) return -1;
        int elapsed = currentTick - stopStartTick;
        return Math.max(0, stopDurationTicks - elapsed);
    }

    public int getTicksUntilDepthChange(int currentTick)
    {
        if (!hasActiveStopTimer()) return -1;
        if (this.getSpecies() == ShoalSpecies.GIANT_KRILL || this.getSpecies() == ShoalSpecies.HADDOCK || this.getSpecies() == ShoalSpecies.SHIMMERING) { return -1; }
        int half = stopDurationTicks / 2;
        int elapsed = currentTick - stopStartTick;
        return Math.max(-1, half - elapsed);
    }

/*
    public boolean isPastDepthChangePoint(int currentTick)
    {
        if (!hasActiveStopTimer()) return false;
        int half = stopDurationTicks / 2;
        return (currentTick - stopStartTick) >= half;
    }
*/


}
