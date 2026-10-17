const { TestEnvironment } = require('jest-environment-node');

class OnnxFriendlyEnvironment extends TestEnvironment {
    constructor(config, context) {
        super(config, context);
        // Use the real (outer) typed arrays so onnxruntime's native code and JS agree
        this.global.ArrayBuffer = ArrayBuffer;
        this.global.Uint8Array = Uint8Array;
        this.global.Int32Array = Int32Array;
        this.global.Float32Array = Float32Array;
        this.global.BigInt64Array = BigInt64Array;
    }
}

module.exports = OnnxFriendlyEnvironment;