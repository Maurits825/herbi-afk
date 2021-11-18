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

import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.Line2D;
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

    //TODO config colors
    @Override
    public Dimension render(Graphics2D graphics) {
        if (!plugin.isInHerbiboarArea())
        {
            return null;
        }

        if (plugin.getPathLinePoints() != null) {
            WorldLines.drawLinesOnWorld(graphics, client, plugin.getPathLinePoints(), Color.CYAN);
        }

        if (plugin.getNextSearchSpot() != null) {
            int x = plugin.getNextSearchSpot().getX();
            int y = plugin.getNextSearchSpot().getY();
            Line2D.Double line = new Line2D.Double(x, y - 18, x, y - 8);

            //DirectionArrow.drawMinimapArrow(graphics, line, Color.CYAN);
            //DirectionArrow.createMinimapDirectionArrow(graphics, client, plugin.getNextSearchSpot(), Color.GREEN);
            //DirectionArrow.renderMinimapArrow(graphics, client, plugin.getNextSearchSpot(), Color.RED);
            DirectionArrow.renderMinimapArrow(graphics, client, new WorldPoint(3406, 3494, 0), Color.CYAN);
        }
        return null;
    }
}
