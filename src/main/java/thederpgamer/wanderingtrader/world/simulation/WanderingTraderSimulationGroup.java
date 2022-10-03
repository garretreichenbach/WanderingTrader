package thederpgamer.wanderingtrader.world.simulation;

import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.simulation.groups.ShipSimulationGroup;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class WanderingTraderSimulationGroup extends ShipSimulationGroup {

	public WanderingTraderSimulationGroup(GameServerState gameServerState) {
		super(gameServerState);
	}

	@Override
	public GroupType getType() {
		return GroupType.TARGET_SECTOR;
	}
}
