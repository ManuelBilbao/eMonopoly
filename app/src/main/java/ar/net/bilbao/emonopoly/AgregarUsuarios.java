package ar.net.bilbao.emonopoly;

import android.app.Dialog;
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
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import top.defaults.colorpicker.ColorPickerPopup;

public class AgregarUsuarios extends AppCompatActivity {

	private ArrayList<Integer> colores = new ArrayList<>();
	private ArrayList<Jugador> jugadores = new ArrayList<>();

	private TableLayout tableLayout;
	private Jugador jugadorActual = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_agregar_usuarios);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		FloatingActionButton fab = findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				addPlayer();
			}
		});

		tableLayout = findViewById(R.id.agregarUsuariosTableLayout);
		(findViewById(R.id.agregarUsuariosBtnEmpezar)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				pedirTarjetas();
			}
		});
	}

	public void colorClick(View view) {
		TableRow row = (TableRow) view.getParent();
		int index = tableLayout.indexOfChild(row);

		new ColorPickerPopup.Builder(AgregarUsuarios.this).initialColor(colores.get(index))
				.enableBrightness(true).enableAlpha(false)
				.okTitle("Aceptar").cancelTitle("Cancelar")
				.showIndicator(true).showValue(false).build().show(view, new ColorPickerPopup.ColorPickerObserver() {
			@Override
			public void onColorPicked(int color) {
				colores.set(index, color);
				view.setBackgroundColor(color);
			}
		});
	}

	private void addPlayer() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		TableRow row = (TableRow) inflater.inflate(R.layout.player_row, null);
		row.getChildAt(0).setBackgroundColor(Color.RED);
		row.getChildAt(1).setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					InputMethodManager imm = (InputMethodManager)   getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
				}
			}
		});

		tableLayout.addView(row);
		colores.add(Color.RED);

		((EditText) row.getChildAt(1)).selectAll();
		row.getChildAt(1).requestFocus();
		InputMethodManager imm = (InputMethodManager)   getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

	}

	public void removePlayer(View view) {
		TableRow row = (TableRow) view.getParent();
		int index = tableLayout.indexOfChild(row);

		tableLayout.removeViewAt(index);
		colores.remove(index);
	}

	private void pedirTarjetas() {
		for (int i = 0; i < tableLayout.getChildCount(); i++) {
			TableRow row = (TableRow) tableLayout.getChildAt(i);

			Jugador jugador = new Jugador(colores.get(i), ((EditText) row.getChildAt(1)).getText().toString(), 1500);
			jugadores.add(jugador);

			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(R.layout.register_card, null);
			((TextView) view.findViewById(R.id.registerCardText)).setText("Inserte tarjeta para\n" + jugador.getNombre());

			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
			alertDialogBuilder.setView(view);

			jugadorActual = jugador;
			NFCUtilities.enableDiscovering(this);
			/* Wait for card */

			/*alertDialogBuilder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			alertDialogBuilder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});*/
			alertDialogBuilder.create().show();
		}

	}

	@Override
	protected void onNewIntent(Intent intent) {
		String uid = NFCUtilities.newIntent(intent);
		if (!uid.equals("") && jugadorActual != null) {
			jugadorActual.setCardUID(uid);
		}
	}
}