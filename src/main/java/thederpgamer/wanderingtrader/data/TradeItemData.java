package thederpgamer.wanderingtrader.data;

import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class TradeItemData {

	private short itemId;
	private int amount;
	private long price;

	public TradeItemData(short itemId, int amount, long price) {
		this.itemId = itemId;
		this.amount = amount;
		this.price = price;
	}

	public short getItemId() {
		return itemId;
	}

	public int getAmount() {
		return amount;
	}

	public long getPrice() {
		return price;
	}

	public ElementInformation getInfo() {
		return ElementKeyMap.getInfo(itemId);
	}
}