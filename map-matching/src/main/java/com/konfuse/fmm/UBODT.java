package com.konfuse.fmm;

import com.konfuse.road.Road;
import com.konfuse.road.RoadPoint;
import javafx.scene.chart.ScatterChart;

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


    public static void fileWriterBinary(String path, List<Record> records) {
        File file=new File(path);

        if(file.exists()){
            System.out.println("File created successfully！");
        }else{
            try {
                boolean flag =file.createNewFile();
                if(flag){
                    System.out.println("File created successfully");
                }else{
                    System.out.println("File creation failed");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            int listSize = records.size();
            out.writeInt(listSize);
            int i = 0;
            for (Record record : records) {
                out.writeLong(record.source);
                out.writeLong(record.target);
                out.writeLong(record.first_n);
                out.writeLong(record.prev_n);
                out.writeLong(record.next_e);
                out.writeDouble(record.cost);
            }
            System.out.println("records size: " + records.size());
            System.out.println("file size: "+ file.length()+" Bytes");
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static UBODT fileReaderBinary(String path, int multiplier) {
        List<Record> records = new ArrayList<>();

        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(path)));
            int listSize = in.readInt();
            for (int i = 0; i < listSize; i++) {
                long source = in.readLong();
                long target = in.readLong();
                long first_n = in.readLong();
                long prev_n = in.readLong();
                long next_e = in.readLong();
                double cost = in.readDouble();
                records.add(new Record(source, target, first_n, prev_n, next_e, cost));
            }
            System.out.println("records size: " + records.size());

            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        UBODT ubodt = read(records, multiplier);
        records.clear();
        return ubodt;
    }

    public static UBODT read(List<Record> records, int multiplier) {
        int buckets = find_prime_number(records.size() / LOAD_FACTOR);
        UBODT ubodt = new UBODT(multiplier, buckets);

        System.out.println("records size: " + records.size());
        for (Record record : records) {
            ubodt.insert(record);
        }

        return ubodt;
    }

    public static UBODT fileReader(String path, int multiplier) {
        List<Record> records = new ArrayList<>();

        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(path));
            String line;
            int i = 0;
            while ((line = reader.readLine()) != null) {
                String[] elements = line.split(",");
                long source = Long.parseLong(elements[0]);
                long target = Long.parseLong(elements[1]);
                long first_n = Long.parseLong(elements[2]);
                long prev_n = Long.parseLong(elements[3]);
                long next_e = Long.parseLong(elements[4]);
                double cost = Double.parseDouble(elements[5]);
                records.add(new Record(source, target, first_n, prev_n, next_e, cost));
            }
            System.out.println("records size:" + records.size());
        } catch (IOException e) {
            e.printStackTrace();
        }

        UBODT ubodt =  read(records, multiplier);
        records.clear();
        return ubodt;
    }

    public static void fileWriter(String path, List<Record> records) {
        BufferedWriter writer = null;

        File file=new File(path);

        if(file.exists()){
            System.out.println("created successfully！");
        }else{
            try {
                boolean flag =file.createNewFile();
                if(flag){
                    System.out.println("File created successfully");
                }else{
                    System.out.println("File creation failed");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            writer = new BufferedWriter(new FileWriter(file));
            for (Record record : records) {
                writer.write(record.source + "," + record.target + "," + record.first_n + "," + record.prev_n + "," + record.next_e + "," + record.cost);
                writer.newLine();
            }
            System.out.println("file size: "+ file.length()+" Bytes");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
