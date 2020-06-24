package com.konfuse;

import com.konfuse.dita.DITATrajectory;
import com.konfuse.dita.DITAGlobalIndex;
import com.konfuse.dita.DITALocalIndex;
import org.apache.flink.api.java.DataSet;

/**
 * @author todd
 * @date 2020/5/22 8:33
 * @description: TODO
 */
public class DITAIndexBuilderResult {
    private DataSet<DITAGlobalIndex> globalIndex;
    private DataSet<DITALocalIndex> localIndex;
    private DataSet<DITATrajectory> data;

    public DITAIndexBuilderResult() {
    }

    public DITAIndexBuilderResult(DataSet<DITAGlobalIndex> globalIndex, DataSet<DITALocalIndex> localIndex, DataSet<DITATrajectory> data) {
        this.globalIndex = globalIndex;
        this.localIndex = localIndex;
        this.data = data;
    }

    public DataSet<DITAGlobalIndex> getGlobalIndex() {
        return globalIndex;
    }

    public void setGlobalIndex(DataSet<DITAGlobalIndex> globalIndex) {
        this.globalIndex = globalIndex;
    }

    public DataSet<DITALocalIndex> getLocalIndex() {
        return localIndex;
    }

    public void setLocalIndex(DataSet<DITALocalIndex> localIndex) {
        this.localIndex = localIndex;
    }

    public DataSet<DITATrajectory> getData() {
        return data;
    }

    public void setData(DataSet<DITATrajectory> data) {
        this.data = data;
    }
}
