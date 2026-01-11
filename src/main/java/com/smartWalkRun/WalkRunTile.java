package com.smartWalkRun;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WalkRunTile {
	private int x;
	private int y;
	private int z;
	private int regionId;
	private MovementType type;
}