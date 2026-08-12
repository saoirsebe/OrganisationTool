package org.example;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.layers.BatchNormalization;
import org.deeplearning4j.nn.conf.layers.EmbeddingLayer;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.MultiDataSet;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.deeplearning4j.nn.transferlearning.TransferLearning;
import org.deeplearning4j.nn.transferlearning.FineTuneConfiguration;
import org.deeplearning4j.nn.conf.graph.MergeVertex;

import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.dataset.api.iterator.MultiDataSetIterator;

public class TaskDurationPredictor {

    // Turn all words in yourVocabularySet into an integer ID
    Map<String, Integer> actionToIndex;
    Map<String, Integer> targetToIndex;
    Map<Integer, String> indexToAction;
    Map<Integer, String> indexToTarget;

    // Used during training to normalise:
    double mean; // mean duration across training set
    double std;  // std dev of duration across training set


    ComputationGraphConfiguration modelConfig(int actionVocabSize, int targetVocabSize, int embeddingDim){
        return new NeuralNetConfiguration.Builder()
                .updater(new Adam(0.001))
                .graphBuilder()
                .addInputs("actionInput", "targetInput")

                // embedding branch for action
                .addLayer("actionEmbedding",
                        new EmbeddingLayer.Builder()
                                .nIn(actionVocabSize)
                                .nOut(embeddingDim)
                                .weightInit(WeightInit.ZERO) // placeholder, overwritten later
                                .build(),
                        "actionInput")
                .addLayer("actionNorm", new BatchNormalization.Builder().build(), "actionEmbedding")


                // embedding branch for target
                .addLayer("targetEmbedding",
                        new EmbeddingLayer.Builder()
                                .nIn(targetVocabSize)
                                .nOut(embeddingDim)
                                .weightInit(WeightInit.ZERO) // placeholder, overwritten later
                                .build(),
                        "targetInput")
                .addLayer("targetNorm", new BatchNormalization.Builder().build(), "targetEmbedding")


                // merge the two embedding outputs into one vector
                .addVertex("merge", new MergeVertex(), "actionNorm", "targetNorm")

                .addLayer("dense1",
                        new DenseLayer.Builder()
                                .nIn(embeddingDim * 2) // merged size = sum of both embedding dims
                                .nOut(64)
                                .activation(Activation.RELU)
                                .build(),
                        "merge")

                .addLayer("output",
                        new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                                .activation(Activation.IDENTITY)
                                .nIn(64)
                                .nOut(1) // predicting duration
                                .build(),
                        "dense1")

                .setOutputs("output")
                .build();
    }

    public void modelSetup() throws IOException {

        List<ParsedTaskDescription> parsedTasksList = getAllParsedTrainingTasks();

        // Create a set of all distinct action and target words in dataset
        Set<String> distinctActions = parsedTasksList.stream()
                .map(task -> task.action)
                .collect(Collectors.toSet());
        Set<String> distinctTargets = parsedTasksList.stream()
                .flatMap(task -> task.targets.stream())
                .collect(Collectors.toSet());


        actionToIndex = new HashMap<>();
        targetToIndex = new HashMap<>();
        indexToAction = new HashMap<>();
        indexToTarget = new HashMap<>();

        // UNK tokens to handle words not in pretrained sets at inference time:
        int aIdx = 0;
        for (String action : distinctActions) {
            actionToIndex.put(action, aIdx++);
        }
        actionToIndex.put("<UNK>", aIdx++);
        int actionVocabSize = actionToIndex.size();

        int tIdx = 0;
        for (String target : distinctTargets) {
            targetToIndex.put(target, tIdx++);
        }
        targetToIndex.put("<UNK>", tIdx++);
        int targetVocabSize = targetToIndex.size();


        // Creating weight matrices to initialise embedding model to weights of pretrained WordVectors
        WordVectors wordVectors = WordVectorSerializer.loadTxtVectors(new File("src/main/resources/glove.6B.100d.txt"));
        int embeddingDim = 100; // to match glove.6B.100d.txt -> 100

        INDArray actionWeightMatrix = Nd4j.zeros(actionVocabSize, embeddingDim);
        for (Map.Entry<String, Integer> entry : actionToIndex.entrySet()) {
            String word = entry.getKey();
            int rowIdx = entry.getValue();
            if (wordVectors.hasWord(word)) {
                double[] vec = wordVectors.getWordVector(word);
                INDArray vecArray = Nd4j.create(vec); // shape [embeddingDim]
                actionWeightMatrix.putRow(rowIdx, vecArray);
            } else {
                actionWeightMatrix.putRow(rowIdx, Nd4j.rand(1, embeddingDim).subi(0.5).muli(0.1));
            }
        }

        INDArray targetWeightMatrix = Nd4j.zeros(targetVocabSize, embeddingDim);
        for (Map.Entry<String, Integer> entry : targetToIndex.entrySet()) {
            String word = entry.getKey();
            int rowIdx = entry.getValue();
            if (wordVectors.hasWord(word)) {
                double[] vec = wordVectors.getWordVector(word);
                INDArray vecArray = Nd4j.create(vec); // shape [embeddingDim]
                targetWeightMatrix.putRow(rowIdx, vecArray);
            } else {
                // if not in the pretrained set: init to random small value as zeros give the model nothing to work with and can cause dead gradients
                targetWeightMatrix.putRow(rowIdx, Nd4j.rand(1, embeddingDim).subi(0.5).muli(0.1));
            }
        }


        ComputationGraphConfiguration conf = modelConfig(actionVocabSize, targetVocabSize,embeddingDim);

        ComputationGraph model = new ComputationGraph(conf);
        model.init();

        model.getLayer("actionEmbedding").setParam("W", actionWeightMatrix);
        model.getLayer("targetEmbedding").setParam("W", targetWeightMatrix);

        ComputationGraph frozenModel = new TransferLearning.GraphBuilder(model)
                .fineTuneConfiguration(new FineTuneConfiguration.Builder().build())
                .setFeatureExtractor("actionEmbedding") // freezes this vertex and everything feeding into it
                .build();


        trainModel(model);
    }

