package thederpgamer.wanderingtrader.data;

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
}