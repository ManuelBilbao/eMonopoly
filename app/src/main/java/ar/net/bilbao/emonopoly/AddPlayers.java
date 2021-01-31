package ar.net.bilbao.emonopoly;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import top.defaults.colorpicker.ColorPickerPopup;

public class AddPlayers extends AppCompatActivity {

	private ArrayList<Integer> colors = new ArrayList<>();
	private ArrayList<Player> players = new ArrayList<>();
	private ArrayList<String> cards = new ArrayList<>();

	private TableLayout tableLayout;
	private AlertDialog alertDialog;
	private boolean dialogCancelled;
	private Player currentPlayer = null;
	private Context context;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_players);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		FloatingActionButton fab = findViewById(R.id.add_players_fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				askForCards();
			}
		});

		tableLayout = findViewById(R.id.add_players_table_layout);
		(findViewById(R.id.add_players_btn_add_player)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addPlayer();
			}
		});

		context = this; // For NFC reading while AlertDialog is on top
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
	 */
	private void addPlayer() {
		// Create a new row with 'player_row' layout
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		TableRow row = (TableRow) inflater.inflate(R.layout.player_row, null);
		row.getChildAt(0).setBackgroundColor(Color.RED);

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
	 * Create a Player instance, register his card and add him to 'players' Array.
	 *
	 * @param index The index of the player to ask for the card
	 * @return		The player if created
	 */
	private Player askForNextCard(int index) {
		if (index >= tableLayout.getChildCount()) return null;

		TableRow row = (TableRow) tableLayout.getChildAt(index);

		Player player = new Player(colors.get(index), ((EditText) row.getChildAt(1)).getText().toString(), 1500);
		players.add(player);

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle(player.getName());
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
				players.clear();
				cards.clear();
				dialogCancelled = true;
			}
		});
		alertDialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				NFCUtilities.disableDiscovering(context);
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
				((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); // Disable 'okay' button until a new card is read
			}
		});
		NFCUtilities.enableDiscovering(context);
		currentPlayer = player;
		alertDialog.show();

		return player;
	}

	/**
	 * Ask for all the players cards
	 */
	private void askForCards() {
		askForNextCard(0);
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