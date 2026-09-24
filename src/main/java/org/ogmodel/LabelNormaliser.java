package org.ogmodel;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.MultiDataSet;
import org.nd4j.linalg.dataset.api.MultiDataSetPreProcessor;

/**
 * Need to normalise training labels to help training stability
 */
public class LabelNormaliser implements MultiDataSetPreProcessor {

    private double mean;
    private double std;
    private boolean fitted = false;

    /**
     * Needs to be called once, on the full training label array, before wrapping the iterator.
     */
    public void fit(INDArray allTrainingLabels) {
        this.mean = allTrainingLabels.meanNumber().doubleValue();
        this.std = allTrainingLabels.stdNumber().doubleValue();

        if (this.std == 0.0) {
            // guard against divide-by-zero if all labels happen to be identical
            this.std = 1.0;
        }

        this.fitted = true;
    }

    @Override
    public void preProcess(MultiDataSet multiDataSet) {
        if (!fitted) {
            throw new IllegalStateException("LabelOnlyNormalizer.fit(...) must be called before use");
        }

        INDArray labels = multiDataSet.getLabels(0); // your single label array
        labels.subi(mean).divi(std); // in-place: (x - mean) / std
    }

    public double denormalize(double normalizedValue) {
        return normalizedValue * std + mean;
    }

    public INDArray denormalize(INDArray normalizedValues) {
        return normalizedValues.mul(std).add(mean);
    }

    public double getMean() {
        return mean;
    }

    public double getStd() {
        return std;
    }
}