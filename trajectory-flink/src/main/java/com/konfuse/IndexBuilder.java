package com.konfuse;

import com.konfuse.geometry.Line;
import com.konfuse.geometry.MBR;
import com.konfuse.geometry.PartitionedMBR;
import com.konfuse.geometry.Point;
import com.konfuse.geopartitioner.LineSTRPartitioner;
import com.konfuse.geopartitioner.PointSTRPartitioner;
import com.konfuse.internal.*;
import org.apache.flink.api.common.functions.*;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.DataSetUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Helps to build an r-tree in flink, using global-local structure.
 * Using two main methods to build r-tree: createLineIndex and createPointIndex
 * which return a index for point and a index for line, respectively.
 *
 * @Author: Konfuse
 * @Date: 2019/12/6 15:33
 */
public class IndexBuilder implements Serializable {
    /**
     * Building method for line.
     *
     * First use the specified partition method to set up a global index based on the sampled data
     * for this data. This step is to set up a partition for the global index.
     *
     * Secondly allocate partitions for each input data and create
     * local indexes in the corresponding partition.
     *
     * Finally create a global index for all local indexes.
     *
     * @param data data set of input lines
     * @param sampleRate sample rate for input data to build a global index for partition
     * @param parallelism is the number of parallel programs specified
     * @param maxNodePerEntry maximum number of entities for each node of the completed index
     * @param minNodePerEntry minimum number of entities for each node of the completed index
     * @return completed index for line
     */
    public LineIndex createLineIndex(DataSet<Line> data, final double sampleRate, int parallelism, final int maxNodePerEntry, final int minNodePerEntry) throws Exception {
        // Step 1: create MBR and STRPartitioner based on sampled data
        LineSTRPartitioner partitioner = this.createLineSTRPartitioner(data, sampleRate, parallelism, maxNodePerEntry, minNodePerEntry);

        // Partition data
        DataSet<Line> partitionedData = data.partitionCustom(partitioner, new KeySelector<Line, Line>() {
            @Override
            public Line getKey(Line line) throws Exception {
                return line;
            }
        });

        // Step 2: build local RTrees
        DataSet<RTree<Line>> localRTree = partitionedData.mapPartition(new RichMapPartitionFunction<Line, RTree<Line>>() {
            @Override
            public void mapPartition(Iterable<Line> iterable, Collector<RTree<Line>> collector) throws Exception {
                ArrayList<Line> lines = new ArrayList<>();
                for (Line line : iterable) {
                    lines.add(line);
                }

                if(!lines.isEmpty()){
                    RTree<Line> rTree = createLineLocalRTree(lines, maxNodePerEntry, minNodePerEntry);
                    collector.collect(rTree);
                }
            }
        });

        // Step 3: build global RTree
        DataSet<RTree<PartitionedMBR>> globalRTree = localRTree
                .map(new RichMapFunction<RTree<Line>, Tuple2<Integer, RTree<Line>>>() {
                    @Override
                    public Tuple2<Integer, RTree<Line>> map(RTree<Line> rTree) throws Exception {
                        //get the partition number where local r-tree belongs to
                        return new Tuple2<>(getRuntimeContext().getIndexOfThisSubtask(), rTree);
                    }
                })
                .reduceGroup(new RichGroupReduceFunction<Tuple2<Integer, RTree<Line>>, RTree<PartitionedMBR>>() {
                    @Override
                    public void reduce(Iterable<Tuple2<Integer, RTree<Line>>> iterable, Collector<RTree<PartitionedMBR>> collector) throws Exception {
                        Iterator<Tuple2<Integer, RTree<Line>>> iter = iterable.iterator();
                        ArrayList<PartitionedMBR> partitionedMBRList = new ArrayList<>();
                        while (iter.hasNext()) {
                            Tuple2<Integer, RTree<Line>> tuple = iter.next();
                            RTree<Line> rtree = tuple.f1;
                            // create partitioned mbr for each local r-tree
                            PartitionedMBR partitionedMBR = new PartitionedMBR(rtree.getRoot().getMBR(), tuple.f0, rtree.getEntryCount());
                            partitionedMBRList.add(partitionedMBR);
                        }
                        // organize the information of the partition where each r-tree is located to create a global index
                        RTree<PartitionedMBR> globalTree = createGlobalRTree(partitionedMBRList, maxNodePerEntry, minNodePerEntry);
                        collector.collect(globalTree);
                    }
                });

        return new LineIndex(globalRTree, localRTree, partitioner, partitionedData);
    }

