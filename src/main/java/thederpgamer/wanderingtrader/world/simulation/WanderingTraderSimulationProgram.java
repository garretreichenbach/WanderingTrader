package thederpgamer.wanderingtrader.world.simulation;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.server.ai.program.common.TargetProgram;
import org.schema.game.server.ai.program.simpirates.SimulationProgramInterface;
import org.schema.game.server.data.simulation.SimPrograms;
import org.schema.game.server.data.simulation.groups.SimulationGroup;
import org.schema.schine.ai.stateMachines.AIConfiguationElementsInterface;
import org.schema.schine.ai.stateMachines.FSMException;
import org.schema.schine.ai.stateMachines.FiniteStateMachine;

import java.util.HashMap;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class WanderingTraderSimulationProgram  extends TargetProgram<SimulationGroup> implements SimulationProgramInterface {

	public WanderingTraderSimulationProgram(SimulationGroup simulationGroup, Vector3i target) {
		super(simulationGroup, false);
		setSectorTarget(target);
	}

	@Override
	public SimPrograms getProgram() {
		return SimPrograms.VISIT_SECTOR;
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
		machines.put(getStartMachine(), new WanderingTraderSimulationMachine(getEntityState(), this));
	}
}
