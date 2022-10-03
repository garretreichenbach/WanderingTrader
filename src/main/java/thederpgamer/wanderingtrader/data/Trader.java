package thederpgamer.wanderingtrader.data;

import api.common.GameCommon;
import api.common.GameServer;
import api.mod.config.PersistentObjectUtil;
import api.utils.game.PlayerUtils;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.ShopInterface;
import org.schema.game.common.data.player.catalog.CatalogPermission;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.network.objects.TradePrice;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.simulation.SimulationManager;
import org.schema.game.server.data.simulation.jobs.SimulationJob;
import thederpgamer.wanderingtrader.WanderingTrader;
import thederpgamer.wanderingtrader.manager.ConfigManager;
import thederpgamer.wanderingtrader.manager.TraderManager;
import thederpgamer.wanderingtrader.world.simulation.WanderingTraderSimulationGroup;
import thederpgamer.wanderingtrader.world.simulation.WanderingTraderSimulationProgram;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Thread that handles the Wandering Trader.
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class Trader extends Thread {

	public enum TraderStatus {
		IDLE,
		MOVING,
		DEAD
	}

	private TraderStatus traderStatus = TraderStatus.DEAD;
	public long lastTraderAction;
	private PlayerData targetPlayer;
	private WanderingTraderSimulationGroup simulationGroup;

	public Trader() {
		super("Wandering Trader");
	}

	@Override
	public void run() {
		while(true) {
			update();
			try {
				Thread.sleep(10000);
			} catch(InterruptedException exception) {
				exception.printStackTrace();
			}
		}
	}

	public void initTrader(final Vector3i... params) {
		final GameServerState gameServerState = GameServerState.instance;
		lastTraderAction = System.currentTimeMillis();
		targetPlayer = getNextPlayerTarget();
		if(GameCommon.getGameState() == null) return;
		if (simulationGroup != null) simulationGroup.deleteMembers();
		if (targetPlayer != null) {
			final SimulationJob simJob = new SimulationJob() {
				@Override
				public void execute(SimulationManager simMan) {
					Vector3i spawnPos;
					Vector3i targetPos;
					if (params != null && params.length == 2) {
						spawnPos = params[0];
						targetPos = params[1];
					} else {
						spawnPos = simMan.getUnloadedSectorAround(targetPlayer.getSector(), new Vector3i());
						targetPos = targetPlayer.getSector();
					}
					simulationGroup = new WanderingTraderSimulationGroup(gameServerState);
					simMan.addGroup(simulationGroup);
					int factionID = FactionManager.NPC_FACTION_START;
					//CatalogPermission[] temp = simMan.getBlueprintList(3, 3, factionID); //Count, Level, Faction
					CatalogPermission[] temp = getBlueprints(3);
					//Todo: Dynamic level system that increases when players are hostile to the trader and slowly decreases over time if the trader is not attacked.
					//CatalogPermission[] bps = new CatalogPermission[temp.length + 1];
					//for(int i = 0; i < temp.length; i++) bps[i] = temp[i];
					//temp[temp.length - 1] = getTraderBP();
					//simulationGroup.createFromBlueprints(pos, simMan.getUniqueGroupUId(), factionID, bps);
					simulationGroup.createFromBlueprints(spawnPos, simMan.getUniqueGroupUId(), factionID, temp);
					simulationGroup.setCurrentProgram(new WanderingTraderSimulationProgram(simulationGroup, targetPos));
				}
			};
			gameServerState.getSimulationManager().addJob(simJob);
			loadTrades();
		}
	}

	private SegmentController getTraderEntity() {
		return GameServer.getServerState().getSegmentControllersByName().get(simulationGroup.getMembers().get(simulationGroup.getMembers().size() - 1));
	}

	private void loadTrades() {
		try {
			ShopInterface shopInterface = (ShopInterface) getTraderEntity();
			shopInterface.modCredits(100000000);
			for(TradeItemData tradeItemData : TraderManager.pool.keySet()) {
				shopInterface.getShopInventory().putNextFreeSlotWithoutException(tradeItemData.getItemId(), tradeItemData.getAmount(), 0);
				((TradePrice) shopInterface.getPrice(tradeItemData.getItemId(), false)).setPrice((int) tradeItemData.getPrice());
			}
		} catch(Exception exception) {
			exception.printStackTrace();
			WanderingTrader.log.warning("Trader ship MUST have a shop module on it!");
		}
	}

	private PlayerData getNextPlayerTarget() {
		PlayerData playerData = null;
		for (Object obj : PersistentObjectUtil.getObjects(WanderingTrader.getInstance().getSkeleton(), PlayerData.class)) {
			PlayerData data = (PlayerData) obj;
			if (data.getSector() != null) {
				if (playerData == null) playerData = data;
				else if (data.getLastVisitTime() < playerData.getLastVisitTime()) playerData = data;
			}
			//Todo: Prioritize players who haven't been hostile in a long time
		}
		return playerData;
	}

	private CatalogPermission[] getBlueprints(int count) {
		ArrayList<CatalogPermission> bps = new ArrayList<>();
		int i = 0;
		for(CatalogPermission permission : Objects.requireNonNull(GameCommon.getGameState()).getCatalogManager().getCatalog()) {
			if(i >= count) break;
			for(String name : ConfigManager.getMainConfig().getList("trader-escort-bps")) {
				if(permission.getUid().contains(name)) {
					bps.add(permission);
					i ++;
					break;
				}
			}
		}
		bps.add(getTraderBP());
		return bps.toArray(new CatalogPermission[0]);
	}

	private CatalogPermission getTraderBP() {
		for(CatalogPermission permission : Objects.requireNonNull(GameCommon.getGameState()).getCatalogManager().getCatalog()) {
			if(permission.getUid().contains(ConfigManager.getMainConfig().getConfigurableValue("trader-main-bp", "Wandering Trader"))) return permission;
		}
		return null;
	}

	/**
	 * Updates the trader's status. If the targeted player is no longer on server, picks a new player.
	 * <p>If the targeted player is still on server, checks if the trader is within range of the player. If they are, stops the move command and notifies the player of their arrival.</p>
	 * <p>If the trader has sat near a player for a while (configurable), and the player has not attempted to trade, will move on to the next player.</p>
	 */
	public void update() {
		switch (traderStatus) {
			case IDLE: //When the trader is idling near a player, waiting for interaction.
				if (System.currentTimeMillis() - lastTraderAction >= (ConfigManager.getMainConfig().getConfigurableLong("trader-idle-timeout-minutes", 15) * 60000) || !isTargetPlayerOnline()) {
					//If the trader has been idling for too long, move on to the next player.
					traderStatus = TraderStatus.MOVING;
					lastTraderAction = System.currentTimeMillis();
					initTrader();
					WanderingTrader.log.info("Trader timed out, moving on to next player.");
					messagePlayer("The wandering trader has left your sector.");
				}
				break;
			case MOVING: //When the trader is moving to a player's sector.
				if (!isTargetPlayerOnline()) {
					//If the player is no longer online, move on to the next player.
					traderStatus = TraderStatus.MOVING;
					lastTraderAction = System.currentTimeMillis();
					initTrader();
					WanderingTrader.log.info("Trader's target player is no longer online, moving on to next player.");
				} else if (isTraderNearPlayer()) {
					//If the trader is near the player, stop moving and notify the player.
					targetPlayer.setLastVisitTime(System.currentTimeMillis());
					traderStatus = TraderStatus.IDLE;
					lastTraderAction = System.currentTimeMillis();
					WanderingTrader.log.info("Trader has arrived near player " + targetPlayer.getPlayerName() + " at in sector " + targetPlayer.getSector().toString());
					messagePlayer("A wandering trader has arrived in your sector.");
					PersistentObjectUtil.removeObject(WanderingTrader.getInstance().getSkeleton(), targetPlayer);
					PersistentObjectUtil.addObject(WanderingTrader.getInstance().getSkeleton(), targetPlayer);
					PersistentObjectUtil.save(WanderingTrader.getInstance().getSkeleton());
				}
				break;
			case DEAD: //When the trader is dead and needs respawning.
				initTrader();
				traderStatus = TraderStatus.MOVING;
				lastTraderAction = System.currentTimeMillis();
				WanderingTrader.log.info("Trader has respawned.");
				break;
		}
		long lastUpdate = System.currentTimeMillis();
	}

	private void messagePlayer(String message) {
		if (targetPlayer != null && targetPlayer.getPlayerState() != null)
			PlayerUtils.sendMessage(targetPlayer.getPlayerState(), message);
	}

	private boolean isTargetPlayerOnline() {
		return targetPlayer != null && targetPlayer.getSector() != null;
	}

	private boolean isTraderNearPlayer() {
		return targetPlayer != null && targetPlayer.getSector() != null && targetPlayer.getSector().equals(getTraderSector());
	}

	private Vector3i getTraderSector() {
		if(getTraderEntity() != null) return getTraderEntity().getSector(new Vector3i());
		else return new Vector3i();
	}

	public void despawn() {
		simulationGroup.deleteMembers();
	}

	public void moveTo(Vector3i sector) {
		if (simulationGroup == null) initTrader(sector, sector);
		else ((WanderingTraderSimulationProgram) simulationGroup.getCurrentProgram()).setSectorTarget(sector);
	}

	public String getStatus() {
		if(simulationGroup != null) return "Current Sector: " + getTraderSector() + "Target Sector: " + ((WanderingTraderSimulationProgram) simulationGroup.getCurrentProgram()).getSectorTarget().toString() + " | Status: " + traderStatus.toString() + "\nDetails: " + simulationGroup.getCurrentProgram().getEntityState().toString();
		else return "Trader is not active.";
	}

	public boolean isDead() {
		return traderStatus == TraderStatus.DEAD;
	}
}
