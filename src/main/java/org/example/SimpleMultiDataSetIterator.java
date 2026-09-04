package org.example;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.MultiDataSet;
import org.nd4j.linalg.dataset.api.MultiDataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.MultiDataSetIterator;
import org.nd4j.linalg.indexing.NDArrayIndex;

/**
 * A Simple Multi DataSet Iterator to feed batches of data to the model when model.fit is called
 */
public class SimpleMultiDataSetIterator implements MultiDataSetIterator {

    private final INDArray actionSeqFull, targetSeqFull, maskFull, labelFull;
    private final int batchSize, numExamples;
    private int cursor = 0;
    private MultiDataSetPreProcessor preProcessor;

    public SimpleMultiDataSetIterator(INDArray actionSeqFull, INDArray targetSeqFull,
                                      INDArray maskFull, INDArray labelFull, int batchSize) {
        this.actionSeqFull = actionSeqFull;
        this.targetSeqFull = targetSeqFull;
        this.maskFull = maskFull;
        this.labelFull = labelFull;
        this.batchSize = batchSize;
        this.numExamples = (int) actionSeqFull.shape()[0];
    }

    @Override
    public boolean hasNext() {
        return cursor < numExamples;
    }

    @Override
    public MultiDataSet next() {
        return next(batchSize);
    }

    @Override
    public MultiDataSet next(int num) {
        int end = Math.min(cursor + num, numExamples); // Ensures iterator doesn't try and read past the end of the data sequences

        INDArray actionBatch = actionSeqFull.get(NDArrayIndex.interval(cursor, end), NDArrayIndex.all());
        INDArray targetBatch = targetSeqFull.get(NDArrayIndex.interval(cursor, end), NDArrayIndex.all());
        INDArray maskBatch = maskFull.get(NDArrayIndex.interval(cursor, end), NDArrayIndex.all());
        INDArray labelBatch = labelFull.get(NDArrayIndex.interval(cursor, end), NDArrayIndex.all());

        cursor = end;


        MultiDataSet mds = new MultiDataSet(
                new INDArray[]{actionBatch, targetBatch},
                new INDArray[]{labelBatch},
                new INDArray[]{maskBatch, maskBatch},
                new INDArray[]{null}
        );

        if (preProcessor != null) {
            preProcessor.preProcess(mds); // Need to normalise labels to help training stability
        }

        return mds;
    }

    @Override
    public void setPreProcessor(MultiDataSetPreProcessor preProcessor) {
        this.preProcessor = preProcessor;
    }

    @Override
    public MultiDataSetPreProcessor getPreProcessor() {
        return preProcessor;
    }

    @Override
    public boolean resetSupported() {
        return true;
    }

    @Override
    public boolean asyncSupported() {
        return true;
    }

    @Override
    public void reset() {
        cursor = 0;
    }
}