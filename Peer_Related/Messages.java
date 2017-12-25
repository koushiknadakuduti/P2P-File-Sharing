package Peer_Related;
import java.util.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
/*
 * This class provides the blueprint for message object. It contains all message parsing operations.
 */
public class Messages {
	public int msglength;
	public byte msgtype;
	public byte[] payload;
	static Byte [] val = {0,1,2,3,4,5,6,7};
	static HashMap<String, Byte> hmap = new HashMap<String, Byte>();
	final static String[] str={"Choke", "Unchoke","Interested","Uninterested","Have","Bitfield","Request","Piece"};
	static {
		hmap.put("Choke", val[0]);
		hmap.put("Unchoke", val[1]);
		hmap.put("Interested", val[2]);
		hmap.put("Uninterested", val[3]);
		hmap.put("Have", val[4]);
		hmap.put("Bitfield", val[5]);
		hmap.put("Request", val[6]);
		hmap.put("Piece", val[7]);

	}

	public String getMessageType()
	{
		return getMessageByByte(this.msgtype);
	}


	public  Messages(String type, byte[] payload)
	{

		if(payload==null)
		{
			msglength = 1;
		}
		else if(payload.length==0)
		{
			msglength = 1;
		}
		else {
			msglength = payload.length + 1;
		}

		msgtype = hmap.get(type);
		this.payload = payload;
	}

	public Messages(int index, byte[] payload)
	{
		String type = "Piece";
		payload = join (index, payload);
		if(payload==null || payload.length==0)
		{
			msglength = 1;
		}
		else {
			msglength = payload.length + 1;
		}
		msgtype = hmap.get(type);
		this.payload = payload;
	}

	public Messages(String type)
	{
		this.msgtype = hmap.get(type);
		this.msglength=1;
		this.payload = null;;
	}

	Byte getTypeOfMessage(String type)
	{
		return hmap.get(type);
	}

	public  Messages(String type, int index)
	{
		byte[] payload = getPieceIndexBytes(index);

		if(payload==null || payload.length==0)

		{
			msglength = 1;
		}

		else {
			msglength = payload.length + 1;
		}

		msgtype = hmap.get(type);
		this.payload = payload;
	}

	public static String getMessageByByte(byte b)
	{
		String res=str[b];
		return res;
	}

	public static Messages getMessage(int length, String type)  {
		if(type.equals("Choke"))
		{
			return new Messages(type);
		}
		else if(type.equals("Unchoke"))
		{
			return new Messages(type);
		}
		else if(type.equals("Interested"))
		{
			return new Messages(type);
		}
		else if(type.equals("Uninterested"))
		{
			return new Messages(type);
		}
		else if(type.equals("Have"))
		{
			return new Messages(type, new byte[length]);
		}
		else if(type.equals("Bitfield"))
		{
			return (length > 0) ? new Messages(type, new byte[length]) : new Messages(type, new byte[0]);
		}
		else if(type.equals("Request"))
		{
			return new Messages(type, new byte[length]);
		}
		else if(type.equals("Piece"))
		{
			return new Messages(type, new byte[length]);
		}
		else
		{
			return  new Messages(type);
		}
	}

	public void read (DataInputStream isr) throws IOException {
		if ((payload != null) && (payload.length) > 0) {
			isr.readFully(payload, 0, payload.length);
		}
	}

	public void write (DataOutputStream osr) throws IOException {
		osr.writeInt (msglength);
		osr.writeByte (msgtype);
		if ((payload != null)) {
			osr.write (payload, 0, payload.length);
		}
	}

	public  int getPieceIndex() {
		return ByteBuffer.wrap(Arrays.copyOfRange(payload, 0, 4)).order(ByteOrder.BIG_ENDIAN).getInt();
	}

	public static byte[] getPieceIndexBytes (int pieceIdx) {
		return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(pieceIdx).array();
	}

	public byte[] getPieceContent() {
		if ((payload == null) || (payload.length <= 4)) {
			return null;
		}
		return Arrays.copyOfRange(payload, 4, payload.length);
	}

	private static byte[] join (int pieceIdx, byte[] second) {
		byte[] concat = new byte[4 + (second == null ? 0 : second.length)];
		System.arraycopy(getPieceIndexBytes (pieceIdx), 0, concat, 0, 4);
		System.arraycopy(second, 0, concat, 4, second.length);
		return concat;
	}
}