    /**
     * Building method for point.
     *
     * First use the specified partition method to set up a global index based on the sampled data
     * for this data. This step is to set up a partition for the global index.
     *
     * Secondly allocate partitions for each input data and create
     * local indexes in the corresponding partition.
     *
     * Finally create a global index for all local indexes.
     *
     * @param data data set of input points
     * @param sampleRate sample rate for input data to build a global index for partition
     * @param parallelism is the number of parallel programs specified
     * @param maxNodePerEntry maximum number of entities for each node of the completed index
     * @param minNodePerEntry minimum number of entities for each node of the completed index
     * @return completed index for point
     */
    public PointIndex createPointIndex(DataSet<Point> data, final double sampleRate, int parallelism, final int maxNodePerEntry, final int minNodePerEntry) throws Exception {
        // Step 1: create MBR and STRPartitioner based on sampled data
        PointSTRPartitioner partitioner = this.createPointSTRPartitioner(data, sampleRate, parallelism, maxNodePerEntry, minNodePerEntry);

        // Partition data
        DataSet<Point> partitionedData = data.partitionCustom(partitioner, new KeySelector<Point, Point>() {
            @Override
            public Point getKey(Point point) throws Exception {
                return point;
            }
        });

        // Step 2: build local RTree
        DataSet<RTree<Point>> localRTree = partitionedData.mapPartition(new RichMapPartitionFunction<Point, RTree<Point>>() {
            @Override
            public void mapPartition(Iterable<Point> iterable, Collector<RTree<Point>> collector) throws Exception {
                ArrayList<Point> points = new ArrayList<>();
                for (Point point : iterable) {
                    points.add(point);
                }

                if(!points.isEmpty()){
                    RTree<Point> rTree = createPointLocalRTree(points, maxNodePerEntry, minNodePerEntry);
                    collector.collect(rTree);
                }
            }
        });

        // Step 3: build global RTree
        DataSet<RTree<PartitionedMBR>> globalRTree = localRTree
                .map(new RichMapFunction<RTree<Point>, Tuple2<Integer, RTree<Point>>>() {
                    @Override
                    public Tuple2<Integer, RTree<Point>> map(RTree<Point> rTree) throws Exception {
                        //get the partition number where local r-tree belongs to
                        return new Tuple2<>(getRuntimeContext().getIndexOfThisSubtask(), rTree);
                    }
                })
                .reduceGroup(new RichGroupReduceFunction<Tuple2<Integer, RTree<Point>>, RTree<PartitionedMBR>>() {
                    @Override
                    public void reduce(Iterable<Tuple2<Integer,  RTree<Point>>> iterable, Collector<RTree<PartitionedMBR>> collector) throws Exception {
                        Iterator<Tuple2<Integer,  RTree<Point>>> iter = iterable.iterator();
                        ArrayList<PartitionedMBR> partitionedMBRList = new ArrayList<>();
                        while (iter.hasNext()) {
                            Tuple2<Integer,  RTree<Point>> tuple = iter.next();
                            RTree<Point> rtree = tuple.f1;
                            // create partitioned mbr for each local r-tree
                            PartitionedMBR partitionedMBR = new PartitionedMBR(rtree.getRoot().getMBR(), tuple.f0, rtree.getEntryCount());
                            partitionedMBRList.add(partitionedMBR);
                        }
                        // organize the information of the partition where each r-tree is located to create a global index
                        RTree<PartitionedMBR> globalTree = createGlobalRTree(partitionedMBRList, maxNodePerEntry, minNodePerEntry);
                        collector.collect(globalTree);
                    }
                });

        return new PointIndex(globalRTree, localRTree, partitioner, partitionedData);
    }

    /**
     * Create a corresponding partition method based on the sampled data.
     * Assign data to the nearest partition in space.
     *
     * @param data data set of input lines
     * @param sampleRate sample rate for input data to build a global index for partition
     * @param parallelism is the number of parallel programs specified
     * @param maxNodePerEntry maximum number of entities for each node of the completed index
     * @param minNodePerEntry minimum number of entities for each node of the completed index
     * @return partition method based on the sampled data.
     */
    private LineSTRPartitioner createLineSTRPartitioner(DataSet<Line> data, double sampleRate, int parallelism, final int maxNodePerEntry, final int minNodePerEntry) throws Exception {
        // create boundary for whole dataset
        // Tuple2<MBR, number of data object>
        DataSet<Tuple2<MBR, Integer>> globalBoundDS = data
                .map(new MapFunction<Line, Tuple2<MBR, Integer>>() {
                    @Override
                    public Tuple2<MBR, Integer> map(Line line) throws Exception {
                        MBR mbr = line.mbr();
                        return new Tuple2<>(mbr, 1);
                    }
                })
                .reduce(new ReduceFunction<Tuple2<MBR, Integer>>() {
                    @Override
                    public Tuple2<MBR, Integer> reduce(Tuple2<MBR, Integer> mbrIntegerTuple2, Tuple2<MBR, Integer> t1) throws Exception {
                        return new Tuple2<>(MBR.union(mbrIntegerTuple2.f0, t1.f0), mbrIntegerTuple2.f1 + t1.f1);
                    }
                });

        // calculate number of MBRs on each dimension
        // the idea is the total number of mbr over all dimensions will be equal to parallelism
        // parallelism is p in the algorithm
        // s is the due slice count of each dimension, i.e. s = Math.sqrt(p)
        int s = (int) Math.ceil(Math.pow(Math.E, Math.log(parallelism) / 2));

        // Sample data
        final DataSet<Line> sampleData = DataSetUtils.sample(data, false, sampleRate);

        // create r-tree containing mbr information for sampled data
        DataSet<RTree<PartitionedMBR>> trees = sampleData.reduceGroup(new RichGroupReduceFunction<Line, RTree<PartitionedMBR>>() {
            private MBR globalBound;

            @Override
            public void open(Configuration parameters) throws Exception {
                super.open(parameters);
                this.globalBound = ((Tuple2<MBR, Integer>)this.getRuntimeContext().getBroadcastVariable("globalBoundDS").get(0)).f0;
            }

            @Override
            public void reduce(Iterable<Line> iterable, Collector<RTree<PartitionedMBR>> collector) throws Exception {
                ArrayList<Line> sampleLines = new ArrayList<>();
                for (Line line : iterable) {
                    sampleLines.add(line);
                }

                // From MBR, create partition points (MBR, partition). The idea is to distribute each MBR to each partition
                ArrayList<PartitionedMBR> partitionedMBRs = packLinesIntoMBR(sampleLines, s, this.globalBound);
                for (int i = 0; i < partitionedMBRs.size(); i++) {
                    // TODO: i% MBR to make sure, partitionnumber is not out of index bound
                    partitionedMBRs.get(i).setPartitionNumber(i % parallelism);
                }
                RTree<PartitionedMBR> rTree = createGlobalRTree(partitionedMBRs, maxNodePerEntry, minNodePerEntry);
                collector.collect(rTree);
            }
        }).withBroadcastSet(globalBoundDS, "globalBoundDS");

        return new LineSTRPartitioner(trees.collect().get(0));
    }

