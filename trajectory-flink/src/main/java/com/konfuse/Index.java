package com.konfuse;

import com.konfuse.geometry.DataObject;
import com.konfuse.internal.RTree;
import org.apache.flink.api.java.DataSet;

/**
 * @Author: Konfuse
 * @Date: 2019/12/6 15:31
 */
public class Index {
    private DataSet<RTree> globalRTree;
    private DataSet<RTree> localRTree;
    private LocalIndexBuilder localIndexBuilder;
    private DataSet<DataObject> data;
}
