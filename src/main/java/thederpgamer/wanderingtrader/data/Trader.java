package thederpgamer.wanderingtrader.data;

import api.mod.config.PersistentObjectUtil;
import api.utils.game.PlayerUtils;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.catalog.CatalogPermission;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.server.controller.EntityNotFountException;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.simulation.SimulationManager;
import org.schema.game.server.data.simulation.groups.ShipSimulationGroup;
import org.schema.game.server.data.simulation.jobs.SimulationJob;
import thederpgamer.wanderingtrader.WanderingTrader;
import thederpgamer.wanderingtrader.manager.ConfigManager;
import thederpgamer.wanderingtrader.world.simulation.WanderingTraderSimulationGroup;
import thederpgamer.wanderingtrader.world.simulation.WanderingTraderSimulationProgram;

import java.sql.SQLException;

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
	private long lastTraderAction;
	private PlayerData targetPlayer;
	private ShipSimulationGroup simulationGroup;
	private String traderName;
	private long lastUpdate;

	public Trader() {
		super("Wandering Trader");
		initTrader();
	}

	@Override
	public void run() {
		super.run();
		update();
	}

	public void initTrader(final Vector3i... params) {
		final GameServerState gameServerState = GameServerState.instance;
		lastTraderAction = System.currentTimeMillis();
		targetPlayer = getNextPlayerTarget();
		if(simulationGroup != null) simulationGroup.deleteMembers();
		if(targetPlayer != null) {
			final SimulationJob simJob = new SimulationJob() {
				@Override
				public void execute(SimulationManager simMan) {
					Vector3i spawnPos;
					Vector3i targetPos;
					if(params != null && params.length == 2) {
						spawnPos = params[0];
						targetPos = params[1];
					} else {
						spawnPos = simMan.getUnloadedSectorAround(new Vector3i(2, 2, 2), new Vector3i());
						targetPos = targetPlayer.getSector();
					}
					simulationGroup = new WanderingTraderSimulationGroup(gameServerState);
					simMan.addGroup(simulationGroup);
					int factionID = FactionManager.TRAIDING_GUILD_ID;
					CatalogPermission[] temp = simMan.getBlueprintList(3,3, factionID); //Count, Level, Faction
					//Todo: Dynamic level system that increases when players are hostile to the trader and slowly decreases over time if the trader is not attacked.
					//CatalogPermission[] bps = new CatalogPermission[temp.length + 1];
					//for(int i = 0; i < temp.length; i++) bps[i] = temp[i];
					//temp[temp.length - 1] = getTraderBP();
					//simulationGroup.createFromBlueprints(pos, simMan.getUniqueGroupUId(), factionID, bps);
					simulationGroup.createFromBlueprints(spawnPos, simMan.getUniqueGroupUId(), factionID, temp);
					traderName = simulationGroup.getMembers().get(0); //Todo: Make sure this corresponds to the main ship
					simulationGroup.setCurrentProgram(new WanderingTraderSimulationProgram(simulationGroup, targetPos));
				}
			};
			gameServerState.getSimulationManager().addJob(simJob);
		}
	}

	private PlayerData getNextPlayerTarget() {
		PlayerData playerData = null;
		for(Object obj : PersistentObjectUtil.getObjects(WanderingTrader.getInstance().getSkeleton(), PlayerData.class)) {
			PlayerData data = (PlayerData) obj;
			if(data.getSector() != null) {
				if(playerData == null) playerData = data;
				else if(data.getLastVisitTime() < playerData.getLastVisitTime()) playerData = data;
			}
			//Todo: Prioritize players who haven't been hostile in a long time
		}
		return playerData;
	}

	/*
	private CatalogPermission getTraderBP() {

	}
	 */

	/*
	private void spawnAdvancedPirates(PlayerState p) {
		final Vector3i spawnPos = new Vector3i(p.getCurrentSector());
		final GameServerState state = GameServerState.instance;
		final String targetUID = p.getFirstControlledTransformableWOExc().getUniqueIdentifier();
		//create a job for the simulationManager to execute
		final SimulationJob simJob = new SimulationJob() {
			@Override
			public void execute(SimulationManager simMan) {
				Vector3i unloadedPos = simMan.getUnloadedSectorAround(spawnPos,new Vector3i());
				//create group
				ShipSimulationGroup myGroup = new AttackSingleEntitySimulationGroup(state,unloadedPos, targetUID);
				simMan.addGroup(myGroup);
				//spawn members
				int factionID = -1;
				CatalogPermission[] bps = simMan.getBlueprintList(3,1,factionID);
				if (bps.length == 0) {
					new NullPointerException("no blueprints avaialbe for faction " + factionID).printStackTrace();
					return;
				}
				myGroup.createFromBlueprints(unloadedPos,simMan.getUniqueGroupUId(),factionID,bps); //seems to try and work but doesnt spawn stuff?
				//add program to group
				PirateSimulationProgram myProgram = new PirateSimulationProgram(myGroup, false);
				myGroup.setCurrentProgram(myProgram);
			}
		};
		state.getSimulationManager().addJob(simJob); //adds job, is synchronized.
	}
	 */


	/**
	 * Updates the trader's status. If the targeted player is no longer on server, picks a new player.
	 * <p>If the targeted player is still on server, checks if the trader is within range of the player. If they are, stops the move command and notifies the player of their arrival.</p>
	 * <p>If the trader has sat near a player for a while (configurable), and the player has not attempted to trade, will move on to the next player.</p>
	 */
	public void update() {
		if(System.currentTimeMillis() - lastUpdate >= 15000) {
			switch(traderStatus) {
				case IDLE: //When the trader is idling near a player, waiting for interaction.
					if(System.currentTimeMillis() - lastTraderAction >= ConfigManager.getMainConfig().getConfigurableLong("trader-idle-timeout", 900000) || !isTargetPlayerOnline()) {
						//If the trader has been idling for too long, move on to the next player.
						traderStatus = TraderStatus.MOVING;
						lastTraderAction = System.currentTimeMillis();
						initTrader();
						WanderingTrader.log.info("Trader timed out, moving on to next player.");
						messagePlayer("The wandering trader has left your sector.");
					}
					break;
				case MOVING: //When the trader is moving to a player's sector.
					if(!isTargetPlayerOnline()) {
						//If the player is no longer online, move on to the next player.
						traderStatus = TraderStatus.MOVING;
						lastTraderAction = System.currentTimeMillis();
						initTrader();
						WanderingTrader.log.info("Trader's target player is no longer online, moving on to next player.");
					} else if(isTraderNearPlayer()) {
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
			lastUpdate = System.currentTimeMillis();
		}
	}

	private void messagePlayer(String message) {
		if(targetPlayer != null && targetPlayer.getPlayerState() != null) PlayerUtils.sendMessage(targetPlayer.getPlayerState(), message);
	}

	private boolean isTargetPlayerOnline() {
		return targetPlayer != null && targetPlayer.getSector() != null;
	}

	private boolean isTraderNearPlayer() {
		return targetPlayer != null && targetPlayer.getSector() != null && targetPlayer.getSector().equals(getTraderSector());
	}

	private Vector3i getTraderSector() {
		try {
			return simulationGroup.getSector(traderName, new Vector3i());
		} catch(EntityNotFountException | SQLException exception) {
			traderStatus = TraderStatus.DEAD;
			return new Vector3i();
		}
	}

	public void despawn() {
		simulationGroup.deleteMembers();
	}

	public void moveTo(Vector3i sector) {
		if(simulationGroup == null) initTrader(sector, sector);
		else ((WanderingTraderSimulationProgram) simulationGroup.getCurrentProgram()).setSectorTarget(sector);
	}

	public String getStatus() {
		if(simulationGroup != null) return "Current Sector: " + getTraderSector().toString() + " | Target Sector: " + ((WanderingTraderSimulationProgram) simulationGroup.getCurrentProgram()).getSectorTarget().toString() + " | Status: " + traderStatus.toString() + "\nDetails: " + simulationGroup.getCurrentProgram().getEntityState().toString();
		else return "Trader is not active.";
	}
}
