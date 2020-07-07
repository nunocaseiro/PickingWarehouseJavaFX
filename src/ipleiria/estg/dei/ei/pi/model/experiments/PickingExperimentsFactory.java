package ipleiria.estg.dei.ei.pi.model.experiments;

import ipleiria.estg.dei.ei.pi.gui.ExperimentsFrameController;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.GAListener;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.GeneticAlgorithm;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.geneticOperators.mutation.Mutation;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.geneticOperators.mutation.MutationInsert;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.geneticOperators.mutation.MutationInversion;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.geneticOperators.mutation.MutationScramble;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.geneticOperators.recombination.*;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.selectionMethods.RankBased;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.selectionMethods.SelectionMethod;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.selectionMethods.Tournament;
import ipleiria.estg.dei.ei.pi.model.picking.PickingGAProblem;
import ipleiria.estg.dei.ei.pi.model.picking.PickingGraph;
import ipleiria.estg.dei.ei.pi.model.picking.PickingIndividual;
import ipleiria.estg.dei.ei.pi.model.picking.PickingManhattanDistance;
import ipleiria.estg.dei.ei.pi.model.search.AStarSearch;
import ipleiria.estg.dei.ei.pi.utils.CollisionsHandling;
import ipleiria.estg.dei.ei.pi.utils.WeightLimitation;

import java.util.ArrayList;
import java.util.Random;

public class PickingExperimentsFactory extends ExperimentsFactory {
    private int populationSize;
    private int maxGenerations;
    private SelectionMethod<PickingIndividual, PickingGAProblem> selection;
    private Recombination<PickingIndividual, PickingGAProblem> recombination;
    private Mutation<PickingIndividual, PickingGAProblem> mutation;
    private PickingGAProblem problem;
    private Experiment<PickingExperimentsFactory,PickingGAProblem> experiment;
    private CollisionsHandling collisionsHandling;
    private WeightLimitation weightLimitation;
    private int timeWeight;
    private int collisionsWeight;
    private int nrAgents;
    private int nrPicks;
    private PickingGraph graph;
    //TODO collisions handling, weight limitation, timeweight, collisions weight


    public PickingExperimentsFactory(ExperimentsFrameController experimentsFrameController) {
        super(experimentsFrameController);

    }

    @Override
    public GeneticAlgorithm generateGAInstance(int seed) {

        GeneticAlgorithm<PickingIndividual,PickingGAProblem> ga = new GeneticAlgorithm<>(new PickingIndividual.PickingIndividualFactory(),selection,recombination,mutation,populationSize,maxGenerations,new Random(seed));
        ga.addGAListener(experimentsController);
        for (ExperimentListener statistic : statistics) {
            ga.addGAListener((GAListener) statistic);
        }

        return ga;
    }

    @Override
    protected Experiment buildExperiment(PickingGraph pickingGraph) {
//        Environment.getInstance().generateRandomLayout();

        this.graph = pickingGraph;
        populationSize = Integer.parseInt(getParameterValue("Population size"));
        maxGenerations = Integer.parseInt(getParameterValue("Max generations"));
        numRuns = Integer.parseInt(getParameterValue("Runs"));
        nrAgents = Integer.parseInt(getParameterValue("NumAgents"));
        nrPicks = Integer.parseInt(getParameterValue("NumPicks"));

        if (getParameterValue("Selection").equals("Tournament")) {
            int tournamentSize = Integer.parseInt(getParameterValue("Tournament size"));
            selection = new Tournament<>(populationSize, tournamentSize);
        }else{
            double selectivePressure= Double.parseDouble(getParameterValue("Selective pressure"));
            selection = new RankBased<>(populationSize,selectivePressure);
        }

        //RECOMBINATION
        double recombinationProbability = Double.parseDouble(getParameterValue("Recombination probability"));
        switch (getParameterValue("Recombination")) {
            case "PMX":
                recombination = new RecombinationPartialMapped<>(recombinationProbability);
                break;
            case "OX1":
                recombination = new RecombinationOX1<>(recombinationProbability);
                break;
            case "OX":
                recombination = new RecombinationOX<>(recombinationProbability);
                break;
            case "CX":
                recombination = new RecombinationCX<>(recombinationProbability);
                break;
        }

        //MUTATION
        double mutationProbability = Double.parseDouble(getParameterValue("Mutation probability"));
        switch (getParameterValue("Mutation")) {
            case "Insert":
                mutation = new MutationInsert<>(mutationProbability);
                break;
            case "Scramble":
                mutation = new MutationScramble<>(mutationProbability);
                break;
            case "Inversion":
                mutation = new MutationInversion<>(mutationProbability);
                break;
        }

        collisionsHandling = CollisionsHandling.valueOf(getParameterValue("Collisions handling"));
        weightLimitation = WeightLimitation.valueOf(getParameterValue("Weight limitation"));
        timeWeight= Integer.parseInt(getParameterValue("Time weight"));
        collisionsWeight= Integer.parseInt(getParameterValue("Collisions weight"));

        problem = new PickingGAProblem(pickingGraph,new AStarSearch<>(new PickingManhattanDistance()), weightLimitation,collisionsHandling);

        String experimentTextualRepresentation = buildExperimentTextualRepresentation();
        String experimentHeader = buildExperimentHeader();
        String experimentConfigurationValues = buildExperimentValues();

        experiment = new Experiment<>(this, numRuns, problem, experimentTextualRepresentation, experimentHeader, experimentConfigurationValues);

        statistics = new ArrayList<>();

        for (String statisticName : parameters.get("Statistics").values) {
            ExperimentListener statistic = buildStatistic(statisticName, experimentHeader);
            statistics.add(statistic);
            experiment.addExperimentListener(statistic);
        }

        return experiment;
    }

    public void generateLayoutAndPicks(){

    }

    private ExperimentListener buildStatistic(
            String statisticName,
            String experimentHeader) {
        switch (statisticName) {
            case "StatisticBestAverage":
                return new StatisticBestAverage(numRuns, experimentHeader);
        }
        return null;
    }

    private String buildExperimentValues() {
        StringBuilder sb = new StringBuilder();
        sb.append(populationSize + "\t");
        sb.append(maxGenerations + "\t");
        sb.append(selection + "\t");
        sb.append(recombination + "\t");
        sb.append(recombination.getProbability() + "\t");
        sb.append(mutation + "\t");
        sb.append(mutation.getProbability() + "\t");
        sb.append(nrAgents + "\t");
        sb.append(nrPicks + "\t");
        return sb.toString();
    }


    private String buildExperimentHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append("Population size:" + "\t");
        sb.append("Max generations:" + "\t");
        sb.append("Selection:" + "\t");
        sb.append("Recombination:" + "\t");
        sb.append("Recombination prob.:" + "\t");
        sb.append("Mutation:" + "\t");
        sb.append("Mutation prob.:" + "\t");
        sb.append("Number agents" + "\t");
        sb.append("Number picks" + "\t");

        return sb.toString();
    }

    private String buildExperimentTextualRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("Population size:" + populationSize + "\r\n");
        sb.append("Max generations:" + maxGenerations + "\r\n");
        sb.append("Selection:" + selection + "\r\n");
        sb.append("Recombination:" + recombination + "\r\n");
        sb.append("Recombination prob.: " + recombination.getProbability() + "\r\n");
        sb.append("Mutation:" + mutation + "\r\n");
        sb.append("Mutation prob.: " + mutation.getProbability()+ "\r\n");
        sb.append("Number agents" + "\r\n");
        sb.append("Number picks" + "\r\n");
        return sb.toString();
    }
}
