package org.example;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.deeplearning4j.nn.conf.layers.EmbeddingLayer;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;


public class Embedding {

    public void embeddingLayerSetup() throws IOException {
        int idx = 0;
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        List<Task> tasks = mapper.readValue(
                new File("src/main/resources/MS-LaTTE_synthetic.json"),
                mapper.getTypeFactory().constructCollectionType(List.class, Task.class)
        );

        // List of all task descriptions
        List<String> yourTaskTitles = tasks.stream()
                .map(task -> task.TaskTitle)
                .toList();

        List<ParsedTaskDescription> parsedTasksList = ParsingToSchema.returnParsedTasks(yourTaskTitles);

        // Create a set of all distinct action and target words in dataset
        Set<String> yourVocabularySet = parsedTasksList.stream()
                .flatMap(task -> Stream.concat(
                        Stream.of(task.action),
                        task.targets.stream()
                ))
                .collect(Collectors.toSet());

        // Turn all words in yourVocabularySet into an integer ID
        Map<String, Integer> wordToIndex = new HashMap<>();
        Map<Integer, String> indexToWord = new HashMap<>();

        for (String word : yourVocabularySet) {
            wordToIndex.put(word, idx);
            indexToWord.put(idx, word);
            idx++;
        }
        int vocabSize = wordToIndex.size();

        // UNK token to handle words not in pretrained set at inference time:
        wordToIndex.put("<UNK>", vocabSize);
        indexToWord.put(vocabSize, "<UNK>");
        vocabSize++;

        // Creating weight matrix to initialise embedding model to weights of pretrained WordVectors
        WordVectors wordVectors = WordVectorSerializer.loadTxtVectors(new File("src/main/resources/glove.6B.100d.txt"));
        int embeddingDim = 100; // to match glove.6B.100d.txt -> 100
        INDArray pretrainedWeightMatrix = Nd4j.zeros(vocabSize, embeddingDim); // Initialise weight matrix to 0's
        for (Map.Entry<String, Integer> entry : wordToIndex.entrySet()) {
            String word = entry.getKey();
            int rowIdx = entry.getValue();

            if (wordVectors.hasWord(word)) {
                double[] vec = wordVectors.getWordVector(word);
                INDArray vecArray = Nd4j.create(vec); // shape [embeddingDim]
                pretrainedWeightMatrix.putRow(rowIdx, vecArray);
            } else {
                // if not in the pretrained set: init to random small value as zeros give the model nothing to work with and can cause dead gradients
                INDArray randomVec = Nd4j.rand(1, embeddingDim).subi(0.5).muli(0.1);
                pretrainedWeightMatrix.putRow(rowIdx, randomVec);
            }
        }

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .updater(new Adam(0.001))
                .list()
                .layer(new EmbeddingLayer.Builder()
                        .nIn(vocabSize)
                        .nOut(embeddingDim)
                        .weightInit(WeightInit.ZERO) // overwritten in step 4 — placeholder only
                        .build())
                .layer(new DenseLayer.Builder()
                        .nIn(embeddingDim)
                        .nOut(64)
                        .activation(Activation.RELU)
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .nIn(64)
                        .nOut(1) // predicting a single value: duration in minutes
                        .build())
                .build();

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();

        INDArray currentEmbeddingWeights = model.getLayer(0).getParam("W");
        System.out.println(currentEmbeddingWeights.shapeInfoToString());
        System.out.println(pretrainedWeightMatrix.shapeInfoToString());
        //model.getLayer(0).setParam("W", pretrainedWeightMatrix);
    }
}
