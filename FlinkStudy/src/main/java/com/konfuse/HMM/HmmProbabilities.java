package com.konfuse.HMM;

/**
 * @Auther todd
 * @Date 2019/12/11
 */
public class HmmProbabilities {
    private final double sigma;
    private final double beta;

    public HmmProbabilities() {
        this(4.07, 0.00959442);
    }

    /**
     * @param sigma standard deviation of the normal distribution
     * @param beta beta parameter of the exponential distribution for 1s sampling interval
     */
    public HmmProbabilities(double sigma, double beta) {
        this.sigma = sigma;
        this.beta = beta;
    }

    /**
     * Returns the logarithmic observation probability density.
     * @param distance Absolute distance between GPS measurement and map matching candidate.
     * @return double measurement probability
     */
    public double measurementLogProbability(double distance) {
        return  ProbabilityDistributions.logNormalDistribution(sigma, distance);
    }

    /**
     * Returns the logarithmic transition probability density for the given transition parameters.
     * In contrast to Newson & Krumm the absolute distance difference is divided by the quadratic
     * time difference to make the beta parameter of the exponential distribution independent of the
     * sampling interval.
     * @param shortestRouteLength Length of the shortest route between two consecutive map matching candidates.
     * @param linearDistance Linear distance between two consecutive GPS measurements.
     * @param timeInterval time interval between two consecutive GPS measurements.
     * @return double transition probability
     */
    public double transitionLogProbability(double shortestRouteLength, double linearDistance, double timeInterval) {
        if (timeInterval < 0.0) {
            throw new IllegalStateException("Time interval between subsequent location measurements must be not less than 0");
        }
        return ProbabilityDistributions.logExponentialDistribution(beta, Math.abs(shortestRouteLength - linearDistance) / (timeInterval * timeInterval));
    }

}
