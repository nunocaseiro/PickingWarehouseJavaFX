package ipleiria.estg.dei.ei.pi.controller;

import com.google.gson.JsonParser;
import ipleiria.estg.dei.ei.pi.gui.MainFrameController;
import ipleiria.estg.dei.ei.pi.model.experiments.Experiment;
import ipleiria.estg.dei.ei.pi.model.experiments.ExperimentsFactory;
import ipleiria.estg.dei.ei.pi.model.experiments.PickingExperimentsFactory;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.GAProblem;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.GeneticAlgorithm;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.geneticOperators.mutation.Mutation;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.geneticOperators.mutation.MutationInsert;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.geneticOperators.mutation.MutationInversion;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.geneticOperators.mutation.MutationScramble;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.geneticOperators.recombination.*;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.selectionMethods.RankBased;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.selectionMethods.SelectionMethod;
import ipleiria.estg.dei.ei.pi.model.geneticAlgorithm.selectionMethods.Tournament;
import ipleiria.estg.dei.ei.pi.model.picking.*;
import ipleiria.estg.dei.ei.pi.model.search.AStarSearch;
import ipleiria.estg.dei.ei.pi.utils.JSONValidator;
import ipleiria.estg.dei.ei.pi.utils.exceptions.InvalidNodeException;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.everit.json.schema.ValidationException;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

public class Controller {

    private Environment<PickingIndividual> environment;
    private MainFrameController mainFrame;
    private SwingWorker<Void, Void> worker;
    private SwingWorker<Void, Void> workerExperiments;

    public Controller(MainFrameController mainFrameController) {
        this.environment = new Environment<>();
        this.mainFrame = mainFrameController;
        initController();
    }

    public void initController() {
        this.mainFrame.getLoadLayoutButton().setOnAction(e -> loadWarehouseLayout());
        this.mainFrame.getLoadPicksButton().setOnAction(e -> loadPicks());
        this.mainFrame.getRunGaButton().setOnAction(e -> runGA());
        this.mainFrame.getStopGAButton().setOnAction(e -> stopGa());
        this.mainFrame.getSimulationButton().setOnAction(e->simulate());
        this.mainFrame.getExperimentsFrameController().getRunExperimentsButton().setOnAction(e->runExperiments());
        this.mainFrame.getExperimentsFrameController().stopExperiments.setOnAction(e->stopExperiment());
        this.mainFrame.manageButtons(false,true,true,true,true,true,true);
    }

    public void stopGa(){
        worker.cancel(true);
        environment.stopGA();
    }

    private void stopExperiment(){
        workerExperiments.cancel(true);
        environment.getExperiment().setStopGa();
    }

    private void runExperiments() {
        if(mainFrame.getExperimentsFrameController().handleErrors().equals("success")) {
            FileChooser fileChooser = new FileChooser();
            File selectedFile = fileChooser.showOpenDialog(Window.getWindows().get(0));
            workerExperiments = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        mainFrame.getExperimentsFrameController().getProgressBar().setProgress(0);
                        mainFrame.getExperimentsFrameController().setRunsProgress(0);
                        if (selectedFile != null) {
                            ExperimentsFactory experimentsFactory = new PickingExperimentsFactory(mainFrame.getExperimentsFrameController(), JsonParser.parseReader(new FileReader(selectedFile.getAbsolutePath())).getAsJsonObject());
                            mainFrame.getExperimentsFrameController().setAllRuns(experimentsFactory.getCountAllRuns());
                            while (experimentsFactory.hasMoreExperiments() || !environment.getExperiment().isStopped()) {
                                environment.setExperiment(experimentsFactory.nextExperiment());
                                environment.getExperiment().run();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    super.done();
                    System.out.println("done experiments");
                }
            };
            workerExperiments.execute();
        }else{
            mainFrame.showAlert(mainFrame.getExperimentsFrameController().handleErrors());
        }
    }

    private void simulate() {
        worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            mainFrame.manageButtons(false,false,true,true,false,false,false);
                            mainFrame.getSimulationFrameController().start(environment.getBestInRun());
                            mainFrame.getSlider().setMax(mainFrame.getSimulationFrameController().getTimeline().getTotalDuration().toMillis());
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
                return null;

            }

