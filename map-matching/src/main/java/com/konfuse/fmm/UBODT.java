package com.konfuse.fmm;

import com.konfuse.road.RoadPoint;

import java.util.*;

/**
 * @Auther todd
 * @Date 2020/1/6
 */
public class UBODT {
    private static long multiplier;
    private static int buckets;
    private double delta;
    private Record[] hashtable;

    public UBODT(long multiplier, int buckets) {
        System.out.println("multiplier: " + multiplier);
        System.out.println("buckets: " + buckets);
        UBODT.multiplier = multiplier;
        UBODT.buckets = buckets;
        hashtable = new Record[buckets];
    }

    public double getDelta() {
        return delta;
    }

    public Record lookUp(long source, long target) {
        int index = calBucketIndex(source, target);

        Record r = hashtable[index];

        while(r != null) {
            if(r.source == source && r.target == target) {
                return r;
            } else {
                r = r.next;
            }
        }
        return r;
    }

    public List<Long> lookShortestPath(long source, long target) {
        ArrayList<Long> edges = new ArrayList<>();
        if(source == target) {
            return edges;
        }

        Record r = lookUp(source, target);

        if(r == null){
            return edges;
        }

        while (r.first_n != target) {
            edges.add(r.next_e);
            r = lookUp(r.first_n, target);
        }
        edges.add(r.next_e);
        return edges;
    }

    public List<Long> constructCompletePath(List<RoadPoint> o_path) {
        if(o_path.isEmpty()) {
            return null;
        }

        List<Long> c_path = new ArrayList<>();
        int N = o_path.size();

        c_path.add(o_path.get(0).edge().id());

        for(int i = 0; i < N - 1; i++) {
            RoadPoint a = o_path.get(i);
            RoadPoint b = o_path.get(i + 1);
            if((a.edge().id() != b.edge().id()) || (a.fraction() > b.fraction())){
                List<Long> shortestPathBetweenAB = lookShortestPath(a.edge().target(), b.edge().source());

                if(shortestPathBetweenAB.isEmpty() && a.edge().target() != b.edge().source()){
                    return null;
                }

                c_path.addAll(shortestPathBetweenAB);
                c_path.add(b.edge().id());
            }
        }
        return c_path;
    }

    public TraversedPath constructTraversedPath(List<RoadPoint> o_path) {
        if(o_path.isEmpty()) {
            return null;
        }

        TraversedPath t_path = new TraversedPath();

        int N = o_path.size();
        t_path.addPath(o_path.get(0).edge().id());

        int current_idx = 0;
        t_path.addIndex(current_idx);

        for(int i = 0; i < N - 1; i++) {
            RoadPoint a = o_path.get(i);
            RoadPoint b = o_path.get(i + 1);
            if((a.edge().id() != b.edge().id()) || (a.fraction() > b.fraction())){
                List<Long> shortestPathBetweenAB = lookShortestPath(a.edge().target(), b.edge().source());

                if(shortestPathBetweenAB.isEmpty() && a.edge().target() != b.edge().source()){
                    return null;
                }

                for (Long seg : shortestPathBetweenAB) {
                    t_path.addPath(seg);
                    ++current_idx;
                }
                t_path.addPath(b.edge().id());
                ++current_idx;
            }
            t_path.addIndex(current_idx);
        }
        return t_path;
    }

    public void insert(Record r) {
        int h = calBucketIndex(r.source, r.target);

        r.next = hashtable[h];
        hashtable[h] = r;

        if(r.cost > delta) {
            delta = r.cost;
        }
    }

    public int calBucketIndex(long source, long target){
        return (int)((source * multiplier + target) % buckets);
    }
}
