package se.kth.ws.aggregator.util;



import se.sics.ktoolbox.aggregator.server.util.DesignProcessor;
import se.sics.ms.aggregator.processor.InternalStateDesignProcessor;
import se.sics.ms.aggregator.processor.SearchRespDesignProcessor;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum related to the design processor.
 *
 * Created by babbar on 2015-09-07.
 */
public enum DesignerEnum {

    AvgSearchResponse("avgSearchResponse", new SearchRespDesignProcessor()),
    AggInternalState("aggregatedInternalState", new InternalStateDesignProcessor());

    private String name;
    private DesignProcessor processor;

    private DesignerEnum(String name, DesignProcessor processor){
        this.name = name;
        this.processor = processor;
    }

    public String getName() {
        return name;
    }

    public DesignProcessor getProcessor() {
        return processor;
    }
}
