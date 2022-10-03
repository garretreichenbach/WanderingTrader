package thederpgamer.wanderingtrader.server;

import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.wanderingtrader.WanderingTrader;
import thederpgamer.wanderingtrader.manager.TraderManager;

import javax.annotation.Nullable;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class WanderingTraderCommand implements CommandInterface {
	@Override
	public String getCommand() {
		return "wandering_trader";
	}

	@Override
	public String[] getAliases() {
		return new String[] {"wt", "wandering_trader"};
	}

	@Override
	public String getDescription() {
		return "Wandering Trader Management Command\n" +
				"/%COMMAND% spawn [x] [y] [z] - Spawns a wandering trader at the specified coordinates. If none are specified, spawns at the senders' location.\n" +
				"/%COMMAND% despawn - Despawns the current wandering trader.\n" +
				"/%COMMAND% move <x> <y> <z> - Makes the wandering trader move to specified coordinates.\n" +
				"/%COMMAND% clear_aggression [player_name|all/*] - Removes any aggression score from the specified player, or from the sender if a name isn't specified.\n" +
				"/%COMMAND% status - Prints debug status info.";
	} //Todo: Command to view and edit current deals

	@Override
	public boolean isAdminOnly() {
		return true;
	}

	@Override
	public boolean onCommand(PlayerState sender, String[] args) {
		try {
			switch(args[0].toLowerCase()) {
				case "spawn":
					if(args.length == 1) TraderManager.getTrader().initTrader(sender.getCurrentSector(), sender.getCurrentSector());
					else if(args.length == 4) {
						int x = Integer.parseInt(args[1]);
						int y = Integer.parseInt(args[2]);
						int z = Integer.parseInt(args[3]);
						TraderManager.getTrader().initTrader(new Vector3i(x, y, z), new Vector3i(x, y, z));
					} else return false;
					break;
				case "despawn":
					TraderManager.getTrader().despawn();
					break;
				case "move":
					if(args.length == 4) {
						int x = Integer.parseInt(args[1]);
						int y = Integer.parseInt(args[2]);
						int z = Integer.parseInt(args[3]);
						TraderManager.getTrader().moveTo(new Vector3i(x, y, z));
					} else return false;
					break;
				case "clear_aggression":
					if(args.length == 1) TraderManager.clearAggression(sender.getName());
					else if(args.length == 2) {
						if(args[1].equalsIgnoreCase("all") || args[1].equalsIgnoreCase("*")) TraderManager.clearAggression(null);
						else TraderManager.clearAggression(args[1]);
					} else return false;
					break;
				case "status":
					String status = TraderManager.getTrader().getStatus();
					PlayerUtils.sendMessage(sender, "Wandering Trader Status: \n" + status);
					WanderingTrader.log.info("Wandering Trader Status: \n" + status);
					break;
			}
		} catch(Exception ignored) {
			return false;
		}
		return true;
	}

	@Override
	public void serverAction(@Nullable PlayerState sender, String[] args) {

	}

	@Override
	public StarMod getMod() {
		return WanderingTrader.getInstance();
	}
}
