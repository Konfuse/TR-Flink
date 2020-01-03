package com.konfuse.markov;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation utilities.
 */
class Utils {

    static int initialHashMapCapacity(int maxElements) {
        // Default load factor of HashMaps is 0.75
        return (int)(maxElements / 0.75) + 1;
    }

    static <S> Map<S, Double> logToNonLogProbabilities(Map<S, Double> logProbabilities) {
        final Map<S, Double> result = new LinkedHashMap<>();
        for (Map.Entry<S, Double> entry : logProbabilities.entrySet()) {
            result.put(entry.getKey(), Math.exp(entry.getValue()));
        }
        return result;
    }

    /**
     * Note that this check must not be used for probability densities.
     */
    static boolean probabilityInRange(double probability, double delta) {
        return probability >= -delta && probability <= 1.0 + delta;
    }

}