    /**
     * Create a corresponding partition method based on the sampled data.
     * Assign data to the nearest partition in space.
     *
     * @param data data set of input points
     * @param sampleRate sample rate for input data to build a global index for partition
     * @param parallelism is the number of parallel programs specified
     * @param maxNodePerEntry maximum number of entities for each node of the completed index
     * @param minNodePerEntry minimum number of entities for each node of the completed index
     * @return partition method based on the sampled data.
     */
    private PointSTRPartitioner createPointSTRPartitioner(DataSet<Point> data, double sampleRate, final int parallelism, final int maxNodePerEntry, final int minNodePerEntry) throws Exception {
        // create boundary for whole dataset
        // Tuple2<MBR, number of data object>
        DataSet<Tuple2<MBR, Integer>> globalBoundDS = data
                .map(new MapFunction<Point, Tuple2<MBR, Integer>>() {
                    @Override
                    public Tuple2<MBR, Integer> map(Point point) throws Exception {
                        MBR mbr = new MBR(point.getX(), point.getY(), point.getX(), point.getY());
                        return new Tuple2<>(mbr, 1);
                    }
                })
                .reduce(new ReduceFunction<Tuple2<MBR, Integer>>() {
                    @Override
                    public Tuple2<MBR, Integer> reduce(Tuple2<MBR, Integer> mbrIntegerTuple2, Tuple2<MBR, Integer> t1) throws Exception {
                        return new Tuple2<>(MBR.union(mbrIntegerTuple2.f0, t1.f0), mbrIntegerTuple2.f1 + t1.f1);
                    }
                });

        // calculate number of MBRs on each dimension
        // the idea is the total number of mbr over all dimensions will be equal to parallelism
        // parallelism is p in the algorithm
        // s is the due slice count of each dimension, i.e. s = Math.sqrt(p)
        int s = (int) Math.ceil(Math.pow(Math.E, Math.log(parallelism) / 2));

        // Sample data
        final DataSet<Point> sampleData = DataSetUtils.sample(data, false, sampleRate);

        // create r-tree containing mbr information for sampled data
        DataSet<RTree<PartitionedMBR>> trees = sampleData.reduceGroup(new RichGroupReduceFunction<Point, RTree<PartitionedMBR>>() {
            private MBR globalBound;

            @Override
            public void open(Configuration parameters) throws Exception {
                super.open(parameters);
                this.globalBound = ((Tuple2<MBR,Integer>)this.getRuntimeContext().getBroadcastVariable("globalBoundDS").get(0)).f0;
            }

            @Override
            public void reduce(Iterable<Point> iterable, Collector<RTree<PartitionedMBR>> collector) throws Exception {
                ArrayList<Point> samplePoints = new ArrayList<>();
                for (Point point : iterable) {
                    samplePoints.add(point);
                }

                // From MBR, create partition points (MBR, partition). The idea is to distribute each MBR to each partition
                ArrayList<PartitionedMBR> partitionedMBRs = packPointsIntoMBR(samplePoints, s, this.globalBound);
                for (int i = 0; i < partitionedMBRs.size(); i++) {
                    // TODO: i% MBR to make sure, partitionnumber is not out of index bound
                    partitionedMBRs.get(i).setPartitionNumber(i % parallelism);
                }
                RTree<PartitionedMBR> rTree = createGlobalRTree(partitionedMBRs, maxNodePerEntry, minNodePerEntry);
                collector.collect(rTree);
            }
        }).withBroadcastSet(globalBoundDS, "globalBoundDS");

        return new PointSTRPartitioner(trees.collect().get(0));
    }

