package org.example;

import org.junit.jupiter.api.Test;

import java.io.IOException;

class taskDurationPredictorTest {

    @Test
    void embeddingLayerSetupTest() throws IOException {
        TaskDurationPredictor taskDurationPredictor = new TaskDurationPredictor();
        taskDurationPredictor.embeddingLayerSetup();
    }
}