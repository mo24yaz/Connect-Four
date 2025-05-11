import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class Client extends Thread {
	private final String host;
	private final int    port;
	private Consumer<String> callback;

    private Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream  in;

    public Client(String host, int port, Consumer<String> callback) {
		this.host     = host;
		this.port     = port;
		this.callback = callback;
	}

	public void setCallback(Consumer<String> callback) {
		this.callback = callback;
	}

	@Override
	public void run() {
		try {
			socket = new Socket(host, port);
			out    = new ObjectOutputStream(socket.getOutputStream());
			in     = new ObjectInputStream(socket.getInputStream());
			socket.setTcpNoDelay(true);

			Object msg;
			while ((msg = in.readObject()) != null) {
				if (callback != null) {
					callback.accept(msg.toString());
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			if (callback != null) {
				callback.accept("DISCONNECT:server");
			}
		} finally {
			cleanup();
		}
	}

	public void send(String msg) {
		try {
			out.writeObject(msg);
			out.flush();
		} catch (IOException e) {
			// ignore
		}
	}

	public void disconnect() {
		try {
			// notify opponent (or server) if you need to
			callback.accept("DISCONNECT:opponent");
			out.flush();
		} catch (Exception e) {
			// maybe the socket’s already half-closed—ignore
		}

		// now close resources
		try { if (in  != null) in .close(); } catch (IOException ignored) {}
		try { if (out != null) out.close(); } catch (Exception ignored) {}
		try { if (socket != null) socket.close(); } catch (IOException ignored) {}
	}

	/**
	 * Send a public chat message
	 * @param message The message text to send
	 */
	public void sendPublicChat(String message) {
		send("PUBLIC_CHAT:" + message);
	}

	public void setUsername(String username) {
        // Default username
        send("SET_USERNAME:" + username);
	}

	private void cleanup() {
		try {
			if (in  != null) in.close();
			if (out != null) out.close();
			if (socket != null) socket.close();
		} catch (IOException ignored) {}
	}
}