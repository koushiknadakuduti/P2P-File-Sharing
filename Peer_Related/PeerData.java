package Peer_Related;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;
/*
 * This class has the peer related info like, peerID, host name, listening port and if it has a file or not. It helps other classes work
 * by providing the necessary peer related info.
 */
public class PeerData {
	public final int peerId;
	public final String hostName;
	public final int listeningPort;
	public final boolean hasFile;
	public AtomicInteger bytesDownloadedFrom;
	public BitSet partsReceived;
	public AtomicBoolean interested ;

	
	
	public PeerData(int peerID, String hName, int lPort, int hFile)
	{
		peerId = peerID;
		hostName = hName;
		listeningPort = lPort;
		hasFile = (hFile == 1) ? true:false;
		bytesDownloadedFrom = new AtomicInteger (0);
		partsReceived = new BitSet();
		interested = new AtomicBoolean (false);

	}

	static HashSet<Integer> getPeerIds(LinkedList<PeerData> peers)
	{
		HashSet<Integer> peerIds = new HashSet<Integer>();
		if(peers!=null && !peers.isEmpty()){
			int i=0;
			while(i<peers.size())
			{
				peerIds.add(peers.get(i).peerId);
				i++;
			}
		}
		return peerIds;}


	public static PeerData getPeerByPeerId(int peerId, LinkedList<PeerData> list)
	{
		int i=0;
		while(i<list.size())
		{
			if((list.get(i).peerId)==peerId)
			{
				return list.get(i);
			}
			else
				i++;
		}
		return null;
	}

	public String getPeerAddress() {
		return hostName;
	}
}
