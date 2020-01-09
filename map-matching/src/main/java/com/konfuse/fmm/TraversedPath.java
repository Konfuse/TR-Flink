package com.konfuse.fmm;

import java.util.LinkedList;
import java.util.List;

/**
 * @Auther todd
 * @Date 2020/1/7
 */
public class TraversedPath {
    public List<Long> completePath;
    public List<Integer> indics;

    public TraversedPath() {
        completePath = new LinkedList<>();
        indics = new LinkedList<>();
    }

    public void addPath(Long id){
        completePath.add(id);
    }

    public void addIndex(Integer index){
        indics.add(index);
    }

    public void setCompletePath(List<Long> completePath) {
        this.completePath = completePath;
    }

    public void setIndics(List<Integer> indics) {
        this.indics = indics;
    }

    public List<Long> getCompletePath() {
        return completePath;
    }

    public List<Integer> getIndics() {
        return indics;
    }
}
