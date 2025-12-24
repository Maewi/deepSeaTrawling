package com.deepseatrawling;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.api.ChatMessageType;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;


@Slf4j
@PluginDescriptor(
	name = "Deep Sea Trawling",
	description = "Tracks Shoals - their movement, depth and relation to your net(s)",
	tags = {"trawl", "trawling", "sailing", "fishing", "shoal", "deep", "sea", "net"}
)
public class DeepSeaTrawling extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private DeepSeaTrawlingConfig config;

	@Inject
	private DeepSeaTrawlingOverlay overlay;

	@Inject
	private DeepSeaTrawlingWidgetOverlay widgetOverlay;

	@Inject
	private TrawlingNetOverlay trawlingNetOverlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private InfoBoxManager infoBoxManager;

    @Inject
    private Notifier notifier;
    private boolean notifiedFull = false;

	private TrawlingNetInfoBox trawlingNetInfoBox;

	public final Set<Integer> trackedShoals = new HashSet<>();

    public final int SKIFF_WORLD_ENTITY_TYPE = 2;
    public final int SLOOP_WORLD_ENTITY_TYPE = 3;
    public Map<Integer, Integer> boats = new HashMap<>();

	private ShoalData nearestShoal;

    public Map<ShoalData.ShoalSpecies, Color> speciesColours = new EnumMap<>(ShoalData.ShoalSpecies.class);

    @Provides
	DeepSeaTrawlingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DeepSeaTrawlingConfig.class);
	}

	private static final int SHOAL_WORLD_ENTITY_TYPE = 4;

	public Net[] netList = {
			new Net(VarbitID.SAILING_SIDEPANEL_BOAT_TRAWLING_NET_0_DEPTH),
			new Net(VarbitID.SAILING_SIDEPANEL_BOAT_TRAWLING_NET_1_DEPTH)
	};

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
		overlayManager.add(widgetOverlay);
		overlayManager.add(trawlingNetOverlay);

		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
		trawlingNetInfoBox = new TrawlingNetInfoBox(icon, this, config);
		infoBoxManager.addInfoBox(trawlingNetInfoBox);

		nearestShoal = null;
		rebuildTrackedShoals();
        rebuildShoalColours();
		log.info("Deep Sea Trawling Plugin Started");

	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		overlayManager.remove(widgetOverlay);
		overlayManager.remove(trawlingNetOverlay);

		if (trawlingNetInfoBox != null) {
			infoBoxManager.removeInfoBox(trawlingNetInfoBox);
			trawlingNetInfoBox = null;
		}
		trackedShoals.clear();
		netObjectByIndex[0] = null;
		netObjectByIndex[1] = null;
		log.info("Deep Sea Trawling Plugin Stopped");
	}

	public ShoalData getNearestShoal() {
		return nearestShoal;
	}

	public final GameObject[] netObjectByIndex = new GameObject[2];

	public int fishQuantity = 0;

	@Subscribe
	public void onWorldEntitySpawned(WorldEntitySpawned event) throws IOException {
		WorldEntity entity = event.getWorldEntity();
		WorldEntityConfig cfg = entity.getConfig();

		if (cfg == null) {
			return;
		}

        if (cfg.getId() != SHOAL_WORLD_ENTITY_TYPE && cfg.getId() != SKIFF_WORLD_ENTITY_TYPE && cfg.getId() != SLOOP_WORLD_ENTITY_TYPE) {
            return;
        }

		WorldView view = entity.getWorldView();
		if (view == null) {
			return;
		}

		int worldViewId = view.getId();

        if(nearestShoal == null && cfg.getId() == SHOAL_WORLD_ENTITY_TYPE)
		{
			nearestShoal = new ShoalData(worldViewId, entity);
		} else if (nearestShoal != null && nearestShoal.getWorldViewId() != worldViewId && cfg.getId() == SHOAL_WORLD_ENTITY_TYPE) {
			nearestShoal = new ShoalData(worldViewId, entity);
		} else if (cfg.getId() == SKIFF_WORLD_ENTITY_TYPE || cfg.getId() == SLOOP_WORLD_ENTITY_TYPE) {
            boats.put(worldViewId, cfg.getId());
        }
	}

	@Subscribe
	public void onWorldEntityDespawned(WorldEntityDespawned event)
	{
        WorldEntity entity = event.getWorldEntity();
        WorldEntityConfig cfg = entity.getConfig();

        if (cfg == null) {
            return;
        }

        boats.remove(entity.getWorldView().getId());
		//?
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned e)
	{
		if (e.getNpc().getId() == NpcID.SAILING_SHOAL_RIPPLES)
		{
			nearestShoal.setShoalNpc(e.getNpc());
			nearestShoal.setDepthFromAnimation();
		}
	}

	public void onNpcDespawned(NpcDespawned e)
	{
		if ( nearestShoal.getShoalNpc() == e.getNpc() )
		{
			nearestShoal.setShoalNpc(null);
			nearestShoal.setDepth(ShoalData.ShoalDepth.UNKNOWN);
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject object = event.getGameObject();
		if(object == null || object.getWorldView() == null) {
			return;
		}

		GameObject obj = event.getGameObject();
		if (obj == null || obj.getWorldView() == null) return;

		int id = obj.getId();

		if (client.getLocalPlayer().getWorldView() != null && obj.getWorldView() != null && client.getLocalPlayer().getWorldView() == obj.getWorldView())
		{
			if (isStarboardNetObject(id)) {
				netObjectByIndex[0] = obj;
				return;
			}

			if (isPortNetObject(id)) {
				netObjectByIndex[1] = obj;
				return;
			}
		}
		ShoalData.ShoalSpecies species = ShoalData.ShoalSpecies.fromGameObjectId(id);
		if (species == null) {
			return;
		}

		int worldViewId = object.getWorldView().getId();
		ShoalData shoal = nearestShoal;
		if (shoal == null) {
			return;
		}

		shoal.setSpecies(species);
		shoal.setShoalObject(object);
		shoal.setDepthFromAnimation();
		shoal.setCurrent(shoal.getWorldEntity().getLocalLocation());

		log.debug("Shoal worldViewId={} species={} objectId={}", worldViewId, species, id);
	}

	public void onGameObjectDespawned(GameObjectDespawned event) {
		GameObject obj = event.getGameObject();
		if (obj == null) return;

		if (netObjectByIndex[0] == obj) netObjectByIndex[0] = null;
		if (netObjectByIndex[1] == obj) netObjectByIndex[1] = null;
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
        ShoalData shoal = getNearestShoal();
		if (shoal == null) {
			return;
		}
		shoal.setDepthFromAnimation();

		shoal.setCurrent(shoal.getWorldEntity().getLocalLocation());

/*

		LocalPoint current = shoal.getWorldEntity().getLocalLocation();
		LocalPoint next = shoal.getWorldEntity().getTargetLocation();

		WorldPoint currentWorldPoint = WorldPoint.fromLocalInstance(client, current);
		WorldPoint last = shoal.getLast();

		boolean isMoving = (current != null && next != null && !current.equals(next));

		if (currentWorldPoint != null && isMoving) {
			if ((last == null || worldDistanceSq(last, currentWorldPoint) < 40 * 40)) {
				shoal.setPathPoints(currentWorldPoint);
			} else {
				shoal.setPathPoints(currentWorldPoint);
			}
		}

		if (shoal.getWasMoving() && !isMoving && current != null)
		{
			shoal.setStopPoints(currentWorldPoint);
		}

		shoal.setWasMoving(isMoving);
		shoal.setLast(currentWorldPoint);

		*/
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		ChatMessageType type = event.getType();

		if (type == ChatMessageType.GAMEMESSAGE || type == ChatMessageType.SPAM)
		{
            String msg = event.getMessage().replaceAll("<[^>]*>","");
            String substring = "";
			if (msg.equals("You empty the nets into the cargo hold.") || msg.equals("You empty the net into the cargo hold.")) {
                fishQuantity = 0;
                log.debug("Emptied nets");
                notifiedFull = false;
            }

			if (msg.contains("Trawler's trust")) {
				// Another message includes the additional fish caught
				return;
			}

			String substring = "";
			if (msg.contains("You catch "))
			{
				int index = "You catch ".length();
				substring = msg.substring(index, msg.indexOf(" ", index + 1));
			} else if (msg.contains(" catches ")) {
				int index = msg.indexOf(" catches ") + " catches ".length();
				substring = msg.substring(index, msg.indexOf(" ", index + 1));
			}

			if (!substring.equals(""))
			{
				fishQuantity += convertToNumber(substring);
                int totalNetSize = 0;
                if (netList[0] != null)
                {
                    totalNetSize += netList[0].getNetSize();
                }
                if (netList[1] != null)
                {
                    totalNetSize += netList[1].getNetSize();
                }
                if (fishQuantity >= totalNetSize && config.notifyNetFull() && !notifiedFull) {
                    notifier.notify("Trawling net(s) full! Empty now!");
                    notifiedFull = true;
                }
			}
		}
	}


	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		int changed = e.getVarbitId();

		switch (changed)
		{

			case VarbitID.SAILING_SIDEPANEL_BOAT_TRAWLING_NET_0_DEPTH:
				netList[0].setNetDepth(e.getValue());
				break;
			case VarbitID.SAILING_SIDEPANEL_BOAT_TRAWLING_NET_1_DEPTH:
				netList[1].setNetDepth(e.getValue());
				break;
		}

	}

	public int localDistanceSq(LocalPoint a, LocalPoint b)
	{
		int dx = a.getX() - b.getX();
		int dy = a.getY() - b.getY();
		return dx * dx + dy * dy;
	}
