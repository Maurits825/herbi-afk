package com.herbiafk;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.herbiboars.HerbiboarPlugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

public class HerbiAfkOverlay extends Overlay {

    private final HerbiAfkPlugin plugin;
    private final HerbiAfkConfig config;

    @Inject
    private Client client;

    @Inject
    public HerbiAfkOverlay(HerbiAfkPlugin plugin, HerbiAfkConfig config) {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!plugin.isInHerbiboarArea())
        {
            return null;
        }

        if (config.showPathLine() && plugin.getPathLinePoints() != null) {
            WorldLines.drawLinesOnWorld(graphics, client, plugin.getPathLinePoints(), config.getLineColor());
        }
        return null;
    }
}
