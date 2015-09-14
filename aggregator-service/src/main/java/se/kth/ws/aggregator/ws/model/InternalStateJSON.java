package se.kth.ws.aggregator.ws.model;

/**
 * JSON for the response to calculate the internal state of the nodes in the
 * system.
 *
 * Created by babbar on 2015-09-10.
 */
public class InternalStateJSON {

    public Integer partitionId;
    public Integer partitionDepth;
    public Long numEntries;
    public Integer leaderId;

    public InternalStateJSON(Integer partitionId, Integer partitionDepth, Long numEntries, Integer leaderId) {

        this.partitionId = partitionId;
        this.partitionDepth = partitionDepth;
        this.numEntries = numEntries;
        this.leaderId = leaderId;
    }


    public Long getNumEntries() {
        return numEntries;
    }

    public Integer getPartitionId() {
        return partitionId;
    }

    public Integer getPartitionDepth() {
        return partitionDepth;
    }

    public Integer getLeaderId() {
        return leaderId;
    }
}
