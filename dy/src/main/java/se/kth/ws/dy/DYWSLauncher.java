/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.kth.ws.dy;

import com.google.common.util.concurrent.SettableFuture;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.ws.sweep.BootstrapNodes;
import se.kth.ws.sweep.core.SweepSyncComponent;
import se.kth.ws.sweep.core.SweepSyncI;
import se.sics.caracaldb.MessageRegistrator;
import se.sics.gvod.common.util.VoDHeartbeatServiceEnum;
import se.sics.gvod.config.GradientConfiguration;
import se.sics.gvod.config.SearchConfiguration;
import se.sics.gvod.manager.toolbox.GVoDSyncI;
import se.sics.gvod.network.GVoDSerializerSetup;
import se.sics.gvod.system.HostManagerComp;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.ktoolbox.aggregator.AggregatorSerializerSetup;
import se.sics.ktoolbox.cc.bootstrap.CCBootstrapComp;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatComp;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatPort;
import se.sics.ktoolbox.ipsolver.IpSolverComp;
import se.sics.ktoolbox.ipsolver.IpSolverPort;
import se.sics.ktoolbox.ipsolver.msg.GetIp;
import se.sics.ms.net.SweepSerializerSetup;
import se.sics.ms.ports.UiPort;
import se.sics.ms.search.SearchPeer;
import se.sics.ms.search.SearchPeerInit;
import se.sics.ms.util.HeartbeatServiceEnum;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.EnumSet;
import se.sics.gvod.system.HostManagerKCWrapper;
import se.sics.ktoolbox.cc.bootstrap.CCOperationPort;
import se.sics.ktoolbox.cc.bootstrap.event.status.CCBootstrapDisconnected;
import se.sics.ktoolbox.cc.bootstrap.event.status.CCBootstrapReady;
import se.sics.ktoolbox.cc.heartbeat.event.status.CCHeartbeatReady;
import se.sics.ktoolbox.chunkmanager.ChunkManagerSerializerSetup;
import se.sics.ktoolbox.croupier.CroupierSerializerSetup;
import se.sics.ktoolbox.croupier.aggregation.CroupierAggregation;
import se.sics.ktoolbox.election.ElectionConfig;
import se.sics.ktoolbox.election.ElectionSerializerSetup;
import se.sics.ktoolbox.election.aggregation.ElectionAggregation;
import se.sics.ktoolbox.gradient.GradientSerializerSetup;
import se.sics.ktoolbox.gradient.aggregation.GradientAggregation;
import se.sics.ktoolbox.util.aggregation.BasicAggregation;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicAddress;
import se.sics.ktoolbox.util.network.nat.NatAwareAddressImpl;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;
import se.sics.ktoolbox.util.status.Status;
import se.sics.ktoolbox.util.status.StatusPort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class DYWSLauncher extends ComponentDefinition {

    private Logger LOG = LoggerFactory.getLogger(DYWSLauncher.class);
    private static GetIp.NetworkInterfacesMask ipType;

    public static void setIpType(GetIp.NetworkInterfacesMask setIpType) {
        ipType = setIpType;
    }
    private static final int BIND_RETRY = 3;

    private Component timerComp;
    private Component ipSolverComp;
    private Component networkComp;
    private Component caracalClientComp;
    private Component heartbeatComp;
    private Component sweepHostComp;
    private Component sweepSyncComp;
    private Component vodHostComp;
    private DYWS dyWS;
    private Socket socket;

    //TODO Alex - fix self address
    private InetAddress ip;
    private KAddress self;

    private SystemKCWrapper systemConfig;

    private SettableFuture<GVoDSyncI> gvodSyncIFuture;
    private SweepSyncI sweepSyncI;

    byte[] vodSchemaId = null;
    //**************************************************************************

    public DYWSLauncher() {
        LOG.info("initiating...");
        if (ipType == null) {
            LOG.error("launcher logic error - ipType not set");
            System.exit(1);
        }
        gvodSyncIFuture = SettableFuture.create();
        registerSerializers();
        registerPortTracking();

        systemConfig = new SystemKCWrapper(config());

        subscribe(handleStart, control);
        subscribe(handleStop, control);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("starting: solvingIp");

            phase1();
        }
    };

    Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            LOG.info("stopping...");
        }
    };

    @Override
    public Fault.ResolveAction handleFault(Fault fault) {
        LOG.error("exception:{} shutting down", fault.getCause());
        System.exit(1);
        return Fault.ResolveAction.RESOLVED;
    }

    private void registerSerializers() {
        MessageRegistrator.register();
        int currentId = 128;
        currentId = BasicSerializerSetup.registerBasicSerializers(currentId);
        currentId = CroupierSerializerSetup.registerSerializers(currentId);
        currentId = GradientSerializerSetup.registerSerializers(currentId);
        currentId = ElectionSerializerSetup.registerSerializers(currentId);
        currentId = AggregatorSerializerSetup.registerSerializers(currentId);
        currentId = ChunkManagerSerializerSetup.registerSerializers(currentId);
        currentId = SweepSerializerSetup.registerSerializers(currentId);
        currentId = GVoDSerializerSetup.registerSerializers(currentId);
    }
    
    private void registerPortTracking() {
        BasicAggregation.registerPorts();
        CroupierAggregation.registerPorts();
        GradientAggregation.registerPorts();
        ElectionAggregation.registerPorts();
    }

    private void phase1() {
        timerComp = create(JavaTimer.class, Init.NONE);
        trigger(Start.event, timerComp.control());
        ipSolverComp = create(IpSolverComp.class, new IpSolverComp.IpSolverInit());
        trigger(Start.event, ipSolverComp.control());
        subscribe(handleGetIp, ipSolverComp.getPositive(IpSolverPort.class));
        trigger(new GetIp.Req(EnumSet.of(ipType)), ipSolverComp.getPositive(IpSolverPort.class));
    }

    public Handler handleGetIp = new Handler<GetIp.Resp>() {
        @Override
        public void handle(GetIp.Resp resp) {
            LOG.info("starting: setting up caracal connection");

            if (!resp.addrs.isEmpty()) {
                ip = resp.addrs.get(0).getAddr();
                if (resp.addrs.size() > 1) {
                    LOG.warn("multiple ips detected, proceeding with:{}", ip);
                }
            } else {
                throw new RuntimeException("ip not detected");
            }

            phase2();
        }
    };

    private void phase2() {

//      Initiate the socket bind operation.
//        buildSysConfig();
//      Start connecting the network and other components.
        connectNetwork();
        connectCaracalClient();
        connectHeartbeat();

        subscribe(handleCaracalDisconnect, caracalClientComp.getPositive(StatusPort.class));
        subscribe(handleCaracalReady, caracalClientComp.getPositive(StatusPort.class));
        subscribe(handleHeartbeatReady, heartbeatComp.getPositive(StatusPort.class));
    }

    /**
     * Start building the system configuration.
     */
