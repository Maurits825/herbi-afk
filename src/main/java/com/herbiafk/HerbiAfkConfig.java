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
			keyName = "lineColor",
			name = "Trail line color",
			description = "Color of the trail lines."
	)
	default Color getLineColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
			keyName = "showMiniMapArrow",
			name = "Display arrow on the mini-map",
			description = "Choose whether a flashing arrows points to the next search spot."
	)
	default boolean showMiniMapArrow()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
			keyName = "arrowColor",
			name = "Minimap arrow color",
			description = "Color of the arrow on the minimap."
	)
	default Color getArrowColor()
	{
		return Color.CYAN;
	}
}
