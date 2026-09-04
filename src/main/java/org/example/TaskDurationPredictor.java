package org.example;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.recurrent.TimeDistributed;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;


import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.deeplearning4j.nn.transferlearning.TransferLearning;
import org.deeplearning4j.nn.transferlearning.FineTuneConfiguration;
import org.deeplearning4j.nn.conf.graph.MergeVertex;

public class TaskDurationPredictor {

    // Turn all words in yourVocabularySet into an integer ID
    private Map<String, Integer> actionToIndex;
    private Map<String, Integer> targetToIndex;
    private Map<Integer, String> indexToAction;
    private Map<Integer, String> indexToTarget;
    private ComputationGraph timePredictionModel;


    ComputationGraphConfiguration modelConfig(int actionVocabSize, int targetVocabSize, int embeddingDim){
        return new NeuralNetConfiguration.Builder()
                .updater(new Adam(0.001))
                .graphBuilder()
                .addInputs("actionSeq", "targetSeq")

                // embedding branch for action
                .addLayer("actionEmbedding",
                        new EmbeddingSequenceLayer.Builder()
                                .nIn(actionVocabSize)
                                .nOut(embeddingDim)
                                .weightInit(WeightInit.ZERO) // placeholder, overwritten later
                                .build(),
                        "actionSeq")

                // embedding branch for target
                .addLayer("targetEmbedding",
                        new EmbeddingSequenceLayer.Builder()
                                .nIn(targetVocabSize)
                                .nOut(embeddingDim)
                                .weightInit(WeightInit.ZERO) // placeholder, overwritten later
                                .build(),
                        "targetSeq")


                // merge output shape: [batch, embeddingDim*2, maxPairs]
                .addVertex("merge", new MergeVertex(), "actionEmbedding", "targetEmbedding")

                // apply the SAME dense transform to every timestep (every pair) independently
                .addLayer("perPairHidden",
                        new TimeDistributed(new DenseLayer.Builder()
                                .nIn(embeddingDim * 2).nOut(32)
                                .activation(Activation.RELU)
                                .build()),
                        "merge")

                // reduce each pair's hidden vector to a single scalar duration
                .addLayer("perPairDuration",
                        new TimeDistributed(new DenseLayer.Builder()
                                .nIn(32).nOut(1)
                                .activation(Activation.IDENTITY)
                                .build()),
                        "perPairHidden")
                // output shape: [batch, 1, maxPairs] — one predicted duration per pair, per timestep

                // sum across the time axis (pairs), ignoring padded slots via the mask
                .addLayer("summedDuration",
                        new GlobalPoolingLayer.Builder(PoolingType.SUM)
                                .build(),
                        "perPairDuration")
                // output shape: [batch, 1] — total predicted duration for the example

                .addLayer("output",
                        new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                                .activation(Activation.IDENTITY)
                                .nIn(1).nOut(1)
                                .build(),
                        "summedDuration")

                .setOutputs("output")
                .build();
    }

