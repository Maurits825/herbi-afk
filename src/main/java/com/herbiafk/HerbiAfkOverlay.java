package com.herbiafk;

import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.herbiboars.HerbiboarPlugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

public class HerbiAfkOverlay extends Overlay {

    private final HerbiAfkPlugin plugin;

    @Inject
    private Client client;

    @Inject
    public HerbiAfkOverlay(HerbiAfkPlugin plugin) {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!plugin.isInHerbiboarArea())
        {
            return null;
        }

        if (plugin.getPathLinePoints() != null) {
            WorldLines.drawLinesOnWorld(graphics, client, plugin.getPathLinePoints(), Color.CYAN);
        }
        return null;
    }
}
