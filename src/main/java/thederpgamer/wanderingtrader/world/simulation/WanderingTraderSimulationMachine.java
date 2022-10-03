package thederpgamer.wanderingtrader.world.simulation;

import org.schema.game.server.ai.program.common.states.WaitingTimed;
import org.schema.game.server.ai.program.simpirates.states.*;
import org.schema.schine.ai.AiEntityStateInterface;
import org.schema.schine.ai.stateMachines.FiniteStateMachine;
import org.schema.schine.ai.stateMachines.Message;
import org.schema.schine.ai.stateMachines.State;
import org.schema.schine.ai.stateMachines.Transition;
import thederpgamer.wanderingtrader.WanderingTrader;
import thederpgamer.wanderingtrader.manager.ConfigManager;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class WanderingTraderSimulationMachine extends FiniteStateMachine<String> {

	public WanderingTraderSimulationMachine(AiEntityStateInterface aiEntityStateInterface, WanderingTraderSimulationProgram program) {
		super(aiEntityStateInterface, program, "");
	}

	@Override
	public void createFSM(String parameter) {
		Transition t_moveToSector = Transition.MOVE_TO_SECTOR;
		Transition t_restart = Transition.RESTART;
		Transition t_plan = Transition.PLAN;
		Transition t_waitCompleted = Transition.WAIT_COMPLETED;
		Transition t_targetSectorReached = Transition.TARGET_SECTOR_REACHED;

		AiEntityStateInterface gObj = getObj();

		Starting starting = new Starting(gObj);
		MovingToSector movingToSectorPlayer = new MovingToSector(gObj);
		MovingToSector movingToSectorHome = new MovingToSector(gObj);
		CheckingForPlayers checkingForPlayers = new CheckingForPlayers(gObj);
		ReturningHome returningHome = new ReturningHome(gObj);
		GoToRandomSector goToRandomSector = new GoToRandomSector(gObj);
		int timeoutSeconds = (int) (ConfigManager.getMainConfig().getConfigurableLong("trader-idle-timeout", 15000) / 1000);
		WaitingTimed waitingInTargetSector = new WaitingTimed(gObj, timeoutSeconds);

		starting.addTransition(t_restart, starting);
		starting.addTransition(t_plan, checkingForPlayers);

		checkingForPlayers.addTransition(t_restart, starting);
		checkingForPlayers.addTransition(t_moveToSector, movingToSectorPlayer);

		movingToSectorPlayer.addTransition(t_restart, starting);
		movingToSectorPlayer.addTransition(t_targetSectorReached, waitingInTargetSector);

		waitingInTargetSector.addTransition(t_restart, starting);
		waitingInTargetSector.addTransition(t_waitCompleted, returningHome);

		returningHome.addTransition(t_restart, starting);
		returningHome.addTransition(t_moveToSector, movingToSectorHome);

		movingToSectorHome.addTransition(t_restart, starting);
		movingToSectorHome.addTransition(t_targetSectorReached, starting);

		goToRandomSector.addTransition(t_restart, starting);
		goToRandomSector.addTransition(t_moveToSector, movingToSectorHome);

		setStartingState(starting);
	}

	@Override
	public void onMsg(Message message) {
		if(ConfigManager.getMainConfig().getConfigurableBoolean("debug-mode", false)) WanderingTrader.log.info("WanderingTraderSimulationMachine.onMsg(): " + message);
	}

	public void addTransition(State from, Transition t, State to) {
		from.addTransition(t, to);
	}
}
