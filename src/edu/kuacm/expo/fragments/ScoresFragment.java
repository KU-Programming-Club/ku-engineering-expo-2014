package edu.kuacm.expo.fragments;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.kuacm.expo.AsyncActivityTask;
import edu.kuacm.expo.ExpoApplication;
import edu.kuacm.expo.R;

public class ScoresFragment extends Fragment {

	private Button mIdSubmitButton;
	private EditText mIdEditText;
	private TextView mTeamNameTextView;
	private ListView mTeamScoresListView;
	private ArrayAdapter<String> mTeamScoresListAdapter;

	private String mTeamName;
	private String mTeamDivision;
	private ArrayList<String> mTeamScores = new ArrayList<String>();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_scores, container, false);

		mIdEditText = (EditText) v.findViewById(R.id.scores_edittext);
		mIdEditText.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
					loadScores();
					return true;
				} else {
					return false;
				}
			}

		});
		mTeamNameTextView = (TextView) v.findViewById(R.id.scores_team_name);
		mTeamScoresListView = (ListView) v.findViewById(R.id.scores_list);
		mIdSubmitButton = (Button) v.findViewById(R.id.scores_submit);
		mIdSubmitButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				loadScores();
			}
		});

		if (savedInstanceState != null) {
			mTeamName = savedInstanceState.getString("teamName");
			mTeamDivision = savedInstanceState.getString("teamDivision");
			mTeamScores = savedInstanceState.getStringArrayList("teamScores");
			if (mTeamName != null && mTeamDivision != null) {
				setTeamScoresLayout(mTeamName, mTeamDivision, mTeamScores);
			}
		}

		return v;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		@SuppressWarnings("unchecked")
		ExpoApplication<FragmentActivity> baseApp = (ExpoApplication<FragmentActivity>) getActivity().getApplication();
		baseApp.detachActivity(getActivity());

		outState.putString("teamName", mTeamName);
		outState.putString("teamDivision", mTeamDivision);
		outState.putStringArrayList("teamScores", mTeamScores);
	}

	@Override
	public void onResume() {
		super.onResume();
		@SuppressWarnings("unchecked")
		ExpoApplication<FragmentActivity> baseApp = (ExpoApplication<FragmentActivity>) getActivity().getApplication();
		baseApp.attachActivity(getActivity());
	}

	/**
	 * Returns whether the device's airplane mode is on.
	 * @param context The {@link Context} to use.
	 * @return {@code true} if airplane mode is on.
	 */
	@SuppressLint({ "NewApi", "InlinedApi" })
	@SuppressWarnings("deprecation")
	public static boolean isAirplaneModeOn(Context context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
			return Settings.System.getInt(context.getContentResolver(), 
					Settings.System.AIRPLANE_MODE_ON, 0) != 0;          
		} else {
			return Settings.Global.getInt(context.getContentResolver(), 
					Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
		}
	}

	private boolean isNetworkConnected() {
		ConnectivityManager conMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = conMgr.getActiveNetworkInfo();
		if (ni == null || !ni.isConnected() || !ni.isAvailable()) {
			return false;
		} else {
			return true;
		}
	}

	private void loadScores() {
		if (isAirplaneModeOn(getActivity())) {
			showToast("ERROR: Disable airplane mode before attempting to connect.");
		} else if (!isNetworkConnected()) {
			showToast("ERROR: No network connectivity.");
		} else {
			try {
				int id = Integer.parseInt(mIdEditText.getText().toString());
				new LoadScoresTask(getActivity(), getTag()).execute(id);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				showToast("ERROR: Input must be an integer.");
			}
		}
	}

	public static class LoadScoresTask extends AsyncActivityTask<FragmentActivity, Integer, Void, JSONObject> {
		//private static final String TEAM_SCORES_URL = "http://kuexpo.cloudapp.net/api/getTeamScore/";
		private static final String TEST_JSON = "{\n" +
				"\"status\": 42,\n" +
				"\"division\": \"Test Division\",\n" +
				"\"team_name\": \"Test Team\",\n" +
				"\"scores\": [\n" +
				"{\n" +
				"\"event\": \"Underwater Basketweaving\",\n" +
				"\"points\": 29.0\n" +
				"},\n" +
				"{\n" +
				"\"event\": \"Dysfunctional Programming\",\n" +
				"\"points\": 13.5\n" +
				"}\n" +
				"]\n" +
				"}";
		private final String FRAG_TAG;

		private ProgressDialog mProgress;
		private String mErrorMsg;

		public LoadScoresTask(FragmentActivity activity, final String fragTag) {
			super(activity);
			FRAG_TAG = fragTag;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			showProgressDialog();
		}

		@Override
		protected void onActivityDetached() {
			if (mProgress != null) {
				mProgress.dismiss();
				mProgress = null;
			}
		}
		@Override
		protected void onActivityAttached() {
			showProgressDialog();
		}

		@Override
		protected JSONObject doInBackground(Integer... params) { 
			/*
			 * Uncomment the following lines if you want to try out the real deal.
			 * It won't work unless a functional URL is given.
			 * Otherwise, try out the tailor-made JSON below.
			 */
			//			HttpClient httpClient = null;
			//			HttpResponse httpResponse = null;
			//			String responseStr = null;
			//
			//			try {
			//				final HttpParams httpParams = new BasicHttpParams();
			//				//Set timeout length to 10 seconds
			//				HttpConnectionParams.setConnectionTimeout(httpParams, 10000);
			//				HttpConnectionParams.setSoTimeout(httpParams, 10000);
			//				httpClient = new DefaultHttpClient(httpParams);
			//				HttpUriRequest request = new HttpGet(TEAM_SCORES_URL + params[0] + "/");
			//
			//				if (!isCancelled()) {
			//					httpResponse = httpClient.execute(request);
			//				}
			//
			//				if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			//					final String entity = EntityUtils.toString(httpResponse.getEntity(), HTTP.UTF_8).trim();
			//					if (entity != null) {
			//						responseStr = entity;
			//					}
			//				} else {
			//					throw new HttpException("Error code " + httpResponse.getStatusLine().getStatusCode());
			//				}
			//			} catch (HttpException e) {
			//				return cancelResult(e, "ERROR: server problem (" + httpResponse.getStatusLine().getStatusCode() + ").");
			//			} catch (HttpHostConnectException e) {
			//				return cancelResult(e, "ERROR: server connection refused.");
			//			} catch (ClientProtocolException e) {
			//				return cancelResult(e, "ERROR: client protocol problem.");
			//			} catch (NoHttpResponseException e) {
			//				return cancelResult(e, "ERROR: the target server failed to respond.");
			//			} catch (ConnectTimeoutException e) {
			//				return cancelResult(e, "ERROR: the server connection timed out.");
			//			} catch (SocketTimeoutException e) {
			//				return cancelResult(e, "ERROR: the server connection timed out.");
			//			} catch (IOException e) {
			//				return cancelResult(e, "ERROR: I/O problem.");
			//			} finally {
			//				httpClient.getConnectionManager().shutdown();
			//			}

			for (int i = 0; i < 9999999; i++) { // Stall to simulate wait
				if (isCancelled()) {
					return null;
				} else {
					continue;
				}
			}
			String responseStr = TEST_JSON;

			return onResponse(responseStr);
		}

		protected JSONObject onResponse(String response) {
			try {
				return new JSONObject(response);
			} catch (JSONException e) {
				return cancelResult(e, "ERROR: Malformed JSON.");
			}
		}

		@Override
		protected void onCancelled(JSONObject result) {
			super.onCancelled(result);

			if (mProgress != null) {
				mProgress.dismiss();
			}

			if (mErrorMsg != null) {
				getFragment().showToast(mErrorMsg);
			}
		}

		@Override
		protected void onPostExecute(JSONObject result) {
			super.onPostExecute(result);

			if (mProgress != null) {
				mProgress.dismiss();
			}

			try {
				//				int status = result.getInt("status");
				String teamName = result.getString("team_name");
				String teamDivision = result.getString("division");

				ArrayList<String> teamScores = new ArrayList<String>();
				JSONArray scoresArr = result.getJSONArray("scores");
				for (int i = 0; i < scoresArr.length(); i++) {
					JSONObject scoreObj = scoresArr.getJSONObject(i);
					String scoreStr = scoreObj.getString("event") + ": " + scoreObj.getDouble("points") + " points";
					teamScores.add(scoreStr);
				}

				getFragment().setTeamScoresLayout(teamName, teamDivision, teamScores);
			} catch (JSONException e) {
				e.printStackTrace();
				getFragment().showToast("ERROR: JSON does not contain expected attributes.");
			}
		}

		private ScoresFragment getFragment() {
			return (ScoresFragment) getActivity().getSupportFragmentManager().findFragmentByTag(FRAG_TAG);
		}

		private void showProgressDialog() {
			if (mProgress == null || !mProgress.isShowing()) {
				for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
					Log.d("TESTTEST", ste.toString());
				}
				mProgress = new ProgressDialog(getActivity());
				mProgress.setIndeterminate(true);
				mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				mProgress.setMessage("Loading scores...");
				mProgress.setCancelable(true);
				mProgress.setOnCancelListener(new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						mErrorMsg = "ERROR: Loading cancelled.";
						cancel(true);
					}
				});

				mProgress.show();
			}
		}

		private JSONObject cancelResult(Exception error, String errorMsg) {
			error.printStackTrace();
			mErrorMsg = errorMsg;
			cancel(true);
			return null;
		}
	}

	/**
	 * Utility method for easily showing a quick message to the user.
	 * @param message The message to display.
	 */
	public void showToast(String message) {
		Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Utility method for easily showing a quick message to the user.
	 * @param message The object that will be converted to a message by calling its
	 * {@link Object#toString() toString()} method.
	 */
	public void showToast(Object message) {
		if (message != null) {
			showToast(message.toString());
		} else {
			showToast("null");
		}
	}

	private void setTeamScoresLayout(String teamName, String teamDivision, ArrayList<String> teamScores) {
		mTeamName = teamName;
		mTeamDivision = teamDivision;
		mTeamScores = teamScores;

		mTeamNameTextView.setText(teamName + " (" + teamDivision + " division)");
		mTeamScoresListAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, teamScores);
		mTeamScoresListView.setAdapter(mTeamScoresListAdapter);
	}

}
