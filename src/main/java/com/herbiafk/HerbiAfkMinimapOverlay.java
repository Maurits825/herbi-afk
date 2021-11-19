package com.herbiafk;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.*;

public class HerbiAfkMinimapOverlay extends Overlay {

    private final HerbiAfkPlugin plugin;
    private final HerbiAfkConfig config;

    @Inject
    private Client client;

    @Inject
    public HerbiAfkMinimapOverlay(HerbiAfkPlugin plugin, HerbiAfkConfig config) {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        this.plugin = plugin;
        this.config = config;
    }
    
    @Override
    public Dimension render(Graphics2D graphics) {
        if (!plugin.isInHerbiboarArea())
        {
            return null;
        }

        if (config.showMiniMapArrow() && plugin.getNextSearchSpot() != null) {
            DirectionArrow.renderMinimapArrow(graphics, client, plugin.getNextSearchSpot(), config.getArrowColor());
        }
        return null;
    }
}