            @Override
            protected void done() {
                super.done();
                mainFrame.manageButtons(false,false,false,true,false,false,false);

            }
        };

        worker.execute();
    }

    private void loadWarehouseLayout() {
        this.environment.addEnvironmentListener(this.mainFrame.getSimulationFrameController());
        try {
            FileChooser fileChooser = new FileChooser();
            File selectedFile = fileChooser.showOpenDialog(Window.getWindows().get(0));

            if(selectedFile != null) {
                JSONValidator.validateJSON(Files.readString(Path.of(selectedFile.getPath())), getClass().getResourceAsStream("/warehouseSchema.json"));

                this.environment.loadWarehouseFile(JsonParser.parseReader(new FileReader(selectedFile.getAbsolutePath())).getAsJsonObject());

                this.mainFrame.manageButtons(false,false,true,true,true,true,true);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ValidationException e) {
            mainFrame.showAlert("Incorrect warehouse file format");
        }
    }

    private void loadPicks() {
        try {
            if (this.environment.getJsonLayout() != null) {
                FileChooser fileChooser = new FileChooser();
                File selectedFile = fileChooser.showOpenDialog(Window.getWindows().get(0));

                if(selectedFile!=null) {
                    JSONValidator.validateJSON(Files.readString(Path.of(selectedFile.getPath())), getClass().getResourceAsStream("/picksSchema.json"));

                    this.environment.loadGraph(JsonParser.parseReader(new FileReader(selectedFile.getAbsolutePath())).getAsJsonObject());

                    this.mainFrame.manageButtons(false,false,false,true,true,true,true);
                }

                this.mainFrame.manageButtons(false,false,false,true,true,true,true);
            }
        } catch (InvalidNodeException | IOException e) {
            e.printStackTrace();
        } catch (ValidationException e) {
            mainFrame.showAlert("Incorrect picks file format");
        }
    }

    private void runGA() {
        if(mainFrame.getGaFrameController().handleErrors().equals("success")) {
            Random random = new Random(mainFrame.getGaFrameController().getSeedGaField());

            GeneticAlgorithm<PickingIndividual, PickingGAProblem> geneticAlgorithm = new GeneticAlgorithm<>(new PickingIndividual.PickingIndividualFactory(),
                    getSelectionMethod(),
                    getRecombinationMethod(),
                    getMutationMethod(),
                    mainFrame.getGaFrameController().getPopSizeField(), mainFrame.getGaFrameController().getGenerationsField(), random);

            geneticAlgorithm.addGAListener(mainFrame.getGaFrameController());
            environment.setGeneticAlgorithm(geneticAlgorithm);
            worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        mainFrame.getGaFrameController().getBestInRunArea().setText("");
                        mainFrame.manageButtons(true, true, true, false, true, true, true);
                        mainFrame.getGaFrameController().getSeriesBestIndividual().getData().clear();
                        mainFrame.getGaFrameController().getSeriesAverageFitness().getData().clear();
                        PickingIndividual individual = geneticAlgorithm.run(new PickingGAProblem(environment.getGraph(), new AStarSearch<>(new PickingManhattanDistance()), mainFrame.getGaFrameController().getWeightLimitationValue(), mainFrame.getGaFrameController().getCollisionsHandlingValue(), mainFrame.getGaFrameController().getTimeWeightField(), mainFrame.getGaFrameController().getCollisionWeightField(), random));
                        environment.setBestInRun(individual);
                        System.out.println(individual.getFitness());
                        System.out.println(individual.getNumberOfCollisions());
                        System.out.println(individual.getNumberTimesOffload());
                        System.out.println(Arrays.toString(individual.getGenome()));

                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    super.done();
                    mainFrame.manageButtons(false, false, false, true, false, true, true);

                }
            };
            worker.execute();
        }else{
            mainFrame.showAlert(mainFrame.getGaFrameController().handleErrors());
        }
    }


    private SelectionMethod<PickingIndividual, PickingGAProblem> getSelectionMethod() {
        switch (mainFrame.getGaFrameController().selectionMethodFieldSelection.getValue()) {
            case Tournament:
                return new Tournament<>(mainFrame.getGaFrameController().getPopSizeField(),mainFrame.getGaFrameController().getTournamentSizeField());
            case Rank:
                return new RankBased<>(mainFrame.getGaFrameController().getPopSizeField(),mainFrame.getGaFrameController().getSelectivePressureField());
        }
        return null;
    }


    private Recombination<PickingIndividual, PickingGAProblem> getRecombinationMethod() {

        switch (mainFrame.getGaFrameController().recombinationMethodField.getValue()) {
            case PMX:
                return new RecombinationPartialMapped<>(mainFrame.getGaFrameController().getRecombinationProbField());
            case OX:
                return new RecombinationOX<>(mainFrame.getGaFrameController().getRecombinationProbField());
            case OX1:
                return new RecombinationOX1<>(mainFrame.getGaFrameController().getRecombinationProbField());
            case CX:
                return new RecombinationCX<>(mainFrame.getGaFrameController().getRecombinationProbField());
        }
        return null;

    }

    private Mutation<PickingIndividual, PickingGAProblem> getMutationMethod(){
        switch (mainFrame.getGaFrameController().mutationMethodField.getValue()) {
            case Insert:
                return new MutationInsert<>(mainFrame.getGaFrameController().getMutationProbField());
            case Inversion:
                return new MutationInversion<>(mainFrame.getGaFrameController().getMutationProbField());
            case Scramble:
                return new MutationScramble<>(mainFrame.getGaFrameController().getMutationProbField());
        }
        return null;
    }






}
