package com.konfuse;

import com.konfuse.geometry.DataObject;
import com.konfuse.internal.MBR;
import com.konfuse.geometry.PartitionedMBR;
import com.konfuse.geometry.Point;
import com.konfuse.internal.RTree;
import org.apache.flink.api.common.functions.Partitioner;
import org.apache.flink.api.java.DataSet;

import java.io.Serializable;

/**
 * Distributed index structure in the whole system.
 *
 * Index includes two types: point index and line index.
 * Each type of two indexes contains a global r-tree which is a PartitionedMBR r-tree.
 *
 * Global r-tree maintains the information of flink partitions.
 *
 * Local r-trees maintain a data set of local trees. And partitioner
 * is the corresponding partition method.
 *
 * data contains all the data records maintained in r-tree.
 *
 * Need to specify generics when initializing the class:
 * either Point or Line.
 *
 * @Author: Konfuse
 * @Date: 2019/12/6 15:31
 */
public abstract class Index<T extends DataObject> implements Serializable {
    private DataSet<RTree<PartitionedMBR>> globalTree;
    private DataSet<RTree<T>> localTrees;
    private Partitioner<T> partitioner;
    private DataSet<T> data;

    public Index(DataSet<RTree<PartitionedMBR>> globalTree, DataSet<RTree<T>> localTrees, Partitioner<T> partitioner, DataSet<T> data) {
        this.globalTree = globalTree;
        this.localTrees = localTrees;
        this.partitioner = partitioner;
        this.data = data;
    }

    public DataSet<RTree<PartitionedMBR>> getGlobalTree() {
        return globalTree;
    }

    public void setGlobalTree(DataSet<RTree<PartitionedMBR>> globalTree) {
        this.globalTree = globalTree;
    }

    public DataSet<RTree<T>> getLocalTrees() {
        return localTrees;
    }

    public void setLocalTrees(DataSet<RTree<T>> localTrees) {
        this.localTrees = localTrees;
    }

    public Partitioner<? extends DataObject> getPartitioner() {
        return partitioner;
    }

    public void setPartitioner(Partitioner<T> partitioner) {
        this.partitioner = partitioner;
    }

    public DataSet<? extends DataObject> getData() {
        return data;
    }

    public void setData(DataSet<T> data) {
        this.data = data;
    }

    /**
     * Get the data objects inside the query area.
     * @param area query area.
     * @return DataSet of data objects according to box range query.
     */
    public abstract DataSet<T> boxRangeQuery(final MBR area);

    /**
     * Get the data objects inside the query circle.
     * @param radius the radius of circle
     * @param queryPoint circle center
     * @return DataSet of data objects according to circle range query.
     */
    public abstract DataSet<T> circleRangeQuery(final Point queryPoint, final double radius);

    /**
     * Get the nearest k data objects from query point.
     * @param queryPoint the point to query
     * @param k the size of result set
     * @return DataSet of data objects according of knn query
     */
    public abstract DataSet<T> knnQuery(final Point queryPoint, final int k);
}
