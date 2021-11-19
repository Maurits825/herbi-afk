package com.herbiafk;

import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.npcoverlay.HighlightedNpc;
import net.runelite.client.game.npcoverlay.NpcOverlayService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.herbiboars.HerbiboarPlugin;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Slf4j
@PluginDescriptor(
	name = "Herbi AFK"
)
@PluginDependency(HerbiboarPlugin.class)
public class HerbiAfkPlugin extends Plugin
{
	@Inject
	@Getter
	private Client client;

	@Inject
	private HerbiAfkConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private HerbiAfkOverlay overlay;

	@Inject
	private HerbiAfkMinimapOverlay minimapOverlay;

	@Inject
	private HerbiboarPlugin herbiboarPlugin;

	@Inject
	private NpcOverlayService npcOverlayService;

	@Getter
	private List<WorldPoint> pathLinePoints;
	@Getter
	private WorldPoint nextSearchSpot;

	private boolean varbitChanged = false;
	private boolean herbiStunned = false;

	private int finishedId = -1;

	private static final List<WorldPoint> END_LOCATIONS = ImmutableList.of(
			new WorldPoint(3693, 3798, 0),
			new WorldPoint(3702, 3808, 0),
			new WorldPoint(3703, 3826, 0),
			new WorldPoint(3710, 3881, 0),
			new WorldPoint(3700, 3877, 0),
			new WorldPoint(3715, 3840, 0),
			new WorldPoint(3751, 3849, 0),
			new WorldPoint(3685, 3869, 0),
			new WorldPoint(3681, 3863, 0)
	);

	private static final String HERBI_STUN = "You stun the creature";
	private static final String HERBI_KC = "Your herbiboar harvest count is:";
	private static final String HERBIBOAR_NAME = "Herbiboar";

	//TODO: show path to closest start?
	//TODO: remove pick menu entry swapper stuff? is allowed?
	//TODO: if outside of range, the line wont render
	//TODO: unhighlight when the thing has nothing, game msg: nothing out of place or something
	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
		overlayManager.add(minimapOverlay);

		npcOverlayService.registerHighlighter(isHerbiboar);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		overlayManager.remove(minimapOverlay);

		npcOverlayService.unregisterHighlighter(isHerbiboar);

		resetTrailData();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case HOPPING:
			case LOGGING_IN:
				resetTrailData();
				break;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		varbitChanged = true;
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (varbitChanged) {
			updateTrailWorldPoints();
			varbitChanged = false;
		}

		if (herbiStunned) {
			updateTrailWorldPoints();
			npcOverlayService.rebuild();
			herbiStunned = false;
		}

		if (config.pathRelativeToPlayer()) {
			if (client.getLocalPlayer() != null) {
				WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();
				if (pathLinePoints != null) {
					pathLinePoints.set(0, playerLoc);
				}
			}
		}
	}

	private void updateTrailWorldPoints() {
		List<? extends Enum<?>> currentPath = herbiboarPlugin.getCurrentPath();
		int currentPathSize = currentPath.size();

		WorldPoint startLocation = null;
		WorldPoint endLocation = null;
		if (herbiStunned) {
			startLocation = END_LOCATIONS.get(finishedId - 1);
			NPC herbi = getHerbiboarNpc();
			if (herbi != null) {
				endLocation = herbi.getWorldLocation();
			}
		}
		else if (currentPathSize >= 1) {
			if (herbiboarPlugin.getFinishId() > 0) {
				startLocation = HerbiboarSearchSpot.valueOf(currentPath.get(currentPathSize - 1).toString()).getLocation();
				finishedId = herbiboarPlugin.getFinishId();
				endLocation = END_LOCATIONS.get(finishedId - 1);
			}
			else if (currentPathSize == 1) {
				startLocation = herbiboarPlugin.getStartPoint();
				endLocation = HerbiboarSearchSpot.valueOf(currentPath.get(0).toString()).getLocation();
			} else {
				startLocation = HerbiboarSearchSpot.valueOf(currentPath.get(currentPathSize - 2).toString()).getLocation();
				endLocation = HerbiboarSearchSpot.valueOf(currentPath.get(currentPathSize - 1).toString()).getLocation();
			}
		}

		if (startLocation != null && endLocation != null) {
			if (config.pathRelativeToPlayer()) {
				if (client.getLocalPlayer() != null) {
					startLocation = client.getLocalPlayer().getWorldLocation();
				}
			}

			nextSearchSpot = endLocation;
			pathLinePoints = Arrays.asList(startLocation, endLocation);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() == ChatMessageType.GAMEMESSAGE) {
			String message = Text.sanitize(Text.removeTags(event.getMessage()));
			if (message.contains(HERBI_STUN)) {
				herbiStunned = true;
			}
			else if (message.contains(HERBI_KC)) {
				resetTrailData();
			}
		}
	}

	private NPC getHerbiboarNpc() {
		final NPC[] cachedNPCs = client.getCachedNPCs();
		for (NPC npc : cachedNPCs) {
			if (npc != null) {
				if (npc.getName() != null && npc.getName().equals(HERBIBOAR_NAME)) {
					return npc;
				}
			}
		}
		return null;
	}

	public final Function<NPC, HighlightedNpc> isHerbiboar = (n) -> {
		boolean isHighlight = config.highlightHerbiHull() || config.highlightHerbiTile() || config.highlightHerbiOutline();
		if (isHighlight && n.getName() != null && n.getName().equals(HERBIBOAR_NAME))
		{
			Color color =config.getHerbiboarColor();
			return HighlightedNpc.builder()
					.npc(n)
					.highlightColor(color)
					.fillColor(ColorUtil.colorWithAlpha(color, color.getAlpha() / 12))
					.hull(config.highlightHerbiHull())
					.tile(config.highlightHerbiTile())
					.outline(config.highlightHerbiOutline())
					.build();
		}
		return null;
	};

	private void resetTrailData() {
		pathLinePoints = null;
		nextSearchSpot = null;
		varbitChanged = false;
		herbiStunned = false;
		finishedId = -1;
	}

	public boolean isInHerbiboarArea() {
		return herbiboarPlugin.isInHerbiboarArea();
	}

	@Provides
	HerbiAfkConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HerbiAfkConfig.class);
	}
}
