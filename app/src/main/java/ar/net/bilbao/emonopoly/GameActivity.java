package ar.net.bilbao.emonopoly;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.BiFunction;

public class GameActivity extends AppCompatActivity {

	private TextView tvMain;
	private TextView tvSecondary;

	private int passGo;
	private ArrayList<Player> players = new ArrayList<>();
	private Player banker;
	private boolean bankerHasCard;
	private boolean negativeBalance;

	private AlertDialog alertDialog;
	private boolean nfcEnabled = false;
	private Player fromPlayer;
	private Player toPlayer;

	private Integer firstOperand;
	private BiFunction<Long, Long, Long> operation;
	boolean clearNext = false;

	WebSocketServer webSocketServer;
	int ip;
	final int REQUEST_CODE = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		Intent intent = getIntent();
		passGo = intent.getIntExtra("passGo", 0);
		bankerHasCard = intent.getBooleanExtra("bankerHasCard", false);
		negativeBalance = intent.getBooleanExtra("negativeBalance", true);
		int playersCount = intent.getIntExtra("playersCount", 0);
		for (int i = 0; i < playersCount; i++)
			players.add((Player) intent.getSerializableExtra("player" + i));
		banker = (Player) intent.getSerializableExtra("banker");

		tvMain = findViewById(R.id.game_main_tv);
		tvSecondary = findViewById(R.id.game_secondary_tv);

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
			ip = getIP();
		} else {
			requestPermissions(new String[] {Manifest.permission.ACCESS_WIFI_STATE}, REQUEST_CODE);
		}

		webSocketServer = new WebSocketServer(players, intToIPString(ip));
		try {
			webSocketServer.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_game, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.game_share_action) {
			shareLink();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		if (nfcEnabled)
			setNFCDiscovering(true);
		super.onResume();
	}

	@Override
	protected void onPause() {
		NFCUtilities.disableDiscovering(this);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		webSocketServer.stop();
		super.onDestroy();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == REQUEST_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				ip = getIP();
			}
		}
	}

	@Override
	public void onBackPressed() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle(R.string.exit);
		alertDialogBuilder.setMessage(R.string.game_exit_message);
		alertDialogBuilder.setPositiveButton(R.string.exit, (dialog, which) -> goBack());
		alertDialogBuilder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
		alertDialogBuilder.create().show();
	}

	private void goBack() {
		super.onBackPressed();
	}

	/**
	 * Enable or disable NFC discovering
	 *
	 * @param state enable NFC discovering if true, disable if false
	 */
	private void setNFCDiscovering(boolean state) {
		nfcEnabled = state;
		if (state)
			NFCUtilities.enableDiscovering(this);
		else
			NFCUtilities.disableDiscovering(this);
	}

	/**
	 * @return Local IP address
	 */
	private int getIP() {
		WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
		return wm.getDhcpInfo().ipAddress;
	}

	/**
	 * @param ip integer that represents the IP number
	 * @return Formatted IP address string
	 */
	private String intToIPString(int ip) {
		return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
	}

	/**
	 * Open share dialog with server URL
	 */
	public void shareLink() {
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, "https://bilbao.net.ar/eMonopoly/?redirect=" + ip + ":8080");
		sendIntent.setType("text/plain");

		Intent shareIntent = Intent.createChooser(sendIntent, null);
		startActivity(shareIntent);
	}

	/**
	 * @param view View that calls the function
	 */
	@SuppressLint("SetTextI18n")
	public void numberClick(View view) {
		String number = ((Button) view).getText().toString();
		if (clearNext) {
			clearNext = false;
			tvMain.setText(number);
			tvSecondary.setText("");
		} else {
			tvMain.setText(tvMain.getText().toString() + number);
		}
	}

	/**
	 * @param view View that calls the function
	 */
	@SuppressLint("SetTextI18n")
	public void operatorClick(View view) {
		if (tvMain.getText().length() == 0 && firstOperand == null) return;

		if (tvMain.getText().length() > 0 && firstOperand != null) resultClick(view); // Accumulate results

		String operator = ((Button) view).getText().toString();

		switch (operator) {
			case "+":
				operation = (a, b) -> a + b;
				break;
			case "-":
				operation = (a, b) -> a - b;
				break;
			case "*":
				operation = (a, b) -> a * b;
				break;
			case "/":
				operation = (a, b) -> Math.round((double)a / b);
				break;
		}

		// Change operator if no number
		if (tvMain.getText().length() == 0)  {
			tvSecondary.setText(firstOperand.toString() + operator);
			return;
		}

		try {
			firstOperand = Integer.parseInt(tvMain.getText().toString());
			tvSecondary.setText(tvMain.getText().toString() + operator);
			tvMain.setText("");
			clearNext = false;
		} catch (NumberFormatException e) {
			Toast.makeText(this, getString(R.string.number_limits_exceeded, Integer.MIN_VALUE, Integer.MIN_VALUE), Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * @param view View that calls the function
	 * @return True if the result is valid
	 */
	@SuppressLint("SetTextI18n")
	public boolean resultClick(View view) {
		if (firstOperand == null ) return true;

		try {
			int result = getResult(firstOperand, Integer.parseInt(tvMain.getText().toString()));

			tvSecondary.setText(tvSecondary.getText().toString() + tvMain.getText().toString());
			tvMain.setText(String.valueOf(result));

			firstOperand = null;
			operation = null;
			clearNext = true;
			return true;
		} catch (ArithmeticException e) {
			Toast.makeText(this, R.string.game_zero_division, Toast.LENGTH_SHORT).show();
			return false;
		} catch (NumberFormatException e) {
			Toast.makeText(this, getString(R.string.number_limits_exceeded, Integer.MIN_VALUE, Integer.MAX_VALUE), Toast.LENGTH_SHORT).show();
			return false;
		} catch (NotRepresentableException e) {
			Toast.makeText(this, R.string.result_exceeded_int_limits, Toast.LENGTH_SHORT).show();
			return false;
		}
	}

	/**
	 * @param view View that calls the function
	 */
	public void passGoClick(View view) {
		if (clearNext) {
			clearNext = false;
			tvSecondary.setText("");
		}
		tvMain.setText(String.valueOf(passGo));
	}

	/**
	 * @param view View that calls the function
	 */
	public void acClick(View view) {
		firstOperand = null;
		operation = null;
		tvMain.setText("");
		tvSecondary.setText("");
	}

	/**
	 * @param view View that calls the function
	 */
	public void backClick(View view) {
		if (tvMain.getText().length() > 0)
			tvMain.setText(tvMain.getText().subSequence(0, tvMain.length() - 1));
	}

	/**
	 * @param view View that calls the function
	 */
	public void okClick(View view) {
		if (!resultClick(view)) return;
		if (tvMain.getText().length() <= 0) return;

		int amount = Integer.parseInt(tvMain.getText().toString());
		Context context = this;
		DialogInterface.OnDismissListener toDismissListener = new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				setNFCDiscovering(false);
				if (toPlayer == null) {
					fromPlayer = null;
					return;
				}

				if (fromPlayer.takeMoney(amount, negativeBalance)) {
					toPlayer.giveMoney(amount);

					int money = fromPlayer.getMoney();
					if (money < 0) {
						Toast.makeText(context, getString(R.string.game_negative_balance_message, fromPlayer.getName()), Toast.LENGTH_SHORT).show();
					} else if (money == 0) {
						Player currentPlayer = fromPlayer;
						AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
						alertBuilder.setTitle(R.string.game_bankrupt_alert_title);
						alertBuilder.setMessage(getString(R.string.game_bankrupt_alert_message, fromPlayer.getName()));
						alertBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								currentPlayer.setHasLost(true);
								webSocketServer.updatePlayers(players);
								Toast.makeText(context, getString(R.string.game_bankrupt_message, currentPlayer.getName()), Toast.LENGTH_SHORT).show();
								dialog.dismiss();
							}
						});
						alertBuilder.setNegativeButton(R.string.no, (dialog1, which) -> dialog1.dismiss());
						alertBuilder.create().show();
					}

					webSocketServer.updatePlayers(players);
					Toast.makeText(context,
							getString(R.string.game_money_transfered, fromPlayer.getName(), toPlayer.getName(), amount),
							Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(context,
							getString(R.string.game_not_enough_money, fromPlayer.getName()),
							Toast.LENGTH_SHORT).show();
				}

				fromPlayer = null;
				toPlayer = null;

				tvMain.setText("");
				tvSecondary.setText("");
			}
		};
		askCard(getString(R.string.game_from), dialog -> {
			setNFCDiscovering(false);
			if (fromPlayer != null) {
				askCard(getString(R.string.game_to), toDismissListener);
			}
		});
	}

	/**
	 * Ask for a player card
	 *
	 * @param title Title of the dialog alert
	 * @param dismissListener callback for the dialog dismiss
	 */
	private void askCard(String title, DialogInterface.OnDismissListener dismissListener) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle(title);
		alertDialogBuilder.setMessage("");
		alertDialogBuilder.setOnDismissListener(dismissListener);
		if (!bankerHasCard) {
			alertDialogBuilder.setNeutralButton(R.string.banker_name, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (fromPlayer == null)
						fromPlayer = banker;
					else
						toPlayer = banker;
					dialog.dismiss();
				}
			});
		}
		alertDialog = alertDialogBuilder.create();
		alertDialog.show();
		setNFCDiscovering(true);
	}

	/**
	 * @param uid A card UID
	 * @return The player with the UID card. Null if card is not registered
	 */
	private Player getPlayer(String uid) {
		if (banker.getCardUID().equals(uid)) return banker;

		for (Player player : players)
			if (player.getCardUID().equals(uid))
				return player;

		return null;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		String uid = NFCUtilities.newIntent(intent);
		if (!uid.equals("")) {
			if (fromPlayer == null) {
				fromPlayer = getPlayer(uid);
				if (fromPlayer == null ) {
					Toast.makeText(this, R.string.game_player_not_found, Toast.LENGTH_SHORT).show();
				} else if (fromPlayer.hasLost()) {
					Toast.makeText(this, getString(R.string.game_player_has_lost, fromPlayer.getName()), Toast.LENGTH_SHORT).show();
					fromPlayer = null;
				} else {
					alertDialog.dismiss();
				}
			} else {
				toPlayer = getPlayer(uid);
				if (toPlayer == null) {
					Toast.makeText(this, R.string.game_player_not_found, Toast.LENGTH_SHORT).show();
				} else if (toPlayer.hasLost()) {
					Toast.makeText(this, getString(R.string.game_player_has_lost, toPlayer.getName()), Toast.LENGTH_SHORT).show();
					toPlayer = null;
				} else if (toPlayer.equals(fromPlayer)) {
					toPlayer = null;
					Toast.makeText(this, R.string.game_same_from_to, Toast.LENGTH_SHORT).show();
				} else {
					alertDialog.dismiss();
				}
			}
		}
	}

	/**
	 * @param a First operand
	 * @param b Second operand
	 * @return the result of a (operator) b
	 * @throws ArithmeticException When division by zero
	 * @throws NotRepresentableException When result is to big (or small) for an Integer
	 */
	private int getResult(int a, int b) throws ArithmeticException, NotRepresentableException {
		long result = operation.apply((long) a, (long) b);
		if (result <= Integer.MAX_VALUE && result >= Integer.MIN_VALUE) return (int) result;
		throw new NotRepresentableException();
	}
}