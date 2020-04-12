package com.konfuse.road;

/**
 * @Author: Konfuse
 * @Date: 2020/4/12 12:48
 */
public interface RoadReader {
    public void open() throws Exception;
    public boolean isOpen();
    public void close() throws Exception;
    public BaseRoad next() throws Exception;
}
