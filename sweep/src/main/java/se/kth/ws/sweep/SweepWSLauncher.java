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
package se.kth.ws.sweep;

import se.kth.ws.sweep.core.SweepSyncI;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.util.EnumSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.ws.sweep.core.SweepSyncComponent;
import se.sics.caracaldb.MessageRegistrator;
import se.sics.gvod.config.GradientConfiguration;
import se.sics.gvod.config.SearchConfiguration;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Kompics;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.ktoolbox.aggregator.AggregatorSerializerSetup;
import se.sics.ktoolbox.cc.bootstrap.CCBootstrapComp;
import se.sics.ktoolbox.cc.bootstrap.CCBootstrapPort;
import se.sics.ktoolbox.cc.common.config.CaracalClientConfig;
import se.sics.ktoolbox.cc.common.op.CCSimpleReady;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatComp;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatPort;
import se.sics.ktoolbox.ipsolver.IpSolverComp;
import se.sics.ktoolbox.ipsolver.IpSolverPort;
import se.sics.ktoolbox.ipsolver.msg.GetIp;
import se.sics.ms.common.ApplicationSelf;
import se.sics.ms.net.SweepSerializerSetup;
import se.sics.ms.ports.UiPort;
import se.sics.ms.search.SearchPeer;
import se.sics.ms.search.SearchPeerInit;
import se.sics.ms.util.HeartbeatServiceEnum;

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
 * FIXME: Fix the launcher with the updated SystemConfig.
 */
public class SweepWSLauncher extends ComponentDefinition {

    private Logger LOG = LoggerFactory.getLogger(SweepWSLauncher.class);

    private static GetIp.NetworkInterfacesMask ipType;

    public static void setIpType(GetIp.NetworkInterfacesMask setIpType) {
        ipType = setIpType;
    }

    private Component ipSolver;
    private Component network;
    private Component timer;
    private Component sweep;
    private Component sweepSync;
    private SweepWS sweepWS;
    private Config config;
    SystemConfig systemConfig;
    private Component ccBootstrap;
    private Component ccHeartbeat;

    public SweepWSLauncher() {


        LOG.info("initiating...");
        if (ipType == null) {
            LOG.error("launcher logic error - ipType not set");
            throw new RuntimeException("launcher logic error - ipType not set");
        }

        int startId = 128;
        registerSerializers(startId);

        subscribe(handleStart, control);
        subscribe(handleStop, control);

        ipSolver = create(IpSolverComp.class, new IpSolverComp.IpSolverInit());
    }

    // TODO Abhi - this should be part of sweep and also rename the SerializerSetup to SweepSerializerSetup
    private void registerSerializers(int startId) {
        int currentId = startId;
        currentId = BasicSerializerSetup.registerBasicSerializers(currentId);
        currentId = CroupierSerializerSetup.registerSerializers(currentId);
        currentId = GradientSerializerSetup.registerSerializers(currentId);
        currentId = ElectionSerializerSetup.registerSerializers(currentId);
        currentId = AggregatorSerializerSetup.registerSerializers(currentId);
        currentId = ChunkManagerSerializerSetup.registerSerializers(currentId);
        SweepSerializerSetup.registerSerializers(currentId);
        MessageRegistrator.register();
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {

            LOG.info("starting: solving ip...");
            Positive<IpSolverPort> ipSolverPort = ipSolver.getPositive(IpSolverPort.class);
            subscribe(handleGetIp, ipSolverPort);

            trigger(new GetIp.Req(EnumSet.of(ipType)), ipSolverPort);
        }
    };

