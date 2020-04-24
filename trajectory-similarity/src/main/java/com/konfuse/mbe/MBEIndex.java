package com.konfuse.mbe;

import com.konfuse.geometry.DataObject;
import com.konfuse.geometry.Rectangle;
import com.konfuse.strtree.IndexBuilder;
import com.konfuse.strtree.MBR;
import com.konfuse.strtree.RTree;
import com.konfuse.util.TrajectoryUtils;
import com.konfuse.util.Tuple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * @Auther todd
 * @Date 2020/4/24
 */
public class MBEIndex implements Serializable {
    private RTree STRtree;

    public MBEIndex(List<MBETrajectory> trajectoryList, double splitPercentage) {
        List<Rectangle> rects = new LinkedList<>();
        for (MBETrajectory mbeTrajectory : trajectoryList) {
            List<MBR> mbrs = mbeTrajectory.greedySplitTrajectory(splitPercentage);
            for (MBR mbr : mbrs) {
                rects.add(new Rectangle(mbeTrajectory.getId(), mbr));
            }
        }
        this.STRtree = new IndexBuilder().createRTreeBySTR(rects.toArray(new Rectangle[rects.size()]));
    }

    public List<Tuple<Integer, Double>> rangeSearch(MBETrajectory query, List<MBETrajectory> trajectoryList, double threshold, double splitPercentage) {
        List<MBR> mbrs = query.greedySplitMBE(splitPercentage);
        List<Integer> candidates = roughlySearch(mbrs);
        List<Integer> filteredCandidates = new LinkedList<>();
        List<Tuple<Integer, Double>> results = new LinkedList<>();
        for (int i = 0; i < candidates.size(); i++) {
            double lowerBound = TrajectoryUtils.calcDTWLowerBound(query, trajectoryList.get(candidates.get(i)), splitPercentage);
            if(lowerBound < threshold) {
                filteredCandidates.add(candidates.get(i));
            }
        }
        for (Integer filteredCandidate : filteredCandidates) {
            double dist = TrajectoryUtils.calcDTWDistanceWithThreshold(query.getTrajectoryData(), trajectoryList.get(filteredCandidate).getTrajectoryData(), threshold);
            if(dist < threshold) {
                results.add(new Tuple<>(filteredCandidate, dist));
            }
        }
        return results;
    }

    public List<Integer> roughlySearch(List<MBR> mbrs){
        HashSet<Integer> candidates = new HashSet<>();
        for (MBR mbr : mbrs) {
            ArrayList<DataObject> dataObjects = STRtree.boxRangeQuery(mbr);
            for (int i = 0; i < dataObjects.size(); i++) {
                candidates.add((int) dataObjects.get(i).getId());
            }
        }
        return new ArrayList<>(candidates);
    }
}
