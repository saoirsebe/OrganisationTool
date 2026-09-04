package org.example;

public record DataIterators(
        SimpleMultiDataSetIterator training,
        SimpleMultiDataSetIterator validation,
        SimpleMultiDataSetIterator test
) {}
