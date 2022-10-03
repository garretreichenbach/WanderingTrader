package thederpgamer.wanderingtrader.manager;

import api.mod.config.PersistentObjectUtil;
import thederpgamer.wanderingtrader.WanderingTrader;
import thederpgamer.wanderingtrader.data.PlayerData;
import thederpgamer.wanderingtrader.data.TradeItemData;
import thederpgamer.wanderingtrader.data.Trader;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Manages the trader.
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class TraderManager {

	public static final HashMap<TradeItemData, Float> pool = new HashMap<>();
	private static Trader trader;

	public static void initialize() {
		generateDeals();
		(trader = new Trader()).start();
	}

	/**
	 * Generates random deals from the trader's pool.
	 */
	public static void generateDeals() {
		ArrayList<String> valueList = ConfigManager.getTradeConfig().getList("trader-pool");
		for(String value : valueList) {
			String[] split = value.split(":");
			TradeItemData itemData = new TradeItemData(Short.parseShort(split[0]), Integer.parseInt(split[1]), Long.parseLong(split[2]));
			float weight = Float.parseFloat(split[3]);
			if(Math.random() < weight) pool.put(itemData, weight);
		}
	}

	/**
	 * Loads the player data map from persistent storage.
	 *
	 * @return The player data map
	 */
	public static HashMap<String, PlayerData> loadPlayerDataMap() {
		HashMap<String, PlayerData> playerMap = new HashMap<>();
		ArrayList<Object> data = PersistentObjectUtil.getObjects(WanderingTrader.getInstance().getSkeleton(), PlayerData.class);
		for(Object object : data) {
			PlayerData playerData = (PlayerData) object;
			playerMap.put(playerData.getPlayerName(), playerData);
		}
		return playerMap;
	}

	/**
	 * Fetches the Trader instance.
	 * <p>If it is null or the thread is dead somehow, makes a new one and starts it.</p>
	 *
	 * @return The Trader instance
	 */
	public static Trader getTrader() {
		//Check if trader is null or thread is dead
		if(trader == null || !trader.isAlive()) {
			//If so, create a new trader
			WanderingTrader.log.warning("Trader thread is either null or dead, creating a new one.");
			(trader = new Trader()).start();
		}
		return trader;
	}

	public static void clearAggression(String playerName) {
		HashMap<String, PlayerData> playerMap = loadPlayerDataMap();
		if(playerName == null) {
			for(PlayerData playerData : playerMap.values()) {
				playerData.setLastTransgression(-1);
				PersistentObjectUtil.removeObject(WanderingTrader.getInstance().getSkeleton(), playerData);
				PersistentObjectUtil.addObject(WanderingTrader.getInstance().getSkeleton(), playerData);
			}
		} else {
			PlayerData playerData = playerMap.get(playerName);
			if(playerData != null) {
				playerData.setLastTransgression(-1);
				PersistentObjectUtil.removeObject(WanderingTrader.getInstance().getSkeleton(), playerData);
				PersistentObjectUtil.addObject(WanderingTrader.getInstance().getSkeleton(), playerData);
			}
		}
		PersistentObjectUtil.save(WanderingTrader.getInstance().getSkeleton());
	}
}