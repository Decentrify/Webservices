//package se.kth.ws.sweep;
//
//import com.typesafe.config.Config;
//import com.typesafe.config.ConfigException;
//import se.sics.caracaldb.Address;
//
//import java.net.InetAddress;
//import java.net.UnknownHostException;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * @author Alex Ormenisan <aaor@kth.se>
// */
//public class BootstrapNodes {
//    public static List<Address> readCaracalBootstrap(Config config) {
//        try {
//            ArrayList<Address> cBootstrap = new ArrayList<Address>();
//            InetAddress ip = InetAddress.getByName(config.getString("caracal.address.ip"));
//            int port = config.getInt("caracal.address.port");
//            cBootstrap.add(new Address(ip, port, null));
//            return cBootstrap;
//        } catch (ConfigException.Missing ex) {
//            throw new RuntimeException("Caracal Bootstrap configuration problem - missing config", ex);
//        } catch (UnknownHostException ex) {
//            throw new RuntimeException("Caracal Bootstrap configuration problem - bad ip", ex);
//        }
//    }
//}