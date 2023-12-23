/*
 * Copyright (c) 2021, Zoinkwiz <https://github.com/Zoinkwiz>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.herbiafk.QuestHelperTools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Client;
import static net.runelite.api.Constants.CHUNK_SIZE;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.RenderOverview;
import net.runelite.api.Varbits;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.ComponentID;


public class QuestPerspective
{
    public static Collection<WorldPoint> toLocalInstance(Client client, WorldPoint worldPoint)
    {
        if (!client.isInInstancedRegion())
        {
            return Collections.singleton(worldPoint);
        }

        // find instance chunks using the template point. there might be more than one.
        List<WorldPoint> worldPoints = new ArrayList<>();

        int[][][] instanceTemplateChunks = client.getInstanceTemplateChunks();
        for (int z = 0; z < instanceTemplateChunks.length; ++z)
        {
            for (int x = 0; x < instanceTemplateChunks[z].length; ++x)
            {
                for (int y = 0; y < instanceTemplateChunks[z][x].length; ++y)
                {
                    int chunkData = instanceTemplateChunks[z][x][y];
                    int rotation = chunkData >> 1 & 0x3;
                    int templateChunkY = (chunkData >> 3 & 0x7FF) * CHUNK_SIZE;
                    int templateChunkX = (chunkData >> 14 & 0x3FF) * CHUNK_SIZE;
                    if (worldPoint.getX() >= templateChunkX && worldPoint.getX() < templateChunkX + CHUNK_SIZE
                            && worldPoint.getY() >= templateChunkY && worldPoint.getY() < templateChunkY + CHUNK_SIZE)
                    {
                        WorldPoint p =
                                new WorldPoint(client.getBaseX() + x * CHUNK_SIZE + (worldPoint.getX() & (CHUNK_SIZE - 1)),
                                        client.getBaseY() + y * CHUNK_SIZE + (worldPoint.getY() & (CHUNK_SIZE - 1)),
                                        z);
                        p = rotate(p, rotation);
                        if (p.isInScene(client))
                        {
                            worldPoints.add(p);
                        }
                    }
                }
            }
        }
        return worldPoints;
    }

    private static WorldPoint rotate(WorldPoint point, int rotation)
    {
        int chunkX = point.getX() & ~(CHUNK_SIZE - 1);
        int chunkY = point.getY() & ~(CHUNK_SIZE - 1);
        int x = point.getX() & (CHUNK_SIZE - 1);
        int y = point.getY() & (CHUNK_SIZE - 1);
        switch (rotation)
        {
            case 1:
                return new WorldPoint(chunkX + y, chunkY + (CHUNK_SIZE - 1 - x), point.getPlane());
            case 2:
                return new WorldPoint(chunkX + (CHUNK_SIZE - 1 - x), chunkY + (CHUNK_SIZE - 1 - y), point.getPlane());
            case 3:
                return new WorldPoint(chunkX + (CHUNK_SIZE - 1 - y), chunkY + x, point.getPlane());
        }
        return point;
    }

    public static LocalPoint getInstanceLocalPoint(Client client, WorldPoint wp)
    {
        WorldPoint instanceWorldPoint = getInstanceWorldPoint(client, wp);
        if (instanceWorldPoint == null)
        {
            return null;
        }

        return LocalPoint.fromWorld(client, instanceWorldPoint);
    }

    public static WorldPoint getInstanceWorldPoint(Client client, WorldPoint wp)
    {
        Collection<WorldPoint> points = QuestPerspective.toLocalInstance(client, wp);

        for (WorldPoint point : points)
        {
            if (point != null)
            {
                return point;
            }
        }
        return null;
    }

    public static Point getMinimapPoint(Client client, WorldPoint start, WorldPoint destination)
    {
        RenderOverview ro = client.getRenderOverview();
        if (ro.getWorldMapData().surfaceContainsPosition(start.getX(), start.getY()) !=
                ro.getWorldMapData().surfaceContainsPosition(destination.getX(), destination.getY()))
        {
            return null;
        }

        int x = (destination.getX() - start.getX());
        int y = (destination.getY() - start.getY());

        float maxDistance = Math.max(Math.abs(x), Math.abs(y));
        x = x * 100;
        y = y * 100;
        x /= maxDistance;
        y /= maxDistance;

        Widget minimapDrawWidget;
        if (client.isResized())
        {
            if (client.getVarbitValue(Varbits.SIDE_PANELS) == 1)
            {

                minimapDrawWidget = client.getWidget(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MINIMAP_DRAW_AREA);
            }
            else
            {
                minimapDrawWidget = client.getWidget(ComponentID.RESIZABLE_VIEWPORT_MINIMAP_DRAW_AREA);
            }
        }
        else
        {
            minimapDrawWidget = client.getWidget(ComponentID.FIXED_VIEWPORT_MINIMAP_DRAW_AREA);
        }

        if (minimapDrawWidget == null)
        {
            return null;
        }

        final int angle = client.getMapAngle() & 0x7FF;

        final int sin = Perspective.SINE[angle];
        final int cos = Perspective.COSINE[angle];

        final int xx = y * sin + cos * x >> 16;
        final int yy = sin * x - y * cos >> 16;

        Point loc = minimapDrawWidget.getCanvasLocation();
        int miniMapX = loc.getX() + xx + minimapDrawWidget.getWidth() / 2;
        int miniMapY = minimapDrawWidget.getHeight() / 2 + loc.getY() + yy;
        return new Point(miniMapX, miniMapY);
    }
}
