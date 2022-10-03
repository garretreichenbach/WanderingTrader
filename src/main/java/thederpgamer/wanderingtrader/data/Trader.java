package thederpgamer.wanderingtrader.data;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.fleet.Fleet;
import thederpgamer.wanderingtrader.manager.ConfigManager;

/**
 * Thread that handles the Wandering Trader.
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class Trader extends Thread {

	public enum TraderStatus {
		IDLE,
		MOVING,
		TRADING,
		DEAD
	}

	private boolean traderActive = false;
	private TraderStatus traderStatus = TraderStatus.IDLE;
	private long lastTraderAction;
	private Fleet traderFleet;

	public Trader() {
		super("Wandering Trader");
		initTrader();
	}

	@Override
	public void run() {
		super.run();
		update();
	}

	public void initTrader() {
		//Generate in a random unloaded sector near 2,2,2
		int xOffset = 2;
		Vector3i sector = new Vector3i();

		traderFleet = new Fleet();
		traderFleet.addMemberFromEntity();
		lastTraderAction = System.currentTimeMillis();
	}

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
		switch(traderStatus) {
			case IDLE: //When the trader is idling near a player, waiting for interaction.
				if(System.currentTimeMillis() - lastTraderAction >= ConfigManager.getMainConfig().getConfigurableLong("trader-idle-timeout", 15000)) {
					//If the trader has been idling for too long, move on to the next player.
					traderStatus = TraderStatus.MOVING;
					lastTraderAction = System.currentTimeMillis();

				}
				break;
			case MOVING: //When the trader is moving to a player's sector.
				break;
			case TRADING: //When the player is actively trading with the trader (not just near the trader).
				break;
			case DEAD: //When the trader is dead and needs respawning.
				break;
		}
	}

	public boolean isTraderActive() {
		return traderActive;
	}

	public void spawnTrader() {
		//Spawn trader and pick a random player (that isn't hostile) to visit.

		traderActive = true;
	}
}
