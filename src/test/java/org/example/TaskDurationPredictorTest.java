package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

class taskDurationPredictorTest {

    @Test
    void embeddingLayerSetupTest() throws IOException {
        TaskDurationPredictor taskDurationPredictor = new TaskDurationPredictor();
        taskDurationPredictor.modelSetup();


    }

}