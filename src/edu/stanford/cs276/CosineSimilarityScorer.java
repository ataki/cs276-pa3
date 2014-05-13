package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CosineSimilarityScorer extends AScorer {
    public CosineSimilarityScorer(Map<String, Double> idfs) {
        super(idfs);
    }

    ///////////////weights///////////////////////////
    double urlweight = 1;
    double titleweight = 1;
    double bodyweight = 1;
    double headerweight = 1;
    double anchorweight = 1;

    double smoothingBodyLength = 500;
    //////////////////////////////////////////

    boolean USE_SUBLINEAR_SCALING = true;

    private double dot(double[] v1, double[] v2) {
        double sum = 0.0;
        for (int i = 0; i < v1.length; i++) {
            sum += v1[i] * v2[i];
        }
        return sum;
    }

    public double getNetScore(Map<String, Map<String, Double>> tfs, Query q, Map<String, Double> tfQuery, Document d) {
        double score;

        double[] docVector = new double[q.queryWords.size()];
        double[] queryVector = new double[q.queryWords.size()];

        for (int i = 0; i < q.queryWords.size(); i++) {
            String term = q.queryWords.get(i);
            double docScore = (
                    urlweight * tfs.get("url").get(term) +
                            titleweight * tfs.get("title").get(term) +
                            bodyweight * tfs.get("body").get(term) +
                            headerweight * tfs.get("header").get(term) +
                            anchorweight * tfs.get("anchor").get(term)
            );
            docVector[i] = docScore;
            queryVector[i] = tfQuery.get(term);
        }

        score = dot(docVector, queryVector);

        return score;
    }


    public void normalizeTFs(Map<String, Map<String, Double>> tfs, Document d, Query q) {
        for (String type : tfs.keySet()) {
            // mapping of term -> raw_score for this field type
            Map<String, Double> docMap = tfs.get(type);

            // make temp mapping of normalized scores
            Map<String, Double> normalizedTerms = new HashMap<String, Double>();
            for (String term : docMap.keySet()) {
                double tf = docMap.get(term);
                if (USE_SUBLINEAR_SCALING) {
                    if (tf > 0) {
                        tf = 1 + Math.log(tf);
                    }
                }
                double normalizedTF = tf / (d.body_length + smoothingBodyLength);
                normalizedTerms.put(term, normalizedTF);
            }

            // replace terms in docMap with normalized score
            // since we don't want to overwrite while iterating
            for (String term : normalizedTerms.keySet()) {
                docMap.put(term, normalizedTerms.get(term));
            }
        }
    }


    @Override
    public double getSimScore(Document d, Query q) {
        Map<String, Map<String, Double>> tfs = this.getDocTermFreqs(d, q);

        this.normalizeTFs(tfs, d, q);

        Map<String, Double> tfQuery = getQueryFreqs(q);


        return getNetScore(tfs, q, tfQuery, d);
    }


}
