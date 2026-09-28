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
public class DurationPredictor implements AutoCloseable {
    private final OrtEnvironment env = OrtEnvironment.getEnvironment(); // ONNX Runtime environment
    private final OrtSession session;                                   // loaded ONNX model
    private final HuggingFaceTokenizer tokenizer;
    private final double mean, std;

    public DurationPredictor(Path dir) throws Exception {
        tokenizer = HuggingFaceTokenizer.newInstance(dir.resolve("tokenizer.json"));
        session = env.createSession(dir.resolve("duration_model.onnx").toString(),
                new OrtSession.SessionOptions());
        JsonObject stats = JsonParser
                .parseString(Files.readString(dir.resolve("target_stats.json")))
                .getAsJsonObject();
        mean = stats.get("mean").getAsDouble();
        std = stats.get("std").getAsDouble();
    }

    public double predictTime(String sentence) throws OrtException {
        Encoding enc = tokenizer.encode(sentence);
        long[][] ids = {enc.getIds()};
        long[][] mask = {enc.getAttentionMask()};
        long[][] types = {enc.getTypeIds()};

        Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input_ids", OnnxTensor.createTensor(env, ids));
        inputs.put("attention_mask", OnnxTensor.createTensor(env, mask));
        if (session.getInputNames().contains("token_type_ids")) {
            inputs.put("token_type_ids", OnnxTensor.createTensor(env, types));
        }

        try (OrtSession.Result result = session.run(inputs)) {
            float[][] logits = (float[][]) result.get(0).getValue();
            double z = logits[0][0];
            return Math.exp(z * std + mean);   // undo standardisation + log
        } finally {
            inputs.values().forEach(OnnxTensor::close);
        }
    }

    @Override public void close() throws Exception {
        session.close();
        tokenizer.close();
    }

    public static void main(String[] args) throws Exception {
        try (DurationPredictor p = new DurationPredictor(Paths.get("onnx_out"))) {
            System.out.println(p.predictTime("Go to the gym"));
        }
    }

}