    List<List<Integer>> getDurationTimes() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        List<Task> tasks = mapper.readValue(
                new File("src/main/resources/MS-LaTTE_synthetic.json"),
                mapper.getTypeFactory().constructCollectionType(List.class, Task.class)
        );


        return tasks.stream()
                .flatMap(task -> task.TimeTaken.stream())
                .map(timeTaken -> timeTaken.EstimatedMinutes)
                .toList();
    }

    void trainModel(ComputationGraph model) throws IOException {

        int numEpochs = 100;
        List<MultiDataSet> multiDataSetsForTestTasks = getMultiDataSetsForTestTasks();

        //predicted = model.output(...) * std + mean
    }

    List<MultiDataSet> getMultiDataSetsForTestTasks() throws IOException {
        List<ParsedTaskDescription> parsedTasksList = getAllParsedTrainingTasks();


        List<List<Integer>> allDurationTimes = getDurationTimes();


        //Turn words into their integer representation using ______ToIndex
        List<Integer> listOfActions = parsedTasksList.stream()
                .map(task-> actionToIndex.getOrDefault(task.action, actionToIndex.get("<UNK>")))
                .toList();

        List<List<Integer>> listOfTargets = parsedTasksList.stream()
                .map(task -> task.targets.stream()
                        .map(target -> targetToIndex.getOrDefault(target, targetToIndex.get("<UNK>")))
                        .toList())
                .toList();


        // Turn all integers into INDArray for inputting into model
        List<INDArray> actionInputArrays = listOfActions.stream()
                .map(actionIdx -> Nd4j.create(new float[]{actionIdx}, new int[]{1, 1}))
                .toList();

        List<List<INDArray>> targetInputArrays = listOfTargets.stream()
                .map(targetsIdxList -> targetsIdxList.stream()
                        .map(targetIdx -> Nd4j.create(new float[]{targetIdx}, new int[]{1, 1}))
                        .toList())
                .toList();

        List<List<INDArray>> labelArrays = allDurationTimes.stream()
                .map(durationTimesList -> durationTimesList.stream()
                        .map(time -> Nd4j.create(new float[]{time.floatValue()}, new int[]{1, 1}))
                        .toList())
                .toList();

        /*
        List<MultiDataSet> listOfMultiDataSets = ;


        MultiDataSet mds = new org.nd4j.linalg.dataset.MultiDataSet(
                new INDArray[]{actionInputArr, targetInputArr},
                new INDArray[]{labelArr}
        );

         */
        return null;
    }

    /**
     *
     * @return returns a list of all parsed tasks in dataset, in the form of ParsedTaskDescription
     * @throws IOException
     */
    List<ParsedTaskDescription> getAllParsedTrainingTasks() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        List<Task> tasks = mapper.readValue(
                new File("src/main/resources/MS-LaTTE_synthetic.json"),
                mapper.getTypeFactory().constructCollectionType(List.class, Task.class)
        );

        // List of all task descriptions
        List<String> allTaskTitles = tasks.stream()
                .map(task -> task.TaskTitle)
                .toList();

        return ParsingToSchema.returnParsedTasks(allTaskTitles);
    }


}
