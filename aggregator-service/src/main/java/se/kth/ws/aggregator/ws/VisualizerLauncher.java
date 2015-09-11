package se.kth.ws.aggregator.ws;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.ws.aggregator.util.DesignerEnum;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.ktoolbox.aggregator.global.api.ports.GlobalAggregatorPort;
import se.sics.ktoolbox.aggregator.global.api.ports.VisualizerPort;
import se.sics.ktoolbox.aggregator.global.api.system.DesignProcessor;
import se.sics.ktoolbox.aggregator.global.core.GlobalAggregator;
import se.sics.ktoolbox.aggregator.global.core.GlobalAggregatorInit;
import se.sics.ktoolbox.aggregator.global.core.Visualizer;
import se.sics.ktoolbox.aggregator.global.core.VisualizerInit;
import se.sics.ktoolbox.ipsolver.IpSolverComp;
import se.sics.ktoolbox.ipsolver.IpSolverPort;
import se.sics.ktoolbox.ipsolver.msg.GetIp;
import se.sics.ms.configuration.MsConfig;
import se.sics.p2ptoolbox.util.config.SystemConfig;
import se.sics.p2ptoolbox.util.helper.SystemConfigBuilder;

import java.net.*;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * The main launcher for the visualizer component.
 * 
 * Created by babbarshaer on 2015-09-10.
 */
public class VisualizerLauncher extends ComponentDefinition{
    
    private static Logger logger = LoggerFactory.getLogger(VisualizerLauncher.class);
    
    private InetAddress ip;
    private Socket socket;
    private Component aggregator;
    private Component visualizer;
    private Component visualizerSyncI;

    private Component timer;
    private Component network;

    private SystemConfigBuilder builder;
    private SystemConfig systemConfig;
    
    private static GetIp.NetworkInterfacesMask ipType;
    public static void setIpType(GetIp.NetworkInterfacesMask setIpType) {
        ipType = setIpType;
    }
    
    public VisualizerLauncher(){
        
        logger.debug("Initializing the component");
        subscribe(startHandler, control);
    }

    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start start) {
            
            logger.debug("Starting the component.");
            doStart();
        }
    };


    /**
     * Trigger start to the components.
     */
    private void doStart(){

        logger.debug("Creating child components.");
        initiatingPhase1();
    }


    /**
     * 
     */
    private void initiatingPhase1(){
        
        logger.debug("Initiating first phase of figuring out the address.");
        
        timer = create(JavaTimer.class, Init.NONE);
        trigger(Start.event, timer.getControl());

        Component ipSolverComp = create(IpSolverComp.class, new IpSolverComp.IpSolverInit());
        trigger(Start.event, ipSolverComp.control());
        subscribe(handleGetIp, ipSolverComp.getPositive(IpSolverPort.class));
        trigger(new GetIp.Req(EnumSet.of(ipType)), ipSolverComp.getPositive(IpSolverPort.class));
        
    }
    
    public Handler handleGetIp = new Handler<GetIp.Resp>() {
        @Override
        public void handle(GetIp.Resp resp) {
            
            logger.info("starting: setting up caracal connection");
            
            if (!resp.addrs.isEmpty()) {
                ip = resp.addrs.get(0).getAddr();
                if (resp.addrs.size() > 1) {
                    logger.warn("multiple ips detected, proceeding with:{}", ip);
                }
            }

            Config config = ConfigFactory.load();
            builder = new SystemConfigBuilder(config).setSelfIp(ip);
            
            initiatingPhase2();
        }
    };
    
    
    private void buildSysConfig(){
        
        logger.debug("Initiating the building of the system configuration.");
        initiateSocketBind();
        
        logger.debug("Socket Bind successful.");
        systemConfig = builder.build();
    }
    
    private void initiateSocketBind (){
        
        try {
            socket = new Socket();
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(builder.getSelfIp(), builder.getSelfPort()));
        }
        catch(Exception e){
            
            logger.error("Unable to bind on socket, as it might be used. Exiting ....!!");
            System.exit(-1);
        }
    }


    /**
     * Second phase of the bootup involving building of the
     * system configuration and creating the network component.
     */
    private void initiatingPhase2(){
        
        logger.debug("Initiating the second phase.");
        
        logger.debug("Building the system configuration.");
        buildSysConfig();
        
        logger.debug("Initiating the network component on the binded address and port");
        network = create(NettyNetwork.class, new NettyInit(systemConfig.self));
        trigger(Start.event, network.getControl());

        logger.debug("Phase 2 seems to be complete .. ");

        initiatingPhase3();
    }


    /**
     * Phase involving the final connections to the components in the system.
     */
    private void initiatingPhase3() {
        
        logger.debug("Initiating the final phase of the bootup.");
        int maxSnapShots = 10;

        Map<String, DesignProcessor> processorMap = getDesignProcessorMap();

        aggregator = create(GlobalAggregator.class, new GlobalAggregatorInit(MsConfig.LOCAL_AGGREGATOR_TIMEOUT));
        visualizer = create(Visualizer.class, new VisualizerInit(maxSnapShots, processorMap));
        
        visualizerSyncI = create(VisualizerSyncComponent.class, Init.NONE);
        
        connect(aggregator.getNegative(Timer.class), timer.getPositive(Timer.class));
        connect(aggregator.getNegative(Network.class), network.getPositive(Network.class));
        
        connect(visualizer.getNegative(GlobalAggregatorPort.class), aggregator.getPositive(GlobalAggregatorPort.class));
        connect(visualizerSyncI.getNegative(VisualizerPort.class), visualizer.getPositive(VisualizerPort.class));
        
        trigger(Start.event, visualizerSyncI.control());
        trigger(Start.event, aggregator.getControl());
        trigger(Start.event, visualizer.getControl());

    }

    private Map<String, DesignProcessor> getDesignProcessorMap(){
        
        Map<String, DesignProcessor> result = new HashMap<String, DesignProcessor>();
        for(DesignerEnum value : DesignerEnum.values()){
            result.put(value.getName(), value.getProcessor());
        }
        
        return result;
    }


    public static void main(String[] args) {
        
        logger.debug("Initiating with the running of the component.");

        GetIp.NetworkInterfacesMask setIpType = GetIp.NetworkInterfacesMask.PUBLIC;
        if (args.length == 1 && args[0].equals("-tenDot")) {
            setIpType = GetIp.NetworkInterfacesMask.TEN_DOT_PRIVATE;
        }
        VisualizerLauncher.setIpType(setIpType);

        if (Kompics.isOn()) {
            Kompics.shutdown();
        }
        Kompics.createAndStart(VisualizerLauncher.class, Runtime.getRuntime().availableProcessors(), 20); // Yes 20 is totally arbitrary
        try {
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            System.exit(1);
        }
        
    }

}