    /**
     * Build global r-tree that contains partitioned mbr information using STR method.
     *
     * r is the total count of records, i.e. entries.size()
     * M is the maximum capacity of each partition
     * p is the total count of partitions, i.e. p = r / M
     * s is the due slice count of each dimension, i.e. s = Math.sqrt(r / M)
     * ctr is records traveling count
     *
     * @param mbrList the partitioned mbrs list
     * @param maxNodePerEntry maximum number of entities for each node of the completed index, i.e. M
     * @param minNodePerEntry minimum number of entities for each node of the completed index, i.e. m
     * @return global r-tree index for partitioned mbrs
     */
    private RTree<PartitionedMBR> createGlobalRTree(ArrayList<PartitionedMBR> mbrList, int maxNodePerEntry, int minNodePerEntry) {
        //calculate leaf node num
        double p = mbrList.size() * 1.0 / maxNodePerEntry;

        //start building r-tree leaf node
        mbrList.sort(new PartitionedMBR.PartitionedMBRComparator(1, true));

        // get entry count
        long entryCount = 0L;
        MBR[] mbrs = new MBR[mbrList.size()];
        for (int i = 0; i < mbrList.size(); i++) {
            mbrs[i] = mbrList.get(i).getMBR();
            entryCount += mbrList.get(i).getEntryCount();
        }

        // if size is less than M, then pack the root directly.
        if (mbrList.size() <= maxNodePerEntry) {
            TreeNode root = new PartitionedLeafNode(mbrList, MBR.union(mbrs));
            return new RTree<>(root, maxNodePerEntry, minNodePerEntry, entryCount);
        }

        int s = (int) Math.ceil(Math.pow(Math.E, Math.log(p) / 2));

        ArrayList<PartitionedMBR> list = new ArrayList<>();
        ArrayList<TreeNode> nextLevel = new ArrayList<>();

        int ctr = 0;
        for (PartitionedMBR partitionedMBR : mbrList) {
            list.add(partitionedMBR);
            ++ctr;
            if (ctr == s * maxNodePerEntry) {
                packPartitionedMBR(list, nextLevel, maxNodePerEntry, minNodePerEntry);
                list.clear();
                ctr = 0;
            }
        }
        // the size of the last slide may be lower than s * M
        if(list.size() > 0) {
            packPartitionedMBR(list, nextLevel, maxNodePerEntry, minNodePerEntry);
            list.clear();
        }
        TreeNode root = leafNodePacking(nextLevel, maxNodePerEntry, minNodePerEntry);
        return new RTree<>(root, maxNodePerEntry, minNodePerEntry, entryCount);
    }

    /**
     * Build local r-tree that contains lines information using STR method.
     *
     * r is the total count of records, i.e. data.size()
     * M is the maximum capacity of each partition
     * p is the total count of partitions, i.e. p = r / M
     * s is the due slice count of each dimension, i.e. s = Math.sqrt(r / M)
     * ctr is records traveling count
     *
     * @param lines the input lines list
     * @param maxNodePerEntry maximum number of entities for each node of the completed index, i.e. M
     * @param minNodePerEntry minimum number of entities for each node of the completed index, i.e. m
     * @return local r-tree index for lines
     */
    private RTree<Line> createLineLocalRTree(ArrayList<Line> lines, int maxNodePerEntry, int minNodePerEntry) {
        //calculate leaf node num
        double p = lines.size() * 1.0 / maxNodePerEntry;

        //start building r-tree leaf node
        lines.sort(new MBR.MBRComparatorWithLine(1, true));

        // get entry count
        long entryCount = lines.size();

        // if size is less than M, then pack the root directly.
        if (lines.size() <= maxNodePerEntry) {
            TreeNode root = new LeafNode<>(lines, Line.unionLines(lines));
            return new RTree<>(root, maxNodePerEntry, minNodePerEntry, entryCount);
        }

        int s = (int) Math.ceil(Math.pow(Math.E, Math.log(p) / 2));
        ArrayList<Line> list = new ArrayList<>();
        ArrayList<TreeNode> nextLevel = new ArrayList<TreeNode>();

        int ctr = 0;
        for (Line line : lines) {
            list.add(line);
            ++ctr;
            if (ctr == s * maxNodePerEntry) {
                packLines(list, nextLevel, maxNodePerEntry, minNodePerEntry);
                list.clear();
                ctr = 0;
            }
        }
        // the size of the last slide may be lower than s * M
        if(list.size() > 0) {
            packLines(list, nextLevel, maxNodePerEntry, minNodePerEntry);
            list.clear();
        }
        TreeNode root = leafNodePacking(nextLevel, maxNodePerEntry, minNodePerEntry);
        return new RTree<>(root, maxNodePerEntry, minNodePerEntry, entryCount);
    }

