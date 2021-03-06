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
import java.util.ArrayList;
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
	private List<WorldPoint> pathLinePoints = new ArrayList<>();
	@Getter
	private WorldPoint nextSearchSpot;

	private WorldPoint startLocation, endLocation;

	private enum HerbiState {
		IDLE,
		FINDING_START,
		HUNTING,
		STUNNED,
	}

	private static boolean varbitChanged = false;
	private HerbiState herbiState;

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

	private static final List<WorldPoint> START_LOCATIONS = ImmutableList.of(
			new WorldPoint(3686, 3870, 0),
			new WorldPoint(3751, 3850, 0),
			new WorldPoint(3695, 3800, 0),
			new WorldPoint(3704, 3810, 0),
			new WorldPoint(3705, 3830, 0)
	);

	private static final String HERBI_STUN = "You stun the creature";
	private static final String HERBI_KC = "Your herbiboar harvest count is:";
	private static final String HERBIBOAR_NAME = "Herbiboar";
	private static final String HERBI_CIRCLES = "The creature has successfully confused you with its tracks, leading you round in circles";
	private static final Integer PATH_LINE_DIVISION = 10;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
		overlayManager.add(minimapOverlay);

		npcOverlayService.registerHighlighter(isHerbiboar);

		pathLinePoints = new ArrayList<>();

		herbiState = HerbiState.IDLE;
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
				herbiState = HerbiState.IDLE;
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
		if (!isInHerbiboarArea()) {
			herbiState = HerbiState.IDLE;
			return;
		}

		switch (herbiState) {
			case FINDING_START:
				if (client.getLocalPlayer() != null) {
					startLocation = client.getLocalPlayer().getWorldLocation();
				}
				endLocation = getNearestStartLocation();
				if (endLocation != null) {
					updatePathLinePoints(startLocation, endLocation);
				}

				if (varbitChanged) {
					updateTrailData();
					varbitChanged = false;
				}
				break;

			case HUNTING:
				if (varbitChanged) {
					updateTrailData();
					varbitChanged = false;
				}

				if (config.pathRelativeToPlayer()) {
					if (client.getLocalPlayer() != null && pathLinePoints != null) {
						startLocation = client.getLocalPlayer().getWorldLocation();
						updatePathLinePoints(startLocation, endLocation);
					}
				}
				break;

			case STUNNED:
				updateTrailData();
				npcOverlayService.rebuild();
				break;

			case IDLE:
				if (varbitChanged) {
					updateTrailData();
					varbitChanged = false;
				}
				break;
		}
	}

	private void updateTrailData() {
		updateStartAndEndLocation();
		if (startLocation != null && endLocation != null) {
			updatePathLinePoints(startLocation, endLocation);
		}
	}

	private void updateStartAndEndLocation() {
		List<? extends Enum<?>> currentPath = herbiboarPlugin.getCurrentPath();
		int currentPathSize = currentPath.size();

		WorldPoint newStartLocation = null;
		WorldPoint newEndLocation = null;

		if (herbiState == HerbiState.STUNNED) {
			newStartLocation = END_LOCATIONS.get(finishedId - 1);
			NPC herbi = getHerbiboarNpc();
			if (herbi != null) {
				newEndLocation = herbi.getWorldLocation();
			}
		}
		else if (currentPathSize >= 1) {
			if (herbiboarPlugin.getFinishId() > 0) {
				newStartLocation = HerbiboarSearchSpot.valueOf(currentPath.get(currentPathSize - 1).toString()).getLocation();
				finishedId = herbiboarPlugin.getFinishId();
				newEndLocation = END_LOCATIONS.get(finishedId - 1);
			}
			else if (currentPathSize == 1) {
				newStartLocation = herbiboarPlugin.getStartPoint();
				newEndLocation = HerbiboarSearchSpot.valueOf(currentPath.get(0).toString()).getLocation();
			} else {
				newStartLocation = HerbiboarSearchSpot.valueOf(currentPath.get(currentPathSize - 2).toString()).getLocation();
				newEndLocation = HerbiboarSearchSpot.valueOf(currentPath.get(currentPathSize - 1).toString()).getLocation();
			}
		}

		if (newStartLocation != null && newEndLocation != null) {
			if (config.pathRelativeToPlayer()) {
				if (client.getLocalPlayer() != null) {
					newStartLocation = client.getLocalPlayer().getWorldLocation();
				}
			}

			nextSearchSpot = newEndLocation;

			startLocation = newStartLocation;
			endLocation = newEndLocation;

			herbiState = HerbiState.HUNTING;
		}
	}

	private WorldPoint getNearestStartLocation() {
		WorldPoint neartestPoint = null;
		WorldPoint player = null;

		if (client.getLocalPlayer() != null) {
			player = client.getLocalPlayer().getWorldLocation();
		}
		if (player == null) {
			 return null;
		}

		double shortestDistance = Double.MAX_VALUE;
		for (WorldPoint startPoint: START_LOCATIONS) {
			double distance = player.distanceTo2D(startPoint);
			if (distance < shortestDistance) {
				neartestPoint = startPoint;
				shortestDistance = distance;
			}
		}

		return  neartestPoint;
	}

	private void updatePathLinePoints(WorldPoint start, WorldPoint end) {
		double distance = start.distanceTo2D(end);
		int divisions = (int)Math.ceil(distance / PATH_LINE_DIVISION);

		pathLinePoints.clear();
		pathLinePoints.add(start);

		if (divisions == 1) {
			pathLinePoints.add(end);
			return;
		}

		double angle = Math.atan2((end.getY()-start.getY()), (end.getX()-start.getX()));
		double deltaH = distance / divisions;
		int deltaX = (int)(deltaH * Math.cos(angle));
		int deltaY = (int)(deltaH * Math.sin(angle));

		int currentX = start.getX();
		int currentY = start.getY();

		for (int i = 1; i < divisions; i++) {
			currentX += deltaX;
			currentY += deltaY;
			pathLinePoints.add(new WorldPoint(currentX, currentY, 0));
		}

		pathLinePoints.add(end);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() == ChatMessageType.GAMEMESSAGE) {
			String message = Text.sanitize(Text.removeTags(event.getMessage()));
			if (message.contains(HERBI_STUN)) {
				herbiState = HerbiState.STUNNED;
			}
			else if (message.contains(HERBI_KC) || message.contains(HERBI_CIRCLES)) {
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
			Color color = config.getHerbiboarColor();
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
		pathLinePoints.clear();

		nextSearchSpot = null;
		startLocation = null;
		endLocation = null;

		finishedId = -1;

		herbiState = HerbiState.FINDING_START;
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
