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
	private List<WorldPoint> pathLinePoints = new ArrayList<>();
	@Getter
	private WorldPoint nextSearchSpot;

	private WorldPoint startLocation, endLocation;

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
	private static final Integer PATH_LINE_DIVISION = 20;

	//TODO: show path to closest start?
	//TODO: remove pick menu entry swapper stuff? is allowed?
	//TODO: unhighlight when the thing has nothing, game msg: nothing out of place or something
	//TODO: readme.md add recommend herbi plugin setting, mention it depends on it
	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
		overlayManager.add(minimapOverlay);

		npcOverlayService.registerHighlighter(isHerbiboar);

		pathLinePoints = new ArrayList<>();
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
			updateTrailData();
			varbitChanged = false;
		}

		if (herbiStunned) {
			updateTrailData();
			npcOverlayService.rebuild();
			herbiStunned = false;
		}

		if (config.pathRelativeToPlayer()) {
			if (client.getLocalPlayer() != null) {
				WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();
				if (pathLinePoints != null) {
					startLocation = playerLoc;
					updatePathLinePoints(startLocation, endLocation);
				}
			}
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
		if (herbiStunned) {
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
		}
	}

	private void updatePathLinePoints(WorldPoint start, WorldPoint end) {
		double angle = Math.atan2((end.getY()-start.getY()), (end.getX()-start.getX()));

		int deltaX = (int)(PATH_LINE_DIVISION * Math.cos(angle));
		int deltaY = (int)(PATH_LINE_DIVISION * Math.sin(angle));

		int currentX = start.getX();
		int currentY = start.getY();

		pathLinePoints.clear();
		pathLinePoints.add(start);

		double distance = Math.sqrt(Math.pow(end.getX()-start.getX(), 2) + Math.pow(end.getY()-start.getY(), 2));
		int divisions = (int)(distance / PATH_LINE_DIVISION);

		for (int i = 0; i < divisions; i++) {
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
		pathLinePoints.clear();
		nextSearchSpot = null;
		startLocation = null;
		endLocation = null;
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
