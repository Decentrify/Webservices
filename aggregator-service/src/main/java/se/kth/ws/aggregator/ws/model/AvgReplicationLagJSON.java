package se.kth.ws.aggregator.ws.model;

/**
 * JSON format file for the average replication
 * lag in the system.
 *
 * Created by babbar on 2015-09-28.
 */
public class AvgReplicationLagJSON {

    private Long time;
    private Long maxLag;
    private Long minLag;
    private Double avgLag;

    public AvgReplicationLagJSON(Long time, Long maxLag, Long minLag, Double avgLag) {
        this.time = time;
        this.maxLag = maxLag;
        this.minLag = minLag;
        this.avgLag = avgLag;
    }


    public Long getTime() {
        return time;
    }

    public Long getMaxLag() {
        return maxLag;
    }

    public Long getMinLag() {
        return minLag;
    }

    public Double getAvgLag() {
        return avgLag;
    }
}
