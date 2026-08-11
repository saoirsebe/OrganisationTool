package org.example;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingTest {

    @Test
    void embeddingLayerSetupTest() throws IOException {
        Embedding embedding = new Embedding();
        embedding.embeddingLayerSetup();
    }
}