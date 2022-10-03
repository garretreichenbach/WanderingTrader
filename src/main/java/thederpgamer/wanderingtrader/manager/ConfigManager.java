package thederpgamer.wanderingtrader.manager;

import api.mod.config.FileConfiguration;
import thederpgamer.wanderingtrader.WanderingTrader;

public class ConfigManager {

	private static FileConfiguration mainConfig;
	private static FileConfiguration tradeConfig;
	private static final String[] defaultMainConfig = {
			"debug-mode: false",
			"max-world-logs: 5"
	};
	private static final String[] defaultTradeConfig = {
			//TODO: Add default trade config
	};

	public static void initialize(WanderingTrader instance) {
		mainConfig = instance.getConfig("config");
		mainConfig.saveDefault(defaultMainConfig);

		tradeConfig = instance.getConfig("trades");
		tradeConfig.saveDefault(defaultTradeConfig);
	}

	public static FileConfiguration getMainConfig() {
		return mainConfig;
	}

	public static FileConfiguration getTradeConfig() {
		return tradeConfig;
	}
}