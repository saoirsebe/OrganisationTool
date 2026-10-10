const ort = require('onnxruntime-node');
const fs = require('fs');
const path = require('path');

const MODEL_DIR = path.join(__dirname, '../../../Models');
const TOKENIZER_DIR = path.join(MODEL_DIR, 'onnx_duration_out');
const modelPath = path.join(TOKENIZER_DIR, 'onnx', 'model.onnx');

let session;
let tokenizer;
let targetStats;

async function loadModel() {
    const { env, AutoTokenizer } = await import('@huggingface/transformers');
    env.allowRemoteModels = false;
    env.allowLocalModels = true;
    env.localModelPath = MODEL_DIR + path.sep;   // folder CONTAINING onnx_duration_out

    // Fail early with a clear message if the tokenizer files are missing
    for (const f of ['tokenizer.json', 'tokenizer_config.json']) {
        const p = path.join(TOKENIZER_DIR, f);
        if (!fs.existsSync(p)) throw new Error(`Missing ${p}`);
    }

    session = await ort.InferenceSession.create(modelPath);
    tokenizer = await AutoTokenizer.from_pretrained('onnx_duration_out');
    targetStats = JSON.parse(fs.readFileSync(path.join(TOKENIZER_DIR, 'target_stats.json'), 'utf8'));
    console.log('Model expects inputs:', session.inputNames);
}

async function predictDuration(task) {
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
    predictDuration
};