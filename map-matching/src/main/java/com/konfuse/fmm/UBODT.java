package com.konfuse.fmm;

import com.konfuse.road.Road;
import com.konfuse.road.RoadPoint;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @Auther todd
 * @Date 2020/1/6
 */
public class UBODT {
    private static long multiplier;
    private static int buckets;
    private double delta;
    private Record[] hashtable;
    private final static double LOAD_FACTOR = 2.0;

    private UBODT(long multiplier, int buckets) {
        System.out.println("multiplier: " + multiplier);
        System.out.println("buckets: " + buckets);
        UBODT.multiplier = multiplier;
        UBODT.buckets = buckets;
        hashtable = new Record[buckets];
    }

//    public static void construct(HashMap<Long, Road> nodes, double max) {
//        BufferedWriter writer = null;
//        try {
//            writer = new BufferedWriter(new FileWriter("UBODT.txt"));
//            writer.write(source + "," + entry.nodeId + "," + first_n + "," + prev_n + "," + next_e + "," + entry.cost);
//            writer.newLine();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (writer != null)
//                    writer.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    public static UBODT read(List<Record> records, int multiplier) {
        int buckets = find_prime_number(records.size() / LOAD_FACTOR);
        UBODT ubodt = new UBODT(multiplier, buckets);

        System.out.println("records size: " + records.size());
        for (Record record : records) {
            ubodt.insert(record);
        }

        return ubodt;
    }

    public static UBODT read(String path, int multiplier) {
        List<Record> records = new ArrayList<>();

        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(path));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] elements = line.split(":");
                long source = Long.parseLong(elements[0]);
                long target = Long.parseLong(elements[1]);
                long first_n = Long.parseLong(elements[2]);
                long prev_n = Long.parseLong(elements[3]);
                long next_e = Long.parseLong(elements[4]);
                double cost = Double.parseDouble(elements[5]);
                records.add(new Record(source, target, first_n, prev_n, next_e, cost));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        UBODT ubodt =  read(records, multiplier);
        records.clear();
        return ubodt;
    }

    private static int find_prime_number(double value) {
        int[] prime_numbers = {
            5003,10039,20029,50047,100669,200003,500000,
            1000039,2000083,5000101,10000103,20000033
        };

        int N = prime_numbers.length;
        for (int prime_number : prime_numbers) {
            if (value <= prime_number) {
                return prime_number;
            }
        }

        return prime_numbers[N - 1];
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
            if((a.edge().id() != b.edge().id()) || (a.fraction() > b.fraction())) {
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
