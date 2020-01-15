package com.konfuse.fmm;

/**
 * @Auther todd
 * @Date 2020/1/6
 */
public class Record {
    protected long source;
    protected long target;
    protected long first_n;
    protected long prev_n;
    protected long next_e;
    protected double cost;
    protected Record next;

    public Record(long source, long target, long first_n, long prev_n, long next_e, double cost) {
        this.source = source;
        this.target = target;
        this.first_n = first_n;
        this.prev_n = prev_n;
        this.next_e = next_e;
        this.cost = cost;
        this.next = null;
    }

    public Record(long source, long target, long first_n, long prev_n, long next_e, long cost, Record next) {
        this.source = source;
        this.target = target;
        this.first_n = first_n;
        this.prev_n = prev_n;
        this.next_e = next_e;
        this.cost = cost;
        this.next = next;
    }

    public long getSource() {
        return source;
    }

    public void setSource(long source) {
        this.source = source;
    }

    public long getTarget() {
        return target;
    }

    public void setTarget(long target) {
        this.target = target;
    }


}
