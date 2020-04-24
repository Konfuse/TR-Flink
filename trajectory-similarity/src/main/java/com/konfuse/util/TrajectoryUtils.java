package com.konfuse.util;

import com.konfuse.dison.DISONTrajectory;
import com.konfuse.dita.DITAConfig;
import com.konfuse.dita.DITATrajectory;
import com.konfuse.geometry.Point;
import com.konfuse.mbe.MBETrajectory;
import com.konfuse.strtree.MBR;

import java.util.List;


/**
 * @Auther todd
 * @Date 2020/4/17
 */
public class TrajectoryUtils {
    public static double calcDTWDistance(List<Point> a, List<Point> b) {
        int len1 = a.size();
        int len2 = b.size();
        double[][] matrix = new double[len1][len2];
        for (int i = 0; i < len1; i++) {
            for (int j = 0; j < len2; j++) {
                matrix[i][j] = Double.MAX_VALUE;
            }
        }
        for (int i = 0; i < len1; i++) {
            for (int j = 0; j < len2; j++) {
                matrix[i][j] = a.get(i).calDistance(b.get(j));
                double left, up, diag;
                if (i > 0) {
                    left = matrix[i - 1][j];
                } else {
                    left = Double.MAX_VALUE;
                }
                if (j > 0) {
                    up = matrix[i][j - 1];
                } else {
                    up = Double.MAX_VALUE;
                }
                if (i > 0 && j > 0) {
                    diag = matrix[i - 1][j - 1];
                } else {
                    diag = Double.MAX_VALUE;
                }
                double last = Math.min(Math.min(left, up), diag);
                if (i > 0 || j > 0) {
                    matrix[i][j] += last;
                }
            }
        }
        return matrix[len1 - 1][len2 - 1];
    }

    public static double calcLORSDistance(DISONTrajectory a, DISONTrajectory b){
        int len1 = a.getTrajectoryData().size();
        int len2 = b.getTrajectoryData().size();
        double[][] matrix = new double[len1][len2];
        for (int i = 0; i < len1; i++) {
            for (int j = 0; j < len2; j++) {
                matrix[i][j] = 0.0;
            }
        }
        for (int i = 0; i < len1; i++) {
            for (int j = 0; j < len2; j++) {
                if(a.getTrajectoryData().get(i).getEdgeId() == b.getTrajectoryData().get(j).getEdgeId()) {
                    if(i > 0 && j > 0){
                        matrix[i][j] = a.getTrajectoryData().get(i).getLength() + matrix[i - 1][j - 1];
                    } else {
                        matrix[i][j] = a.getTrajectoryData().get(i).getLength();
                    }
                } else {
                    double left, up;
                    if (i > 0) {
                        left = matrix[i - 1][j];
                    } else {
                        left = 0;
                    }
                    if (j > 0) {
                        up = matrix[i][j - 1];
                    } else {
                        up = 0;
                    }
                    matrix[i][j] = Math.max(left, up);
                }
            }
        }
        return matrix[len1 - 1][len2 - 1];
    }

    public static double calcLCRSDistance(DISONTrajectory a, DISONTrajectory b){
        double distance = calcLORSDistance(a, b);
        return distance / (a.getLength() + b.getLength() - distance);
    }

    public static double calcDTWDistanceWithThreshold(List<Point> a, List<Point> b, double threshold) {
        int len1 = a.size();
        int len2 = b.size();
        double[][] matrix = new double[len1][len2];
        for (int i = 0; i < len1; i++) {
            double minDist = DITAConfig.thresholdMax;
            for (int j = 0; j < len2; j++) {
                if (i > 0 || j > 0) {
                    double left, up, diag;
                    if (i > 0) {
                        left = matrix[i - 1][j];
                    } else {
                        left = DITAConfig.thresholdMax;
                    }
                    if (j > 0) {
                        up = matrix[i][j - 1];
                    } else {
                        up = DITAConfig.thresholdMax;
                    }
                    if (i > 0 && j > 0) {
                        diag = matrix[i - 1][j - 1];
                    } else {
                        diag = DITAConfig.thresholdMax;
                    }
                    double last = Math.min(Math.min(left, up), diag);
                    if (last > threshold) {
                        matrix[i][j] = DITAConfig.thresholdMax;
                    } else {
                        matrix[i][j] = a.get(i).calDistance(b.get(j)) + last;
                    }
                } else {
                    matrix[i][j] = a.get(i).calDistance(b.get(j));
                }
                minDist = Math.min(minDist, matrix[i][j]);
            }
            if (minDist > threshold) {
                return DITAConfig.thresholdMax;
            }
        }
        return matrix[len1 - 1][len2 - 1];
    }

