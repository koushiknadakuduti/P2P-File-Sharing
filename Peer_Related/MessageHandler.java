package Peer_Related;
import Logging.logrecord;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class MessageHandler {

	private FileManager fileManager;
	private boolean isChoked;
	private LogRecord logger;
	private PeerManager peerManager;
	private AtomicInteger remotePeerID;

	public MessageHandler(AtomicInteger remotePeerId, FileManager fmObj, PeerManager pmObj)
	{
		remotePeerID = remotePeerId;
		isChoked = false;
		fileManager = fmObj;
		peerManager = pmObj;
	}

	public Messages handleHandshake(HandShake msg)
	{
		BitSet b = fileManager.partsPeerContains();
		if(!b.isEmpty()){
			return(new Messages("Bitfield",b.toByteArray()));
		}
		return null;
	}

	public synchronized Messages genericMessageHandle( Messages msg)
	{
		if(msg!=null)
		{
			String msgType = msg.getMessageType();
			if(msgType=="Choke")
			{
				logrecord.getLogRecord().choked(remotePeerID.get());
				return null;
			}

			else if(msgType=="Unchoke")
			{
				isChoked = false;
				logrecord.getLogRecord().unchoked(remotePeerID.get());
				return requestForPiece();
			}

			else if(msgType=="Interested")
			{
				peerManager.setIsInterested(remotePeerID.get());
				logrecord.getLogRecord().receivedInterested(remotePeerID.get());
				return null;
			}

			else if(msgType=="Uninterested")
			{
				peerManager.setIsNotInterested(remotePeerID.get());
				logrecord.getLogRecord().receivedNotInterested(remotePeerID.get());
				return null;
			}

			else if(msgType=="Have")
			{
				final int index = ByteBuffer.wrap(Arrays.copyOfRange(msg.payload, 0, 4)).order(ByteOrder.BIG_ENDIAN).getInt();
				peerManager.updateHave(index, remotePeerID.get());
				logrecord.getLogRecord().receivedHave(remotePeerID.get(),index);
				return !fileManager.partsPeerContains().get(index) ? new Messages("Interested") : new Messages("Uninterested");
			}

			else if(msgType=="Bitfield")
			{
				BitSet bitset = BitSet.valueOf(msg.payload);
				peerManager.updateBitField(remotePeerID.get(), bitset);
				bitset.andNot(fileManager.partsPeerContains());
				return !bitset.isEmpty() ? new Messages("Interested") : new Messages("Uninterested");

			}

			else if(msgType=="Request")
			{
				logrecord.getLogRecord().peerLogger.log(Level.INFO, " for piece ");
				int pieceRequestedFor = ByteBuffer.wrap(Arrays.copyOfRange(msg.payload, 0, 4)).order(ByteOrder.BIG_ENDIAN).getInt();
				System.out.println( " Piece requested for " + pieceRequestedFor + " : "+remotePeerID);
				if(pieceRequestedFor!=-1 && fileManager.partsPeerContains().get(pieceRequestedFor) && peerManager.canTransferToPeer(remotePeerID.get()))
				{
					byte[] temp = fileManager.getPiece(pieceRequestedFor, fileManager.partFilesPath);
					if(temp != null){
						return new Messages("Piece",temp);
					}
					return null;

				}}

			else if(msgType=="Piece")
			{
				if(msg.getMessageType().equals("Request"))
					return null;
				int sentPieceIndex = ByteBuffer.wrap(Arrays.copyOfRange(msg.payload, 0, 4)).order(ByteOrder.BIG_ENDIAN).getInt();
				logrecord.getLogRecord().pieceDownloaded(remotePeerID.get(), msg.getPieceIndex(), fileManager.partsPeerContains().cardinality());
				fileManager.addPiece(sentPieceIndex, msg.getPieceContent());
				peerManager.receivedPart(remotePeerID.get(), msg.getPieceContent().length);
				return requestForPiece();
			}
		}
		return null;
	}

	private Messages requestForPiece()
	{
		if(!isChoked)
		{
			logrecord.getLogRecord().peerLogger.log(Level.INFO, "Asking for piece beore ");
			int indexOfPieceToRequest = fileManager.partsToRequest(peerManager.getReceivedParts(remotePeerID.get()));
			System.out.println("peer2 asking for  index " + indexOfPieceToRequest);
			return indexOfPieceToRequest >= 0 ? new Messages("Request",indexOfPieceToRequest) : new Messages("Uninterested");
		}
		return null;
	}

}

