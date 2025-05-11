import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.function.Consumer;

public class Server {
	private int count = 1;
	// unique ID generator for game sessions
	private int sessionIdCounter = 1;


	private final Queue<ClientThread> waiting = new ArrayDeque<>();
	private final List<GameSession> sessions = new ArrayList<>();
	private final Consumer<Serializable> callback;

	// List of unique usernames of the user
	private final List<String> usedUsernames = Collections.synchronizedList(new ArrayList<>());

//	public Server() { this(null); }
	public Server(Consumer<Serializable> callback) {
		this.callback = callback;

		// Add some reserved usernames
		usedUsernames.add("Server");
		usedUsernames.add("Admin");
		usedUsernames.add("System");

		new TheServer().start();
	}

	// returns the number of active games being played
	public int getSessionCount() {
		return sessions.size();
	}

    // provides us with the total number of clients connected
	public int getCurrentClients() {
		return sessions.size() * 2 + waiting.size();
	}

	// Shows how many users are waiting on the multiplayer session for another player to join
	public int getWaitingCount() {
		return waiting.size();
	}

	//the main server thread
	private class TheServer extends Thread {
		@Override
		public void run() {
			try (ServerSocket serverSocket = new ServerSocket(5555)) {
				// log server setup
				if (callback != null)
					callback.accept("CONN:Server listening on port " + serverSocket.getLocalPort());
				// connects the new client in and matchmakes them with another client on the server
				// for a multiplayer game
				while (true) {
					Socket sock = serverSocket.accept();
					ClientThread ct = new ClientThread(sock, count++);
					ct.start();

					// Log connection
					if (callback != null)
						callback.accept("CONN:Client #" + ct.count + " connected with username " + ct.username);

					ClientThread opponent = null;
					synchronized (waiting) {
						// removes any dead threads from the queue
						Iterator<ClientThread> it = waiting.iterator();
						while (it.hasNext()) {
							ClientThread t = it.next();
							if (!t.isAlive()) {
								it.remove();
							}
						}
						// checks if there is a client in the waiting queue
						// if no one is present then add the client in the queue
						// else connect the client with another one in the queue
						if (waiting.isEmpty()) {
							waiting.add(ct);
						} else {
							opponent = waiting.remove();
						}
					}

					if (opponent == null) {
						// No opponent yet
						ct.send("STATUS:WAITING");
						// Send current username
						ct.send("USERNAME_ACCEPTED:" + ct.username);
						// Send list of active usernames
						ct.send("USERNAME_LIST:" + getUsernameList());

						if (callback != null)
							callback.accept("GAME:Client #" + ct.count +
									" waiting (" + waiting.size() + " in queue)");
					} else {
						// Start a new session
						int sid = sessionIdCounter++;
						if (callback != null)
							callback.accept("GAME:Starting session " + sid +
									" between Client #" + opponent.count +
									" (" + opponent.username + ") and Client #" + ct.count +
									" (" + ct.username + ")");
						GameSession session = new GameSession(sid, opponent, ct);
						sessions.add(session);
						session.start();
					}
				}

			} catch (IOException e) {
				if (callback != null)
					callback.accept("CONN:Server socket error: " + e.getMessage());
			}
		}
	}

	// helper function to seperate the usernames
	private String getUsernameList() {
		return String.join(",", usedUsernames);
	}

	// private class to show an active game session
	private class GameSession {
		private final int id;
		private final ClientThread p1, p2;
		private final Random rand = new Random();

		public GameSession(int id, ClientThread a, ClientThread b) {
			this.id = id;
			this.p1 = a;
			this.p2 = b;
			a.setSession(this);
			b.setSession(this);

			// Send opponent usernames
			a.send("OPPONENT_NAME:" + b.username);
			b.send("OPPONENT_NAME:" + a.username);
		}

		//starts a session and chooses a random player to go first
		public void start() {
			// randomly choose who starts
			if (rand.nextBoolean()) {
				p1.send("TURN:true");
				p2.send("TURN:false");
			} else {
				p2.send("TURN:true");
				p1.send("TURN:false");
			}
		}

		// Handles the moves of the two player
		public void relayMove(ClientThread from, String msg) {
			// Check if this is a username change request
			if (msg.startsWith("SET_USERNAME:")) {
				processUsernameRequest(from, msg.substring(13));
				return;
			}

			// forward the move/reset
			ClientThread to = (from == p1 ? p2 : p1);
			to.send(msg);
			from.send("TURN:false");
			to.send("TURN:true");

			// notes the moves
			if (callback != null) {
				String detail = msg.startsWith("MOVE:")
						? "column " + msg.substring(5)
						: msg;
				callback.accept("GAME:Session " + id +
						": Client #" + from.count +
						" (" + from.username + ")" +
						" played " + detail);
			}
		}