    /**
     * Build local r-tree that contains points information using STR method.
     *
     * r is the total count of records, i.e. data.size()
     * M is the maximum capacity of each partition
     * p is the total count of partitions, i.e. p = r / M
     * s is the due slice count of each dimension, i.e. s = Math.sqrt(r / M)
     * ctr is records traveling count
     *
     * @param points the input points list
     * @param maxNodePerEntry maximum number of entities for each node of the completed index, i.e. M
     * @param minNodePerEntry minimum number of entities for each node of the completed index, i.e. m
     * @return local r-tree index for points
     */
    private RTree<Point> createPointLocalRTree(ArrayList<Point> points, int maxNodePerEntry, int minNodePerEntry) {
        //calculate leaf node num
        double p = points.size() * 1.0 / maxNodePerEntry;

        //start building r-tree leaf node
        points.sort(new Point.PointComparator(1));

        // get entry count
        long entryCount = points.size();

        // if size is less than M, then pack the root directly.
        if (points.size() <= maxNodePerEntry) {
            TreeNode root = new LeafNode<>(points, Point.unionPoints(points));
            return new RTree<>(root, maxNodePerEntry, minNodePerEntry, entryCount);
        }

        int s = (int) Math.ceil(Math.pow(Math.E, Math.log(p) / 2));
        ArrayList<Point> list = new ArrayList<>();
        ArrayList<TreeNode> nextLevel = new ArrayList<>();

        int ctr = 0;
        for (Point point : points) {
            list.add(point);
            ++ctr;
            if (ctr == s * maxNodePerEntry) {
                packPoints(list, nextLevel, maxNodePerEntry, minNodePerEntry);
                list.clear();
                ctr = 0;
            }
        }
        // the size of the last slide may be lower than s * M
        if(list.size() > 0) {
            packPoints(list, nextLevel, maxNodePerEntry, minNodePerEntry);
            list.clear();
        }
        TreeNode root = leafNodePacking(nextLevel, maxNodePerEntry, minNodePerEntry);
        return new RTree<>(root, maxNodePerEntry, minNodePerEntry, entryCount);
    }

    /**
     * Pack the sampled lines to partitioned mbrs to be used for partitioning.
     *
     * r is the total count of records, i.e. data.size()
     * p is the total count of partitions, i.e. parallelism.
     * M here is determined by parallelism, i.e. M = r / parallelism.
     * s is the due slice count of each dimension, i.e. s = Math.sqrt(parallelism)
     * ctr is records traveling count
     *
     * @param data list of lines
     * @param s the due slice count of each dimension
     * @param globalBound global boundary
     * @return the partitioned mbrs
     */
    private ArrayList<PartitionedMBR> packLinesIntoMBR(ArrayList<Line> data, int s, MBR globalBound) {
        // sort data by x first
        // s is the due slice count of each dimension, i.e. s = Math.sqrt(p)
        // slideCapacity is the capacity of each slide
        data.sort(new Line.LineComparatorByCenter(1));
        int slideCapacity = (int) Math.ceil(data.size() * 1.0 / s);
        int nodeCapacity = (int) Math.ceil(data.size() * 1.0 / s / s);

        ArrayList<PartitionedMBR> partitionedMBRs = new ArrayList<>();

        // sliceBound is the record of bound of slice
        MBR sliceBound = new MBR();
        sliceBound.setY1(globalBound.getY1());
        sliceBound.setY2(globalBound.getY2());

        for (int i = 0; i < s; i++) {
            // calculate index of group in data
            int startIndex = i * slideCapacity;
            int endIndex = (i + 1) * slideCapacity - 1;

            if (i == s - 1) {
                endIndex = data.size() - 1;
            }

            sliceBound.setX1(sliceBound.getX2());
            sliceBound.setX2(data.get(endIndex).getCenterPoint().getX());

            if (i == 0) {
                // for group 0, lower bound = min bound of global data
                // upper bound = last element of group
                sliceBound.setX1(globalBound.getX1());
            }

            if (i == s - 1) {
                // for last group, lower bound = upper bound of previous group
                // upper bound = max bound of global data
                sliceBound.setX2(globalBound.getX2());
            }

            ArrayList<Line> list = new ArrayList<>(data.subList(startIndex, endIndex + 1));
            packSampleLines(list, partitionedMBRs, sliceBound, s, nodeCapacity);
        }

        return partitionedMBRs;
    }

    /**
     * Pack the sampled points to partitioned mbrs to be used for partitioning.
     *
     * r is the total count of records, i.e. data.size()
     * p is the total count of partitions, i.e. parallelism.
     * M here is determined by parallelism, i.e. M = r / parallelism.
     * s is the due slice count of each dimension, i.e. s = Math.sqrt(parallelism)
     * ctr is records traveling count
     *
     * @param data list of points
     * @param s the due slice count of each dimension
     * @param globalBound global boundary
     * @return the partitioned mbrs
     */
    private ArrayList<PartitionedMBR> packPointsIntoMBR(ArrayList<Point> data, int s, MBR globalBound) {
        // sort data by x first
        // s is the due slice count of each dimension, i.e. s = Math.sqrt(p)
        // slideCapacity is the capacity of each slide
        data.sort(new Point.PointComparator(1));
        int slideCapacity = (int) Math.ceil(data.size() * 1.0 / s);
        int nodeCapacity = (int) Math.ceil(data.size() * 1.0 / s / s);

        ArrayList<PartitionedMBR> partitionedMBRs = new ArrayList<>();

        // sliceBound is the record of bound of slice
        MBR sliceBound = new MBR();
        sliceBound.setY1(globalBound.getY1());
        sliceBound.setY2(globalBound.getY2());

        for (int i = 0; i < s; i++) {
            // calculate index of group in data
            int startIndex = i * slideCapacity;
            int endIndex = (i + 1) * slideCapacity - 1;

            if (i == s - 1) {
                endIndex = data.size() - 1;
            }

            sliceBound.setX1(sliceBound.getX2());
            sliceBound.setX2(data.get(endIndex).getX());

            if (i == 0) {
                // for group 0, lower bound = min bound of global data
                // upper bound = last element of group
                sliceBound.setX1(globalBound.getX1());
            }

            if (i == s - 1) {
                // for last group, lower bound = upper bound of previous group
                // upper bound = max bound of global data
                sliceBound.setX2(globalBound.getX2());
            }

            ArrayList<Point> list = new ArrayList<>(data.subList(startIndex, endIndex + 1));
            packSamplePoints(list, partitionedMBRs, sliceBound, s, nodeCapacity);
        }

        return partitionedMBRs;
    }

