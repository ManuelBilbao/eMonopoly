package ar.net.bilbao.emonopoly;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.function.BiFunction;

public class GameActivity extends AppCompatActivity {

	private TextView tvMain;
	private TextView tvSecondary;

	private int passGo;
	private ArrayList<Player> players = new ArrayList<>();
	private boolean bankerHasCard;
	private boolean negativeBalance;

	private AlertDialog alertDialog;
	private boolean nfcEnabled = false;
	private Player fromPlayer;
	private Player toPlayer;

	private Integer firstOperand;
	private BiFunction<Integer, Integer, Integer> operation;
	boolean clearNext = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);

		Intent intent = getIntent();
		passGo = intent.getIntExtra("passGo", 0);
		bankerHasCard = intent.getBooleanExtra("bankerHasCard", false);
		negativeBalance = intent.getBooleanExtra("negativeBalance", true);
		int playersCount = intent.getIntExtra("playersCount", 0);
		for (int i = 0; i < playersCount; i++) {
			players.add((Player) intent.getSerializableExtra("player" + i));
		}

		tvMain = findViewById(R.id.game_main_tv);
		tvSecondary = findViewById(R.id.game_secondary_tv);
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
	public void onBackPressed() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle(R.string.exit);
		alertDialogBuilder.setMessage(R.string.game_exit_message);
		alertDialogBuilder.setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				goBack();
			}
		});
		alertDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
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
		if (tvMain.getText().length() == 0) return;

		String operator = ((Button) view).getText().toString();

		switch (operator) {
			case "+":
				operation = Integer::sum;
				break;
			case "-":
				operation = (a, b) -> a - b;
				break;
			case "*":
				operation = (a, b) -> a * b;
				break;
			case "/":
				operation = (a, b) -> a / b;
				break;
		}

		firstOperand = Integer.parseInt(tvMain.getText().toString());
		tvSecondary.setText(tvMain.getText().toString() + operator);
		tvMain.setText("");
		clearNext = false;
	}

	/**
	 * @param view View that calls the function
	 */
	@SuppressLint("SetTextI18n")
	public void resultClick(View view) {
		if (firstOperand != null ) {
			int result = operation.apply(firstOperand, Integer.parseInt(tvMain.getText().toString()));

			tvSecondary.setText(tvSecondary.getText().toString() + tvMain.getText().toString());
			tvMain.setText(String.valueOf(result));

			firstOperand = null;
			operation = null;
			clearNext = true;
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
		if (tvMain.getText().length() > 0) {
			int amount = Integer.parseInt(tvMain.getText().toString());
			Context context = this;
			DialogInterface.OnDismissListener toDismissListener = new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					setNFCDiscovering(false);
					if (toPlayer == null) {
						fromPlayer = null;
					} else {
						if (fromPlayer.takeMoney(amount)) {
							toPlayer.giveMoney(amount);
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
				}
			};
			askCard(getString(R.string.game_from), dialog -> {
				setNFCDiscovering(false);
				if (fromPlayer != null) {
					askCard(getString(R.string.game_to), toDismissListener);
				}
			});
		}
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
		alertDialog = alertDialogBuilder.create();
		alertDialog.show();
		setNFCDiscovering(true);
	}

	/**
	 * @param uid A card UID
	 * @return The player with the UID card. Null if card is not registered
	 */
	private Player getPlayer(String uid) {
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
				} else {
					alertDialog.dismiss();
				}
			} else {
				toPlayer = getPlayer(uid);
				if (toPlayer == null) {
					Toast.makeText(this, R.string.game_player_not_found, Toast.LENGTH_SHORT).show();
				} else if (toPlayer.equals(fromPlayer)) {
					toPlayer = null;
					Toast.makeText(this, R.string.game_same_from_to, Toast.LENGTH_SHORT).show();
				} else {
					alertDialog.dismiss();
				}
			}
		}
	}
}