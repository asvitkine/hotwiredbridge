package hotwiredbridge;

import hotwiredbridge.hotline.*;

import java.io.*;
import java.net.*;

public class HotlineServer {
	public static void main(String[] args) throws IOException {
		ServerSocket serverSocket = new ServerSocket(4000);
		System.out.println("X");
		while (true) {
			Socket socket = serverSocket.accept();
			DataInputStream in = new DataInputStream(socket.getInputStream());
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			byte[] handshake = new byte[8];
			in.readFully(handshake);
			if (!new String(handshake).equals("TRTPHOTL")) {
				socket.close();
				continue;
			}
			System.out.println(in.readInt()); // version number - who cares!

			out.write(HotlineUtils.pack("BN", "TRTP".getBytes(), 0));
			out.flush();
	
			Transaction t;
			
			Transaction loginTransaction = Transaction.readFromStream(in);
			System.out.println("Got transaction: " + loginTransaction.toString());
			
			t = new Transaction(Transaction.REPLY, 0, loginTransaction.getTaskNumber());
			t.addObject(new TransactionObject(TransactionObject.SOCKET, new byte[] {(byte)0,(byte)1}));
			Transaction.writeToStream(t, out);
			t = new Transaction(Transaction.REQUEST, Transaction.ID_USERLIST, 1);
			t.addObject(new TransactionObject(TransactionObject.PRIVS, new byte[8]));
			t.addObject(new TransactionObject(TransactionObject.USER, new UserListTransaction.User(1, 0, 0, "Batman").toByteArray()));
			Transaction.writeToStream(t, out);

			while (true) {
				t = Transaction.readFromStream(in);
				System.out.println("Got transaction: " + t.toString());
				switch (t.getId()) {
				case Transaction.ID_GETNEWS:
					t = new Transaction(Transaction.REPLY, 0, t.getTaskNumber());
					t.addObject(new TransactionObject(TransactionObject.MESSAGE, new byte[0]));
					Transaction.writeToStream(t, out);
					break;
				case Transaction.ID_GETUSERLIST:
					t = new Transaction(Transaction.REPLY, 0, t.getTaskNumber());
					t.addObject(new TransactionObject(TransactionObject.USER, new UserListTransaction.User(1, 0, 0, "Batman").toByteArray()));
					t.addObject(new TransactionObject(TransactionObject.SUBJECT, new byte[0]));
					Transaction.writeToStream(t, out);
					break;
				default:
					t = new Transaction(Transaction.REPLY, 0, t.getTaskNumber(), true);
					Transaction.writeToStream(t, out);
				}
			}
		}
	}
}
