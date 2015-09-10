package se.kth.ws.aggregator.ws.model;

/**
 * JSON for the response to calculate the internal state of the nodes in the
 * system.
 *
 * Created by babbar on 2015-09-10.
 */
public class InternalStateJSON {

    public int nodeId;
    public int partitionId;
    public int partitionDepth;
    public int leaderId;

    public InternalStateJSON(int nodeId, int partitionId, int partitionDepth, int leaderId) {

        this.nodeId = nodeId;
        this.partitionId = partitionId;
        this.partitionDepth = partitionDepth;
        this.leaderId = leaderId;
    }


    public int getNodeId() {
        return nodeId;
    }

    public int getPartitionId() {
        return partitionId;
    }

    public int getPartitionDepth() {
        return partitionDepth;
    }

    public int getLeaderId() {
        return leaderId;
    }
}
