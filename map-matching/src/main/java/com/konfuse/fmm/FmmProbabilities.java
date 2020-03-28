package com.konfuse.fmm;

import com.konfuse.markov.Distributions;

/**
 * @Author: Konfuse
 * @Date: 2020/1/17 11:35
 */
public class FmmProbabilities {
    private final double sigma;
    private final double beta;

    /**
     * Sets default values for sigma and beta.
     */
    public FmmProbabilities() {
        /*
         * Sigma taken from Newson&Krumm.
         * Beta empirically computed from the Microsoft ground truth data for shortest route
         * lengths and 60 s sampling interval but also works for other sampling intervals.
         */
        this(4.07, 0.00959442);
    }

    /**
     * @param sigma standard deviation of the normal distribution [m] used for modeling the
     * GPS error
     */
    public FmmProbabilities(double sigma, double beta) {
        this.sigma = sigma;
        this.beta = beta;
    }

    /**
     * Returns the logarithmic emission probability density.
     *
     * @param distance Absolute distance [m] between GPS measurement and map matching candidate.
     */
    public double emissionLogProbability(double distance) {
        return Distributions.logNormalDistribution(sigma, distance);
    }

    /**
     * Returns the emission probability.
     *
     * @param distance Absolute distance [m] between GPS measurement and map matching candidate.
     */
    public double emissionProbability(double distance) {
        return Distributions.normalDistribution(sigma, distance);
    }

    /**
     * Returns the logarithmic transition probability density for the given transition
     * parameters.
     *
     * @param shortestPathDist Length of the shortest route [m] between two consecutive map matching
     * candidates.
     * @param linearDist Linear distance [m] between two consecutive GPS measurements.
     */
    public double transitionLogProbability(double shortestPathDist, double linearDist, double timeDiff) {
        double transitionMetric = normalizedTransitionMetric(shortestPathDist, linearDist, timeDiff);
        return Distributions.logExponentialDistribution(beta, transitionMetric);
    }

    public double transitionProbability(double shortestPathDist, double linearDist) {
        double transitionProbability = 1.0;
        if (linearDist < 0.000001) {
            transitionProbability = shortestPathDist > 0.000001 ? 0 : 1.0;
        } else {
            transitionProbability = linearDist > shortestPathDist ? shortestPathDist/linearDist : linearDist/shortestPathDist;
        }
        return transitionProbability;
    }

    /**
     * Returns a transition metric for the transition between two consecutive map matching
     * candidates.
     *
     * In contrast to Newson & Krumm the absolute distance difference is divided by the quadratic
     * time difference to make the beta parameter of the exponential distribution independent of the
     * sampling interval.
     */
    private double normalizedTransitionMetric(double routeLength, double linearDistance, double timeDiff) {
        if (timeDiff < 0.0) {
            throw new IllegalStateException("Time difference between subsequent location measurements must be >= 0.");
        }
        return Math.abs(linearDistance - routeLength) / (timeDiff * timeDiff);
    }
}
