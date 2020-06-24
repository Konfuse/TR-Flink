package com.konfuse;

import com.konfuse.dison.DISONGlobalIndex;
import com.konfuse.dison.DISONLocalIndex;
import org.apache.flink.api.java.DataSet;

/**
 * @author todd
 * @date 2020/5/24 22:44
 * @description: TODO
 */
public class DISONIndexBuilderResult {

    private DataSet<DISONGlobalIndex> globalIndexDataSet;
    private DataSet<DISONLocalIndex> localIndexDataSet;
//    private DataSet<DISONTrajectory> data;

    public DISONIndexBuilderResult() {
    }

    public DISONIndexBuilderResult(DataSet<DISONGlobalIndex> globalIndexDataSet, DataSet<DISONLocalIndex> localIndexDataSet){ //, DataSet<DISONTrajectory> data) {
        this.globalIndexDataSet = globalIndexDataSet;
        this.localIndexDataSet = localIndexDataSet;
//        this.data = data;
    }

    public DataSet<DISONGlobalIndex> getGlobalIndexDataSet() {
        return globalIndexDataSet;
    }

    public void setGlobalIndexDataSet(DataSet<DISONGlobalIndex> globalIndexDataSet) {
        this.globalIndexDataSet = globalIndexDataSet;
    }

    public DataSet<DISONLocalIndex> getLocalIndexDataSet() {
        return localIndexDataSet;
    }

    public void setLocalIndexDataSet(DataSet<DISONLocalIndex> localIndexDataSet) {
        this.localIndexDataSet = localIndexDataSet;
    }

//    public DataSet<DISONTrajectory> getData() {
//        return data;
//    }
//
//    public void setData(DataSet<DISONTrajectory> data) {
//        this.data = data;
//    }
}
