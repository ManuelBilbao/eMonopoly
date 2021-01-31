package ar.net.bilbao.emonopoly;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import static ar.net.bilbao.emonopoly.NFCUtilities.disableDiscovering;
import static ar.net.bilbao.emonopoly.NFCUtilities.enableDiscovering;
import static ar.net.bilbao.emonopoly.NFCUtilities.newIntent;

public class MainActivity extends AppCompatActivity {

	private List<String> ids = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		FloatingActionButton fab = findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).show();
			}
		});
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
		enableDiscovering(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		disableDiscovering(this);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		String id = newIntent(intent);
		if (!id.equals("")) {
			tarjetaDetectada(id);
		}
	}

	private void tarjetaDetectada(String id) {
		if (ids.contains(id))
			ids.remove(id);
		else
			ids.add(id);

		StringBuilder string = new StringBuilder();
		for (String m : ids) {
			string.append("\n").append(m);
		}
		((TextView) findViewById(R.id.textview_first)).setText("NFC TAGs\n" + string.toString().trim());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public void handleClick(View view) {
		TextView textView = findViewById(R.id.textview_second);
		String texto = textView.getText().toString();
		texto = texto.concat(((Button) view).getText().toString());
		textView.setText(texto);
	}
}