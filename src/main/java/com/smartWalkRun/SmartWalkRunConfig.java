package com.smartWalkRun;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(SmartWalkRunConfig.GROUP)
public interface SmartWalkRunConfig extends Config
{
	String GROUP = "smartwalkrun";
	String TILE_DATA_KEY = "tileData";

	@ConfigItem(
			keyName = TILE_DATA_KEY,
			name = "Tile Data",
			description = "Stored data for smart walk/run tiles",
			hidden = true
	)
	default String tileData() {
		return "";
	}

	@ConfigItem(
		keyName = "drawTileLabel",
		name = "Draw Tile Label",
		description = "Show text indicating if the tile is a Run or Walk tile"
	)
	default boolean drawTileLabel() {
		return false;
	}

	@ConfigItem(
		keyName = "walkTileColor",
		name = "Walk Tile Color",
		description = "Color of the Walk tiles"
	)
	default Color walkTileColor() {
		return Color.YELLOW;
	}

	@ConfigItem(
		keyName = "runTileColor",
		name = "Run Tile Color",
		description = "Color of the Run tiles"
	)
	default Color runTileColor() {
		return Color.GREEN;
	}
}
