package thederpgamer.wanderingtrader.world.simulation;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.element.Element;
import org.schema.game.common.data.world.Universe;
import org.schema.game.server.ai.program.common.states.WaitingTimed;
import org.schema.game.server.ai.program.simpirates.states.MovingToSector;
import org.schema.game.server.ai.program.simpirates.states.Starting;
import org.schema.game.server.controller.EntityNotFountException;
import org.schema.game.server.data.simulation.groups.SimulationGroup;
import org.schema.schine.ai.AiEntityStateInterface;
import org.schema.schine.ai.MachineProgram;
import org.schema.schine.ai.stateMachines.*;
import thederpgamer.wanderingtrader.WanderingTrader;
import thederpgamer.wanderingtrader.manager.ConfigManager;
import thederpgamer.wanderingtrader.manager.TraderManager;

import java.sql.SQLException;
import java.util.HashMap;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class WanderingTraderSimulationProgram extends MachineProgram<WanderingTraderSimulationGroup> {

	private final Vector3i targetPos;

	public WanderingTraderSimulationProgram(WanderingTraderSimulationGroup simulationGroup, Vector3i targetPos) {
		super(simulationGroup, false);
		this.targetPos = targetPos;
	}

	@Override
	public void onAISettingChanged(AIConfiguationElementsInterface aiConfiguationElementsInterface) throws FSMException {

	}

	@Override
	protected String getStartMachine() {
		return "PROGRAM";
	}

	@Override
	protected void initializeMachines(HashMap<String, FiniteStateMachine<?>> hashMap) {
		machines.put(getStartMachine(), new WanderingTraderSimulationMachine(getEntityState(), this, targetPos));
	}

	public void setSectorTarget(Vector3i sector) {
		targetPos.set(sector);
	}

	public Vector3i getSectorTarget() {
		return targetPos;
	}

	private static class WanderingTraderSimulationMachine extends FiniteStateMachine<Vector3i> {

		public WanderingTraderSimulationMachine(AiEntityStateInterface aiEntityStateInterface, MachineProgram<? extends AiEntityState> program, Vector3i sector) {
			super(aiEntityStateInterface, program, sector);
		}

		@Override
		public void createFSM(Vector3i vector3i) {
			Transition t_moveToSector = Transition.MOVE_TO_SECTOR;
			Transition t_waitCompleted = Transition.WAIT_COMPLETED;
			Transition t_targetSectorReached = Transition.TARGET_SECTOR_REACHED;

			AiEntityStateInterface gObj = getObj();

			WT_Starting starting = new WT_Starting(gObj);
			WT_MovingToSector movingToSector = new WT_MovingToSector(gObj);
			int timeoutSeconds = (int) (ConfigManager.getMainConfig().getConfigurableLong("trader-idle-timeout-minutes", 15) * 60);
			WT_WaitingTimed waitingInTargetSector = new WT_WaitingTimed(gObj, timeoutSeconds);

			starting.addTransition(t_moveToSector, movingToSector);
			movingToSector.addTransition(t_targetSectorReached, waitingInTargetSector);
			waitingInTargetSector.addTransition(t_waitCompleted, starting);

			setStartingState(starting);
		}

		@Override
		public void onMsg(Message message) {
			WanderingTrader.log.info("WanderingTraderSimulationMachine.onMsg(): " + message);
		}

		@Override
		public void update() throws FSMException {
			super.update();
			WanderingTrader.log.info("WanderingTraderSimulationMachine.update(): " + getMachineProgram().getEntityState());
		}
	}

	private static class WT_Starting extends Starting {

		public WT_Starting(AiEntityStateInterface gObj) {
			super(gObj);
		}

		@Override
		public boolean onUpdate() throws FSMException {
			stateTransition(Transition.MOVE_TO_SECTOR);
			return false;
		}
	}

	private static class WT_MovingToSector extends MovingToSector {

		private long lastMove;

		public WT_MovingToSector(AiEntityStateInterface gObj) {
			super(gObj);
		}

		@Override
		public boolean onUpdate() throws FSMException {
			WanderingTraderSimulationProgram p = (WanderingTraderSimulationProgram) getEntityState().getCurrentProgram();
			if(p.getSectorTarget() == null) {
				stateTransition(Transition.RESTART);
				return true;
			}

			Vector3i pos = new Vector3i();
			for (int i = 0; i < getSimGroup().getMembers().size(); i++) {
				try {
					getSimGroup().getSector(getSimGroup().getMembers().get(i), pos);
					if(pos.equals(p.getSectorTarget())) {
						stateTransition(Transition.TARGET_SECTOR_REACHED);
						return true;
					}
				} catch (EntityNotFountException | SQLException e) {
					e.printStackTrace();
				}
			}

			if (System.currentTimeMillis() - lastMove > SimulationGroup.SECTOR_SPEED_MS) {
				boolean existsGroupInSector = getSimGroup().getState().getSimulationManager().existsGroupInSector(p.getSectorTarget());
				boolean occ = false;
				for(int i = 0; i < getSimGroup().getMembers().size(); i++) {
					boolean noError;
					if(getSimGroup().getState().getUniverse().isSectorLoaded(p.getSectorTarget())) noError = getSimGroup().moveToTarget(getSimGroup().getMembers().get(i), p.getSectorTarget());
					else {
						if(existsGroupInSector) {
							occ = true;
							noError = true;
							Vector3i dir = Element.DIRECTIONSi[Universe.getRandom().nextInt(Element.DIRECTIONSi.length)];
							try {
								Vector3i secPos = getSimGroup().getSector(getSimGroup().getMembers().get(i), new Vector3i());
								secPos.add(dir);
								getSimGroup().moveToTarget(getSimGroup().getMembers().get(i), secPos);
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else {
							noError = getSimGroup().moveToTarget(getSimGroup().getMembers().get(i), p.getSectorTarget());
						}
					}
					if(!noError) {
						System.err.println("[SIMULATION] Exception while moving entity: REMOVING FROM MEMBERS: " + getSimGroup().getMembers().get(i));
						getSimGroup().getMembers().remove(i);
						i--;
					}
					lastMove = System.currentTimeMillis();
				}
				if(occ) System.err.println("[MOVING TO SECTOR] Position " + p.getSectorTarget() + " occupied for " + getSimGroup().getMembers());
			}
			return false;
		}
	}

	private static class WT_WaitingTimed extends WaitingTimed {
		public WT_WaitingTimed(AiEntityStateInterface gObj, int timeoutSeconds) {
			super(gObj, timeoutSeconds);
		}

		@Override
		public boolean onUpdate() throws FSMException {
			if(super.onUpdate() && TraderManager.getTrader().lastTraderAction >= (ConfigManager.getMainConfig().getConfigurableLong("trader-idle-timeout-minutes", 15) * 60000)) {
				stateTransition(Transition.RESTART);
				return true;
			}
			return false;
		}
	}
}
