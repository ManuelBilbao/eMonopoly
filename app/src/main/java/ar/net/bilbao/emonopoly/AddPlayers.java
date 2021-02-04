package ar.net.bilbao.emonopoly;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import top.defaults.colorpicker.ColorPickerPopup;

public class AddPlayers extends AppCompatActivity {

	private ArrayList<Integer> colors = new ArrayList<>();
	private ArrayList<Player> players = new ArrayList<>();
	private ArrayList<String> cards = new ArrayList<>();

	private TableLayout tableLayout;
	private EditText etMoney;
	private EditText etPassGo;

	private AlertDialog alertDialog;
	private boolean nfcEnabled = false;
	private boolean dialogCancelled;
	private Player currentPlayer = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_players);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		tableLayout = findViewById(R.id.add_players_table_layout);

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		etMoney = findViewById(R.id.add_players_et_money);
		etPassGo = findViewById(R.id.add_players_et_pass_go);
		etMoney.setText(sharedPreferences.getString("start_money", ""));
		etPassGo.setText(sharedPreferences.getString("pass_go_money", ""));
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
	 * Open a ColorPickerPopup when the color button is clicked and set the new color
	 *
	 * @param view View (color button) that calls the function
	 */
	public void colorClick(View view) {
		TableRow row = (TableRow) view.getParent();
		int index = tableLayout.indexOfChild(row);

		new ColorPickerPopup.Builder(AddPlayers.this).initialColor(colors.get(index))
				.enableBrightness(true).enableAlpha(false)
				.okTitle(getString(R.string.okay))
				.cancelTitle(getString(R.string.cancel))
				.showIndicator(true).showValue(false)
				.build().show(view, new ColorPickerPopup.ColorPickerObserver() {
					@Override
					public void onColorPicked(int color) {
						colors.set(index, color);
						view.setBackgroundColor(color);
					}
				});
	}

	/**
	 * Add new player to the list
	 *
	 * @param view View that calls the function
	 */
	public void addPlayer(View view) {
		// Create a new row with 'player_row' layout
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		TableRow row = (TableRow) inflater.inflate(R.layout.player_row, null);
		row.getChildAt(0).setBackgroundColor(Color.RED);

		row.getChildAt(1).setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) return;
				EditText et = (EditText) v;
				int index = tableLayout.indexOfChild(row);

				if (et.getText().length() == 0) {
					et.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
				} else {
					et.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(0x03, 0xDA, 0xC5)));
				}
			}
		});

		tableLayout.addView(row);
		colors.add(Color.RED);

		// Give focus to Name EditText and open the keyboard
		((EditText) row.getChildAt(1)).selectAll();
		row.getChildAt(1).requestFocus();
		InputMethodManager imm = (InputMethodManager)   getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
	}

	/**
	 * Remove a player from the list
	 *
	 * @param view View (remove button) that calls the function
	 */
	public void removePlayer(View view) {
		TableRow row = (TableRow) view.getParent();
		int index = tableLayout.indexOfChild(row);

		tableLayout.removeViewAt(index);
		colors.remove(index);
	}

	/**
	 * Create a player and add it to players list
	 *
	 * @param index Number of row in tableLayout
	 * @return Created player
	 */
	private Player createPlayer(int index) {
		TableRow row = (TableRow) tableLayout.getChildAt(index);

		Player player = new Player(colors.get(index),
								((EditText) row.getChildAt(1)).getText().toString(),
								Integer.parseInt(etMoney.getText().toString()));
		players.add(player);

		return player;
	}

	/**
	 * Create a Player instance, register his card and add him to 'players' Array.
	 *
	 * @param index The index of the player to ask for the card
	 */
	private void askForNextCard(int index) {
		if (index >= tableLayout.getChildCount()) {
			startGame();
			return;
		}

		currentPlayer = createPlayer(index);

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle(currentPlayer.getName());
		alertDialogBuilder.setMessage("");
		alertDialogBuilder.setCancelable(false);
		alertDialogBuilder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		alertDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialogCancelled = true;
			}
		});
		alertDialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				setNFCDiscovering(false);
				if (dialogCancelled) {
					dialogCancelled = false;
					return;
				}
				askForNextCard(index + 1);
			}
		});

		alertDialog = alertDialogBuilder.create();
		alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
//				((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); // Disable 'okay' button until a new card is read
			}
		});
		alertDialog.show();

		setNFCDiscovering(true);
	}

	/**
	 * Ask for all the players cards
	 */
	private void askForCards() {
		askForNextCard(0);
	}

	/**
	 * @return true if there is at least one player without name
	 */
	private boolean playerWithNoName() {
		for (int i = 0; i < tableLayout.getChildCount(); i++) {
			TableRow row = (TableRow) tableLayout.getChildAt(i);
			EditText et = (EditText) row.getChildAt(1);
			if (et.getText().length() == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if players meet the requirements and start asking cards
	 *
	 * @param view View that calls de function
	 */
	public void clickStart(View view) {
		if (tableLayout.getChildCount() < 2) {
			Snackbar.make(view, R.string.add_players_few_players, Snackbar.LENGTH_SHORT).show();
		} else if (playerWithNoName()) {
			Snackbar.make(view, R.string.add_players_empty_names, Snackbar.LENGTH_SHORT).show();
		} else {
			players.clear();
			cards.clear();
			askForCards();
		}
	}

	/**
	 * When all players were registered with their cards, start the game
	 */
	private void startGame() {
		Intent intent = new Intent(this, GameActivity.class);
		intent.putExtra("passGo", Integer.parseInt(etPassGo.getText().toString()));
		intent.putExtra("playersCount", players.size());
		for (int i = 0; i < players.size(); i++) {
			intent.putExtra("player" + i, players.get(i));
		}
		startActivity(intent);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		String uid = NFCUtilities.newIntent(intent);
		if (!uid.equals("") && alertDialog != null) {
			if (cards.contains(uid)) {
				alertDialog.setMessage(getString(R.string.add_players_alert_dialog_card_repeated, players.get(cards.indexOf(uid)).getName()));
			} else {
				currentPlayer.setCardUID(uid);
				cards.add(uid);
				alertDialog.setMessage(uid);
				alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); // Enable 'okay' button to continue
			}
		}
	}
}