/*
	public int worldDistanceSq(WorldPoint a, WorldPoint b)
	{
		int dx = a.getX() - b.getX();
		int dy = a.getY() - b.getY();
		return dx * dx + dy * dy;
	}
*/
	private void rebuildTrackedShoals() {
		trackedShoals.clear();

		if(config.showGiantKrill()) {
			for (int id : ShoalTypes.GIANT_KRILL.getIds()) {
				trackedShoals.add(id);
			}
		}
		if(config.showHaddock()) {
			for (int id : ShoalTypes.HADDOCK.getIds()) {
				trackedShoals.add(id);
			}
		}
		if(config.showHalibut()) {
			for (int id : ShoalTypes.HALIBUT.getIds()) {
				trackedShoals.add(id);
			}
		}
		if(config.showYellowfin()) {
			for (int id : ShoalTypes.YELLOWFIN.getIds()) {
				trackedShoals.add(id);
			}
		}
		if(config.showBluefin()) {
			for (int id : ShoalTypes.BLUEFIN.getIds()) {
				trackedShoals.add(id);
			}
		}
		if(config.showMarlin()) {
			for (int id : ShoalTypes.MARLIN.getIds()) {
				trackedShoals.add(id);
			}
		}
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event) {
		if (!event.getGroup().equals("deepseatrawling")) {
			return;
		}
		rebuildTrackedShoals();
        rebuildShoalColours();

		/*
		StringBuilder builder = new StringBuilder();
		builder.append("Shoal wv=").append(nearestShoal.getWorldViewId()).append(" species=").append(nearestShoal.getSpecies()).append(" path=[");

		for (WorldPoint worldPoint : nearestShoal.getPathPoints()) {
			builder.append(worldPoint.getX()).append(", ").append(worldPoint.getY()).append(", 0|");
		}
		builder.append("] stops=[");
		for (WorldPoint worldPoint : nearestShoal.getStopPoints()) {
			builder.append(worldPoint.getX()).append(", ").append(worldPoint.getY()).append(", 0|");
		}

		log.info(builder.toString());*/
	}

	private static final Map<String, Integer> WORD_NUMBERS = Map.of(
			"a", 1,
			"two", 2,
			"three", 3,
			"four", 4,
			"five", 5,
			"six", 6,
			"seven", 7,
			"eight", 8,
			"nine", 9,
			"ten", 10
	);

	private int convertToNumber(String s)
	{
		s = s.toLowerCase();

		Integer v = WORD_NUMBERS.get(s);
		if (v != null)
		{
			return v;
		}

		throw new IllegalArgumentException("Unknown quantity: " + s);
	}
	public boolean isPortNetObject(int objectId)
	{
		return objectId == net.runelite.api.gameval.ObjectID.SAILING_ROPE_TRAWLING_NET_3X8_PORT
				|| objectId == net.runelite.api.gameval.ObjectID.SAILING_LINEN_TRAWLING_NET_3X8_PORT
				|| objectId == net.runelite.api.gameval.ObjectID.SAILING_HEMP_TRAWLING_NET_3X8_PORT
				|| objectId == net.runelite.api.gameval.ObjectID.SAILING_COTTON_TRAWLING_NET_3X8_PORT;
	}

	public boolean isStarboardNetObject(int objectId)
	{
		return objectId == net.runelite.api.gameval.ObjectID.SAILING_ROPE_TRAWLING_NET_3X8_STARBOARD
				|| objectId == net.runelite.api.gameval.ObjectID.SAILING_LINEN_TRAWLING_NET_3X8_STARBOARD
				|| objectId == net.runelite.api.gameval.ObjectID.SAILING_HEMP_TRAWLING_NET_3X8_STARBOARD
				|| objectId == net.runelite.api.gameval.ObjectID.SAILING_COTTON_TRAWLING_NET_3X8_STARBOARD
				|| objectId == net.runelite.api.gameval.ObjectID.SAILING_ROPE_TRAWLING_NET
				|| objectId == net.runelite.api.gameval.ObjectID.SAILING_LINEN_TRAWLING_NET
				|| objectId == net.runelite.api.gameval.ObjectID.SAILING_HEMP_TRAWLING_NET
				|| objectId == net.runelite.api.gameval.ObjectID.SAILING_COTTON_TRAWLING_NET;
	}

    private void rebuildShoalColours() {
        speciesColours.clear();
        speciesColours.put(ShoalData.ShoalSpecies.GIANT_KRILL, config.giantKrillColour());
        speciesColours.put(ShoalData.ShoalSpecies.YELLOWFIN, config.yellowfinColour());
        speciesColours.put(ShoalData.ShoalSpecies.HADDOCK, config.haddockColour());
        speciesColours.put(ShoalData.ShoalSpecies.HALIBUT, config.halibutColour());
        speciesColours.put(ShoalData.ShoalSpecies.BLUEFIN, config.bluefinColour());
        speciesColours.put(ShoalData.ShoalSpecies.MARLIN, config.marlinColour());
        speciesColours.put(ShoalData.ShoalSpecies.SHIMMERING, config.shimmeringColour());
        speciesColours.put(ShoalData.ShoalSpecies.GLISTENING, config.glisteningColour());
        speciesColours.put(ShoalData.ShoalSpecies.VIBRANT, config.vibrantColour());

    }

}
