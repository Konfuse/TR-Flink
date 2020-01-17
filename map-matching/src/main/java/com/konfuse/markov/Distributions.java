package com.konfuse.markov;

import static java.lang.Math.*;

/**
 * Implements various probability distributions.
 */
public class Distributions {

    public static double normalDistribution(double sigma, double x) {
        return 1.0 / (sqrt(2.0 * PI) * sigma) * exp(-0.5 * pow(x / sigma, 2));
    }

    /**
     * Use this function instead of Math.log(normalDistribution(sigma, x)) to avoid an
     * arithmetic underflow for very small probabilities.
     */
    public static double logNormalDistribution(double sigma, double x) {
        return Math.log(1.0 / (sqrt(2.0 * PI) * sigma)) + (-0.5 * pow(x / sigma, 2));
    }

    /**
     * @param beta =1/lambda with lambda being the standard exponential distribution rate parameter
     */
    public static double exponentialDistribution(double beta, double x) {
        return 1.0 / beta * exp(-x / beta);
    }

    /**
     * Use this function instead of Math.log(exponentialDistribution(beta, x)) to avoid an
     * arithmetic underflow for very small probabilities.
     *
     * @param beta =1/lambda with lambda being the standard exponential distribution rate parameter
     */
    public static double logExponentialDistribution(double beta, double x) {
        return log(1.0 / beta) - (x / beta);
    }
}
