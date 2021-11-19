package com.herbiafk;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.*;

@ConfigGroup("example")
public interface HerbiAfkConfig extends Config
{
	@ConfigItem(
			position = 1,
			keyName = "showPathLine",
			name = "Show path lines",
			description = "Show trail path lines on the world."
	)
	default boolean showPathLine()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
			position = 2,
			keyName = "lineColor",
			name = "Path line color",
			description = "Color of the trail path lines."
	)
	default Color getLineColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
			position = 3,
			keyName = "showMiniMapArrow",
			name = "Display arrow on the minimap",
			description = "Choose whether a flashing arrows points to the next search spot."
	)
	default boolean showMiniMapArrow()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
			position = 4,
			keyName = "arrowColor",
			name = "Minimap arrow color",
			description = "Color of the arrow on the minimap."
	)
	default Color getArrowColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
			position = 5,
			keyName = "showMiniMaplines",
			name = "Display path lines on the minimap",
			description = "Display the trail path lines on the minimap"
	)
	default boolean showMiniMaplines()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
			position = 6,
			keyName = "minimapPathColor",
			name = "Minimap path lines color",
			description = "Color of the trail path lines on the minimap."
	)
	default Color getMinimapPathColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
			position = 7,
			keyName = "highlightHerbiTile",
			name = "Highlight herbiboar tile",
			description = "Highlights herbiboar tile at the end of the trail."
	)
	default boolean highlightHerbiTile()
	{
		return false;
	}

	@ConfigItem(
			position = 8,
			keyName = "highlightHerbiHull",
			name = "Highlight herbiboar hull",
			description = "Highlights herbiboar hull at the end of the trail."
	)
	default boolean highlightHerbiHull()
	{
		return true;
	}

	@ConfigItem(
			position = 9,
			keyName = "highlightHerbiOutline",
			name = "Highlight herbiboar outline",
			description = "Highlights herbiboar outline at the end of the trail."
	)
	default boolean highlightHerbiOutline()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
			position = 10,
			keyName = "herbiboarColor",
			name = "Herbiboar highlight color",
			description = "Color of the herbiboar highlight."
	)
	default Color getHerbiboarColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
			position = 11,
			keyName = "pathRelativeToPlayer",
			name = "Path relative to player",
			description = "Make the trail path line relative to the player."
	)
	default boolean pathRelativeToPlayer()
	{
		return true;
	}
}
