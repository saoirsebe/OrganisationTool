package org.ogmodel;

public record DataIterators(
        SimpleMultiDataSetIterator training,
        SimpleMultiDataSetIterator validation,
        SimpleMultiDataSetIterator test

) {}

