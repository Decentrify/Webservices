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
import com.typesafe.config.Config;
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
import se.sics.ms.net.SerializerSetup;
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
import se.sics.nat.NatSerializerSetup;
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
    private Component ipSolverComp;
    private Component natDetectionComp;
    private Component natComp;
    private Component globalCroupierComp;
    private Component caracalClientComp;
    private Component heartbeatComp;
    private Component sweepHostComp;
    private Component sweepSyncComp;
    private Component vodHostComp;
    private DYWS dyWS;
    private SystemConfigBuilder systemConfigBuilder;
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
        this.systemConfigBuilder = new SystemConfigBuilder(ConfigFactory.load());
        gvodSyncIFuture = SettableFuture.create();

        subscribe(handleStart, control);
        subscribe(handleStop, control);
    }

    //*****************************CONTROL**************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting - timer, ipSolver", logPrefix);
            connectTimer();
            connectIpSolver();
        }
    };

    Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            LOG.info("{}stopping...", logPrefix);
        }
    };

    @Override
    public Fault.ResolveAction handleFault(Fault fault) {
        LOG.error("{}child component failure:{}", logPrefix, fault);
        System.exit(1);
        return Fault.ResolveAction.RESOLVED;
    }

    //****************************ADDRESS_DETECTION*****************************
    private void connectTimer() {
        timerComp = create(JavaTimer.class, Init.NONE);
        trigger(Start.event, timerComp.control());
    }

    private void connectIpSolver() {
        ipSolverComp = create(IpSolverComp.class, new IpSolverComp.IpSolverInit());
        subscribe(handleGetIp, ipSolverComp.getPositive(IpSolverPort.class));
        trigger(Start.event, ipSolverComp.control());
        trigger(new GetIp.Req(EnumSet.of(GetIp.NetworkInterfacesMask.ALL)), ipSolverComp.getPositive(IpSolverPort.class));
    }

    public Handler handleGetIp = new Handler<GetIp.Resp>() {
        @Override
        public void handle(GetIp.Resp resp) {
            LOG.info("{}received ips:{}", logPrefix, resp.addrs);
            if (!resp.addrs.isEmpty()) {
                Iterator<IpAddressStatus> it = resp.addrs.iterator();
                while (it.hasNext()) {
                    IpAddressStatus next = it.next();
                    if (IpHelper.isPublic(next.getAddr())) {
                        localIp = next.getAddr();
                        break;
                    }
                }
                if (localIp == null) {
                    it = resp.addrs.iterator();
                    while (it.hasNext()) {
                        IpAddressStatus next = it.next();
                        if (IpHelper.isPrivate(next.getAddr())) {
                            localIp = next.getAddr();
                            break;
                        }
                    }
                }
                if (localIp == null) {
                    localIp = resp.addrs.get(0).getAddr();
                }
                if (resp.addrs.size() > 1) {
                    LOG.warn("{}multiple ips detected, proceeding with:{}", logPrefix, localIp);
                }
                LOG.info("{}starting: private ip:{}", logPrefix, localIp);
                LOG.info("{}starting: stunClient", logPrefix);
                connectNatDetection();
            } else {
                LOG.error("{}no private ip detected", logPrefix);
                throw new RuntimeException("no private ip detected");
            }
        }
    };

    private void connectNatDetection() {
        natDetectionComp = create(NatDetectionComp.class, new NatDetectionComp.NatDetectionInit(
                new BasicAddress(localIp, systemConfigBuilder.getSelfPort(), systemConfigBuilder.getSelfId()),
                new NatInitHelper(ConfigFactory.load()),
                new SCNetworkHook.Definition() {

                    @Override
                    public SCNetworkHook.InitResult setUp(ComponentProxy proxy, SCNetworkHook.Init hookInit) {
                        Component[] comp = new Component[1];
                        LOG.info("{}binding on stun:{}", new Object[]{logPrefix, hookInit.adr});
                        //network
                        comp[0] = proxy.create(NettyNetwork.class, new NettyInit(hookInit.adr));
                        proxy.trigger(Start.event, comp[0].control());
                       
                        return new SCNetworkHook.InitResult(comp[0].getPositive(Network.class), comp);
                    }

                    @Override
                    public void tearDown(ComponentProxy proxy, SCNetworkHook.Tear hookTear) {
                        proxy.trigger(Stop.event, hookTear.components[0].control());
                    }
                }));

        connect(natDetectionComp.getNegative(Timer.class), timerComp.getPositive(Timer.class));
        subscribe(handleNatReady, natDetectionComp.getPositive(NatDetectionPort.class));
        trigger(Start.event, natDetectionComp.control());
    }

    private Handler handleNatReady = new Handler<NatReady>() {
        @Override
        public void handle(NatReady ready) {
            LOG.info("{}nat detected:{} public ip:{} private ip:{}",
                    new Object[]{logPrefix, ready.nat, ready.publicIp, localIp});
            systemConfigBuilder.setSelfIp(ready.publicIp);
            systemConfigBuilder.setSelfNat(ready.nat);
            if (ready.nat.type.equals(Nat.Type.UPNP)) {
                subscribe(handleMapPorts, natDetectionComp.getPositive(UpnpPort.class));
                subscribe(handleUnmapPorts, natDetectionComp.getPositive(UpnpPort.class));
                Map<Integer, Pair<Protocol, Integer>> mapPort = new HashMap<Integer, Pair<Protocol, Integer>>();
                mapPort.put(systemConfigBuilder.getSelfPort(), Pair.with(Protocol.UDP, systemConfigBuilder.getSelfPort()));
                trigger(new MapPorts.Req(UUID.randomUUID(), mapPort), natDetectionComp.getPositive(UpnpPort.class));
            } else {
                buildSysConfig();
                connectRest();
            }
        }
    };
    
     Handler handleMapPorts = new Handler<MapPorts.Resp>() {
        @Override
        public void handle(MapPorts.Resp resp) {
            LOG.info("{}received map:{}", logPrefix, resp.ports);
            int localPort = systemConfigBuilder.getSelfPort();
            int upnpPort = resp.ports.get(systemConfigBuilder.getSelfPort()).getValue1();
            if (localPort != upnpPort) {
                //TODO Alex - fix
                LOG.error("{}not handling yet upnp port different than local");
                throw new RuntimeException("not handling yet upnp port different than local");
            }
            buildSysConfig();
            connectRest();
        }
    };

    Handler handleUnmapPorts = new Handler<UnmapPorts.Resp>() {
        @Override
        public void handle(UnmapPorts.Resp resp) {
            LOG.info("received unmap:{}", resp.ports);
        }
    };
    
    private void buildSysConfig() {
//        initiateSocketBind();
        LOG.debug("{}Socket successfully bound to ip :{} and port: {}", 
                new Object[]{logPrefix, systemConfigBuilder.getSelfIp(), systemConfigBuilder.getSelfPort()});

        LOG.debug("{}Building the system configuration.");
        systemConfig = systemConfigBuilder.build();
    }

    /**
     * Try to bind on the socket and keep a reference of the socket.
     */
    private void initiateSocketBind() {

        LOG.debug("{}Initiating the binding on the socket to keep the port being used by some other service.", logPrefix);

        int retries = BIND_RETRY;
        Socket socket;
        while (retries > 0) {

//          Port gets updated, so needs to be reset.
            Integer selfPort = systemConfigBuilder.getSelfPort();

            try {
                
                LOG.debug("{}Trying to bind on the socket1 with ip: {} and port: {}", 
                        new Object[]{logPrefix, localIp, selfPort});
                socket = new Socket();
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(localIp, selfPort));
                socket.close();
                break;  // If exception is not thrown, break the loop.
            } catch (IOException e) {

                LOG.debug("{}Socket Bind failed, retrying.", logPrefix);
                systemConfigBuilder.setPort();
            }

            retries--;
        }

        if (retries <= 0) {
            LOG.error("{}Unable to bind on a socket, exiting.", logPrefix);
            throw new RuntimeException("Unable to identify port for the socket to bind on.");
        }

    }

    //************************BASIC_SERVICES************************************
    private void connectRest() {
        connectNatCroupier();
        connectCaracal();

        subscribe(handleCaracalDisconnect, caracalClientComp.getPositive(CCBootstrapPort.class));
        subscribe(handleCaracalReady, caracalClientComp.getPositive(CCBootstrapPort.class));
        subscribe(handleHeartbeatReady, heartbeatComp.getPositive(CCHeartbeatPort.class));
    }

    private void connectNatCroupier() {
        CroupierConfig croupierConfig = new CroupierConfig(systemConfig.config);
        NatInitHelper natInit = new NatInitHelper(systemConfig.config);
        globalCroupierComp = create(CroupierComp.class, new CroupierComp.CroupierInit(systemConfig, croupierConfig, natInit.globalCroupierOverlayId));
        natComp = create(NatTraverserComp.class, new NatTraverserComp.NatTraverserInit(
                systemConfig,
                new NatInitHelper(ConfigFactory.load()),
                new NatNetworkHook.Definition() {

                    @Override
                    public NatNetworkHook.InitResult setUp(ComponentProxy proxy, NatNetworkHook.Init hookInit) {
                        Component[] comp = new Component[2];
                        if (!localIp.equals(hookInit.adr.getIp())) {
                            LOG.info("{}binding on private:{}", logPrefix, localIp.getHostAddress());
                            System.setProperty("altBindIf", localIp.getHostAddress());
                        }
                        LOG.info("{}binding on nat:{}", new Object[]{logPrefix, hookInit.adr});
                        //network
                        comp[0] = proxy.create(NettyNetwork.class, new NettyInit(hookInit.adr));
                        proxy.trigger(Start.event, comp[0].control());
                        
                         //chunkmanager
                        comp[1] = proxy.create(ChunkManagerComp.class, new ChunkManagerComp.CMInit(systemConfig, new ChunkManagerConfig(systemConfig.config)));
                        proxy.connect(comp[1].getNegative(Network.class), comp[0].getPositive(Network.class));
                        proxy.connect(comp[1].getNegative(Timer.class), hookInit.timer);
                        proxy.trigger(Start.event, comp[1].control());
                        return new NatNetworkHook.InitResult(comp[1].getPositive(Network.class), comp);
                    }

                    @Override
                    public void tearDown(ComponentProxy proxy, NatNetworkHook.Tear hookTear) {
                        proxy.trigger(Stop.event, hookTear.components[0].control());
                        proxy.trigger(Stop.event, hookTear.components[1].control());
                        proxy.disconnect(hookTear.components[1].getNegative(Network.class), hookTear.components[0].getPositive(Network.class));
                        proxy.disconnect(hookTear.components[1].getNegative(Timer.class), hookTear.timer);
                    }

                },
                new CroupierConfig(ConfigFactory.load())));

        connect(globalCroupierComp.getNegative(Timer.class), timerComp.getPositive(Timer.class));
        connect(globalCroupierComp.getNegative(SelfAddressUpdatePort.class), natComp.getPositive(SelfAddressUpdatePort.class));
        connect(globalCroupierComp.getNegative(Network.class), natComp.getPositive(Network.class), new IntegerOverlayFilter(natInit.globalCroupierOverlayId));
        connect(natComp.getNegative(Timer.class), timerComp.getPositive(Timer.class));
        connect(natComp.getNegative(CroupierPort.class), globalCroupierComp.getPositive(CroupierPort.class));

        trigger(Start.event, natComp.control());
        trigger(Start.event, globalCroupierComp.control());
        trigger(new CroupierUpdate(new GlobalCroupierView()), globalCroupierComp.getNegative(SelfViewUpdatePort.class));
        trigger(new CroupierJoin(natInit.croupierBoostrap), globalCroupierComp.getPositive(CroupierControlPort.class));
    }

    private void connectCaracal() {
        CaracalClientConfig ccConfig = new CaracalClientConfig(systemConfig.config);
        caracalClientComp = create(CCBootstrapComp.class, new CCBootstrapComp.CCBootstrapInit(systemConfig, ccConfig, ConfigHelper.readCaracalBootstrap(systemConfig.config)));
        connect(caracalClientComp.getNegative(Timer.class), timerComp.getPositive(Timer.class));
        connect(caracalClientComp.getNegative(Network.class), natComp.getPositive(Network.class));
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
        connectSweep();
        connectSweepSync();
        connectVoDHost();
        startWebservice();
    }

    private void connectVoDHost() {
        vodHostComp = create(HostManagerComp.class, new HostManagerComp.HostManagerInit(new HostManagerConfig(systemConfig.config), gvodSyncIFuture, vodSchemaId));
        connect(vodHostComp.getNegative(Network.class), natComp.getPositive(Network.class));
        connect(vodHostComp.getNegative(Timer.class), timerComp.getPositive(Timer.class));
        connect(vodHostComp.getNegative(CCBootstrapPort.class), caracalClientComp.getPositive(CCBootstrapPort.class));
        connect(vodHostComp.getNegative(CCHeartbeatPort.class), heartbeatComp.getPositive(CCHeartbeatPort.class));
        trigger(Start.event, vodHostComp.control());
    }

    private void connectSweep() {
        GradientConfig gradientConfig = new GradientConfig(systemConfig.config);
        CroupierConfig croupierConfig = new CroupierConfig(systemConfig.config);
        ElectionConfig electionConfig = new ElectionConfig(systemConfig.config);
        ChunkManagerConfig chunkManagerConfig = new ChunkManagerConfig(systemConfig.config);
        TreeGradientConfig treeGradientConfig = new TreeGradientConfig(systemConfig.config);

        sweepHostComp = create(SearchPeer.class, new SearchPeerInit(systemConfig, croupierConfig,
                SearchConfiguration.build(), GradientConfiguration.build(),
                chunkManagerConfig, gradientConfig, electionConfig, treeGradientConfig));
        connect(sweepHostComp.getNegative(Timer.class), timerComp.getPositive(Timer.class));
        connect(sweepHostComp.getNegative(Network.class), natComp.getPositive(Network.class));
        connect(sweepHostComp.getNegative(CCHeartbeatPort.class), heartbeatComp.getPositive(CCHeartbeatPort.class));
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
        serializerId = SerializerSetup.registerSerializers(serializerId);
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