//    private void buildSysConfig(){
//
//      Initiate the socket bind operation.
//        initiateSocketBind();
//        LOG.debug("Socket successfully sound to ip :{} and port: {}", ip, dyPort);
//    }
//
//    /**
//     * Try to bind on the socket and keep a
//     * reference of the socket.
//     */
//    private void initiateSocketBind() {
//
//        LOG.debug("Initiating the binding on the socket to keep the port being used by some other service.");
//
//        int retries = BIND_RETRY;
//        while (retries > 0) {
//
////          Port gets updated, so needs to be reset.
//
//            try {
//
//                LOG.debug("Trying to bind on the socket1 with ip: {} and port: {}", ip, dyPort);
//                bindOperation(ip, dyPort);
//                break;  // If exception is not thrown, break the loop.
//            }
//
//            catch (IOException e) {
//                retries--;
//                LOG.debug("Socket Bind failed, retrying.");
//                throw new RuntimeException("could not bind on dy port");
//            }
//        }
//
//        if(retries <= 0) {
//            LOG.error("Unable to bind on a socket, exiting.");
//            throw new RuntimeException("Unable to identify port for the socket to bind on.");
//        }
//
//    }
//
//    /**
//     * Based on the ip and port, create a socket to bind on that address and port.
//     * @param selfIp ip-address
//     * @param selfPort port
//     * @throws IOException
//     */
//    private void bindOperation(InetAddress selfIp, Integer selfPort) throws IOException {
//
//        socket = new Socket();
//        socket.setReuseAddress(true);
//        socket.bind(new InetSocketAddress(selfIp, selfPort));
//    }
//
//
//    /**
//     * The method is used to release the socket by initiating
//     * close method on the socket.
//     */
//    private void releaseSocket() throws IOException {
//
//        if(this.socket != null && !this.socket.isClosed())
//            this.socket.close();
//    }
    private void connectNetwork() {
        self = NatAwareAddressImpl.open(new BasicAddress(ip, systemConfig.port, systemConfig.id));
        LOG.info("starting with self local address:{}", self);
        networkComp = create(NettyNetwork.class, new NettyInit(self));
        trigger(Start.event, networkComp.control());
    }

    private void connectCaracalClient() {

        caracalClientComp = create(CCBootstrapComp.class, new CCBootstrapComp.CCBootstrapInit(self));
        connect(caracalClientComp.getNegative(Timer.class), timerComp.getPositive(Timer.class), Channel.TWO_WAY);
        connect(caracalClientComp.getNegative(Network.class), networkComp.getPositive(Network.class), Channel.TWO_WAY);
        trigger(Start.event, caracalClientComp.control());
    }

    private void connectHeartbeat() {
        heartbeatComp = create(CCHeartbeatComp.class, new CCHeartbeatComp.CCHeartbeatInit(self));
        connect(heartbeatComp.getNegative(Timer.class), timerComp.getPositive(Timer.class), Channel.TWO_WAY);
        connect(heartbeatComp.getNegative(CCOperationPort.class), caracalClientComp.getPositive(CCOperationPort.class), Channel.TWO_WAY);
        connect(heartbeatComp.getNegative(StatusPort.class), caracalClientComp.getPositive(StatusPort.class), Channel.TWO_WAY);
        trigger(Start.event, heartbeatComp.control());
    }

    ClassMatchedHandler handleCaracalReady
            = new ClassMatchedHandler<CCBootstrapReady, Status.Internal<CCBootstrapReady>>() {

                @Override
                public void handle(CCBootstrapReady content, Status.Internal<CCBootstrapReady> container) {

                    LOG.info("starting: received schemas");
                    vodSchemaId = content.caracalSchemaData.getId("gvod.metadata");
                    if (vodSchemaId == null) {
                        LOG.error("exception:vod schema undefined shutting down");
                        System.exit(1);
                    }

                    if (dyWS != null) {
                        dyWS.setIsServerDown(false);
                    }
                }
            };

    /**
     * Caracal client gets disconnected.
     */
    ClassMatchedHandler handleCaracalDisconnect
            = new ClassMatchedHandler<CCBootstrapDisconnected, Status.Internal<CCBootstrapDisconnected>>() {

                @Override
                public void handle(CCBootstrapDisconnected content, Status.Internal<CCBootstrapDisconnected> container) {

                    LOG.debug("Caracal client disconnected, need to initiate counter measures.");

                    //Inform the web service if it has already been booted.
                    if (dyWS != null) {
                        dyWS.setIsServerDown(true);
                    }
                }
            };

    private Handler handleHeartbeatReady = new Handler<Status.Internal<CCHeartbeatReady>>() {
        @Override
        public void handle(Status.Internal<CCHeartbeatReady> e) {
            LOG.info("starting: system");
            phase3();
        }
    };

    private void phase3() {
        connectSweep();
        connectSweepSync();
        connectVoDHost();
        startWebservice();
    }

    private void connectVoDHost() {
        vodHostComp = create(HostManagerComp.class, new HostManagerComp.HostManagerInit(new HostManagerKCWrapper(config(), self),
                gvodSyncIFuture, vodSchemaId));
        connect(vodHostComp.getNegative(Network.class), networkComp.getPositive(Network.class), Channel.TWO_WAY);
        connect(vodHostComp.getNegative(Timer.class), timerComp.getPositive(Timer.class), Channel.TWO_WAY);
        connect(vodHostComp.getNegative(CCOperationPort.class), caracalClientComp.getPositive(CCOperationPort.class), Channel.TWO_WAY);
        connect(vodHostComp.getNegative(CCHeartbeatPort.class), heartbeatComp.getPositive(CCHeartbeatPort.class), Channel.TWO_WAY);
        trigger(Start.event, vodHostComp.control());
    }

    private void connectSweep() {
        ElectionConfig electionConfig = new ElectionConfig(ConfigFactory.load());

        sweepHostComp = create(SearchPeer.class, new SearchPeerInit(self, SearchConfiguration.build(),
                electionConfig, GradientConfiguration.build()));
        connect(sweepHostComp.getNegative(Timer.class), timerComp.getPositive(Timer.class), Channel.TWO_WAY);
        connect(sweepHostComp.getNegative(Network.class), networkComp.getPositive(Network.class), Channel.TWO_WAY);
        connect(sweepHostComp.getNegative(CCHeartbeatPort.class), heartbeatComp.getPositive(CCHeartbeatPort.class), Channel.TWO_WAY);
        trigger(Start.event, sweepHostComp.control());
    }

    private void connectSweepSync() {
        sweepSyncComp = create(SweepSyncComponent.class, Init.NONE);
        sweepSyncI = (SweepSyncI) sweepSyncComp.getComponent();
        connect(sweepSyncComp.getNegative(UiPort.class), sweepHostComp.getPositive(UiPort.class));
        trigger(Start.event, sweepSyncComp.control());
    }

    private void startWebservice() {
        LOG.info("starting webservice");

        try {
            dyWS = new DYWS(sweepSyncI, gvodSyncIFuture);
            Config config = ConfigFactory.load();
            String[] args = new String[]{"server", config.getString("webservice.server")};
            dyWS.run(args);
        } catch (ConfigException.Missing ex) {
            LOG.error("bad configuration, could not find webservice.server");
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            LOG.error("webservice error");
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) throws IOException {
        HeartbeatServiceEnum.CROUPIER.setServiceId((byte) 1);
        VoDHeartbeatServiceEnum.CROUPIER.setServiceId((byte) 2);
        GetIp.NetworkInterfacesMask setIpType = GetIp.NetworkInterfacesMask.PUBLIC;
        if (args.length == 1 && args[0].equals("-tenDot")) {
            setIpType = GetIp.NetworkInterfacesMask.TEN_DOT_PRIVATE;
        }
        DYWSLauncher.setIpType(setIpType);

        if (Kompics.isOn()) {
            Kompics.shutdown();
        }
        Kompics.createAndStart(DYWSLauncher.class, Runtime.getRuntime().availableProcessors(), 20); // Yes 20 is totally arbitrary
        try {
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            System.exit(1);
        }
    }
}
