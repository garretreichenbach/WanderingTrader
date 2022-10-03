package thederpgamer.wanderingtrader.data;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.PlayerNotFountException;

/**
 * Data storage class that is used to store player data pertaining to the Wandering Trader.
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class PlayerData {

	private String playerName;
	private long lastVisitTime;
	private long lastTransgression;

	/**
	 * Constructor for creating new data.
	 *
	 * @param playerName
	 */
	public PlayerData(String playerName) {
		this.playerName = playerName;
		this.lastVisitTime = -1;
		this.lastTransgression = -1;
	}

	public String getPlayerName() {
		return playerName;
	}

	public long getLastVisitTime() {
		return lastVisitTime;
	}

	public void setLastVisitTime(long lastVisitTime) {
		this.lastVisitTime = lastVisitTime;
	}

	public long getLastTransgression() {
		return lastTransgression;
	}

	public void setLastTransgression(long lastTransgression) {
		this.lastTransgression = lastTransgression;
	}

	public Vector3i getSector() {
		try {
			return GameServerState.instance.getPlayerFromName(playerName).getCurrentSector();
		} catch(PlayerNotFountException exception) {
			return null;
		}
	}

	public PlayerState getPlayerState() {
		try {
			return GameServerState.instance.getPlayerFromName(playerName);
		} catch(PlayerNotFountException exception) {
			return null;
		}
	}
}