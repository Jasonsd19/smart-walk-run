package com.smartWalkRun;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Collection;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

public class SmartWalkRunOverlay extends Overlay {
	private final Client client;
	private final SmartWalkRunPlugin plugin;
	private final SmartWalkRunConfig config;

	@Inject
	private SmartWalkRunOverlay(Client client, SmartWalkRunPlugin plugin, SmartWalkRunConfig config) {
		this.client = client;
		this.plugin = plugin;
		this.config = config;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(OverlayPriority.MED);
	}

	// TODO: Only attempt to render tiles in current region. Currently, we iterate entire list of tiles
	@Override
	public Dimension render(Graphics2D graphics) {
		boolean inInstance = client.isInInstancedRegion();

		for (java.util.Map.Entry<WorldPoint, MovementType> entry : plugin.getTileMap().entrySet()) {
			WorldPoint worldPoint = entry.getKey();
			MovementType type = entry.getValue();

			if (inInstance) {
				Collection<WorldPoint> worldPoints = WorldPoint.toLocalInstance(client, worldPoint);
				for (WorldPoint wp : worldPoints) {
					renderTile(graphics, wp, type);
				}

				continue;
			}

			renderTile(graphics, worldPoint, type);
		}

		return null;
	}

	private void renderTile(Graphics2D graphics, WorldPoint worldPoint, MovementType type) {
		LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);
		if (localPoint == null) {
			return;
		}

		Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
		if (poly != null) {
			Color color = (type == MovementType.RUN) ? config.runTileColor() : config.walkTileColor();
			OverlayUtil.renderPolygon(graphics, poly, color);
		}

		if (config.drawTileLabel()) {
			String text = (type == MovementType.RUN) ? "Run" : "Walk";
			Point textLocation = Perspective.getCanvasTextLocation(client, graphics, localPoint, text, 0);
			if (textLocation != null) {
				OverlayUtil.renderTextLocation(graphics, textLocation, text, Color.WHITE);
			}
		}
	}
}
