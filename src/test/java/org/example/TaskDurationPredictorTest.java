package org.example;

import org.junit.jupiter.api.Test;

import static org.example.TrainingModel.createAndTrainTaskDurationPredictor;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

class taskDurationPredictorTest {

    @Test
    void embeddingLayerSetupTest() throws Exception {
        TaskDurationPredictor taskDurationPredictor = new TaskDurationPredictor();
        taskDurationPredictor.modelSetup();
        createAndTrainTaskDurationPredictor();



    }

}