		// Handle username change requests
		private void processUsernameRequest(ClientThread client, String requestedUsername) {
			String oldUsername = client.username;

			// Check if requested username is already in use
			if (usedUsernames.contains(requestedUsername)) {
				// Username is taken
				client.send("USERNAME_TAKEN:" + requestedUsername);
			} else {
				// Username is available
				usedUsernames.remove(oldUsername);
				client.username = requestedUsername;
				usedUsernames.add(requestedUsername);

				// Confirm to the client
				client.send("USERNAME_ACCEPTED:" + requestedUsername);

				// Notify opponent about username change
				ClientThread opponent = (client == p1 ? p2 : p1);
				opponent.send("OPPONENT_RENAMED:" + requestedUsername);

				// Log the change
				if (callback != null) {
					callback.accept("GAME:Client #" + client.count +
							" changed username from " + oldUsername +
							" to " + requestedUsername);
				}
			}
		}

		// Checks if a player disconnects in the session
		public void playerLeft(ClientThread gone) {
			ClientThread other = (gone == p1 ? p2 : p1);
			other.send("DISCONNECT:opponent");
			if (callback != null)
				callback.accept("GAME:Session " + id +
						": Client #" + gone.count +
						" (" + gone.username + ")" +
						" disconnected");

			// Remove the username when a player leaves
			usedUsernames.remove(gone.username);

			sessions.remove(this);
		}
	}

	// Class for the connected client thread
	class ClientThread extends Thread {
		private final Socket sock;
		private final int count;
		private ObjectInputStream in;
		private ObjectOutputStream out;
		private GameSession session;
		private String username;

		public void setSession(GameSession s) { this.session = s; }

		public ClientThread(Socket sock, int count) {
			this.sock = sock;
			this.count = count;
			this.username = "User" + count; // Default username
			usedUsernames.add(this.username); // Register the username

			try {
				out = new ObjectOutputStream(sock.getOutputStream());
				in  = new ObjectInputStream(sock.getInputStream());
				sock.setTcpNoDelay(true);

				// Send connected message with client ID
				send("CONNECTED:" + count);

			} catch (IOException e) {
				if (callback != null)
					callback.accept("CONN:Streams not open for client #" + count);
			}
		}

		void send(String msg) {
			try {
				out.writeObject(msg);
				out.flush();
			} catch (IOException ignored) {}
		}

		@Override
		public void run() {
			try {
				while (true) {
					String data = in.readObject().toString();
					// log raw incoming
					if (callback != null)
						callback.accept("CONN:Client #" + count + " (" + username + ") sent: " + data);

					// Handle username change requests when not in a session
					if (session == null && data.startsWith("SET_USERNAME:")) {
						String requestedUsername = data.substring(13);
						processUsernameRequest(requestedUsername);
					}
					// if session is established, route through it
					else if (session != null) {
						session.relayMove(this, data);
					}
				}
			} catch (Exception e) {
				if (callback != null)
					callback.accept("CONN:Client #" + count + " (" + username + ") disconnected");
				if (session != null) {
					session.playerLeft(this);
				} else {
					// Remove username for players waiting in queue
					usedUsernames.remove(username);
					synchronized (waiting) {
						waiting.remove(this);
					}
				}
			} finally {
				try {
					if (in  != null) in.close();
					if (out != null) out.close();
					if (sock != null) sock.close();
				} catch (IOException ignored) {}
			}
		}

		// Process username change for clients not in a session
		private void processUsernameRequest(String requestedUsername) {
			String oldUsername = username;

			// Check if requested username is already in use
			if (usedUsernames.contains(requestedUsername)) {
				// Username is taken
				send("USERNAME_TAKEN:" + requestedUsername);
			} else {
				// Username is available
				usedUsernames.remove(oldUsername);
				username = requestedUsername;
				usedUsernames.add(username);

				// Confirm to the client
				send("USERNAME_ACCEPTED:" + username);

				// Send updated username list
				send("USERNAME_LIST:" + getUsernameList());

				// Log the change
				if (callback != null) {
					callback.accept("CONN:Client #" + count +
							" changed username from " + oldUsername +
							" to " + username);
				}
			}
		}
	}
}