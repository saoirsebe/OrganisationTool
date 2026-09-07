package org.example;

import org.nd4j.linalg.api.ndarray.INDArray;

public record DataIterators(
        SimpleMultiDataSetIterator training,
        SimpleMultiDataSetIterator validation,
        SimpleMultiDataSetIterator test

) {}

