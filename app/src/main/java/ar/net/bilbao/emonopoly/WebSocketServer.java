package ar.net.bilbao.emonopoly;

import android.graphics.Color;

import org.json.JSONException;
import org.json.JSONObject;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
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

	ArrayList<DebugWebSocket> conexiones = new ArrayList<>();

	ArrayList<Player> players = new ArrayList<>();
	boolean bankerHasCard;
	String response;

	public WebSocketServer(ArrayList<Player> players, boolean bankerHasCard) {
		super(8080);
		updatePlayers(players);
		this.bankerHasCard = bankerHasCard;
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
		if (bankerHasCard)
			players.remove(players.size() - 1);
		this.players = players;

		JSONObject jsonObject = new JSONObject();
		for (Player player : players) {
			try {
				JSONObject o = new JSONObject().accumulate("index", player.getIndex()).accumulate("name", player.getName()).accumulate("money", player.getMoney()).accumulate("lost", player.hasLost());
				jsonObject.accumulate("players", o);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		System.out.println(jsonObject.toString());
		broadcast(jsonObject.toString());
	}

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
			msg.append("<div class='col-12 col-md-6 my-2'>");
			msg.append("	<div class='player' id='player-").append(player.getIndex()).append("' style='background-color: #").append(colorToHex(player.getColor())).append("'>");
			msg.append("		<h3 class='m-0'>").append(player.getName()).append("<span class='float-right'>$ ").append(NumberFormat.getNumberInstance(Locale.getDefault()).format(player.getMoney())).append("</span></h3>");
			msg.append("	</div>");
			msg.append("</div>");
		}

		msg.append("</div>" +
				"</div>" +
				"<script>" +
				"	var players;" +
				"	websocket = new WebSocket('ws://192.168.0.77:8080');" +
				"	websocket.onmessage = function(msg) {" +
				"		players = JSON.parse(msg.data).players;" +
				"		console.log(players);" +
				"		for(i = 0; i < players.length; i++) {" +
				"			console.log(players[i]);" +
				"			document.getElementById('player-' + i).innerText = players[i].name + ': $' + players[i].money;" +
				"		}" +
				"	}" +
				"</script>" +
				"</body>" +
				"</hmtl>");

		return msg.toString();
	}

	private String colorToHex(int color) {
		int red = Color.red(color);
		int green = Color.green(color);
		int blue = Color.blue(color);
		return Integer.toHexString(red) + Integer.toHexString(green) + Integer.toHexString(blue);
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