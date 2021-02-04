package ar.net.bilbao.emonopoly;

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
import java.util.Enumeration;
import java.util.function.BiFunction;

public class GameActivity extends AppCompatActivity {

	private TextView tvMain;
	private TextView tvSecondary;

	private int passGo;
	private ArrayList<Player> players = new ArrayList<>();
	
	private AlertDialog alertDialog;
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
		int playersCount = intent.getIntExtra("playersCount", 0);
		for (int i = 0; i < playersCount; i++) {
			players.add((Player) intent.getSerializableExtra("player" + i));
		}

		tvMain = findViewById(R.id.game_main_tv);
		tvSecondary = findViewById(R.id.game_secondary_tv);
	}

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

	public void passGoClick(View view) {
		if (clearNext) {
			clearNext = false;
			tvSecondary.setText("");
		}
		tvMain.setText(String.valueOf(passGo));
	}

	public void acClick(View view) {
		firstOperand = null;
		operation = null;
		tvMain.setText("");
		tvSecondary.setText("");
	}

	public void backClick(View view) {
		if (tvMain.getText().length() > 0)
			tvMain.setText(tvMain.getText().subSequence(0, tvMain.length() - 1));
	}

	public void okClick(View view) {
		if (tvMain.getText().length() > 0) {
			int amount = Integer.parseInt(tvMain.getText().toString());
			Context context = this;
			DialogInterface.OnDismissListener toDismissListener = new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					NFCUtilities.disableDiscovering(context);
					if (toPlayer == null) {
						fromPlayer = null;
					} else {
						if (fromPlayer.takeMoney(amount)) {
							toPlayer.giveMoney(amount);
							Toast.makeText(context,
									getString(R.string.game_money_transfered, fromPlayer.getName(), toPlayer.getName(), amount),
									Toast.LENGTH_LONG).show();
						} else {
							Toast.makeText(context, fromPlayer.getName() + " doesn't have enough money", Toast.LENGTH_SHORT).show();
						}

						fromPlayer = null;
						toPlayer = null;

						tvMain.setText("");
						tvSecondary.setText("");
					}
				}
			};
			askCard("From", dialog -> {
				NFCUtilities.disableDiscovering(context);
				if (fromPlayer != null) {
					askCard("To", toDismissListener);
				}
			});
		}
	}

	private void askCard(String title, DialogInterface.OnDismissListener dismissListener) {
		Context context = this;
		NFCUtilities.enableDiscovering(context);

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle(title);
		alertDialogBuilder.setMessage("");
		alertDialogBuilder.setOnDismissListener(dismissListener);
		alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

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
					Toast.makeText(this, "Jugador no encontrado", Toast.LENGTH_SHORT).show();
				} else {
					alertDialog.dismiss();
				}
			} else {
				toPlayer = getPlayer(uid);
				if (toPlayer == null) {
					Toast.makeText(this, "Jugador no encontrado", Toast.LENGTH_SHORT).show();
				} else if (toPlayer.equals(fromPlayer)) {
					Toast.makeText(this, "From and To can't be the same", Toast.LENGTH_SHORT).show();
				} else {
					alertDialog.dismiss();
				}
			}
		}
	}
}