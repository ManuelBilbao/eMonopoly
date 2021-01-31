package ar.net.bilbao.emonopoly;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            // Set start_money and pass_go_money as Number fields
            final EditTextPreference.OnBindEditTextListener onBindEditTextListener = editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.selectAll();
            };
            ((EditTextPreference) getPreferenceManager().findPreference("start_money")).setOnBindEditTextListener(onBindEditTextListener);
            ((EditTextPreference) getPreferenceManager().findPreference("pass_go_money")).setOnBindEditTextListener(onBindEditTextListener);

            // Restore values to default
            ((Preference) getPreferenceManager().findPreference("restore_defaults")).setOnPreferenceClickListener((Preference.OnPreferenceClickListener) preference -> {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                editor.clear();
                editor.apply();
                PreferenceManager.setDefaultValues(getActivity(), R.xml.root_preferences, true);
                getPreferenceScreen().removeAll();
                onCreatePreferences(null, null);
                Toast.makeText(getActivity(), getString(R.string.settings_values_restored), Toast.LENGTH_SHORT).show();
                return true;
            });
        }
    }
}