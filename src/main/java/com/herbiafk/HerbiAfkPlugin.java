package com.herbiafk;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.VarbitChanged;
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
	private WorldPoint startLocation, endLocation;

	private enum HerbiState
	{
		IDLE,
		FINDING_START,
		HUNTING,
		STUNNED,
	}

	private static boolean varbitChanged = false;
	private HerbiState herbiState;

	private int finishedId = -1;

	private static final String HERBI_STUN = "You stun the creature";
	private static final String HERBI_KC = "Your herbiboar harvest count is:";
	private static final String HERBIBOAR_NAME = "Herbiboar";
	private static final String HERBI_CIRCLES = "The creature has successfully confused you with its tracks, leading you round in circles";

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
	public void onGameTick(GameTick event)
	{
		if (!isInHerbiboarArea())
		{
			herbiState = HerbiState.IDLE;
			return;
		}

		if (client.getLocalPlayer() == null)
		{
			return;
		}

		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

		if (varbitChanged)
		{
			updateStartAndEndLocation();
			varbitChanged = false;
		}

		switch (herbiState)
		{
			case FINDING_START:
				startLocation = playerLocation;
				endLocation = Utils.getNearestStartLocation(playerLocation);
				break;

			case HUNTING:
				if (config.pathRelativeToPlayer())
				{
					startLocation = playerLocation;
				}

				break;

			case STUNNED:
				startLocation = config.pathRelativeToPlayer() ? playerLocation : HerbiAfkData.END_LOCATIONS.get(finishedId - 1);
				WorldPoint herbiLocation = getHerbiboarLocation();
				if (herbiLocation != null)
				{
					endLocation = herbiLocation;
				}
				npcOverlayService.rebuild();
				break;

			case IDLE:
				break;
		}

		if (startLocation != null && endLocation != null)
		{
			pathLinePoints = Utils.getPathLinePoints(startLocation, endLocation);
		}
	}

	private void updateStartAndEndLocation()
	{
		List<? extends Enum<?>> currentPath = herbiboarPlugin.getCurrentPath();
		int currentPathSize = currentPath.size();
		if (currentPathSize < 1)
		{
			return;
		}

		WorldPoint newStartLocation;
		WorldPoint newEndLocation;

		if (herbiboarPlugin.getFinishId() > 0)
		{
			newStartLocation = HerbiboarSearchSpot.valueOf(currentPath.get(currentPathSize - 1).toString()).getLocation();
			finishedId = herbiboarPlugin.getFinishId();
			newEndLocation = HerbiAfkData.END_LOCATIONS.get(finishedId - 1);
		}
		else if (currentPathSize == 1)
		{
			newStartLocation = herbiboarPlugin.getStartPoint();
			newEndLocation = HerbiboarSearchSpot.valueOf(currentPath.get(0).toString()).getLocation();
		}
		else
		{
			newStartLocation = HerbiboarSearchSpot.valueOf(currentPath.get(currentPathSize - 2).toString()).getLocation();
			newEndLocation = HerbiboarSearchSpot.valueOf(currentPath.get(currentPathSize - 1).toString()).getLocation();
		}

		startLocation = newStartLocation;
		endLocation = newEndLocation;

		herbiState = HerbiState.HUNTING;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() == ChatMessageType.GAMEMESSAGE)
		{
			String message = Text.sanitize(Text.removeTags(event.getMessage()));
			if (message.contains(HERBI_STUN))
			{
				herbiState = HerbiState.STUNNED;
			}
			else if (message.contains(HERBI_KC) || message.contains(HERBI_CIRCLES))
			{
				resetTrailData();
				herbiState = HerbiState.FINDING_START;
			}
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!isInHerbiboarArea())
		{
			return;
		}

		if (config.dynamicMenuEntrySwap())
		{
			swapTrailMenuEntries(event);
		}
		if (config.npcMenuEntrySwap())
		{
			hideNpcMenuEntries(event);
		}
	}

	private void swapTrailMenuEntries(MenuEntryAdded event)
	{
		String target = event.getTarget();
		for (String menuTarget : HerbiAfkData.TRAIL_MENU_ENTRY_TARGETS)
		{
			if (target.contains(menuTarget))
			{
				MenuEntry entry = event.getMenuEntry();
				WorldPoint entryTargetPoint = WorldPoint.fromScene(client, entry.getParam0(), entry.getParam1(), client.getPlane());

				switch (herbiState)
				{
					case FINDING_START:
					case HUNTING:
						if (!entryTargetPoint.equals(endLocation))
						{
							entry.setDeprioritized(true);
						}
						break;
					case STUNNED:
						entry.setDeprioritized(true);
						break;
				}

				return;
			}
		}
	}

	private void hideNpcMenuEntries(MenuEntryAdded event)
	{
		String target = event.getTarget();
		for (String menuTarget : HerbiAfkData.NPC_MENU_ENTRY_TARGETS)
		{
			if (target.contains(menuTarget))
			{
				MenuEntry entry = event.getMenuEntry();

				switch (herbiState)
				{
					case FINDING_START:
					case HUNTING:
					case STUNNED:
						entry.setDeprioritized(true);
						break;
				}

				return;
			}
		}
	}

	private WorldPoint getHerbiboarLocation()
	{
		final NPC[] cachedNPCs = client.getCachedNPCs();
		for (NPC npc : cachedNPCs)
		{
			if (npc != null)
			{
				if (npc.getName() != null && npc.getName().equals(HERBIBOAR_NAME))
				{
					return npc.getWorldLocation();
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

	private void resetTrailData()
	{
		pathLinePoints.clear();

		startLocation = null;
		endLocation = null;

		finishedId = -1;
	}

	public boolean isInHerbiboarArea()
	{
		return herbiboarPlugin.isInHerbiboarArea();
	}

	@Provides
	HerbiAfkConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HerbiAfkConfig.class);
	}
}
