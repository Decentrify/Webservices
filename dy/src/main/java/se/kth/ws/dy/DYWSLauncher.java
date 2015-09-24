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

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.SettableFuture;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.ws.sweep.core.SweepSyncComponent;
import se.kth.ws.sweep.core.SweepSyncI;
import se.sics.caracaldb.MessageRegistrator;
import se.sics.gvod.common.util.VoDHeartbeatServiceEnum;
import se.sics.gvod.config.GradientConfiguration;
import se.sics.gvod.config.SearchConfiguration;
import se.sics.gvod.manager.toolbox.GVoDSyncI;
import se.sics.gvod.network.GVoDSerializerSetup;
import se.sics.gvod.system.HostManagerComp;
import se.sics.gvod.system.HostManagerConfig;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.ktoolbox.cc.bootstrap.CCBootstrapComp;
import se.sics.ktoolbox.cc.bootstrap.CCBootstrapPort;
import se.sics.ktoolbox.cc.bootstrap.msg.CCDisconnected;
import se.sics.ktoolbox.cc.bootstrap.msg.CCReady;
import se.sics.ktoolbox.cc.common.config.CaracalClientConfig;
import se.sics.ktoolbox.cc.common.op.CCSimpleReady;
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
import se.sics.p2ptoolbox.util.helper.SystemConfigBuilder;
import se.sics.p2ptoolbox.util.serializer.BasicSerializerSetup;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.javatuples.Pair;
import se.sics.ktoolbox.aggregator.AggregatorSerializerSetup;
import se.sics.ktoolbox.ipsolver.util.IpAddressStatus;
import se.sics.ktoolbox.ipsolver.util.IpHelper;
import se.sics.nat.NatDetectionComp;
import se.sics.nat.NatDetectionPort;
import se.sics.nat.NatInitHelper;
import se.sics.nat.NatLauncherProxy;
import se.sics.nat.NatSerializerSetup;
import se.sics.nat.NatSetup;
import se.sics.nat.NatSetupResult;
import se.sics.nat.NatTraverserComp;
import se.sics.nat.common.croupier.GlobalCroupierView;
import se.sics.nat.hooks.NatNetworkHook;
import se.sics.nat.hp.SHPSerializerSetup;
import se.sics.nat.pm.PMSerializerSetup;
import se.sics.nat.stun.NatReady;
import se.sics.nat.stun.StunSerializerSetup;
import se.sics.nat.stun.client.SCNetworkHook;
import se.sics.nat.stun.upnp.UpnpPort;
import se.sics.nat.stun.upnp.msg.MapPorts;
import se.sics.nat.stun.upnp.msg.UnmapPorts;
import se.sics.nat.stun.upnp.util.Protocol;
import se.sics.p2ptoolbox.chunkmanager.ChunkManagerComp;
import se.sics.p2ptoolbox.croupier.CroupierComp;
import se.sics.p2ptoolbox.croupier.CroupierControlPort;
import se.sics.p2ptoolbox.croupier.CroupierPort;
import se.sics.p2ptoolbox.croupier.msg.CroupierJoin;
import se.sics.p2ptoolbox.croupier.msg.CroupierUpdate;
import se.sics.p2ptoolbox.util.config.ConfigHelper;
import se.sics.p2ptoolbox.util.filters.IntegerOverlayFilter;
import se.sics.p2ptoolbox.util.nat.Nat;
import se.sics.p2ptoolbox.util.nat.NatedTrait;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.proxy.ComponentProxy;
import se.sics.p2ptoolbox.util.traits.AcceptedTraits;
import se.sics.p2ptoolbox.util.update.SelfAddressUpdatePort;
import se.sics.p2ptoolbox.util.update.SelfViewUpdatePort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class DYWSLauncher extends ComponentDefinition {

    private Logger LOG = LoggerFactory.getLogger(DYWSLauncher.class);
    private String logPrefix = "";
    private static GetIp.NetworkInterfacesMask ipType;

    public static void setIpType(GetIp.NetworkInterfacesMask setIpType) {
        ipType = setIpType;
    }
    private static final int BIND_RETRY = 3;

    private Component timerComp;
    private Positive<Network> network;
    private Positive<SelfAddressUpdatePort> adrUpdate;
    private Positive<CroupierPort> globalCroupier;
    private Component caracalClientComp;
    private Component heartbeatComp;
    private Component sweepHostComp;
    private Component sweepSyncComp;
    private Component vodHostComp;
    private DYWS dyWS;
    
    private SystemConfig systemConfig;

    private SettableFuture<GVoDSyncI> gvodSyncIFuture;
    private SweepSyncI sweepSyncI;
    
    private InetAddress localIp;
    private byte[] vodSchemaId = null;
    //**************************************************************************

    public DYWSLauncher() {
        LOG.info("{}initiating...", logPrefix);
        if (ipType == null) {
            LOG.error("launcher logic error - ipType not set");
            System.exit(1);
        }
        gvodSyncIFuture = SettableFuture.create();

        subscribe(handleStart, control);
    }

   //*****************************CONTROL**************************************
    private Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting", logPrefix);
            connectNStartTimer();
            connectNStartNat();
            LOG.info("{}waiting for nat", logPrefix);
        }
    };

    private void connectNStartTimer() {
        timerComp = create(JavaTimer.class, Init.NONE);
        trigger(Start.event, timerComp.control());
    }

    private void connectNStartNat() {
        NatSetup natSetup = new NatSetup(new DYWSLauncherProxy(),
                timerComp.getPositive(Timer.class),
                new SystemConfigBuilder(ConfigFactory.load()));
        natSetup.setup();
        natSetup.start(false);
    }

    private void connectNStartApp() {
        connectCaracal();

        subscribe(handleCaracalDisconnect, caracalClientComp.getPositive(CCBootstrapPort.class));
        subscribe(handleCaracalReady, caracalClientComp.getPositive(CCBootstrapPort.class));
        subscribe(handleHeartbeatReady, heartbeatComp.getPositive(CCHeartbeatPort.class));
    }

    public class DYWSLauncherProxy implements NatLauncherProxy {

        @Override
        public void startApp(NatSetupResult result) {
            DYWSLauncher.this.network = result.network;
            DYWSLauncher.this.adrUpdate = result.adrUpdate;
            DYWSLauncher.this.globalCroupier = result.globalCroupier;
            DYWSLauncher.this.systemConfig = result.systemConfig;
            LOG.info("{}nat started with:{}", logPrefix, result.systemConfig.self);
            DYWSLauncher.this.connectNStartApp();
        }

        @Override
        public <P extends PortType> Positive<P> requires(Class<P> portType) {
            return DYWSLauncher.this.requires(portType);
        }

        @Override
        public <P extends PortType> Negative<P> provides(Class<P> portType) {
            return DYWSLauncher.this.provides(portType);
        }

        @Override
        public Negative<ControlPort> getControlPort() {
            return DYWSLauncher.this.control;
        }

        @Override
        public <T extends ComponentDefinition> Component create(Class<T> definition, Init<T> initEvent) {
            return DYWSLauncher.this.create(definition, initEvent);
        }

        @Override
        public <T extends ComponentDefinition> Component create(Class<T> definition, Init.None initEvent) {
            return DYWSLauncher.this.create(definition, initEvent);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative) {
            return DYWSLauncher.this.connect(positive, negative);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative, ChannelFilter filter) {
            return DYWSLauncher.this.connect(positive, negative, filter);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive) {
            return DYWSLauncher.this.connect(negative, positive);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive, ChannelFilter filter) {
            return DYWSLauncher.this.connect(negative, positive, filter);
        }

        @Override
        public <P extends PortType> void disconnect(Negative<P> negative, Positive<P> positive) {
            DYWSLauncher.this.disconnect(negative, positive);
        }

        @Override
        public <P extends PortType> void disconnect(Positive<P> positive, Negative<P> negative) {
            DYWSLauncher.this.disconnect(positive, negative);
        }

        @Override
        public <P extends PortType> void trigger(KompicsEvent e, Port<P> p) {
            DYWSLauncher.this.trigger(e, p);
        }

        @Override
        public <E extends KompicsEvent, P extends PortType> void subscribe(Handler<E> handler, Port<P> port) throws ConfigurationException {
            DYWSLauncher.this.subscribe(handler, port);
        }
    }

    //************************BASIC_SERVICES************************************
    private void connectCaracal() {
        CaracalClientConfig ccConfig = new CaracalClientConfig(systemConfig.config);
        caracalClientComp = create(CCBootstrapComp.class, new CCBootstrapComp.CCBootstrapInit(systemConfig, ccConfig, ConfigHelper.readCaracalBootstrap(systemConfig.config)));
        connect(caracalClientComp.getNegative(Timer.class), timerComp.getPositive(Timer.class));
        connect(caracalClientComp.getNegative(Network.class), network);
        trigger(Start.event, caracalClientComp.control());

        heartbeatComp = create(CCHeartbeatComp.class, new CCHeartbeatComp.CCHeartbeatInit(systemConfig, ccConfig));
        connect(heartbeatComp.getNegative(Timer.class), timerComp.getPositive(Timer.class));
        connect(heartbeatComp.getNegative(CCBootstrapPort.class), caracalClientComp.getPositive(CCBootstrapPort.class));
        trigger(Start.event, heartbeatComp.control());
    }

    private Handler handleCaracalReady = new Handler<CCReady>() {
        @Override
        public void handle(CCReady event) {
            LOG.info("{}starting: caracal ready", logPrefix);
            vodSchemaId = event.caracalSchemaData.getId("gvod.metadata");
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
    private Handler<CCDisconnected> handleCaracalDisconnect = new Handler<CCDisconnected>() {
        @Override
        public void handle(CCDisconnected event) {

            LOG.warn("{} caracal disconnected", logPrefix);

//          Inform the web service if it has already been booted.
            if (dyWS != null) {
                dyWS.setIsServerDown(true);
            }
        }
    };

    private Handler handleHeartbeatReady = new Handler<CCSimpleReady>() {
        @Override
        public void handle(CCSimpleReady e) {
            LOG.info("{}starting: heartbeat ready", logPrefix);
            connectApplication();
        }
    };

    //***************************APPLICATION************************************
    private void connectApplication() {
        connectNStartSweep();
        connectSweepSync();
        connectNStartVoDHost();
        startWebservice();
    }

    private void connectNStartVoDHost() {
        vodHostComp = create(HostManagerComp.class, new HostManagerComp.HostManagerInit(new HostManagerConfig(systemConfig.config), gvodSyncIFuture, vodSchemaId));
        connect(vodHostComp.getNegative(Network.class), network);
        connect(vodHostComp.getNegative(Timer.class), timerComp.getPositive(Timer.class));
        connect(vodHostComp.getNegative(CCBootstrapPort.class), caracalClientComp.getPositive(CCBootstrapPort.class));
        connect(vodHostComp.getNegative(CCHeartbeatPort.class), heartbeatComp.getPositive(CCHeartbeatPort.class));
        connect(vodHostComp.getNegative(SelfAddressUpdatePort.class), adrUpdate);
        trigger(Start.event, vodHostComp.control());
    }

    private void connectNStartSweep() {
        GradientConfig gradientConfig = new GradientConfig(systemConfig.config);
        CroupierConfig croupierConfig = new CroupierConfig(systemConfig.config);
        ElectionConfig electionConfig = new ElectionConfig(systemConfig.config);
        ChunkManagerConfig chunkManagerConfig = new ChunkManagerConfig(systemConfig.config);
        TreeGradientConfig treeGradientConfig = new TreeGradientConfig(systemConfig.config);

        sweepHostComp = create(SearchPeer.class, new SearchPeerInit(systemConfig, croupierConfig,
                SearchConfiguration.build(), GradientConfiguration.build(),
                chunkManagerConfig, gradientConfig, electionConfig, treeGradientConfig));
        connect(sweepHostComp.getNegative(Timer.class), timerComp.getPositive(Timer.class));
        connect(sweepHostComp.getNegative(Network.class), network);
        connect(sweepHostComp.getNegative(CCHeartbeatPort.class), heartbeatComp.getPositive(CCHeartbeatPort.class));
        connect(sweepHostComp.getNegative(SelfAddressUpdatePort.class), adrUpdate);
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
            String[] args = new String[]{"server", systemConfig.config.getString("webservice.server")};
            dyWS.run(args);
        } catch (ConfigException.Missing ex) {
            LOG.error("bad configuration, could not find webservice.server");
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            LOG.error("webservice error");
            throw new RuntimeException(ex);
        }
    }

    private static void systemSetup() {
        MessageRegistrator.register();
        int serializerId = 128;
        serializerId = BasicSerializerSetup.registerBasicSerializers(serializerId);
        serializerId = StunSerializerSetup.registerSerializers(serializerId);
        serializerId = CroupierSerializerSetup.registerSerializers(serializerId);
        serializerId = PMSerializerSetup.registerSerializers(serializerId);
        serializerId = SHPSerializerSetup.registerSerializers(serializerId);
        serializerId = NatSerializerSetup.registerSerializers(serializerId);
        serializerId = GradientSerializerSetup.registerSerializers(serializerId);
        serializerId = ElectionSerializerSetup.registerSerializers(serializerId);
        serializerId = AggregatorSerializerSetup.registerSerializers(serializerId);
        serializerId = ChunkManagerSerializerSetup.registerSerializers(serializerId);
        serializerId = SweepSerializerSetup.registerSerializers(serializerId);
        serializerId = GVoDSerializerSetup.registerSerializers(serializerId);

        if (serializerId > 255) {
            throw new RuntimeException("switch to bigger serializerIds, last serializerId:" + serializerId);
        }

        ImmutableMap acceptedTraits = ImmutableMap.of(NatedTrait.class, 0);
        DecoratedAddress.setAcceptedTraits(new AcceptedTraits(acceptedTraits));
    }

    public static void main(String[] args) throws IOException {
        HeartbeatServiceEnum.CROUPIER.setServiceId((byte) 1);
        VoDHeartbeatServiceEnum.CROUPIER.setServiceId((byte) 2);
        GetIp.NetworkInterfacesMask setIpType = GetIp.NetworkInterfacesMask.PUBLIC;
        if (args.length == 1 && args[0].equals("-tenDot")) {
            setIpType = GetIp.NetworkInterfacesMask.TEN_DOT_PRIVATE;
        }
        DYWSLauncher.setIpType(setIpType);

        systemSetup();

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
