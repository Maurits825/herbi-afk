package com.herbiafk;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.Graphics2D;
import java.awt.Dimension;

public class HerbiAfkOverlay extends Overlay {

    private final HerbiAfkPlugin plugin;
    private final HerbiAfkConfig config;

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
            WorldLines.drawLinesOnWorld(graphics, plugin.getClient(), plugin.getPathLinePoints(), config.getLineColor());
        }
        return null;
    }
}
