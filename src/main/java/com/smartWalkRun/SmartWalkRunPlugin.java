package com.smartWalkRun;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.util.ArrayList;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Smart Walk Run",
	description = "Automatically walk or run in configured areas",
	tags = {"movement", "run", "walk", "quality of life"}
)
public class SmartWalkRunPlugin extends Plugin {
	private static final String MARK_WALK_OPTION = "Mark Walk";
	private static final String MARK_RUN_OPTION = "Mark Run";
	private static final String UNMARK_OPTION = "Unmark";
	private static final String WALK_HERE_OPTION = "Walk here";

	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Gson gson;

	// TODO: Load only relevant region tiles - convert into WalkRunTile class
	private final Map<WorldPoint, MovementType> tileMap = new HashMap<>();

	private boolean pendingCtrlRelease = false;
	private boolean pendingCtrlPress = false;

	@Inject
	private SmartWalkRunOverlay overlay;

	@Override
	protected void startUp() throws Exception {
		overlayManager.add(overlay);
		loadCache();
	}

	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(overlay);
		tileMap.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged) {
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
			loadCache();
		}
	}

	@Subscribe
	public void onClientTick(ClientTick clientTick) {
		if (pendingCtrlRelease) {
			setCtrl(false);
			pendingCtrlRelease = false;
		}

		if (pendingCtrlPress) {
			setCtrl(true);
			pendingCtrlPress = false;
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event) {
		if (event.getOption().equals(WALK_HERE_OPTION)) {
			if (!client.isKeyPressed(KeyCode.KC_SHIFT)) {
				return;
			}

			Tile selectedSceneTile = client.getSelectedSceneTile();
			if (selectedSceneTile == null) {
				return;
			}

			WorldPoint worldPoint = getWorldPoint(selectedSceneTile);

			MenuEntry parentEntry = client.createMenuEntry(-1)
					.setOption("Smart Walk Run")
					.setTarget(event.getTarget())
					.setType(MenuAction.RUNELITE);

			Menu subMenu = parentEntry.createSubMenu();

			if (tileMap.containsKey(worldPoint)) {
				subMenu.createMenuEntry(-1)
						.setOption(UNMARK_OPTION)
						.setTarget(event.getTarget())
						.setType(MenuAction.RUNELITE)
						.onClick(this::handleMenuAction);
				return;
			}

			subMenu.createMenuEntry(-1)
				.setOption(MARK_WALK_OPTION)
				.setTarget(event.getTarget())
				.setType(MenuAction.RUNELITE)
				.onClick(this::handleMenuAction);

			subMenu.createMenuEntry(-1)
				.setOption(MARK_RUN_OPTION)
				.setTarget(event.getTarget())
				.setType(MenuAction.RUNELITE)
				.onClick(this::handleMenuAction);
		}
	}

	private void handleMenuAction(MenuEntry entry) {
		Tile selectedSceneTile = client.getSelectedSceneTile();
		if (selectedSceneTile == null) {
			return;
		}

		WorldPoint worldPoint = getWorldPoint(selectedSceneTile);

		switch (entry.getOption()) {
			case MARK_WALK_OPTION:
				tileMap.put(worldPoint, MovementType.WALK);
				break;
			case MARK_RUN_OPTION:
				tileMap.put(worldPoint, MovementType.RUN);
				break;
			case UNMARK_OPTION:
				tileMap.remove(worldPoint);
				break;
		}

		updateCache();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		if (event.getMenuAction() != MenuAction.WALK) {
			return;
		}

		Tile targetTile = client.getSelectedSceneTile();
		if (targetTile == null) {
			return;
		}

		WorldPoint targetPoint = getWorldPoint(targetTile);
		MovementType desiredMovement = tileMap.get(targetPoint);

		if (desiredMovement == null) {
			return;
		}

		boolean isRunning = client.getVarpValue(173) == 1;
		boolean isCtrlPressed = client.isKeyPressed(KeyCode.KC_CONTROL);
		boolean shouldInvert = false;

		if (isRunning && desiredMovement == MovementType.WALK) {
			shouldInvert = true;
		}

		if (!isRunning && desiredMovement == MovementType.RUN) {
			shouldInvert = true;
		}

		if (shouldInvert && !isCtrlPressed) {
			setCtrl(true);
			pendingCtrlRelease = true;
		}

		if (!shouldInvert && isCtrlPressed) {
			setCtrl(false);
			pendingCtrlPress = true;
		}

		log.debug("Processed movement - desiredMovement: {}, isRunning: {}, isCtrlPressed: {}, shouldInvert: {}", desiredMovement, isRunning, isCtrlPressed, shouldInvert);
	}

	private void setCtrl(boolean pressed) {
		if (client.getCanvas() == null) return;

		KeyEvent event = new KeyEvent(
			client.getCanvas(),
			pressed ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED,
			System.currentTimeMillis(),
			0,
			KeyEvent.VK_CONTROL,
			KeyEvent.CHAR_UNDEFINED
		);

		client.getCanvas().dispatchEvent(event);
		log.debug("{} CTRL KEY", pressed ? "PRESSED" : "RELEASED");
	}

	private WorldPoint getWorldPoint(Tile tile) {
		if (tile == null) {
			return null;
		}

		WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, tile.getLocalLocation());
		if (worldPoint == null) {
			worldPoint = tile.getWorldLocation();
		}

		return worldPoint;
	}

	// TODO: Instead of loading all tiles into tileMap only load in tiles relevant to the current region
	//  reload relevant tiles everytime region changes
	private void loadCache() {
		tileMap.clear();
		if (client.getGameState() != GameState.LOGGED_IN) {
			return;
		}

		String json = configManager.getConfiguration(SmartWalkRunConfig.GROUP, SmartWalkRunConfig.TILE_DATA_KEY);
		if (Strings.isNullOrEmpty(json)) {
			return;
		}

		List<WalkRunTile> tiles = gson.fromJson(json, new TypeToken<List<WalkRunTile>>(){}.getType());
		for (WalkRunTile wrt : tiles) {
			WorldPoint wp = WorldPoint.fromRegion(wrt.getRegionId(), wrt.getX(), wrt.getY(), wrt.getZ());
			tileMap.put(wp, wrt.getType());
		}
	}

	// TODO: Cache tiles by regionID so we are only updating the relevant subset of tiles
	//  and not updating the entire list of tiles
	private void updateCache() {
		List<WalkRunTile> tiles = new ArrayList<>();
		for (Map.Entry<WorldPoint, MovementType> entry : tileMap.entrySet()) {
			WorldPoint wp = entry.getKey();
			tiles.add(new WalkRunTile(wp.getRegionX(), wp.getRegionY(), wp.getPlane(), wp.getRegionID(), entry.getValue()));
		}

		String json = gson.toJson(tiles);
		configManager.setConfiguration(SmartWalkRunConfig.GROUP, SmartWalkRunConfig.TILE_DATA_KEY, json);
	}

	@Provides
	SmartWalkRunConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(SmartWalkRunConfig.class);
	}

	public Map<WorldPoint, MovementType> getTileMap() {
		return tileMap;
	}
}
