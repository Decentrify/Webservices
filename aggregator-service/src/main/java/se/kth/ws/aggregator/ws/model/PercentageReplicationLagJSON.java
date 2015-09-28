package se.kth.ws.aggregator.ws.model;

/**
 * JSON format file for the percentile replication lag in
 * different regions in the system.
 *
 * Created by babbar on 2015-09-28.
 */
public class PercentageReplicationLagJSON {

    private Long time;
    private Long fifty;
    private Long seventyFive;
    private Long ninety;

    public PercentageReplicationLagJSON(Long time, Long fifty, Long seventyFive, Long ninety) {
        this.time = time;
        this.fifty = fifty;
        this.seventyFive = seventyFive;
        this.ninety = ninety;
    }


    public Long getTime() {
        return time;
    }

    public Long getFifty() {
        return fifty;
    }

    public Long getSeventyFive() {
        return seventyFive;
    }

    public Long getNinety() {
        return ninety;
    }
}