    public Handler handleGetIp = new Handler<GetIp.Resp>() {
        @Override
        public void handle(GetIp.Resp resp) {
            LOG.info("starting system");

            InetAddress ip = null;
            if (!resp.addrs.isEmpty()) {
                ip = resp.addrs.get(0).getAddr();
                if (resp.addrs.size() > 1) {
                    LOG.warn("multiple ips detected, proceeding with:{}", ip);
                }
            }
            connectComponents(ip);
        }
    };

    Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            LOG.info("stopping...");
        }
    };

    private void connectComponents(InetAddress ip) {

        config = ConfigFactory.load();
//        systemConfig = new SystemConfig(config, ip);

        timer = create(JavaTimer.class, Init.NONE);
        trigger(Start.event, timer.control());
        network = create(NettyNetwork.class, new NettyInit(systemConfig.self));
        trigger(Start.event, network.control());

        CaracalClientConfig ccConfig = new CaracalClientConfig(config);
        ccBootstrap = create(CCBootstrapComp.class, new CCBootstrapComp.CCBootstrapInit(systemConfig, ccConfig, BootstrapNodes.readCaracalBootstrap(config)));
        connect(ccBootstrap.getNegative(Network.class), network.getPositive(Network.class));
        connect(ccBootstrap.getNegative(Timer.class), timer.getPositive(Timer.class));

        ccHeartbeat = create(CCHeartbeatComp.class, new CCHeartbeatComp.CCHeartbeatInit(systemConfig, ccConfig));
        connect(ccHeartbeat.getNegative(Timer.class), timer.getPositive(Timer.class));
        connect(ccHeartbeat.getNegative(CCBootstrapPort.class), ccBootstrap.getPositive(CCBootstrapPort.class));

        trigger(Start.event, ccBootstrap.control());
        trigger(Start.event, ccHeartbeat.control());

        subscribe(ccReadyHandler, ccHeartbeat.getPositive(CCHeartbeatPort.class));
    }

    /**
     * Handler indicating the readiness of the caracal client.
     *
     */
    Handler<CCSimpleReady> ccReadyHandler = new Handler<CCSimpleReady>() {
        @Override
        public void handle(CCSimpleReady ccReady) {

            LOG.error("Received Caracal Client Ready Event.");
            createNConnectSweep(systemConfig);
            createNConnectSweepSync();
            sweepWS = new SweepWS((SweepSyncI) sweepSync.getComponent());
            startWebservice();
        }
    };

    private void createNConnectSweep(SystemConfig systemConfig) {

        GradientConfig gradientConfig = new GradientConfig(config);
        CroupierConfig croupierConfig = new CroupierConfig(config);
        ElectionConfig electionConfig = new ElectionConfig(config);
        ChunkManagerConfig chunkManagerConfig = new ChunkManagerConfig(config);
        TreeGradientConfig treeGradientConfig = new TreeGradientConfig(config);

        sweep = create(SearchPeer.class, new SearchPeerInit(systemConfig, croupierConfig,
                SearchConfiguration.build(), GradientConfiguration.build(),
                chunkManagerConfig, gradientConfig, electionConfig, treeGradientConfig));
        connect(timer.getPositive(Timer.class), sweep.getNegative(Timer.class));
        connect(network.getPositive(Network.class), sweep.getNegative(Network.class));
        connect(ccHeartbeat.getPositive(CCHeartbeatPort.class), sweep.getNegative(CCHeartbeatPort.class));

        trigger(Start.event, sweep.control());
    }

    private void createNConnectSweepSync() {
        sweepSync = create(SweepSyncComponent.class, Init.NONE);
        connect(sweep.getPositive(UiPort.class), sweepSync.getNegative(UiPort.class));
        trigger(Start.event, sweepSync.control());
    }

    private void startWebservice() {
        LOG.info("starting webservice");
        String[] args = null;
        try {
            args = new String[]{"server", config.getString("webservice.server")};
            sweepWS.run(args);
        } catch (ConfigException.Missing ex) {
            LOG.error("bad configuration, could not find webservice.server");
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            LOG.error("webservice error");
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) throws IOException {
        HeartbeatServiceEnum.CROUPIER.setServiceId((byte)1);
        GetIp.NetworkInterfacesMask setIpType = GetIp.NetworkInterfacesMask.PUBLIC;
        if (args.length == 1 && args[0].equals("-tenDot")) {
            setIpType = GetIp.NetworkInterfacesMask.TEN_DOT_PRIVATE;
        }
        if (Kompics.isOn()) {
            Kompics.shutdown();
        }
//        MsConfig.init(args);
        System.setProperty("java.net.preferIPv4Stack", "true");
        SweepWSLauncher.setIpType(setIpType);
        Kompics.createAndStart(SweepWSLauncher.class, Runtime.getRuntime().availableProcessors(), 20); // Yes 20 is totally arbitrary
        try {
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            System.exit(1);
        }
    }
}
