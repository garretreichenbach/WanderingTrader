package thederpgamer.wanderingtrader;

import api.listener.Listener;
import api.listener.events.controller.ServerInitializeEvent;
import api.listener.events.player.PlayerJoinWorldEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.mod.config.PersistentObjectUtil;
import thederpgamer.wanderingtrader.data.PlayerData;
import thederpgamer.wanderingtrader.manager.ConfigManager;
import thederpgamer.wanderingtrader.manager.TraderManager;
import thederpgamer.wanderingtrader.server.WanderingTraderCommand;
import thederpgamer.wanderingtrader.util.DataUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class WanderingTrader extends StarMod {

	//Instance
	private static WanderingTrader instance;
	public WanderingTrader() {

	}
	public static WanderingTrader getInstance() {
		return instance;
	}
	public static void main(String[] args) {
	}

	//Data
	public static Logger log;

	@Override
	public void onEnable() {
		instance = this;
		ConfigManager.initialize(this);
		initLogger();

		registerListeners();
		registerCommands();
	}

	@Override
	public void onServerCreated(ServerInitializeEvent serverInitializeEvent) {
		TraderManager.initialize();
	}
	private void initLogger() {
		String logFolderPath = DataUtils.getWorldDataPath() + "/logs";
		File logsFolder = new File(logFolderPath);
		if(!logsFolder.exists()) logsFolder.mkdirs();
		else {
			if(logsFolder.listFiles() != null && logsFolder.listFiles().length > 0) {
				File[] logFiles = new File[logsFolder.listFiles().length];
				int j = logFiles.length - 1;
				for(int i = 0; i < logFiles.length && j >= 0; i++) {
					logFiles[i] = logsFolder.listFiles()[j];
					j--;
				}

				for(File logFile : logFiles) {
					String fileName = logFile.getName().replace(".txt", "");
					int logNumber = Integer.parseInt(fileName.substring(fileName.indexOf("log") + 3)) + 1;
					String newName = logFolderPath + "/log" + logNumber + ".txt";
					if(logNumber < ConfigManager.getMainConfig().getInt("max-world-logs") - 1) logFile.renameTo(new File(newName));
					else logFile.delete();
				}
			}
		}
		try {
			File newLogFile = new File(logFolderPath + "/log0.txt");
			if(newLogFile.exists()) newLogFile.delete();
			newLogFile.createNewFile();
			log = Logger.getLogger(newLogFile.getPath());
			FileHandler handler = new FileHandler(newLogFile.getPath());
			log.addHandler(handler);
			SimpleFormatter formatter = new SimpleFormatter();
			handler.setFormatter(formatter);
		} catch(IOException exception) {
			exception.printStackTrace();
		}
	}

	private void registerListeners() {
		StarLoader.registerListener(PlayerJoinWorldEvent.class, new Listener<PlayerJoinWorldEvent>() {
			@Override
			public void onEvent(PlayerJoinWorldEvent event) {
				HashMap<String, PlayerData> dataMap = TraderManager.loadPlayerDataMap();
				if(!dataMap.containsKey(event.getPlayerName())) {
					PlayerData playerData = new PlayerData(event.getPlayerName());
					//dataMap.put(playerData.getPlayerName(), playerData); Don't bother putting into map, just save it to persistent storage
					PersistentObjectUtil.addObject(WanderingTrader.instance.getSkeleton(), playerData);
					PersistentObjectUtil.save(WanderingTrader.instance.getSkeleton());
				}
			}
		}, this);
	}

	private void registerCommands() {
		StarLoader.registerCommand(new WanderingTraderCommand());
	}
}
