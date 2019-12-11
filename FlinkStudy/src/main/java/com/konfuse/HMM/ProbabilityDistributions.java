package com.konfuse.HMM;

import static java.lang.Math.*;
import static java.lang.Math.pow;

/**
 * @Auther todd
 * @Date 2019/12/11
 */
public class ProbabilityDistributions {
    /**
     * Use this function to calculate normal distribution
     * @param sigma
     * @param x
     * @return double normal distribution
     */
    static double normalDistribution(double sigma, double x){
        return 1.0 / (sqrt(2.0 * PI) * sigma) * exp(-0.5 * pow(x / sigma, 2));
    }

    /**
     * Use this function to avoid an arithmetic underflow for very small probabilities.
     * @param sigma
     * @param x
     * @return double log(normal distribution)
     */
    public static double logNormalDistribution(double sigma, double x) {
        return Math.log(1.0 / (sqrt(2.0 * PI) * sigma)) + (-0.5 * pow(x / sigma, 2));
    }

    /**
     * Use this function to calculate exponential Probability
     * @param beta
     * @param x
     * @return double exponential probability
     */
    public static double exponentialProbability(double beta, double x){
        return 1.0 / beta * exp(-x / beta);
    }

    /**
     * Use this function to avoid an arithmetic underflow for very small probabilities.
     * @param beta
     * @param x
     * @return double log(exponential distribution)
     */
    static double logExponentialDistribution(double beta, double x) {
        return log(1.0 / beta) - (x / beta);
    }

}

