package eu.cyberkat.h2owirelessbalancecheck;

/* H2O Wireless Data Balance Checker app, aka "H2O Data Balance"
 * Copyright (c) 2014, Dylan J. Morrison <insidious@cyberkat.eu>
 * 
 * Permission to use, copy, modify, and/or distribute this software
 * for any purpose with or without fee is hereby granted, provided 
 * that the above copyright notice and this permission notice
 * appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL
 * WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR
 * CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF
 * CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import eu.cyberkat.views.AutoScaleTextView;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;




public class MainActivity extends Activity {
	public boolean Scaling;
	
	public static String ordinal(int i) {
	    String[] sufixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
	    switch (i % 100) {
	    case 11:
	    case 12:
	    case 13:
	        return i + "th";
	    default:
	        return i + sufixes[i % 10];

	    }
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
     super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button button = (Button)this.findViewById(R.id.button1);
    	final ProgressBar pb = getMyProgBar();
    	pb.setMax(100);
    	final AutoScaleTextView text1 = (AutoScaleTextView)this.findViewById(R.id.textView2);
    	final TextView text2 = (TextView)this.findViewById(R.id.textView3);
    	if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
    		text1.setVisibility(View.VISIBLE);
    		text2.setVisibility(View.GONE);
    		Scaling = true;
    	} else {
    		text1.setVisibility(View.GONE);
    		text2.setVisibility(View.VISIBLE);
    		Scaling = false;
    	}
        button.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				final EditText phonenum = getPhoneNum();
				String number = phonenum.getText().toString();
				new GrabTheBalance().execute(number);
			}
		});
    }
    
    public EditText getPhoneNum() {
    	
    	final EditText phonenum = (EditText)this.findViewById(R.id.editText2);
    	return phonenum;
    }

    public AutoScaleTextView getStatusText() {
    	
    	final AutoScaleTextView status = (AutoScaleTextView)this.findViewById(R.id.textView2);
    	return status;
    }
    public TextView getNonScaleStatusText() {
    	final TextView status = (TextView)this.findViewById(R.id.textView3);
    	return status;
    }
    public void setStatusText(String text) {
    	if (Scaling) {
    		AutoScaleTextView status = getStatusText();
    		status.setText(text);
    	} else {
    		TextView status = getNonScaleStatusText();
    		status.setText(text);
    	}
    }
    public ProgressBar getMyProgBar() {
    	
    	final ProgressBar progbar = (ProgressBar)this.findViewById(R.id.progressBar1);
    	return progbar;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.action_about:
    		PackageInfo pInfo = null;
			try {
				pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			} catch (NameNotFoundException e) { pInfo = null; }
			if (pInfo == null) {
				return false;
			}
    		String version = pInfo.versionName;
    		int versionCode = pInfo.versionCode;
    		Context aboutToastContext = getApplicationContext();
			CharSequence aboutToastText = "H2O Data Balance v" + version + ", (" + ordinal(versionCode) + " Revision)\nCopyright (c) 2014, Dylan J. Morrison <insidious@cyberkat.eu>, Licensed under the ISC license.";
			int aboutToastDuration = Toast.LENGTH_LONG;
			Toast aboutToast = Toast.makeText(aboutToastContext, aboutToastText, aboutToastDuration);
			aboutToast.show();
    		return true;
    	case R.id.action_grabnum:
    		TelephonyManager tMgr = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
    		String mPhoneNumber = tMgr.getLine1Number();
    		if (mPhoneNumber == null) {
    			Context numberErrorContext = getApplicationContext();
    			CharSequence numberErrorText = "Unable to get device phone number.";
    			int numberErrorDuration = Toast.LENGTH_SHORT;
    			Toast toast = Toast.makeText(numberErrorContext, numberErrorText, numberErrorDuration);
    			toast.show();
    		} else {
    			EditText textbox = (EditText)this.findViewById(R.id.editText2);
    			mPhoneNumber = mPhoneNumber.replaceAll("[^\\d]", "");
    			textbox.setText(mPhoneNumber);
    		}
    		return true;
    	default:
            return super.onOptionsItemSelected(item);
    	}
    }
    private class GrabTheBalance extends AsyncTask<String, Integer, String>{
    	
    	protected void onPreExecute() {
			setStatusText("Processing...");
    	}
    	@Override
    	protected String doInBackground(String... params) {
    		
    		if (params[0] != null) {
    	    	String result="";
    	    	HttpClient httpclient = new DefaultHttpClient();
    	    	
    	    	publishProgress(15);
    	    	HttpGet httpget = new HttpGet("https://www.h2owirelessnow.com/pageControl.php?page=get_balance&min=" + params[0]);
    	    	
    	    	publishProgress(30);
    	    	HttpResponse response;
    	    	try {
    	    		response = httpclient.execute(httpget);
        	    	
    	    		publishProgress(45);
    	    		HttpEntity entity = response.getEntity();
        	    	
    	    		publishProgress(60);
    	    		if (entity != null) {
    	        		InputStream instream = entity.getContent();
    	        		result= convertStreamToString(instream);
    	        		instream.close();
    	    	    	
    	        		publishProgress(75);
    	    		}
    	    	} catch (Exception e) {result= "Error: " + e.toString();} 
    	    	if (result != "") {
    	    		String[] results = result.split(";");
    	    		for (int i = 0; i < results.length; i++){
    	    			if (results[i].contains("data_bal")) {
    	    				String[] finalresult = results[i].split("'");
    	    				result = finalresult[1];
    	    			}
    	    		}
        	    	
    	    		publishProgress(90);
    	    	} else {
    	    		result = "ERROR: Results null";
    	    	}
    	    	
    	    	publishProgress(100);
    	    	return result;
    		} else {
    			return "ERROR: Got null input.";
    		}
    	}
    	
    	protected void onPostExecute(String result){
    		Log.w("RESULT", "\"" + result + "\"");
    		if (result.contains("N/A")) {
    			setStatusText("Data Balance Not Found");
    			publishProgress(0);
    		} else {
    			setStatusText(result + " Remaining");
    			publishProgress(0);
    		}			
    	}
    	
    	protected void onProgressUpdate(Integer... progress){
    		ProgressBar pb;
    		pb = getMyProgBar();
    		pb.setProgress(progress[0]);
    	}
    }
    private static String convertStreamToString(InputStream is) {
    	
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
