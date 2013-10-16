package kimathi.Kenya.nearfc;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	String results;
	public static final String MIME_TEXT_PLAIN="text/plain";
	public static final String TAG = "nfcDemo";
	private TextView mTextView;
	private TextView nTextView;
	private NfcAdapter mNfcAdapter;
	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.activity_main);
		mTextView = (TextView) findViewById(R.id.textview_explanation);
		nTextView = (TextView) findViewById(R.id.textView_display);
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (mNfcAdapter==null){
			Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		if (!mNfcAdapter.isEnabled()){
			mTextView.setText("NFC Disabled");
		}else{
			mTextView.setText(R.string.explanation);
			mTextView.setText("NFC Enabled");
		}
		handleIntent(getIntent());
	}
	
	protected void onResume(){
		super.onResume();
		setupForegroundDispatch(this, mNfcAdapter);
		super.onPause();
	}
	
	protected void onPause(){
		stopForegroundDispatch(this, mNfcAdapter);
        super.onPause();
	}
	
	protected void newIntent(Intent intent){
		handleIntent(intent);
	}

	private void handleIntent(Intent intent){
		String action = intent.getAction();
	    if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
	        String type = intent.getType();
	        if (MIME_TEXT_PLAIN.equals(type)) {
	            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
	            new NdefReaderTask().execute(tag);
	        } else {
	            Log.d(TAG, "Wrong mime type: " + type);
	        }
	    } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
	        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
	        String[] techList = tag.getTechList();
	        String searchedTech = Ndef.class.getName();
	        for (String tech : techList) {
	            if (searchedTech.equals(tech)) {
	                new NdefReaderTask().execute(tag);
	                break;
	            }
	        }
	    }
	}
	
	private class NdefReaderTask extends AsyncTask<Tag, Void, String> {
	    @Override
	    protected String doInBackground(Tag... params) {
	        Tag tag = params[0];
	        Ndef ndef = Ndef.get(tag);
	        if (ndef == null) {
	            return null;
	        }
	        NdefMessage ndefMessage = ndef.getCachedNdefMessage();
	        NdefRecord[] records = ndefMessage.getRecords();
	        for (NdefRecord ndefRecord : records) {
	            if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
	                try {
	                    return readText(ndefRecord);
	                } catch (UnsupportedEncodingException e) {
	                    Log.e(TAG, "Unsupported Encoding", e);
	                }
	            }
	        }
	        return null;
	    }
	    private String readText(NdefRecord record) throws UnsupportedEncodingException {
	        byte[] payload = record.getPayload();
	        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
	        int languageCodeLength = payload[0] & 0063;
	        return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
	    }
	    @Override
	    protected void onPostExecute(String result) {
	        if (result != null) {
	            nTextView.setText("Card Value : " + result);
	            String text=nTextView.getText().toString();
	            sendSMS("enter the phone number for the server if your using a web based sms server", text);
	        }
	    }
	}
	
	
	public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);
        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};
        
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }
	
	 public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
	        adapter.disableForegroundDispatch(activity);
	    }
	 
	 public void sendSMS(String phoneNumber, String message){
		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(phoneNumber, null, message, null, null);
	 }
	 
	 
}
