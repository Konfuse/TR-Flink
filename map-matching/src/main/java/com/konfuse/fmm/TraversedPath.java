package com.konfuse.fmm;

import java.util.LinkedList;
import java.util.List;

/**
 * @Auther todd
 * @Date 2020/1/7
 */
public class TraversedPath {
    public List<Long> completePath;
    public List<Integer> indexes;

    public TraversedPath() {
        completePath = new LinkedList<>();
        indexes = new LinkedList<>();
    }

    public void addPath(Long id){
        completePath.add(id);
    }

    public void addIndex(Integer index){
        indexes.add(index);
    }

    public void setCompletePath(List<Long> completePath) {
        this.completePath = completePath;
    }

    public void setIndexes(List<Integer> indexes) {
        this.indexes = indexes;
    }

    public List<Long> getCompletePath() {
        return completePath;
    }

    public List<Integer> getIndexes() {
        return indexes;
    }
}
