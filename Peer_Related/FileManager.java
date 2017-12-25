package Peer_Related;
import Logging.logrecord;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/*
 * File Manager class helps us perform file related operations. It helps us split the file into pieces while transmitting or merge 
 * all the received pieces into a single file. It creates the path directories for peers. It also tracks the pieces which the peer 
 * has received and keeps asking the pieces it should still receive. 
 */

public class FileManager
{
	private final int peerId;
	private String fileName;
	private final boolean hasFile;
	private final BitSet partsPeerContains;
	private final int partSize;
	public static  int bitsetSize;
	private int fileSize;
	public static String partFilesPath;
	public static String originalFilePath;
	private BitSet partsRequested;
	public static String ouptutFileCreationPath;

	CreatePeers createPeerObj = null;

	public FileManager(PeerData peer, Properties obj,CreatePeers createPeerObj) throws Exception
	{
		peerId = peer.peerId;
		hasFile = peer.hasFile;
		partSize = Integer.parseInt(obj.getProperty("PieceSize"));
		fileSize = Integer.parseInt(obj.getProperty("FileSize"));
		bitsetSize = (int) Math.ceil ((float)fileSize/(float)partSize);
		this.createPeerObj = createPeerObj;
		fileName = obj.getProperty("FileName");
		ouptutFileCreationPath = "./peer_"+peerId+"/files/" + fileName;
		partFilesPath = "./peer_"+peerId+"/files/parts/";


		partsPeerContains = new BitSet(bitsetSize);
		partsRequested = new BitSet(bitsetSize);

		File partsDirectory = new File(partFilesPath);
		partsDirectory.mkdirs();
		File file = new File(ouptutFileCreationPath);

		if(hasFile)
		{
			int i=0;
			while (i < bitsetSize){
				partsPeerContains.set(i, true);
				i++;
			}
			try
			{
				divideFile(partSize, ouptutFileCreationPath, partFilesPath);
			}
			catch(Exception e)
			{
				System.err.println(e);
			}
		}
	}

	public BitSet partsPeerContains()
	{
		return (BitSet)partsPeerContains.clone();
	}

	public synchronized void addPiece(int i, byte[] p)
	{

		if(!hasPart(i))
		{
			partsPeerContains.set(i);
			saveToDirectory(i, p);
			createPeerObj.gotPart(i);
			if(isFileCompleted())
			{
				combineFile(partsPeerContains.cardinality());
				createPeerObj.fileHasCompleted();
			}
		}
	}

	public  void saveToDirectory(int i, byte[] p)
	{
		FileOutputStream op = null;
		try
		{
			File f = new File(partFilesPath+"/"+i);
			op = new FileOutputStream(f);
			createPeerObj.gotPart(i);
			op.write(p);
			op.flush();
			op.close();
		}
		catch(IOException e)
		{
			System.err.println(e);
		}
	}

	public synchronized boolean hasPart(int index)
	{
		if(index >=0 && index<partsPeerContains.size()) {
			return partsPeerContains.get(index);
		}
		return false;
	}
	public static byte[] getPieceIndexBytes (int pieceIdx) {
		return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(pieceIdx).array();
	}

	public byte[] getPiece(int index, String path)
	{
		if(hasPart(index) && path.trim() != "")
		{
			byte[] b = new byte[4];
			b = getPieceIndexBytes(index);
			try
			{
				File f = new File(path+ "/" + Integer.toString(index));
				FileInputStream in = null;
				try
				{
					in = new FileInputStream(f);

					byte[] barray = new byte[(int)f.length()];
					byte[] barrayfinal = new byte[(int)f.length() + b.length];
					in.read(barray);
					for(int i=0;i<4;i++)
					{
						barrayfinal[i] = b[i];
					}

					for(int i=4;i<barrayfinal.length;i++)
					{
						barrayfinal[i] = barray[i-4];
					}

					return barrayfinal;
				}
				catch(FileNotFoundException e)
				{
					System.err.println(e);
				}
			}
			catch(Exception e)
			{
				System.err.println(e);
			}
		}
		return null;
	}

	synchronized int partsToRequest(BitSet partsRemotePeerHas)
	{
		partsRemotePeerHas.andNot(partsPeerContains());
		partsRemotePeerHas.andNot(partsRequested);

		if(!partsRemotePeerHas.isEmpty())
		{
			List<Integer> listOfSetIndices = new ArrayList();
			for(int i = partsRemotePeerHas.nextSetBit(0); i != -1 ; i = partsRemotePeerHas.nextSetBit(i+1)){
				listOfSetIndices.add(i);
			}
			if(listOfSetIndices.size()>0){
				int index = listOfSetIndices.get(new Random().nextInt(listOfSetIndices.size()));
				partsRequested.set(index);
				new java.util.Timer().schedule(
						new java.util.TimerTask() {
							@Override
							public void run() {
								synchronized (partsRequested) {
									partsRequested.clear(index);
								}
							}
						},3000);
				return index;
			}
		}
		return -1;
	}


	private boolean isFileCompleted()
	{
		for (int i = 0; i < bitsetSize; i++){
			if (!partsPeerContains.get(i)){
				return false;
			}
		}
		logrecord.getLogRecord().fileComplete();
		return true;
	}

	private void divideFile(int partSize, String fileCreationPath, String partsPath) throws Exception
	{
		if(partSize <= 0 || fileName.trim() == "")
		{
			throw new Exception ("File Name Invalid or Part Size less than 0");
		}
		else
		{
			FileInputStream ip = null;
			FileOutputStream op = null;

			try
			{
				ip = new FileInputStream(fileCreationPath);
				byte[] chunkOfFile;
				int temp = fileSize;
				int bytesToRead = partSize;
				int read = 0;
				int chunkNumber = 0;
				while (temp > 0)
				{
					if (temp < bytesToRead)
						bytesToRead = temp;

					chunkOfFile = new byte[bytesToRead];
					read = ip.read(chunkOfFile);
					temp -= read;

					op = new FileOutputStream(new File(partsPath + "/" + Integer.toString(chunkNumber++)));
					op.write(chunkOfFile);
					op.flush();
					op.close();
					chunkOfFile = null;
					op = null;
				}
				ip.close();
			}
			catch(IOException e)
			{
				System.err.println(e);
			}
		}
	}

	private void combineFile(int numberOfParts)
	{
		System.out.println("Inside merge file" + numberOfParts);
		if(numberOfParts > 0)
		{
			try
			{
				FileOutputStream os  = new FileOutputStream(new File(ouptutFileCreationPath));
				FileInputStream is = null;
				byte[] temp = null;
				for(int i=0; i<numberOfParts; i++)
				{
					File f = new File(partFilesPath+"/"+i);
					is = new FileInputStream(f);
					temp = new byte[(int)f.length()];
					is.read(temp);
					os.write(temp);
					temp = null;
					is.close();
					is = null;
				}
				os.close();
			}
			catch(Exception e)
			{
				System.out.println(e);
			}
		}
	}
}
