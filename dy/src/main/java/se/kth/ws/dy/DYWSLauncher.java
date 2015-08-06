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
import se.kth.ws.sweep.core.SweepSyncI;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.util.EnumSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.ws.sweep.BootstrapNodes;
import se.kth.ws.sweep.core.SweepSyncComponent;
import se.sics.caracaldb.MessageRegistrator;
import se.sics.gvod.common.util.VoDHeartbeatServiceEnum;
import se.sics.gvod.config.GradientConfiguration;
import se.sics.gvod.config.SearchConfiguration;
import se.sics.gvod.manager.toolbox.GVoDSyncI;
import se.sics.gvod.network.GVoDSerializerSetup;
import se.sics.gvod.system.HostManagerComp;
import se.sics.gvod.system.HostManagerConfig;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.ktoolbox.cc.bootstrap.CCBootstrapComp;
import se.sics.ktoolbox.cc.bootstrap.CCBootstrapPort;
import se.sics.ktoolbox.cc.bootstrap.msg.CCReady;
import se.sics.ktoolbox.cc.common.config.CaracalClientConfig;
import se.sics.ktoolbox.cc.common.op.CCSimpleReady;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatComp;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatPort;
import se.sics.ktoolbox.ipsolver.IpSolverComp;
import se.sics.ktoolbox.ipsolver.IpSolverPort;
import se.sics.ktoolbox.ipsolver.msg.GetIp;
import se.sics.ms.common.ApplicationSelf;
import se.sics.ms.net.SerializerSetup;
import se.sics.ms.ports.UiPort;
import se.sics.ms.search.SearchPeer;
import se.sics.ms.search.SearchPeerInit;
import se.sics.ms.util.HeartbeatServiceEnum;
import se.sics.p2ptoolbox.aggregator.network.AggregatorSerializerSetup;
import se.sics.p2ptoolbox.chunkmanager.ChunkManagerConfig;
import se.sics.p2ptoolbox.chunkmanager.ChunkManagerSerializerSetup;
import se.sics.p2ptoolbox.croupier.CroupierConfig;
import se.sics.p2ptoolbox.croupier.CroupierSerializerSetup;
import se.sics.p2ptoolbox.election.core.ElectionConfig;
import se.sics.p2ptoolbox.election.network.ElectionSerializerSetup;
import se.sics.p2ptoolbox.gradient.GradientConfig;
import se.sics.p2ptoolbox.gradient.GradientSerializerSetup;
import se.sics.p2ptoolbox.tgradient.TreeGradientConfig;
import se.sics.p2ptoolbox.util.config.SystemConfig;
import se.sics.p2ptoolbox.util.serializer.BasicSerializerSetup;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class DYWSLauncher extends ComponentDefinition {

    private Logger LOG = LoggerFactory.getLogger(DYWSLauncher.class);
    private static GetIp.NetworkInterfacesMask ipType;

    public static void setIpType(GetIp.NetworkInterfacesMask setIpType) {
        ipType = setIpType;
    }

    private Component timerComp;
    private Component ipSolverComp;
    private Component networkComp;
    private Component caracalClientComp;
    private Component heartbeatComp;
    private Component sweepHostComp;
    private Component sweepSyncComp;
    private Component vodHostComp;
    private DYWS dyWS;
    private Config config;
    private SystemConfig systemConfig;
    private CaracalClientConfig ccConfig;

    private SettableFuture<GVoDSyncI> gvodSyncIFuture;
    private SweepSyncI sweepSyncI;

    private HostManagerConfig gvodConfig;
    
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
        currentId = SerializerSetup.registerSerializers(currentId);
        currentId = GVoDSerializerSetup.registerSerializers(currentId);
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

            InetAddress ip = null;
            if (!resp.addrs.isEmpty()) {
                ip = resp.addrs.get(0).getAddr();
                if (resp.addrs.size() > 1) {
                    LOG.warn("multiple ips detected, proceeding with:{}", ip);
                }
            }

            config = ConfigFactory.load();
            systemConfig = new SystemConfig(config, ip);
            ccConfig = new CaracalClientConfig(config);
            gvodConfig = new HostManagerConfig(config, ip);

            phase2();
        }
    };

    private void phase2() {
        connectNetwork();
        connectCaracalClient();
        connectHeartbeat();
        subscribe(handleCaracalReady, caracalClientComp.getPositive(CCBootstrapPort.class));
        subscribe(handleHeartbeatReady, heartbeatComp.getPositive(CCHeartbeatPort.class));
    }
    
    private void connectNetwork() {
        networkComp = create(NettyNetwork.class, new NettyInit(systemConfig.self));
        trigger(Start.event, networkComp.control());
    }
    
    private void connectCaracalClient() {
        
        caracalClientComp = create(CCBootstrapComp.class, new CCBootstrapComp.CCBootstrapInit(systemConfig, ccConfig, BootstrapNodes.readCaracalBootstrap(config)));
        connect(caracalClientComp.getNegative(Timer.class), timerComp.getPositive(Timer.class));
        connect(caracalClientComp.getNegative(Network.class), networkComp.getPositive(Network.class));
        trigger(Start.event, caracalClientComp.control());
    }

    private void connectHeartbeat() {
        heartbeatComp = create(CCHeartbeatComp.class, new CCHeartbeatComp.CCHeartbeatInit(systemConfig, ccConfig));
        connect(heartbeatComp.getNegative(Timer.class), timerComp.getPositive(Timer.class));
        connect(heartbeatComp.getNegative(CCBootstrapPort.class), caracalClientComp.getPositive(CCBootstrapPort.class));
        trigger(Start.event, heartbeatComp.control());
    }

    private Handler handleCaracalReady = new Handler<CCReady>() {
        @Override
        public void handle(CCReady event) {
            LOG.info("starting: received schemas");
            vodSchemaId = event.caracalSchemaData.getId("gvod.metadata");
            if (vodSchemaId == null) {
                LOG.error("exception:vod schema undefined shutting down");
                System.exit(1);
            }
        }
    };

    private Handler handleHeartbeatReady = new Handler<CCSimpleReady>() {
        @Override
        public void handle(CCSimpleReady e) {
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
        vodHostComp = create(HostManagerComp.class, new HostManagerComp.HostManagerInit(gvodConfig, gvodSyncIFuture, vodSchemaId));
        connect(vodHostComp.getNegative(Network.class), networkComp.getPositive(Network.class));
        connect(vodHostComp.getNegative(Timer.class), timerComp.getPositive(Timer.class));
        connect(vodHostComp.getNegative(CCBootstrapPort.class), caracalClientComp.getPositive(CCBootstrapPort.class));
        connect(vodHostComp.getNegative(CCHeartbeatPort.class), heartbeatComp.getPositive(CCHeartbeatPort.class));
        trigger(Start.event, vodHostComp.control());
    }
    
    private void connectSweep() {
        GradientConfig gradientConfig = new GradientConfig(config);
        CroupierConfig croupierConfig = new CroupierConfig(config);
        ElectionConfig electionConfig = new ElectionConfig(config);
        ChunkManagerConfig chunkManagerConfig = new ChunkManagerConfig(config);
        TreeGradientConfig treeGradientConfig = new TreeGradientConfig(config);

        //TODO Abhi - why aren't you building this applicationSelf in SearchPeer and instead risk me handing this reference to someone else - shared object problem
        ApplicationSelf applicationSelf = new ApplicationSelf(systemConfig.self);
        sweepHostComp = create(SearchPeer.class, new SearchPeerInit(applicationSelf, systemConfig, croupierConfig,
                SearchConfiguration.build(), GradientConfiguration.build(),
                chunkManagerConfig, gradientConfig, electionConfig, treeGradientConfig));
        connect(sweepHostComp.getNegative(Timer.class), timerComp.getPositive(Timer.class));
        connect(sweepHostComp.getNegative(Network.class), networkComp.getPositive(Network.class));
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
