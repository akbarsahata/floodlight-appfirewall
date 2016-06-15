package net.floodlightcontroller.appfirewall;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.core.IFloodlightProviderService;

import net.floodlightcontroller.appfirewall.DataHandling;
import net.floodlightcontroller.appfirewall.AppFirewallWebRoutable;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.Set;

import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppFirewall implements IOFMessageListener, IFloodlightModule {
	
	protected IFloodlightProviderService floodlightProvider;
	protected IRestApiService restApiProvider;
	protected Set<Long> macAddresses;
	protected static Logger logger;
	
	public static DataHandling dh;
	
	@Override
	public String getName() {
		return AppFirewall.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IRestApiService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		restApiProvider = context.getServiceImpl(IRestApiService.class);
	    macAddresses = new ConcurrentSkipListSet<Long>();
	    logger = LoggerFactory.getLogger(AppFirewall.class);
	    
	    AppFirewall.dh = new DataHandling();
	    try {
			List<String> urls = Files.readAllLines(FileSystems.getDefault().getPath("/home/akbarsahata/Floodlight/floodlight/src/main/java/net/floodlightcontroller/appfirewall", "nsfw.txt"), StandardCharsets.UTF_8);
			Iterator<String> it = urls.iterator();
			while (it.hasNext()){
				AppFirewall.dh.addURL(it.next());
			}
		} catch (Exception e){
			System.out.println(e.getMessage());
		}
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		restApiProvider.addRestletRoutable(new AppFirewallWebRoutable());

	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		 
		switch(msg.getType()){
		case PACKET_IN:
			 /* Retrieve the deserialized packet in message */
	        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        	String dstIP = null;
        	
        	byte[] serializedEth = eth.serialize();
	        
	        /*
	         * Check if IPV4
	         */
	        if (eth.getEtherType() == EthType.IPv4){
	        	IPv4 ipv4 = (IPv4) eth.getPayload();
	        	dstIP = ipv4.getDestinationAddress().toString();
	        	
	        	/*
	        	 * Check if TCP
	        	 */
	        	if (ipv4.getProtocol().equals(IpProtocol.TCP)){
	        		TCP tcp = (TCP) ipv4.getPayload();
	        		TransportPort dstPort = tcp.getDestinationPort();
	        		String port = dstPort.toString();
	        		
	        		Data data = (Data) tcp.getPayload();
        			byte[] serializedData = data.serialize();
        			String stringData = new String(serializedData);
        			short flags = tcp.getFlags();
	        		/*
	        		 * Check if HTTP(s) or if trough proxy, IF NOT just flood
	        		 */
	        		if (port.equals("80") && String.valueOf(flags).equals("24")) {
	        			if(stringData.length() > 0) {
	        				int indexHost = stringData.indexOf("Host:");
	        				if (indexHost < 0) {
	        					//logger.info("Host not found");
	        					forwardPacketFlood(sw, serializedEth);
	        				} else {
	        					Thread blocker = new Blocker(stringData, sw, eth, serializedEth);
	        					
	        					try {
	        						blocker.start();
	        					} catch (Exception e) {
	        						logger.info(e.getMessage());
	        					}
	        					
	        				}
	        			/*
	        			 * No payload, just forward
	        			 */
	        			} else {
			        		forwardPacketFlood(sw, serializedEth);
	        			}
	        		} else if (port.equals("8080") && String.valueOf(flags).equals("24")){
	        			if(stringData.length() > 0) {
	        				int indexHost = stringData.indexOf("Host:");
	        				if (indexHost < 0) {
	        					//logger.info("Host not found");
	        					forwardPacketFlood(sw, serializedEth);
	        				} else {
	        					Thread blocker = new BlockerProxy(stringData, sw, eth, serializedEth);
	        					
	        					try {
	        						blocker.start();
	        					} catch (Exception e) {
	        						logger.info(e.getMessage());
	        					}
	        					
	        				}
	        			/*
	        			 * No payload, just forward
	        			 */
	        			} else {
			        		forwardPacketFlood(sw, serializedEth);
	        			}
	        		}else {
	        			//logger.debug("No Payload, FORWARD");
		        		forwardPacketFlood(sw, serializedEth);
	        		}
	        	/*
	        	 * IF NOT TCP, JUST FLOOD IT
	        	 */
	        	} else {
	        		forwardPacketFlood(sw, serializedEth);
	        	}
        	/*
        	 * IF NOT IPV4, JUST FLOOD IT
        	 */
	        } else {
	        	forwardPacketFlood(sw, serializedEth);
	        }
	        break;
		default:
			logger.info("YEAY");
			break;
		}
		 
		/*
		 * Detects host MAC Address when first time connected
		 */
		 Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
	 
	        Long sourceMACHash = eth.getSourceMACAddress().getLong();
	        if (!macAddresses.contains(sourceMACHash)) {
	            macAddresses.add(sourceMACHash);
	            logger.info("MAC Address: {} seen on switch: {}",
	                    eth.getSourceMACAddress().toString(),
	                    sw.getId().toString());
	        }
		return Command.CONTINUE;
	}
	
	private static boolean block(String stringData){
		boolean output;
		String subdir = stringData.substring(stringData.indexOf(" ")+1, stringData.indexOf(" ", stringData.indexOf(" ")+1));
		int indexHost = stringData.indexOf("Host:");
		
		String[] hostAddress = stringData.substring(indexHost, stringData.indexOf("\n", indexHost)).split(" ");
		hostAddress[1] = hostAddress[1].substring(0, hostAddress[1].length() - 1); 
		String url = hostAddress[1]+subdir;
		logger.info(url);
		synchronized(AppFirewall.dh){
			output = AppFirewall.dh.findURL(url);
		}
		return output;
	}
	

	private static boolean blockProxy(String stringData){
		boolean output;
		String subdir = stringData.substring(stringData.indexOf(" ")+1, stringData.indexOf(" ", stringData.indexOf(" ")+1));
		
		String url = subdir;
		logger.info(url);
		synchronized(AppFirewall.dh){
			output = AppFirewall.dh.findURL(url);
		}
		return output;
	}
	
	private void forwardPacketFlood(IOFSwitch sw, byte[] serializedData) {
		List<OFAction> actions = new ArrayList<OFAction>(1);
		actions.add(sw.getOFFactory().actions().output(OFPort.FLOOD, 0xffFFffFF));
		
		OFPacketOut po = sw.getOFFactory().buildPacketOut() 
			    .setData(serializedData)
			    .setActions(actions)
			    .setInPort(OFPort.CONTROLLER)
			    .build();
		
		sw.write(po);
	}
	
	private static void forwardPacketStatic(IOFSwitch sw, byte[] serializedData) {
		List<OFAction> actions = new ArrayList<OFAction>(1);
		actions.add(sw.getOFFactory().actions().output(OFPort.FLOOD, 0xffFFffFF));
		
		OFPacketOut po = sw.getOFFactory().buildPacketOut() 
			    .setData(serializedData)
			    .setActions(actions)
			    .setInPort(OFPort.CONTROLLER)
			    .build();
		
		sw.write(po);
	}
	
	private static void dropPacket(IOFSwitch sw, byte[] serializedData) {
		List<OFAction> actions = new ArrayList<OFAction>(1);
		
		OFPacketOut po = sw.getOFFactory().buildPacketOut() 
			    .setData(serializedData)
			    .setActions(actions)
			    .setInPort(OFPort.CONTROLLER)
			    .build();
		
		sw.write(po);
		
		logger.info("dropping");
	}
	
	private static void send403(IOFSwitch sw, Ethernet eth){
		Ethernet l2 = eth;
		IPv4 l3 = (IPv4) eth.getPayload();
		TCP l4 = (TCP) l3.getPayload();
		
		MacAddress sourceMac = l2.getSourceMACAddress();
		MacAddress dstMac = l2.getDestinationMACAddress();
		
		IPv4Address sourceIp = l3.getSourceAddress();
		IPv4Address dstIp = l3.getDestinationAddress();
		
		TransportPort sourcePort = l4.getSourcePort();
		TransportPort dstPort = l4.getDestinationPort();
		int seq = l4.getAcknowledge();
		
		
		String dataHTML = "HTTP/1.1 403 Forbidden\r\nCache-Control: private\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: 13\r\nConnection: keep-alive\r\n\n403 Forbidden"; //\n<html><head><title>Firewall<title><head></html><body><h1>Restricted!</h1></body>";
		
		Data _l7 = new Data();
		_l7.setData(dataHTML.getBytes());
		
		TCP _l4 = new TCP();
		_l4.setDestinationPort(sourcePort);
		_l4.setSourcePort(dstPort);
		_l4.setSequence(seq);
		_l4.setAcknowledge(l4.getSequence() + l4.getPayload().serialize().length);
		_l4.setFlags(Short.parseShort("24"));
		
		IPv4 _l3 = new IPv4();
		_l3.setDestinationAddress(sourceIp);
		_l3.setSourceAddress(dstIp);
		_l3.setFlags(Byte.parseByte("2"));
		_l3.setTtl(Byte.MAX_VALUE);
		_l3.setFragment(false);
		
		Ethernet _l2 = new Ethernet();
		_l2.setDestinationMACAddress(sourceMac);
		_l2.setSourceMACAddress(dstMac);
		_l2.setEtherType(EthType.IPv4);
		
		_l2.setPayload(_l3);
		_l3.setPayload(_l4);
		_l4.setPayload(_l7);
		
		byte[] serialized = _l2.serialize();
		
		List<OFAction> actions = new ArrayList<OFAction>(1);
		actions.add(sw.getOFFactory().actions().output(OFPort.FLOOD, 0xffFFffFF));
		
		OFPacketOut po = sw.getOFFactory().buildPacketOut() 
			    .setData(serialized)
			    .setActions(actions)
			    .setInPort(OFPort.CONTROLLER)
			    .build();
		
		sw.write(po);
		
		logger.info("sending 403");
	}
	
	class Blocker extends Thread {
		
		private String stringData;
		private IOFSwitch sw;
		private Ethernet eth;
		private byte[] serializedEth;
		private Thread t;
		
		public Blocker(String stringData, IOFSwitch sw, Ethernet eth, byte[] serializedByte){
			this.stringData = stringData;
			this.sw = sw;
			this.eth = eth;
			this.serializedEth = serializedByte;
		}

		@Override
		public void run(){
			if (AppFirewall.block(stringData)){
				AppFirewall.send403(sw, eth);
				AppFirewall.dropPacket(sw, serializedEth);
			} else {
				AppFirewall.forwardPacketStatic(sw, serializedEth);
			}
		}
		
		public void start(){
			if (t == null){
				t = new Thread(this, "blocker thread");
			}
			t.start();
		}
		
	}
	
class BlockerProxy extends Thread {
		
		private String stringData;
		private IOFSwitch sw;
		private Ethernet eth;
		private byte[] serializedEth;
		private Thread t;
		
		public BlockerProxy(String stringData, IOFSwitch sw, Ethernet eth, byte[] serializedByte){
			this.stringData = stringData;
			this.sw = sw;
			this.eth = eth;
			this.serializedEth = serializedByte;
		}

		@Override
		public void run(){
			if (AppFirewall.blockProxy(stringData)){
				AppFirewall.send403(sw, eth);
				AppFirewall.dropPacket(sw, serializedEth);
			} else {
				AppFirewall.forwardPacketStatic(sw, serializedEth);
			}
		}
		
		public void start(){
			if (t == null){
				t = new Thread(this, "blocker thread");
			}
			t.start();
		}
		
	}

}
