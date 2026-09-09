package org.example;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.example.TrainingModel.createAndTrainTaskDurationPredictor;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

class TaskDurationPredictorTest {


    @Test
    void embeddingLayerSetupTest() throws Exception {
        //createAndTrainTaskDurationPredictor();
        TaskDurationPredictor durationPredictorModel = new FakeTaskDurationPredictor();
        durationPredictorModel.setInitialNumEpochs(15);
        durationPredictorModel.setNumEpochs(20);
        durationPredictorModel.modelSetup();
        durationPredictorModel.trainModel();

    }

     /*

    @Test
    void paddingContentShouldNotAffectOutput_whenMaskedCorrectly() throws IOException {
        int maxPairs = 5;
        int realPairs = 2; // both examples in this test have this many real pairs

        TaskDurationPredictor taskDurationPredictor = new TaskDurationPredictor();
        taskDurationPredictor.modelSetup();
        ComputationGraph model = taskDurationPredictor.getTimePredictionModel();


        // Example A: real pairs at positions 0,1; padding (positions 2-4) filled with index 0 ---
        INDArray actionSeqA = Nd4j.zeros(1, maxPairs);
        INDArray targetSeqA = Nd4j.zeros(1, maxPairs);
        INDArray maskA = Nd4j.zeros(1, maxPairs);
        actionSeqA.putScalar(new int[]{0, 0}, 3);
        actionSeqA.putScalar(new int[]{0, 1}, 5);
        targetSeqA.putScalar(new int[]{0, 0}, 2);
        targetSeqA.putScalar(new int[]{0, 1}, 7);
        maskA.putScalar(new int[]{0, 0}, 1.0);
        maskA.putScalar(new int[]{0, 1}, 1.0);
        // positions 2-4 are padded with index 0

        // Example B: SAME real pairs, but padding (positions 2-4) filled with a DIFFERENT valid index
        INDArray actionSeqB = actionSeqA.dup();
        INDArray targetSeqB = targetSeqA.dup();
        INDArray maskB = maskA.dup();
        for (int j = realPairs; j < maxPairs; j++) {
            actionSeqB.putScalar(new int[]{0, j}, 8); // different, non-zero pad token
            targetSeqB.putScalar(new int[]{0, j}, 9);
        }

        model.setLayerMaskArrays(
                new INDArray[]{maskA, maskA},
                null
        );

        INDArray[] outA = model.output(actionSeqA, targetSeqA);

        model.setLayerMaskArrays(
                new INDArray[]{maskB, maskB},
                null
        );

        INDArray[] outB = model.output(actionSeqB, targetSeqB);

        double predictionA = outA[0].getDouble(0);
        double predictionB = outB[0].getDouble(0);

        assertEquals(predictionA, predictionB, 1e-6,
                "Prediction changed when only PADDED (masked) values changed -mask is not propagating correctly through the branch.");
    }


    @Test
    void changingRealPairValuesShouldChangeOutput_sanityCheck() throws IOException {
        // Guards against a trivial pass above: if the network has one constant output regardless of input, the first test would falsely "pass".
        int maxPairs = 5;

        TaskDurationPredictor taskDurationPredictor = new TaskDurationPredictor();
        taskDurationPredictor.modelSetup();
        ComputationGraph model = taskDurationPredictor.getTimePredictionModel();

        INDArray actionSeqA = Nd4j.zeros(1, maxPairs);
        INDArray targetSeqA = Nd4j.zeros(1, maxPairs);
        actionSeqA.putScalar(new int[]{0, 0}, 3);
        targetSeqA.putScalar(new int[]{0, 0}, 2);

        INDArray actionSeqB = actionSeqA.dup();
        INDArray targetSeqB = targetSeqA.dup();
        actionSeqB.putScalar(new int[]{0, 0}, 7); // different REAL pair value

        double predictionA = model.output(actionSeqA, targetSeqA)[0].getDouble(0);
        double predictionB = model.output(actionSeqB, targetSeqB)[0].getDouble(0);

        assertNotEquals(predictionA, predictionB, 1e-9,
                "Output did not change when a REAL (unmasked) input changed — network may be degenerate, invalidating the masking test above.");
    }

      */


}