    /**
     * Pack lines to leaf nodes.
     *
     * @param lines the sorted lines by x dimension in a slide.
     * @param nextLevel the list to load leaf nodes.
     * @param maxNodePerEntry maximum number of entities for each node of the completed index
     * @param minNodePerEntry minimum number of entities for each node of the completed index
     */
    private void packLines(ArrayList<Line> lines, ArrayList<TreeNode> nextLevel, int maxNodePerEntry, int minNodePerEntry) {
        // sort by the y dimension
        lines.sort(new MBR.MBRComparatorWithLine(2, true));

        // pack lines to leaf nodes
        LeafNode<Line> leafNode = new LeafNode<>();
        for (Line line : lines) {
            leafNode.getEntries().add(line);
            if (leafNode.getEntries().size() == maxNodePerEntry) {
                leafNode.setMBR(Line.unionLines(leafNode.getEntries()));
                nextLevel.add(leafNode);
                leafNode = new LeafNode<>();
            }
        }

        // the size of the last leaf node may be lower than m.
        // add records into it from neighbor node until the last node no less than m.
        if (leafNode.getEntries().size() > 0) {
            if (leafNode.getEntries().size() < minNodePerEntry) {
                LeafNode<Line> swapped = (LeafNode<Line>) nextLevel.get(nextLevel.size() - 1);
                ArrayList<Line> swappedLines = swapped.getEntries();
                ArrayList<Line> lastLines = leafNode.getEntries();
                while (leafNode.getEntries().size() < minNodePerEntry) {
                    lastLines.add(0, swappedLines.remove(swappedLines.size() - 1));
                }
            }
            leafNode.setMBR(Line.unionLines(leafNode.getEntries()));
            nextLevel.add(leafNode);
        }
    }

    /**
     * Pack points to leaf nodes.
     *
     * @param points the sorted points by x dimension in a slide.
     * @param nextLevel the list to load leaf nodes.
     * @param maxNodePerEntry maximum number of entities for each node of the completed index
     * @param minNodePerEntry minimum number of entities for each node of the completed index
     */
    private void packPoints(ArrayList<Point> points, ArrayList<TreeNode> nextLevel, int maxNodePerEntry, int minNodePerEntry) {
        // sort by the y dimension
        points.sort(new Point.PointComparator(2));

        // pack points to leaf nodes
        LeafNode<Point> leafNode = new LeafNode<>();
        for (Point point : points) {
            leafNode.getEntries().add(point);
            if (leafNode.getEntries().size() == maxNodePerEntry) {
                leafNode.setMBR(Point.unionPoints(leafNode.getEntries()));
                nextLevel.add(leafNode);
                leafNode = new LeafNode<>();
            }
        }

        // the size of the last leaf node may be lower than m.
        // add records into it from neighbor node until the last node no less than m.
        if (leafNode.getEntries().size() > 0) {
            if (leafNode.getEntries().size() < minNodePerEntry) {
                LeafNode<Point> swapped = (LeafNode<Point>) nextLevel.get(nextLevel.size() - 1);
                ArrayList<Point> swappedPoints = swapped.getEntries();
                ArrayList<Point> lastPoints = leafNode.getEntries();
                while (leafNode.getEntries().size() < minNodePerEntry) {
                    lastPoints.add(0, swappedPoints.remove(swappedPoints.size() - 1));
                }
            }
            leafNode.setMBR(Point.unionPoints(leafNode.getEntries()));
            nextLevel.add(leafNode);
        }
    }

