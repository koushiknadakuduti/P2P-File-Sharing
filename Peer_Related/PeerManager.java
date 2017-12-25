package Peer_Related;
import Logging.logrecord;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerManager implements Runnable {

	/* Peer Manager manages all the peer related functionalities. It has functionalities like 
	 * determining peers to choke, unchoke, maintaining preferred peers. It also finds if the peer has received all the  
	 * pieces or not. Peer manager maintains relation with other peers.
	 */
	
	LinkedList<PeerData> preferredNeighbours;
	OptimisticUnchoke unchoker = null;
	LinkedList<PeerData> peersExceptLocal = new LinkedList<PeerData>();
	CreatePeers obj = null;
	AtomicBoolean peerFileCompleted ;


	PeerManager(LinkedList<PeerData> peersExceptLocal, CreatePeers obj)
	{
		this.peersExceptLocal = peersExceptLocal;
		unchoker = new OptimisticUnchoke(obj);
		this.obj = obj;
		peerFileCompleted = CreatePeers.peer.hasFile?new AtomicBoolean(true):new AtomicBoolean(false);

	}

	@Override
	public void run() {

		Thread t = new Thread(unchoker);

		t.start();
		while(true)
		{
			try {
				Thread.sleep(Integer.parseInt(PeerProcess.common_cfg.getProperty("UnchokingInterval"))*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			LinkedList<PeerData> interestedPeers = getInterestedPeers(peersExceptLocal);
			HashSet<Integer> interestedPeersIds = new HashSet<Integer>();
			interestedPeersIds = PeerData.getPeerIds(interestedPeers);

			LinkedList<PeerData> preferredPeers  = getPreferredPeers(interestedPeers,peerFileCompleted.get());
			HashSet<Integer> preferredPeersIds = new HashSet<Integer>();
			preferredPeersIds = PeerData.getPeerIds(preferredPeers);
			if(preferredPeersIds!=null && preferredPeersIds.size()>0){
				logrecord.getLogRecord().changeOfPrefereedNeighbors(preferredPeersIds);
			}

			this.preferredNeighbours = preferredPeers;


			LinkedList<PeerData> peersToChoke = getPeersToChoke(preferredPeers);
			HashSet<Integer> peersToChokeIds= new HashSet<Integer>();
			peersToChokeIds = PeerData.getPeerIds(peersToChoke);

			obj.chokePeers(peersToChokeIds);
			obj.unchokePeers(preferredPeersIds);

			LinkedList<PeerData> optimisticallyUcPeers = getOptimisticallyUcPeers(preferredPeers,interestedPeers);
			HashSet<Integer> optimisticallyUnchokePeerIds = new HashSet<Integer>();
			optimisticallyUnchokePeerIds = PeerData.getPeerIds(optimisticallyUcPeers);




			if(optimisticallyUcPeers!=null){
				unchoker.setUnchokable(optimisticallyUcPeers);}


		}}



	public void setPeerFileCompleted()
	{
		peerFileCompleted.set(true);
	}


	synchronized void receivedPart(int peerId, int size) {
		PeerData peer  = searchForPeer(peerId);                   
		if (peer != null) {
			peer.bytesDownloadedFrom.addAndGet(size);
		}
	}



	synchronized LinkedList<PeerData> getOptimisticallyUcPeers(LinkedList<PeerData> preferredPeers, LinkedList<PeerData> interestedPeers)
	{
		if(interestedPeers.size()>2){
			LinkedList<PeerData> peers = new LinkedList<PeerData>(interestedPeers);
			int i=0;
			while(i<preferredPeers.size()){
				peers.remove(preferredPeers.get(i));
				i++;
			}

			return peers;}
		else
			return null;
	}


	synchronized LinkedList<PeerData> getPreferredPeers(LinkedList<PeerData> interestedPeers,boolean peerFileCompleted)
	{
		LinkedList<PeerData> preferredPeers = new LinkedList<PeerData>();
		if( interestedPeers!=null && !interestedPeers.isEmpty()) {
			if (peerFileCompleted) {
				Collections.shuffle(interestedPeers);

			} else {
				Collections.sort(interestedPeers, new Comparator<PeerData>() {
					@Override
					public int compare(PeerData o1, PeerData o2) {
						if (o1.bytesDownloadedFrom.get() > o2.bytesDownloadedFrom.get()) {
							return 1;
						} else {
							return -1;
						}

					}
				});
			}

			if(interestedPeers.size()>=2){
				preferredPeers.add(interestedPeers.get(0));
				preferredPeers.add(interestedPeers.get(1));

			}
			else{
				int i=0;
				while(i<interestedPeers.size()){
					preferredPeers.add(interestedPeers.get(i));
					i++;

				}
			}
		}
		return preferredPeers;

	}

	synchronized LinkedList<PeerData> getPeersToChoke(LinkedList<PeerData> preferredPeers)
	{
		LinkedList<PeerData> peersToChoke = new LinkedList<PeerData>(peersExceptLocal);
		int i=0;
		while(i<preferredPeers.size()){
			peersToChoke.remove(preferredPeers.get(i));
			i++;
		}
		return peersToChoke;
	}





	public synchronized void setIsInterested(int idOfRemotePeer)
	{

		PeerData interestedPeer = searchForPeer(idOfRemotePeer);

		if(interestedPeer!=null)
			interestedPeer.interested.set(true);

	}

	public synchronized void setIsNotInterested(int idOfRemotePeer)
	{
		PeerData notInterestedPeer = searchForPeer(idOfRemotePeer);
		if(null != notInterestedPeer)
			notInterestedPeer.interested.set (false);

	}


	public PeerData searchForPeer(int peerID)
	{
		if(peersExceptLocal!=null &&!peersExceptLocal.isEmpty())
		{
			int i=0;
			while(i<peersExceptLocal.size())
			{
				if(peerID == peersExceptLocal.get(i).peerId){
					return peersExceptLocal.get(i);
				}
				else{
					i++;
				}
			}
		}
		return null;
	}


	public synchronized void updateHave(int pieceIndex, int remotePeerID)
	{
		PeerData peer = searchForPeer(remotePeerID);
		if(null != peer)
			peer.partsReceived.set(pieceIndex);
		haveNeighboursCompleted();
	}

	synchronized boolean stillInterested(int peerId, BitSet bitset) {
		PeerData peer  = searchForPeer(peerId);
		if (peer != null) {
			BitSet pBitset = (BitSet) peer.partsReceived.clone();
			pBitset.andNot(bitset);
			return ! pBitset.isEmpty();
		}
		return false;
	}


	private synchronized void haveNeighboursCompleted() {

		int i=0;
		while(i<peersExceptLocal.size())
		{
			if (peersExceptLocal.get(i).partsReceived.cardinality() < FileManager.bitsetSize) {
				return;
			}
			i++;
		}
		obj.neighboursHaveCompleted();
	}

	public synchronized void updateBitField(int remotePeerID, BitSet bitfield)
	{
		PeerData peer =  searchForPeer(remotePeerID);
		if(peer!= null)
			peer.partsReceived = bitfield;
		haveNeighboursCompleted();
	}

	public synchronized boolean canTransferToPeer(int remotePeerID)
	{

		PeerData peer = searchForPeer(remotePeerID);
		System.out.println(preferredNeighbours + " " + unchoker.unchokable.size() + " " + remotePeerID + " " + PeerData.getPeerByPeerId(remotePeerID,peersExceptLocal).interested );
		if(preferredNeighbours!=null && unchoker.unchokable!=null){
			System.out.println();
			return (preferredNeighbours.contains(peer) || unchoker.unchokable.contains(peer));
		}
		else{
			return false;
		}
	}


	public synchronized BitSet getReceivedParts(int remotePeerID)
	{
		PeerData peer  = searchForPeer(remotePeerID);
		if (peer != null)
			return (BitSet) peer.partsReceived.clone();
		return new BitSet();

	}

	synchronized LinkedList<PeerData> getInterestedPeers(LinkedList<PeerData> peers)
	{
		LinkedList<PeerData> interestedPeers= new LinkedList<PeerData>();
		int i=0;
		while(i<peers.size())
		{
			if(peers.get(i).interested.get()) {
				interestedPeers.add(peers.get(i));
			}
			i++;
		}
		return interestedPeers;
	}
}