    public static double calcDTWCellsEstimation(DITATrajectory a, DITATrajectory b, double threshold){
        List<Tuple<MBR, Integer>> aCells = a.getCells();
        List<Tuple<MBR, Integer>> bCells = b.getCells();
        int aSize = aCells.size();
        int bSize = bCells.size();
        double totalDistance = 0;
        for (int i = 0; i < aSize; i++) {
            if (totalDistance <= threshold){
                double dist = DITAConfig.thresholdMax;
                for (int j = 0; j < bSize; j++) {
                    dist = Math.min(dist, calcCellDistance(aCells.get(i), bCells.get(j)));
                }
                totalDistance += dist * aCells.get(i).f1;
            } else {
                return DITAConfig.thresholdMax;
            }
        }
        return totalDistance;
    }

    public static double evalWithTrajectory(DITATrajectory a, DITATrajectory b){
        return Math.max(calcCellsEstimation(a, b), calcCellsEstimation(b, a));
    }

    public static double calcDTWLowerBound(MBETrajectory query, MBETrajectory candidate, double splitPercentage){
        List<MBR> queryMbrs = query.greedySplitMBE(splitPercentage);
        List<MBR> candidateMbrs = query.greedySplitTrajectory(splitPercentage);
        int len1 = queryMbrs.size();
        int len2 = candidateMbrs.size();

        double[][] matrix = new double[len1][len2];
        for (int i = 0; i < len1; i++) {
            for (int j = 0; j < len2; j++) {
                matrix[i][j] = Double.MAX_VALUE;
            }
        }
        for (int i = 0; i < len1; i++) {
            for (int j = 0; j < len2; j++) {
                matrix[i][j] = minDist(queryMbrs.get(i), candidateMbrs.get(j));
                double left, up, diag;
                if (i > 0) {
                    left = matrix[i - 1][j];
                } else {
                    left = Double.MAX_VALUE;
                }
                if (j > 0) {
                    up = matrix[i][j - 1];
                } else {
                    up = Double.MAX_VALUE;
                }
                if (i > 0 && j > 0) {
                    diag = matrix[i - 1][j - 1];
                } else {
                    diag = Double.MAX_VALUE;
                }
                double last = Math.min(Math.min(left, up), diag);
                if (i > 0 || j > 0) {
                    matrix[i][j] += last;
                }
            }
        }
        return matrix[len1 - 1][len2 - 1];
    }



    private static double calcCellsEstimation(DITATrajectory a, DITATrajectory b) {
        List<Tuple<MBR, Integer>> aCells = a.getCells();
        List<Tuple<MBR, Integer>> bCells = b.getCells();
        int aSize = aCells.size();
        int bSize = bCells.size();
        double totalDistance = 0;
        for (int i = 0; i < aSize; i++) {
            double dist = DITAConfig.thresholdMax;
            for (int j = 0; j < bSize; j++) {
                dist = Math.min(dist, calcCellDistance(aCells.get(i), bCells.get(j)));
            }
            totalDistance += dist * aCells.get(i).f1;
        }
        return totalDistance;
    }

    private static double calcCellDistance(Tuple<MBR, Integer> a, Tuple<MBR, Integer> b) {
        double ans = 0;
        if (a.f0.getX2() < b.f0.getX1()) {
            ans += (b.f0.getX1() - a.f0.getX2()) * (b.f0.getX1() - a.f0.getX2());
        } else if (a.f0.getX1() > b.f0.getX2()) {
            ans += (a.f0.getX1() - b.f0.getX2()) * (a.f0.getX1() - b.f0.getX2());
        }
        if (a.f0.getY2() < b.f0.getY1()) {
            ans += (b.f0.getY1() - a.f0.getY2()) * (b.f0.getY1() - a.f0.getY2());
        } else if (a.f0.getY1() > b.f0.getY2()) {
            ans += (a.f0.getY1() - b.f0.getY2()) * (a.f0.getY1() - b.f0.getY2());
        }
        return ans;
    }

    private static double minDist(MBR a, MBR b) {
        double sum = 0.0;
        if(a.getX2() < b.getX1()) {
            sum += (b.getX1() - a.getX2()) * (b.getX1() - a.getX2());
        } else if(b.getX2() < a.getX1()) {
            sum += (a.getX1() - b.getX2()) * (a.getX1() - b.getX2());
        }

        if(a.getY2() < b.getY1()) {
            sum += (b.getY1() - a.getY2()) * (b.getY1() - a.getY2());
        } else if(b.getY2() < a.getY1()) {
            sum += (a.getY1() - b.getY2()) * (a.getY1() - b.getY2());
        }
        return Math.sqrt(sum);
    }
}
