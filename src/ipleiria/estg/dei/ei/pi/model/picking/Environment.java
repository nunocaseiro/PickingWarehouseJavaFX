package ipleiria.estg.dei.ei.pi.model.picking;

import com.google.gson.JsonObject;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.GAProblem;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.GeneticAlgorithm;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.Individual;
import ipleiria.estg.dei.ei.pi.utils.exceptions.InvalidNodeException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Environment<I extends Individual<? extends GAProblem>> {

    private PickingGraph graph;
    private I bestInRun;
    private HashMap<String, List<Node>> pairsMap;
    private Boolean pauseGA;
    private JsonObject jsonLayout;
    private ArrayList<EnvironmentListener> environmentListeners;
    private GeneticAlgorithm<PickingIndividual, PickingGAProblem> geneticAlgorithm;

    public Environment() {
        this.graph = new PickingGraph();
        this.environmentListeners = new ArrayList<>();
        this.jsonLayout = null;
    }

    public void setGeneticAlgorithm(GeneticAlgorithm<PickingIndividual, PickingGAProblem> geneticAlgorithm) {
        this.geneticAlgorithm = geneticAlgorithm;
    }



    public PickingGraph getGraph() {
        return graph;
    }

    public JsonObject getJsonLayout() {
        return jsonLayout;
    }

    public I getBestInRun() {
        return bestInRun;
    }

    public void setBestInRun(I bestInRun) {
        this.bestInRun = bestInRun;
    }

    public void loadWarehouseFile(JsonObject jsonLayout) {
        this.jsonLayout = jsonLayout;
    }

    public void loadGraph(JsonObject jsonPicks) throws InvalidNodeException {
        this.graph.createGraphFromFile(this.jsonLayout, jsonPicks);
        fireCreateEnvironment();
        fireCreateSimulationPicks();
    }

    public synchronized void addEnvironmentListener(EnvironmentListener l) {
        if (!environmentListeners.contains(l)) {
            environmentListeners.add(l);
        }
    }

    public void fireUpdateEnvironment() {
        for (EnvironmentListener listener : environmentListeners) {
            listener.updateEnvironment();
        }
    }

    public void fireCreateEnvironment() {
        for (EnvironmentListener listener : environmentListeners) {
            listener.createEnvironment(graph.getDecisionNodes(),graph.getEdges(),graph.getAgents(),graph.getOffloadArea(),graph.getMaxLine(),graph.getMaxColumn());
        }
    }

    /*public void fireCreateSimulation() {
        for (EnvironmentListener listener : listeners) {
            listener.createSimulation();
        }
    }*/

    public void fireCreateSimulationPicks() {
        for (EnvironmentListener listener : environmentListeners) {
            listener.createSimulationPicks(graph.getPicks());
        }
    }


    public void stopGA() {
        this.geneticAlgorithm.setStopped(true);
    }
}
