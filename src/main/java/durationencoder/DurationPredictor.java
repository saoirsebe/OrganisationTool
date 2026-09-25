package durationencoder;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.*;
import java.util.*;

/**
 * Inference wrapper around Hugging Face encoder + regression head model exported to ONNX
 */
public class DurationPredictor {
    private final OrtEnvironment env = OrtEnvironment.getEnvironment(); // ONNX Runtime environment
    private final OrtSession session;                                   // loaded ONNX model
    private final HuggingFaceTokenizer tokenizer;
    private final double mean, std;

    public DurationPredictor(Path dir) throws Exception {
        tokenizer = HuggingFaceTokenizer.newInstance(dir.resolve("tokenizer.json"));
        session = env.createSession(dir.resolve("model.onnx").toString(),
                new OrtSession.SessionOptions());
        JsonObject stats = JsonParser
                .parseString(Files.readString(dir.resolve("target_stats.json")))
                .getAsJsonObject();
        mean = stats.get("mean").getAsDouble();
        std = stats.get("std").getAsDouble();
    }


}