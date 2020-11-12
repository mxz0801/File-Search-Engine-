package cecs429.weight;

import cecs429.index.DiskPositionalIndex;
import cecs429.index.Posting;

import java.util.HashMap;

public class Default implements Strategy{

    @Override
    public Float getWdt(Integer tftd, double tokensInDoc, double aveTokensInCorpus, double aveTftd) {
        return (float)(1 + Math.log(tftd)/Math.log(Math.E));
    }

    @Override
    public Float getWqt(Integer corpusSize, Integer dft) {
        return (float) (Math.log(1 + corpusSize / dft)/Math.E);
    }

    @Override
    public Float getLd(double docWeight, double byteSize) {
        return (float)docWeight;
    }

}
