package Peer_Related;
import Logging.logrecord;

import java.util.*;

/*
 * This class helps us determine the peer which should be optimistically unchoked at intervals given in the config file.
 */

public class OptimisticUnchoke implements Runnable {

    LinkedList<PeerData> unchokable = new LinkedList<PeerData>();
    HashSet<Integer> unchokablePeerIds = new HashSet<Integer>();
    CreatePeers createPeerObj;

    public OptimisticUnchoke(CreatePeers obj)
    {
        createPeerObj = obj;
    }

    synchronized void setUnchokable(LinkedList<PeerData> unchokablePeers)
    {
        unchokable.clear();
        unchokable = unchokablePeers;
        unchokablePeerIds = PeerData.getPeerIds(unchokablePeers);
    }
    public void run()
    {
        try {
            Thread.sleep(Integer.parseInt(PeerProcess.common_cfg.getProperty("OptimisticUnchokingInterval"))*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(!unchokable.isEmpty() && unchokable.size()>0)
        {
            int index=new Random().nextInt(unchokable.size());
            int peerId=unchokable.get(index).peerId;

            logrecord.getLogRecord().changeOfOptimisticallyUnchokedNeighbors(peerId);


            if(createPeerObj.connectionsList!=null){
            Iterator<Handler> it = createPeerObj.connectionsList.iterator();

            while(it.hasNext())
            {

                Handler newHandler = (Handler)it.next();
                Collection<Integer> peersToUnchoke = new Vector<Integer>();
                if(newHandler.remotePeerId.get() == peerId)
                {

                    peersToUnchoke.add(peerId);
                }
                createPeerObj.unchokePeers(peersToUnchoke);
            }
        }
    }
}}
