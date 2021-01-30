package ar.net.bilbao.emonopoly;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;

import java.util.concurrent.Callable;

public class NFCUtilities {
	public static String byteArrayToHexString(byte [] inarray) {
		int i, j, in;
        String [] hex = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        String out= "";
        for(j = 0 ; j < inarray.length ; ++j) {
	        in = (int) inarray[j] & 0xff;
	        i = (in >> 4) & 0x0f;
	        out += hex[i];
	        i = in & 0x0f;
	        out += hex[i];
        }
        return out;
	}

	private static final String[][] techList = new String[][] {
			new String[] {
					NfcA.class.getName(),
					NfcB.class.getName(),
					NfcF.class.getName(),
					NfcV.class.getName(),
					IsoDep.class.getName(),
					MifareClassic.class.getName(),
					MifareUltralight.class.getName(),
					Ndef.class.getName()
			}
	};

	public static void enableDiscovering(Context context) {
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, context.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		IntentFilter filter = new IntentFilter();
		filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
		filter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
		filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);

		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);
		nfcAdapter.enableForegroundDispatch((Activity) context, pendingIntent, new IntentFilter[]{filter}, techList);
	}

	public static void disableDiscovering(Context context) {
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);
		nfcAdapter.disableForegroundDispatch((Activity) context);
	}

	public static boolean isEnabled(Context context) {
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);
		return nfcAdapter.isEnabled();
	}

	public static String newIntent(Intent intent) {
		if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
			String id = byteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
			return id;
		}
		return "";
	}

	public interface TagDetectedCallback {
		public void make(String id);
	}
}
