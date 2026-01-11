package com.smartWalkRun;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SmartWalkRunPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SmartWalkRunPlugin.class);
		RuneLite.main(args);
	}
}