package Peer_Related;

import Logging.logrecord;
import java.io.*;
import java.util.*;


public class PeerProcess {

	static String common_info =  "Common.cfg";
	static String peer_info= "PeerInfo.cfg";
	static Properties common_cfg = new Properties();
	final static LinkedList<PeerData> allPeers = new LinkedList<PeerData>();


	public static void main(String args[]){

		int peerId = Integer.valueOf(args[0].trim());  // taking peerId from arguments 
		String line = null;

		//Parsing Common config file
		
		try {
			BufferedReader buffer = new BufferedReader(new FileReader(common_info));

			while((line=buffer.readLine())!=null) {
				String[] temp = line.split(" ");
				common_cfg.setProperty(temp[0], temp[1]);
			}
			buffer.close();
		}
		catch (Exception e) {
			System.err.println(e);;
		}

		//Parsing PeerInfo config file 
		try {
			BufferedReader buffer = new BufferedReader(new FileReader(peer_info));
			while((line = buffer.readLine())!=null)
			{
				String temp[] = line.split(" ");
				allPeers.add(new PeerData(Integer.valueOf(temp[0].trim()),temp[1],Integer.valueOf(temp[2].trim()),Integer.valueOf(temp[3].trim())));
			}
			buffer.close();
		}
		catch(Exception e) {
			System.err.println(e);;
		}

		logrecord.getLogRecord().setLoggerForPeer(peerId);

		PeerData peer = PeerData.getPeerByPeerId(peerId,allPeers);

		// Starting connections
		try {
			CreatePeers peer_setup = new CreatePeers(common_cfg, allPeers, peer);
			peer_setup.startingThreadsandMethods();
			Thread t = new Thread(peer_setup);
			t.start();
			peer_setup.connectToOtherPeers();
		}
		catch (Exception e)
		{
			System.err.println(e);
		}

	}
}
