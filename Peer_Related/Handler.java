package Peer_Related;
import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import Logging.logrecord;

/*
 * This class acts as gateway for data exchange between peers. It sends and receives the handshakes, checks the incoming messages 
 * send it to the message handler to take necessary actions.
 */

class ObjectReader extends DataInputStream implements ObjectInput  {
	boolean isHandshakeReceived = false;

	public ObjectReader(InputStream in)
	{
		super(in);

	}

	public Object readObject()
	{
		try{

			if(!isHandshakeReceived) {
				System.out.println("In readObject funtion handshake not yet received block");
				HandShake handShake = new HandShake();
				if (handShake.msgIsHandShake(this)) {
					isHandshakeReceived = true;
					return handShake;
				}
				else{
					System.out.println("handshake is not received properly");
				}

			}
			else
			{
				try
				{
					final int length = readInt();
					final int payloadLength = length - 1;
					Messages message = Messages.getMessage(payloadLength,Messages.getMessageByByte(readByte()));
					message.read(this);
					return message;
				}
				catch( Exception e)
				{
					e.printStackTrace();
				}
			}
		}


		catch (Exception E)
		{
			E.printStackTrace();
		}

		return null;
	}
}

class ObjectWriter extends DataOutputStream implements ObjectOutput{

	public ObjectWriter(OutputStream out)
	{
		super(out);
	}

	public void writeObject(Object obj) throws IOException {
		if (obj instanceof HandShake) {
			((HandShake) obj).write(this);
		}
		else {
			((Messages) obj).write(this);
		}

	}
}

public class Handler implements Runnable {
	Socket sok = null;
	int localpeer = -1;
	int expectedRemotePeer = -1;
	AtomicInteger remotePeerId = new AtomicInteger(-1);
	ObjectReader inReader = null;
	ObjectWriter outWriter = null;
	BlockingQueue<Messages> queue = new LinkedBlockingQueue<>();
	FileManager fm = null;
	PeerManager pm = null;
	class CheckingMessages implements Runnable {
		boolean isRemotePeerIdChoked = true;
		public void run() {
			while (true) {
				try {
					if(queue!=null && !queue.isEmpty()){
						Messages m = queue.take();

						if(m==null){continue;}
						if (remotePeerId.get() != -1) {
							if (m.getMessageType().equals("Choke") && !isRemotePeerIdChoked) {
								isRemotePeerIdChoked = true;

								sendMessage(m);
							} else if (m.getMessageType().equals("Unchoke") && isRemotePeerIdChoked) {
								isRemotePeerIdChoked = false;
								sendMessage(m);
							}

							else {
								sendMessage(m);
							}

						}
					}


				} catch (InterruptedException e) {
					e.printStackTrace();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}


	public Handler(Socket connection, int localPeer, int expectedRemotePeer, FileManager f1, PeerManager p1) throws IOException {
		this.sok = connection;
		this.localpeer = localPeer;
		outWriter = new ObjectWriter(sok.getOutputStream());
		this.fm = f1;
		this.pm = p1;
		this.expectedRemotePeer = expectedRemotePeer;

	}

	public boolean equals(Handler obj) {
		return obj.remotePeerId == this.remotePeerId;
	}

	public void run() {
		try {

			CheckingMessages cm = new CheckingMessages();
			Thread t = new Thread(cm);
			t.setName("Thread to check messages");
			t.start();

			inReader = new ObjectReader(sok.getInputStream());

			HandShake handshake = new HandShake(localpeer);
			System.out.println("handshake received from ");
			outWriter.writeObject(handshake);
			System.out.println("handshake written");
			HandShake msg = (HandShake) inReader.readObject();
			remotePeerId.set(msg.getPeerId());

			if (expectedRemotePeer!=-1 && (remotePeerId.get() != expectedRemotePeer)) {
				throw new Exception("Remote peer id " + remotePeerId + " does not match with the expected id: " + expectedRemotePeer);    //to reframe
			}
			logrecord.getLogRecord().MakesConnection(remotePeerId.get());
			MessageHandler msgHandler = new MessageHandler(remotePeerId, fm, pm);
			sendMessage(msgHandler.handleHandshake(msg));

			while (true) {

				try {
					Messages otherMessage = (Messages) inReader.readObject();
					sendMessage(msgHandler.genericMessageHandle(otherMessage));
				}

				catch (Exception e) {
					e.printStackTrace();
					break;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public int getRemotePeerId(){
		return remotePeerId.get();
	}

	public synchronized void pushInQueue(final Messages m) {
		queue.add(m);
	}

	public synchronized void sendMessage(Messages message) throws IOException {

		if (message != null) {
			outWriter.writeObject(message);

		}
	}}
