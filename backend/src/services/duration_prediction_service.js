const ort = require('onnxruntime-node');
const { AutoTokenizer } = require('@xenova/transformers');
const fs = require('fs');
const path = require('path');

const MODEL_DIR = path.join(__dirname, 'ml/models');

let session;
let tokenizer;
let targetStats;

async function loadModel() {
    session = await ort.InferenceSession.create(path.join(MODEL_DIR, 'duration_model.onnx'));
    tokenizer = await AutoTokenizer.from_pretrained(MODEL_DIR);
    targetStats = JSON.parse(fs.readFileSync(path.join(MODEL_DIR, 'target_stats.json'), 'utf8')); // holds mean and std used to standardise during training.

    console.log('Model expects inputs:', session.inputNames); // sanity-check names at startup
}

async function predict_duration(task) {
    if (!session) {
        throw new Error('Model not loaded — call loadModel() first');
    }

    // Tokenize. { truncation: true } matches training.
    const encoded = await tokenizer(task, { truncation: true, max_length: 128 });

    // transformers.js gives BigInt64Array-backed tensors already — reshape to [1, seqLen]
    const seqLen = encoded.input_ids.dims[1];
    const makeTensor = (data) => new ort.Tensor('int64', data.data, [1, seqLen]);

    const feeds = {};
    if (session.inputNames.includes('input_ids')) feeds.input_ids = makeTensor(encoded.input_ids);
    if (session.inputNames.includes('attention_mask')) feeds.attention_mask = makeTensor(encoded.attention_mask);
    if (session.inputNames.includes('token_type_ids') && encoded.token_type_ids) {
        feeds.token_type_ids = makeTensor(encoded.token_type_ids);
    }

    const results = await session.run(feeds);
    const outputName = session.outputNames[0];
    const z = results[outputName].data[0]; // standardized log-minutes

    // Undo the transform applied in train.py: log then standardize
    const minutes = Math.exp(z * targetStats.std + targetStats.mean);
    return minutes;
}

module.exports = {
    loadModel,
    predict_duration
};