    /**
     * Pack lines to partitioned mbrs.
     *
     * @param data the sorted lines by x dimension in a slide.
     * @param partitionedMBRs the list to load partitioned mbrs.
     * @param sliceBound slice bound of the completed index.
     * @param s slice count
     * @param nodeCapacity capacity of each node in the completed index
     */
    private void packSampleLines(ArrayList<Line> data, ArrayList<PartitionedMBR> partitionedMBRs, MBR sliceBound, int s, int nodeCapacity) {
        // sort by the y dimension
        data.sort(new Line.LineComparatorByCenter(2));

        // nodeBound is the record of bound of node
        MBR nodeBound = new MBR();
        nodeBound.setX1(sliceBound.getX1());
        nodeBound.setX2(sliceBound.getX2());

        for (int i = 0; i < s; i++) {
            // calculate index of node in data
            int startIndex = i * nodeCapacity;
            int endIndex = (i + 1) * nodeCapacity - 1;

            if (i == s - 1) {
                endIndex = data.size() - 1;
            }

            nodeBound.setY1(nodeBound.getY2());
            nodeBound.setY2(data.get(endIndex).getCenterPoint().getY());

            if (i == 0) {
                // for group 0, lower bound = min bound of global data
                // upper bound = last element of group
                nodeBound.setY1(sliceBound.getY1());
            }

            if (i == s - 1) {
                // for last group, lower bound = upper bound of previous group
                // upper bound = max bound of global data
                nodeBound.setY2(sliceBound.getY2());
            }
            partitionedMBRs.add(new PartitionedMBR(new MBR(nodeBound), 0, endIndex - startIndex + 1));
        }
    }

    /**
     * Pack points to partitioned mbrs.
     *
     * @param data the sorted points by x dimension in a slide.
     * @param partitionedMBRs the list to load partitioned mbrs.
     * @param sliceBound slice bound of the completed index
     * @param s slice count
     * @param nodeCapacity capacity of each node in the completed index
     */
    private void packSamplePoints(ArrayList<Point> data, ArrayList<PartitionedMBR> partitionedMBRs, MBR sliceBound, int s, int nodeCapacity) {
        // sort by the y dimension
        data.sort(new Point.PointComparator(2));

        // nodeBound is the record of bound of node
        MBR nodeBound = new MBR();
        nodeBound.setX1(sliceBound.getX1());
        nodeBound.setX2(sliceBound.getX2());
        for (int i = 0; i < s; i++) {
            // calculate index of node in data
            int startIndex = i * nodeCapacity;
            int endIndex = (i + 1) * nodeCapacity - 1;

            if (i == s - 1) {
                endIndex = data.size() - 1;
            }

            nodeBound.setY1(nodeBound.getY2());
            nodeBound.setY2(data.get(endIndex).getY());

            if (i == 0) {
                // for group 0, lower bound = min bound of global data
                // upper bound = last element of group
                nodeBound.setY1(sliceBound.getY1());
            }

            if (i == s - 1) {
                // for last group, lower bound = upper bound of previous group
                // upper bound = max bound of global data
                nodeBound.setY2(sliceBound.getY2());
            }
            partitionedMBRs.add(new PartitionedMBR(new MBR(nodeBound), 0, endIndex - startIndex + 1));
        }
    }

    /**
     * Pack partitioned mbrs to leaf nodes.
     *
     * @param partitionedMBRs the sorted partitioned mbrs by x dimension in a slide.
     * @param nextLevel the list to load leaf nodes.
     * @param maxNodePerEntry maximum number of entities for each node of the completed index
     * @param minNodePerEntry minimum number of entities for each node of the completed index
     */
    private void packPartitionedMBR(ArrayList<PartitionedMBR> partitionedMBRs, ArrayList<TreeNode> nextLevel, int maxNodePerEntry, int minNodePerEntry) {
        // sort by the y dimension
        partitionedMBRs.sort(new PartitionedMBR.PartitionedMBRComparator(2, true));

        // pack PartitionedMBR to leaf nodes
        PartitionedLeafNode leafNode = new PartitionedLeafNode();
        for (PartitionedMBR partitionedMBR : partitionedMBRs) {
            leafNode.getEntries().add(partitionedMBR);
            if (leafNode.getEntries().size() == maxNodePerEntry) {
                ArrayList<PartitionedMBR> list = leafNode.getEntries();
                MBR[] mbrs = new MBR[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    mbrs[i] = list.get(i).getMBR();
                }
                leafNode.setMBR(MBR.union(mbrs));
                nextLevel.add(leafNode);
                leafNode = new PartitionedLeafNode();
            }
        }

        // the size of the last leaf node may be lower than m.
        // add records into it from neighbor node until the last node no less than m.
        if (leafNode.getEntries().size() > 0) {
            if (leafNode.getEntries().size() < minNodePerEntry) {
                PartitionedLeafNode swapped = (PartitionedLeafNode) nextLevel.get(nextLevel.size() - 1);
                ArrayList<PartitionedMBR> swappedPartitionedMBRs = swapped.getEntries();
                ArrayList<PartitionedMBR> lastPartitionedMBR = leafNode.getEntries();
                while (leafNode.getEntries().size() < minNodePerEntry) {
                    lastPartitionedMBR.add(0, swappedPartitionedMBRs.remove(swappedPartitionedMBRs.size() - 1));
                }
            }
            ArrayList<PartitionedMBR> list = leafNode.getEntries();
            MBR[] mbrs = new MBR[list.size()];
            for (int i = 0; i < list.size(); i++) {
                mbrs[i] = list.get(i).getMBR();
            }
            leafNode.setMBR(MBR.union(mbrs));
            nextLevel.add(leafNode);
        }
    }

