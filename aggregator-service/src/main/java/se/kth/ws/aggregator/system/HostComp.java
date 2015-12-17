package se.kth.ws.aggregator.system;

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
import se.sics.ktoolbox.aggregator.server.*;
import se.sics.ktoolbox.aggregator.server.util.DesignProcessor;
import se.sics.ktoolbox.ipsolver.IpSolverComp;
import se.sics.ktoolbox.ipsolver.IpSolverPort;
import se.sics.ktoolbox.ipsolver.msg.GetIp;
import se.sics.p2ptoolbox.util.config.SystemConfig;
import se.sics.p2ptoolbox.util.helper.SystemConfigBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Component responsible for creating and initiating the
 * global aggregator and the visualizer component
 *
 * Created by babbar on 2015-09-07.
 */
public class HostComp extends ComponentDefinition {

    private Logger logger = LoggerFactory.getLogger(HostComp.class);
    private static final int BIND_RETRY = 3;
    private static final int MAX_SNAPSHOTS = 100;

    private Component globalAggregator;
    private Component visualizer;
    private Component timer;
    private Component network;
    private Component ipSolver;

    private Socket socket;
    private Config config;
    private SystemConfigBuilder builder;
    private SystemConfig systemConfig;
    private static GetIp.NetworkInterfacesMask ipType;

    public HostComp(HostCompInit init){
        doInit(init);
        subscribe(startHandler, control);
    }


    public void doInit(HostCompInit init){

        logger.debug("Initializing the component");

        if(ipType == null){
            logger.error("ipType not set, exiting !!");
            System.exit(-1);
        }
    }


    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {

            logger.debug("Component has been successfully started.");
            initiatePhase1();
        }
    };


    /**
     * Set the interfaces mask which enables the choosing of
     * a particular ip address.
     *
     * @param setIpType ip type.
     */
    public static void setIpType(GetIp.NetworkInterfacesMask setIpType) {
        ipType = setIpType;
    }

    /**
     * Phase One involves getting the ip address from the ipresolver component.
     * This is done in case the node has many network interfaces, so this will return a single
     * address.
     *
     */
    public void initiatePhase1(){

        logger.debug("Initiating the first phase of the launch.");

        timer = create(JavaTimer.class, Init.NONE);
        trigger(Start.event, timer.control());

        ipSolver = create(IpSolverComp.class, new IpSolverComp.IpSolverInit());
        trigger(Start.event, ipSolver.control());

        subscribe(handleGetIp, ipSolver.getPositive(IpSolverPort.class));
        trigger(new GetIp.Req(EnumSet.of(ipType)), ipSolver.getPositive(IpSolverPort.class));
    }


    /**
     * The request to fetch the ip address is made to the ip resolver component.
     * The response contains the addresses available to the node.
     */
    Handler<GetIp.Resp> handleGetIp = new Handler<GetIp.Resp>() {
        @Override
        public void handle(GetIp.Resp resp) {
            logger.debug("Handling the response from the Ip Solver component.");

            InetAddress ip = null;
            if (!resp.addrs.isEmpty()) {
                ip = resp.addrs.get(0).getAddr();
                if (resp.addrs.size() > 1) {
                    logger.warn("multiple ips detected, proceeding with:{}", ip);
                }
            }

            config = ConfigFactory.load();
            builder = new SystemConfigBuilder(config).setSelfIp(ip);

            initiatePhase2();
        }
    };


    /**
     * Start with the next phase of the Boot Up in which we try to mainly bind on the
     * socket and pass the control to the next network component.
     *
     */
    private void initiatePhase2() {

        logger.debug("Initiating the second phase of launch.");

        buildSysConfig();
        connectNetwork();

//      Start connecting the main components.
        initiatePhase3();
    }


    /**
     * Final phase of the connection in which the node will create the final
     * application components and form the appropriate connections.
     */
    private void initiatePhase3() {

        logger.debug("Initiating the third phase of launch");

        globalAggregator = create(GlobalAggregator.class, new GlobalAggregatorInit(5000));
        trigger(Start.event, globalAggregator.control());

        Map<String, DesignProcessor> processorMap = getDesignerProcessMap();
        visualizer = create(Visualizer.class, new VisualizerInit(MAX_SNAPSHOTS, processorMap));
        trigger(Start.event, visualizer.control());

        connect(globalAggregator.getNegative(Timer.class), timer.getPositive(Timer.class));
        connect(globalAggregator.getNegative(Network.class), network.getPositive(Network.class));
        connect(globalAggregator.getPositive(GlobalAggregatorPort.class), visualizer.getNegative(GlobalAggregatorPort.class));

    }


    /**
     * Get the designer processor map which will indicate the information
     * regarding design processor to be used.
     *
     * @return map.
     */
    public Map<String, DesignProcessor> getDesignerProcessMap() {

        Map<String, DesignProcessor> map = new HashMap<String, DesignProcessor>();
        for(DesignerEnum designerEnum : DesignerEnum.values()){
            map.put(designerEnum.getName(), designerEnum.getProcessor());
        }

        return map;
    }


    /**
     * Now initiate a connection to the network component.
     * In order for the connection to be made, the self address constructed by the
     * loading of config needs to be supplied.
     */
    private void connectNetwork() {
        network = create(NettyNetwork.class, new NettyInit(systemConfig.self));
        trigger(Start.event, network.control());
    }




    /**
     * Start building the system configuration.
     */
    private void buildSysConfig(){

//      Initiate the socket bind operation.
        initiateSocketBind();
        logger.debug("Socket successfully sound to ip :{} and port: {}", builder.getSelfIp(), builder.getSelfPort());

//      Once the port and identifier are set build the configuration.
        logger.debug("Building the system configuration.");
        systemConfig = builder.build();
    }



    /**
     * Try to bind on the socket and keep a
     * reference of the socket.
     */
    private void initiateSocketBind() {

        logger.debug("Initiating the binding on the socket to keep the port being used by some other service.");
        InetAddress selfIp = builder.getSelfIp();

        int retries = BIND_RETRY;
        while (retries > 0) {

//          Port gets updated, so needs to be reset.
            Integer selfPort = builder.getSelfPort();

            try {
                logger.debug("Trying to bind on the socket1 with ip: {} and port: {}", selfIp, selfPort);
                bindOperation(selfIp, selfPort);
                break;  // If exception is not thrown, break the loop.
            }

            catch (IOException e) {
                logger.debug("Socket Bind failed, retrying.");
                builder.setPort();
            }

            retries--;
        }

        if(retries <= 0) {
            logger.error("Unable to bind on a socket, exiting.");
            throw new RuntimeException("Unable to identify port for the socket to bind on.");
        }
    }

    /**
     * Based on the ip and port, create a socket to bind on that address and port.
     * @param selfIp ip-address
     * @param selfPort port
     * @throws IOException
     */
    private void bindOperation(InetAddress selfIp, Integer selfPort) throws IOException {

        socket = new Socket();
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(selfIp, selfPort));
    }

}
