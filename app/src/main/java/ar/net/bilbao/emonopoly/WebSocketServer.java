package ar.net.bilbao.emonopoly;

import org.json.JSONException;
import org.json.JSONObject;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.websockets.CloseCode;
import org.nanohttpd.protocols.websockets.NanoWSD;
import org.nanohttpd.protocols.websockets.WebSocket;
import org.nanohttpd.protocols.websockets.WebSocketFrame;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class WebSocketServer extends NanoWSD {

	final static int PORT = 8080;

	ArrayList<DebugWebSocket> conexiones = new ArrayList<>();

	ArrayList<Player> players = new ArrayList<>();
	String ip;

	public WebSocketServer(ArrayList<Player> players, String ip) {
		super(PORT);
		this.ip = ip;
		updatePlayers(players);
	}

	@Override
	protected WebSocket openWebSocket(IHTTPSession handshake) {
		return new DebugWebSocket(this, handshake);
	}

	@Override
	protected Response serve(IHTTPSession session) {
		return Response.newFixedLengthResponse(makeResponse());
	}

	protected void updatePlayers(ArrayList<Player> players) {
		this.players = players;

		JSONObject jsonObject = new JSONObject();
		for (Player player : players) {
			try {
				JSONObject o = new JSONObject()
						.accumulate("index", player.getIndex())
						.accumulate("name", player.getName())
						.accumulate("money", NumberFormat.getNumberInstance(Locale.getDefault()).format(player.getMoney()))
						.accumulate("lost", player.hasLost());
				jsonObject.accumulate("players", o);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		System.out.println(jsonObject.toString());
		broadcast(jsonObject.toString());
	}

	@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
	private String makeResponse() {
		StringBuilder msg = new StringBuilder("<html>" +
				"<head>" +
				"	<meta charset=\"utf-8\">" +
				"	<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">" +
				"	<link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/bootstrap@4.6.0/dist/css/bootstrap.min.css\" integrity=\"sha384-B0vP5xmATw1+K9KRQjQERJvTumQW0nPEzvF6L/Z6nronJ3oUOFUFpCjEUQouq2+l\" crossorigin=\"anonymous\">" +
				"	<style>" +
				"		.player {" +
				"			padding: 2em;" +
				"			border-radius: 10px" +
				"		}" +
				"	</style>" +
				"</head>" +
				"<body>" +
				"	<div class='container'>" +
				"		<div class='row mt-2'>");
		for (Player player : players) {
			msg.append("<div class='col-12 col-md-6 my-2'>" +
						"	<div class='player' id='player-" + player.getIndex() + "' style='background-color: #" + colorToHex(player.getColor()) + "'>" +
						"		<h3 class='m-0'>" + player.getName() + "<span class='float-right'>$ " + NumberFormat.getNumberInstance(Locale.getDefault()).format(player.getMoney()) + "</span></h3>" +
						"	</div>" +
						"</div>");
		}

		msg.append("</div>" +
				"</div>" +
				"<script>" +
				"	var players;" +
				"	websocket = new WebSocket('ws://" + ip + ":" + PORT + "');" +
				"	websocket.onmessage = function(msg) {" +
				"		players = JSON.parse(msg.data).players;" +
				"		for(i = 0; i < players.length; i++) {" +
				"			document.getElementById('player-' + i).children[0].innerHTML = players[i].name + '<span class=\"float-right\">$ ' + players[i].money + '</span>';" +
				"		}" +
				"	}" +
				"</script>" +
				"</body>" +
				"</hmtl>");

		return msg.toString();
	}

	private String colorToHex(int color) {
		return Integer.toHexString(color).substring(2); // First 2 digits are Alpha
	}

	public void broadcast(String payload) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				for (DebugWebSocket socket : conexiones) {
					try {
						socket.send(payload);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
		thread.start();
	}

	private static class DebugWebSocket extends WebSocket {

		private final WebSocketServer server;
		private TimerTask ping = null;
		private static final byte[] PING_PAYLOAD = "1337DEADBEEFC001".getBytes();

		public DebugWebSocket(WebSocketServer server, IHTTPSession handshakeRequest) {
			super(handshakeRequest);
			this.server = server;
		}

		@Override
		protected void onOpen() {
			if (ping == null) {
				ping = new TimerTask() {
					@Override
					public void run() {
						try {
							ping(PING_PAYLOAD);
						} catch (IOException e) {
							ping.cancel();
						}
					}
				};
				new Timer().schedule(ping, 1000, 2000);
			}
			System.out.println("Websocket open!");
			server.conexiones.add(this);
		}

		@Override
		protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
			System.out.println("Websocket closing..." +
					" Initializer: " + ((initiatedByRemote) ? "Remote" : "Self")  +
					" Code: " + code +
					" Reason: " + reason);
			if (ping != null) ping.cancel();
			server.conexiones.remove(this);
		}

		@Override
		protected void onMessage(WebSocketFrame message) {
			try {
                message.setUnmasked();
                sendFrame(message);
				this.send("Recibido!");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
		}

		@Override
		protected void onPong(WebSocketFrame pong) {
			System.out.println("PONG: " + pong);
		}

		@Override
		protected void onException(IOException exception) {
			System.out.println("Exception! " + exception);
		}
	}
}