    /**
     * Pack nodes to non-leaf nodes.
     *
     * @param height current height of the building tree, stored as height[0].
     * @param list the sorted nodes by x dimension in a slide.
     * @param nextLevel the list to load non-leaf nodes.
     * @param maxNodePerEntry maximum number of entities for each node of the completed index
     * @param minNodePerEntry minimum number of entities for each node of the completed index
     */
    private void packNodes(ArrayList<TreeNode> list, ArrayList<TreeNode> nextLevel, final int maxNodePerEntry, final int minNodePerEntry, int[] height) {
        // sort by the y dimension
        list.sort(new MBR.MBRComparatorWithTreeNode(2, true));

        // pack nodes to non-leaf nodes
        NonLeafNode nonLeafNode = new NonLeafNode(maxNodePerEntry, height[0] + 1);
        for (TreeNode treeNode : list) {
            nonLeafNode.getChildNodes().add(treeNode);
            if (nonLeafNode.getChildNodes().size() == maxNodePerEntry) {
                nonLeafNode.setMBR(MBR.union(nonLeafNode.getMBRs()));
                nextLevel.add(nonLeafNode);
                nonLeafNode = new NonLeafNode(maxNodePerEntry, height[0] + 1);
            }
        }

        // the size of the last node may be lower than m.
        // add records into it from neighbor node until the last node's size no less than m.
        if (nonLeafNode.getChildNodes().size() > 0) {
            if (nonLeafNode.getChildNodes().size() < minNodePerEntry) {
                NonLeafNode swapped = (NonLeafNode) nextLevel.get(nextLevel.size() - 1);
                ArrayList<TreeNode> lastTreeNodes = nonLeafNode.getChildNodes();
                ArrayList<TreeNode> swappedTreeNodes = swapped.getChildNodes();
                while (nonLeafNode.getChildNodes().size() < minNodePerEntry) {
                    lastTreeNodes.add(0, swappedTreeNodes.remove(swappedTreeNodes.size() - 1));
                }
            }
            nonLeafNode.setMBR(MBR.union(nonLeafNode.getMBRs()));
            nextLevel.add(nonLeafNode);
        }
    }

    /**
     * Pack the leaf nodes that have been packed from data objects in the last step.
     *
     * @param treeNodes list of leaf nodes
     * @param maxNodePerEntry maximum number of entities for each node of the completed index
     * @param minNodePerEntry minimum number of entities for each node of the completed index
     * @return a non-leaf node as root
     */
    private TreeNode leafNodePacking(ArrayList<TreeNode> treeNodes, final int maxNodePerEntry, final int minNodePerEntry) {
        // calculate partition num
        double p = treeNodes.size() * 1.0 / maxNodePerEntry;

        // start build r-tree structure bottom-to-up recursively
        treeNodes.sort(new MBR.MBRComparatorWithTreeNode(1, true));
        int[] height = new int[]{1};
        treeNodes = buildRecursivelyBySTR(p, treeNodes, maxNodePerEntry, minNodePerEntry, height);

        // pack the root
        NonLeafNode nonLeafNode = new NonLeafNode(maxNodePerEntry, height[0] + 1);
        nonLeafNode.setChildNodes(treeNodes);
        nonLeafNode.setMBR(MBR.union(nonLeafNode.getMBRs()));

        return nonLeafNode;
    }


    /**
     * Build r-tree bottom-to-up recursively
     *
     * @param p partition num.
     * @param entries list of tree nodes.
     * @param height current height of building tree, stored as height[0].
     * @param maxNodePerEntry maximum number of entities for each node of the completed index
     * @param minNodePerEntry minimum number of entities for each node of the completed index
     * @return list of tree nodes that have been packed
     */
    private ArrayList<TreeNode> buildRecursivelyBySTR(double p, ArrayList<TreeNode> entries, final int maxNodePerEntry, final int minNodePerEntry, int[] height) {
        // entries num in node should be no more than M, but if size <= M, return entries directly.
        if (entries.size() <= maxNodePerEntry) {
            return entries;
        }

        // calculate slides num
        int s = (int) Math.ceil(Math.pow(Math.E, Math.log(p) / 2));
        ArrayList<TreeNode> list = new ArrayList<>();
        ArrayList<TreeNode> nextLevel = new ArrayList<>();

        // start pack nodes
        int ctr = 0;
        for (TreeNode treeNode : entries) {
            list.add(treeNode);
            ++ctr;
            if (ctr == s * maxNodePerEntry) {
                packNodes(list, nextLevel, maxNodePerEntry, minNodePerEntry, height);
                list.clear();
                ctr = 0;
            }
        }

        // the size of the last slide may be lower than s * M
        if(list.size() > 0) {
            packNodes(list, nextLevel, maxNodePerEntry, minNodePerEntry, height);
            list.clear();
        }
        height[0]++;
        return buildRecursivelyBySTR(nextLevel.size() * 1.0 / maxNodePerEntry, nextLevel, maxNodePerEntry, minNodePerEntry, height);
    }
}