    /**
     * Method called to create the time prediction model, must be called first before training and use
     * @throws IOException
     */
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
            actionToIndex.put(action, aIdx);
            indexToAction.put(aIdx,action);
            aIdx++;
        }
        actionToIndex.put("<UNK>", aIdx);
        indexToAction.put(aIdx, "<UNK>");
        int actionVocabSize = actionToIndex.size();

        int tIdx = 0;
        for (String target : distinctTargets) {
            targetToIndex.put(target, tIdx);
            indexToTarget.put(tIdx,target);
            tIdx++;
        }
        targetToIndex.put("<UNK>", tIdx);
        indexToTarget.put(tIdx, "<UNK>");
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

        ComputationGraph initialModel = new ComputationGraph(conf);
        initialModel.init();

        initialModel.getLayer("actionEmbedding").setParam("W", actionWeightMatrix);
        initialModel.getLayer("targetEmbedding").setParam("W", targetWeightMatrix);

        timePredictionModel = new TransferLearning.GraphBuilder(initialModel)
                .fineTuneConfiguration(new FineTuneConfiguration.Builder().build())
                .setFeatureExtractor("actionEmbedding", "targetEmbedding") // freezes embedding layers until model has stabilised
                .build();

        saveModel();
        System.out.println("Finished model setup");

    }

    List<List<Integer>> getDurationTimes() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
                true);
        List<Task> tasks = mapper.readValue(
                new File("src/main/resources/MS-LaTTE_synthetic.json"),
                mapper.getTypeFactory().constructCollectionType(List.class, Task.class)
        );

        return tasks.stream()
                .map(task -> task.TimeTaken.stream()
                        .map(timeTaken -> timeTaken.EstimatedMinutes)
                        .toList())
                .toList();


    }

    /**
     * Called as first training run to un-freeze model after first 100 epochs
     * @throws IOException
     */
    void initialModelTraining() throws IOException {
        int numEpochs = 100;
        SimpleMultiDataSetIterator trainingIterator = getMultiDataSetIterator();
        for (int epoch = 0; epoch < numEpochs; epoch++) {
            trainingIterator.reset();
            timePredictionModel.fit(trainingIterator);
            System.out.println("Epoch " + epoch + " score: " + timePredictionModel.score());
        }
        timePredictionModel = new TransferLearning.GraphBuilder(timePredictionModel)
                .fineTuneConfiguration(new FineTuneConfiguration.Builder()
                        .updater(new Adam(0.0001)) // use a LOWER learning rate once unfrozen
                        .build())
                .build(); // no setFeatureExtractor -> everything trainable, including embeddings

        saveModel();
    }

    void trainModel() throws IOException {
        if (timePredictionModel == null) {
            loadModel();
        }

        int numEpochs = 100;
        SimpleMultiDataSetIterator trainingIterator = getMultiDataSetIterator();
        for (int epoch = 0; epoch < numEpochs; epoch++) {
            trainingIterator.reset();
            timePredictionModel.fit(trainingIterator);
            System.out.println("Epoch " + epoch + " score: " + timePredictionModel.score());
        }
        saveModel();
    }


    SimpleMultiDataSetIterator getMultiDataSetIterator() throws IOException {
        List<ParsedTaskDescription> parsedTasksList = getAllParsedTrainingTasks();
        List<List<Integer>> allDurationTimes = getDurationTimes();


        //Turn words into their integer representation using ______ToIndex
        List<Integer> listOfActionInts = parsedTasksList.stream()
                .map(task-> actionToIndex.getOrDefault(task.action, actionToIndex.get("<UNK>")))
                .toList();

        List<List<Integer>> listOfTargets = parsedTasksList.stream()
                .map(task -> task.targets.stream()
                        .map(target -> targetToIndex.getOrDefault(target, targetToIndex.get("<UNK>")))
                        .toList())
                .toList();


        // build mask and pad each example to same number of action,target pairs
        int maxPairs = listOfTargets.stream().mapToInt(List::size).max().orElse(1); // Finds the highest number of targets (action,target pairs needed)
        int numExamples = parsedTasksList.size();
        int trainingExamples = numExamples * 2; // Doubled to account for one training example for each duration label

        if (listOfTargets.size() != numExamples){
            throw new IOException("list of targets size != numExamples");
        }

        INDArray actionSeq = Nd4j.zeros(trainingExamples, maxPairs); // action mask initialised to 0's
        INDArray targetSeq = Nd4j.zeros(trainingExamples, maxPairs);
        INDArray mask = Nd4j.zeros(trainingExamples, maxPairs); // 1 = real pair, 0 = padding
        INDArray labels = Nd4j.zeros(trainingExamples, 1);

        // Turn all integers into INDArray for inputting into model
        for (int i = 0; i < numExamples; i++) {
            List<Integer> TargetsList = listOfTargets.get(i);
            for (int j = 0; j < TargetsList.size(); j++) {
                actionSeq.putScalar(new int[]{i, j}, listOfActionInts.get(i));
                targetSeq.putScalar(new int[]{i, j}, TargetsList.get(j));
                mask.putScalar(new int[]{i, j}, 1.0);
            }
            labels.putScalar(new int[]{i, 0}, allDurationTimes.get(i).get(0));

            for (int j = 0; j < TargetsList.size(); j++) {
                actionSeq.putScalar(new int[]{i+numExamples, j}, listOfActionInts.get(i));
                targetSeq.putScalar(new int[]{i+numExamples, j}, TargetsList.get(j));
                mask.putScalar(new int[]{i+numExamples, j}, 1.0);
            }
            labels.putScalar(new int[]{i+numExamples, 0}, allDurationTimes.get(i).get(1));
        }

        SimpleMultiDataSetIterator iterator = new SimpleMultiDataSetIterator(actionSeq, targetSeq, mask, labels, 32); // batchSize
        LabelNormaliser normaliser = new LabelNormaliser();
        normaliser.fit(labels);
        iterator.setPreProcessor(normaliser);
        return iterator;

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

    ComputationGraph getTimePredictionModel(){
        return timePredictionModel;
    }


    public void saveModel() throws IOException {
        ModelSerializer.writeModel(
                timePredictionModel,
                new File("task-duration-predictor.zip"),
                true
        );
    }


    public void loadModel() throws IOException {
        timePredictionModel =
                ModelSerializer.restoreComputationGraph(
                        new File("task-duration-predictor.zip")
                );
    }


}
