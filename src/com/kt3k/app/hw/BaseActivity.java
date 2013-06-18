package com.kt3k.app.hw;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.ads.*;

public class BaseActivity extends Activity {

	private static final String JS_INTERFACE_NAME = "device";

	private static final int SUB_ACTIVITY_CODE_GENERAL = 1001;

	private Handler handler;

	private WebView webView;
	private AdView adView;
	private LinearLayout layout;

	private LondonBridge jsi;

	private String url = null;
	private String html = null;
	private String baseUrl = "";
	private String extraJSON = "";
	private Boolean adEnabled = true;

	private String activityResult = null;


	/**
	 * ウインドウ関連の設定をする。
	 */
	private void windowSetting() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
	}

	/**
	 * set up the main layout.
	 */
	private void setUpLayout() {
		// set up layout and add subviews.
		layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		setContentView(layout);

		// set up and layout the WebView
		setUpWebView();
		layout.addView(webView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f));
		//layout.addView(webView);

		// if the ad-flag is true, then set up and layout the AdView.
		if (adEnabled) {
			setUpAdView();
			adView.setGravity(Gravity.BOTTOM);
			LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.0f);
			p.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
			layout.addView(adView, p);
		}
	}

	/**
	 * webView 関連の設定をする。
	 */
	@SuppressLint("SetJavaScriptEnabled") private void setUpWebView() {
		webView = new WebView(this);
		if(url == null || "".equals(url)) {
			url = this.getAppHome() + this.getAppIndex();
		}

		// スクロールバーのスタイルを設定
		webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		webView.setScrollbarFadingEnabled(true);

		// WebViewClient と WebChromeClient をセット
		webView.setWebViewClient(new CustomWebViewClient());
		webView.setWebChromeClient(new WebChromeClient());


		//webView.getSettings().setUseWideViewPort(true);
		//webView.getSettings().setLoadWithOverviewMode(true);
		//webView.setInitialScale(100);


		// JavaScript を有効化
		webView.getSettings().setJavaScriptEnabled(true);
		// DomStorage (Web Storage) を有効化
		webView.getSettings().setDomStorageEnabled(true);
		// DB の path を指定する。
		webView.getSettings().setDatabasePath((new File(getCacheDir(), "/database")).toString());
		// 背景色
		webView.setBackgroundColor(Color.BLACK);

		// JavaScript interface 関連設定
		jsi = new LondonBridge(this, webView);
		webView.addJavascriptInterface(jsi, JS_INTERFACE_NAME);


		if (html == null || "".equals(html)) {
			webView.loadUrl(url);
		} else {
			webView.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", "");
		}
	}

	private void setUpAdView() {
		adView = new AdView(this, AdSize.BANNER, getPublisherId());

		AdRequest adRequest = new AdRequest();
		adRequest.addTestDevice(AdRequest.TEST_EMULATOR);               // Emulator
		adRequest.addTestDevice("DA5B0069BA8827B46FD8DBDB70EB7FAE");                      // Test Android Device
		adView.loadAd(adRequest);
	}

	private String getStringWithDefault(String name, String defaultValue) {
		String appHome = null;
		try {
			int resId = R.string.class.getField(name).getInt(null);
			appHome = this.getString(resId);
		} catch (Exception e) {
			appHome = defaultValue;
		}
		return appHome;
	}

	private String getAppHome() {
		return this.getStringWithDefault("app_home", "file:///android_asset/");
	}

	private String getAppIndex() {
		return this.getStringWithDefault("app_index", "index.html");
	}

	private String getPublisherId() {
		return this.getStringWithDefault("app_publisher_id", "");
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		handler = new Handler();

		windowSetting();

		Intent i = getIntent();
		url = i.getStringExtra("url");
		html = i.getStringExtra("html");
		baseUrl = i.getStringExtra("baseUrl");
		adEnabled = !("disable".equals(i.getStringExtra("ad")));

		setUpLayout();

		String menuJson = i.getStringExtra("menu");
		jsi.addOptionsMenuByJSON(menuJson);

		String preHooksJson = i.getStringExtra("preHooks");
		jsi.parsePreHooks(preHooksJson);

		extraJSON = i.getStringExtra("extraJSON");
	}

	public void log(String str) {
		Log.d(getString(R.string.app_name), str);
	}

	/**
	 * メニュー選択時の処理
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		OptionMenuItem listener = jsi.getFunction(item.getItemId());

		if(null == listener) {
			return true;
		}

		//log(listener.callback);
		jsi.jsExec(listener.callback);
		return true;
	}

	/**
	 * メニュー出現時の処理
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean ret = super.onCreateOptionsMenu(menu);
		jsi.onPrepareOptionsMenu(menu);
		return ret;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SUB_ACTIVITY_CODE_GENERAL) {
			if (resultCode == RESULT_OK) {
				activityResult = data.getStringExtra("result");
				if (activityResult != null) {
					jsi.jsExec("onResult()");
				}
			}
		}
	}

	@Override
	public void onDestroy(){
		this.adView.destroy();
		this.webView.destroy();

		super.onDestroy();
	}

	class CustomWebViewClient extends WebViewClient {
		private ProgressDialog dialog;

		public CustomWebViewClient() {
			super();
			dialog = null;
		}

		//ページ読み込み開始時の動作
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			//ダイアログを作成して表示
			dialog = new ProgressDialog(view.getContext());
			dialog.setMessage(getStringWithDefault("wait_message", "Loading"));
			dialog.show();

			//jsi.injectJSON();
		}

		//ページ読み込み終了時の動作
		@Override
		public void onPageFinished(WebView view, String url) {
			if (null != dialog) {
				//ダイアログを削除
				dialog.dismiss();
				dialog = null;
			}

			if (jsi.preHooksExist()) {
				jsi.firePreHooks();
			}
		}
	}

	private class MySSLSocketFactory extends SSLSocketFactory {
		SSLContext sslContext = SSLContext.getInstance("TLS");

		public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
			super(truststore);

			TrustManager tm = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] chain, String authType) {
				}

				public void checkServerTrusted(X509Certificate[] chain, String authType) {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};

			sslContext.init(null, new TrustManager[] { tm }, null);
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
			return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
		}

		@Override
		public Socket createSocket() throws IOException {
			return sslContext.getSocketFactory().createSocket();
		}
	}

	public class OptionMenuItem {
		public final int id;
		public final String	label;
		public final String callback;

		public OptionMenuItem(int id, final String label, final String func) {
			this.id = id;
			this.label = label;
			this.callback = func;
		}
	}

	public class LondonBridge {
		final private String home;

		private TimePickerDialog timePicker;
		private DatePickerDialog datePicker;

		private ProgressDialog progDialog;

		private int menuCount = 0;

		private List<OptionMenuItem> menuListBuffer;
		private List<OptionMenuItem> menuList;
		/**
		 * 開始時フック
		 */
		private List<String> preHooks;
		/**
		 * 終了時フック
		 */
		private List<String> postHooks;
		private Activity activity;
		private WebView webView;
		private Boolean optionClearFlg = false;

		private LondonBridge(Activity activity, WebView webView) {
			home = getAppHome();
			menuListBuffer = new ArrayList<OptionMenuItem>();
			menuList = new ArrayList<OptionMenuItem>();
			preHooks = new ArrayList<String>();
			postHooks = new ArrayList<String>();
			this.activity = activity;
			this.webView = webView;
		}

		public String getHome() {
			return home;
		}

		public String getJSONString() {
			return extraJSON;
		}

		public String getResultJSONString() {
			return activityResult;
		}

		/**
		 * javascript 関数表現を受け取って、そのページに対する bookmarklet として実行する。
		 * @param code 実行する javascript コード (全体が関数式か関数名になってる必要がある。関数名にする場合はその関数が実行するページ側で定義されている必要がある。)
		 */
		public void jsExec(final String code) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					webView.loadUrl("javascript:(" + code + ")()");
				}
			});
		}

		private void jsListExec(final List<String> jsList) {
			for (String code: jsList) {
				jsExec(code);
			}
		}

		public void firePreHooks() {
			jsListExec(preHooks);
		}

		public void firePostHooks() {
			jsListExec(postHooks);
		}

		public Boolean preHooksExist() {
			return !preHooks.isEmpty();
		}

		public Boolean postHooksExist() {
			return !postHooks.isEmpty();
		}

		public void parsePreHooks(final String preHooksJson) {
			JSONArray preHooks = makeJSONArray(preHooksJson);

			if (preHooks != null) {
				for (int i = 0; i < preHooks.length(); i++) {
					try {
						String hook = preHooks.getString(i);
						this.preHooks.add(hook);
					} catch (JSONException e) {
					}
				}
			}
		}

		private JSONArray makeJSONArray(final String str) {
			if (str == null) {
				return null;
			}

			JSONArray result;
			try {
				result = new JSONArray(str);
			} catch (JSONException e) {
				result = null;
			}
			return result;
		}

		private List<String> makeStringList(final JSONArray json) {
			List<String> items = new ArrayList<String>();
			for (int i = 0; i < json.length(); i++) {
				try {
					String label = json.getString(i);
					items.add(label);
				} catch (JSONException e) {
				}
			}
			return items;
		}

		private List<String> makeStringList(final String jsonString) {
			return makeStringList(makeJSONArray(jsonString));
		}

		private CharSequence[] makeCharSequenceArray(final String jsonString) {
			List<String> list = makeStringList(jsonString);
			return list.toArray(new CharSequence[list.size()]);
		}

		private List<Boolean> makeBooleanList(final String jsonString) {
			return makeBooleanList(makeJSONArray(jsonString));
		}

		private List<Boolean> makeBooleanList(final JSONArray json) {
			List<Boolean> items = new ArrayList<Boolean>();
			for (int i = 0; i < json.length(); i++) {
				try {
					Boolean val = json.getBoolean(i);
					items.add(val);
				} catch (JSONException e) {
				}
			}
			return items;
		}

		/*private boolean[] makeBooleanArray(final String jsonString) {
			List<Boolean> list = makeBooleanList(jsonString);
			return makeBooleanArray(list, list.size());
		}*/

		private boolean[] makeBooleanArray(final String jsonString, int length) {
			List<Boolean> list = makeBooleanList(jsonString);
			return makeBooleanArray(list, length);
		}

		private boolean[] makeBooleanArray(final List<Boolean> list, int length) {
			boolean[] array = new boolean[length];
			for (int i = 0; i < length; i ++) {
				if (i < list.size()) {
					array[i] = list.get(i);
				} else {
					array[i] = false;
				}
			}
			return array;
		}

		/**
		 * ラベルとコールバックを指定して、オプションメニューを１個追加する。
		 * @param label オプションメニューに表示するラベル。
		 * @param callback オプションメニュー選択時に実行されるコールバック。(javascript 関数式 or 関数名)
		 */
		public void addOptionMenu(final String label, final String callback) {
			menuListBuffer.add(new OptionMenuItem(menuCount++, label, callback));
		}

		/**
		 * JSON 形式の文字列から、オプションメニューを設定する。
		 * JSON は [item, item, ...] という形式で、
		 * 各 item は {"label": 表示ラベル, "callback": コールバックする javascript 関数式 or 関数名}
		 * @param menuJson JSON 文字列
		 */
		public void addOptionsMenuByJSON(final String menuJson) {
			JSONArray menuList = makeJSONArray(menuJson);

			if (menuList != null) {
				for (int i = 0; i < menuList.length(); i++) {
					try {
						JSONObject menu = menuList.getJSONObject(i);
						String label = menu.getString("label");
						String callback = menu.getString("callback");
						jsi.addOptionMenu(label, callback);
					} catch (JSONException e) {
					}
				}
			}
		}

		/**
		 * オプションメニューをクリアする。
		 */
		public void clearOptionsMenu() {
			optionClearFlg = true;
			menuListBuffer = new ArrayList<OptionMenuItem>();
		}

		/**
		 * オプションメニューを開く。
		 */
		public void openOptionsMenu() {
			activity.openOptionsMenu();
		}

		/*public void setListener( final String func ) {
			//String param = "javascript:flyappSystemFunc('(" + func + ")();');";
			String param = "javascript:(" + func + ")();";
			//m_Web.loadUrl( param );
		}*/

		/**
		 * Shared Preferences に key に対応する値 value を保存する。
		 * @param key
		 * @param value
		 */
		public void set(final String key, final String value) {
			final Editor editor = getPrefs().edit();
			editor.putString(key, value);
			editor.commit();
		}

		/**
		 * Shared Preferences から key に対応する値を取り出す。無かった場合は、defualtValue で指定したデフォルト値を返す。
		 * @param key 取り出したいキー
		 * @param defaultValue 値が無かった場合のデフォルト値
		 */
		public String get(final String key, final String defaultValue) {
			return getPrefs().getString(key, defaultValue);
		}

		public boolean has(final String key) {
			return getPrefs().contains(key);
		}

		/**
		 * Shared Preferences から key に対応する値を削除する。
		 * @param key 削除したいキー
		 */
		public void remove(final String key) {
			final Editor editor = getPrefs().edit();
			editor.remove(key);
			editor.commit();
		}

		/**
		 * Shared Preferences をすべて削除する。
		 */
		public void dataClear() {
			Editor editor = getPrefs().edit();
			for (String key: getPrefs().getAll().keySet()) {
				editor.remove(key);
			}
			editor.commit();
		}

		/**
		 * Shared Preference をダンプする。
		 * @return Shared Preference のダンプ
		 */
		public String dataDump() {
			return getPrefs().getAll().toString();
		}

		private SharedPreferences getPrefs() {
			return PreferenceManager.getDefaultSharedPreferences(activity);
		}

		public boolean onPrepareOptionsMenu(Menu menu) {
			if (optionClearFlg) {
				menu.clear();
				optionClearFlg = false;
			}
			for (int i = 0; i < menuListBuffer.size(); ++i) {
				OptionMenuItem item = menuListBuffer.get(i);
				menu.add(Menu.NONE, Menu.FIRST + item.id, Menu.NONE, item.label);
				menuList.add( item );
			}

			menuListBuffer = new ArrayList<OptionMenuItem>();

			return true;
		}

		public OptionMenuItem getFunction(int id) {
			OptionMenuItem item = null;
			id -= Menu.FIRST;

			for( int i = 0; i < menuList.size(); ++i ){
				item = (OptionMenuItem)menuList.get( i );
				if( item.id == id ) {
					break;
				}
				item = null;
			}
			return item;
		}

		private void kickActivity(String action, String param, String errorCallback) {
			Intent intent = null;

			if( null != param && !param.equals("") ) {
				Uri uri = Uri.parse(param);
				intent = new Intent(action, uri);
			} else {
				intent = new Intent(action);
			}

			try {
				startActivity(intent);
			} catch (Exception e) {
				if (errorCallback != null && !"".equals(errorCallback)) {
					jsExec(errorCallback);
				}
			}
		}

		public void openBrowser(String uri, String errorCallback) {
			kickActivity(Intent.ACTION_VIEW, uri, errorCallback);
		}

		public void openBrowser(String uri) {
			openBrowser(uri, null);
		}

		/**
		 * webView をリロードする。
		 */
		public void reload() {
			webView.reload();
		}

		public void openUrlWithJSON(String url, String menuJSON, String preHooks, String json) {
			Intent i = new Intent(activity, BaseActivity.class);
			i.putExtra("url", url);
			i.putExtra("menu", menuJSON);
			i.putExtra("preHooks", preHooks);
			i.putExtra("extraJSON", json);
			activity.startActivityForResult(i, SUB_ACTIVITY_CODE_GENERAL);
		}

		public void openUrl(String url, String menuJSON, String preHooks) {
			openUrlWithJSON(url, menuJSON, preHooks, "{}");
		}

		public void openUrl(String url, String menuJSON) {
			openUrl(url, menuJSON, null);
		}

		public void openUrl(String url) {
			openUrl(url, "[]", null);
		}

		public void openHtmlWithJSON(String html, String baseUrl, String menuJSON, String preHooks, String json) {
			Intent i = new Intent(activity, BaseActivity.class);
			i.putExtra("html", html);
			i.putExtra("baseUrl", baseUrl);
			i.putExtra("menu", menuJSON);
			i.putExtra("preHooks", preHooks);
			i.putExtra("extraJSON", json);
			activity.startActivityForResult(i, SUB_ACTIVITY_CODE_GENERAL);
		}

		public void openHtml(String html, String baseUrl, String menuJSON, String preHooks) {
			openHtmlWithJSON(html, baseUrl, menuJSON, preHooks, "{}");
		}

		public void openHtml(String html, String baseUrl, String menuJSON) {
			openHtml(html, baseUrl, menuJSON, null);
		}

		public void openHtml(String html, String baseUrl) {
			openHtml(html, baseUrl, "[]", null);
		}

		public void openHtml(String html) {
			openHtml(html, getHome(), "[]", null);
		}

		/**
		 * url に GET リクエストし、レスポンスの HTML を引数として、callback 関数を実行する。
		 * エラーだった場合は、errorCallback 関数を実行する。
		 * @param url
		 * @param callback GET リクエスト成功時の JavaScript callback
		 * @param errorCallback GET リクエスト失敗時の JavaScript callback
		 */
		public void httpGet(final String url, final String callback, final String errorCallback) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
						trustStore.load(null, null);

						SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
						sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

						AndroidHttpClient ahc = AndroidHttpClient.newInstance("Android UserAgent");

						ahc.getConnectionManager().getSchemeRegistry().unregister("https");
						ahc.getConnectionManager().getSchemeRegistry().register(new Scheme("https", sf, 443));

						//log(url);
						//log(callback);
						HttpResponse res = ahc.execute(new HttpGet(url));
						final String html = JSONObject.quote(httpResponseGetContent(res, "UTF-8"));
						if (callback != null) {
							jsi.jsExec("function(){(" + callback + ")(" + html + ");}");
						}
						ahc.close();
					} catch (Exception e) {
						e.printStackTrace();
						jsi.jsExec(errorCallback);

					}
				}
			}).start();
		}

		public void httpGet(String url, String callback) {
			httpGet(url, callback, null);
		}

		public void httpPost(String url, String params, String callback, String errorCallback) {
			String html = null;
			try {
				AndroidHttpClient ahc = AndroidHttpClient.newInstance("Android UserAgent");
				HttpResponse res = ahc.execute(new HttpGet(url));
				html = JSONObject.quote(httpResponseGetContent(res, "UTF-8"));
				if (callback != null) {
					jsi.jsExec("function(){(" + callback + ")(" + html + ");}");
				}
				ahc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void httpPost(String url, String params, String callback) {
			httpPost(url, params, callback, null);
		}

		private String httpResponseGetContent(HttpResponse res, String encoding) throws IllegalStateException, IOException {
			BufferedReader reader = new BufferedReader(new InputStreamReader(res.getEntity().getContent(), encoding));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			return sb.toString();
		}

		public void showProgressDialog(final String message) {
			if (progDialog == null) {
				progDialog = new ProgressDialog(activity);
			}

			if (message != null) {
				progDialog.setMessage(message);
			} else {
				progDialog.setMessage(getStringWithDefault("wait_message", "Loading"));
			}

			progDialog.show();
		}

		public void showProgressDialog() {
			showProgressDialog(null);
		}

		public void dismissProgressDialog() {
			if (progDialog != null) {
				progDialog.dismiss();
				progDialog = null;
			}
		}
		/**
		 * alert ダイアログを表示する。
		 * @param title タイトル
		 * @param message メッセージ
		 * @param callback コールバック
		 * @param cancellable キャンセルボタンを表示するかどうか
		 */
		public void alert(final String title, final String message, final String callback, final Boolean cancellable) {
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);

			if (title != null) {
				builder.setTitle(title);
			}

			if (message != null) {
				builder.setMessage(message);
			}

			if (callback != null) {
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int i) {
						jsi.jsExec(callback);
						dialog.dismiss();
					}
				});
			} else {
				builder.setPositiveButton("OK", null);
			}

			if (cancellable != null && cancellable) {
				builder.setNegativeButton("CANCEL", null);
			}

			AlertDialog ad = builder.create();
			ad.show();
		}

		public void alert(final String title, final String message, final String callback) {
			alert(title, message, callback, null);
		}

		public void alert(final String title, final String message) {
			alert(title, message, null);
		}

		public void alert(final String message) {
			alert(null, message);
		}

		public void confirm(final String message, final String callback) {
			alert(null, message, callback, true);
		}

		public void showTimePicker(int hour, int minute, final String callback) {
			timePicker = new TimePickerDialog(activity, new TimePickerDialog.OnTimeSetListener() {
				@Override
				public void onTimeSet(TimePicker view, int hour, int minute) {
					jsi.jsExec("function(){(" + callback + ")(" + hour + "," + minute + ");}");
				}
			}, hour, minute, true);
			timePicker.show();
		}

		public void showDatePicker(int year, int month, int day, final String callback) {
			datePicker = new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
				@Override
				public void onDateSet(DatePicker view, int year, int month, int day) {
					jsi.jsExec("function(){(" + callback + ")(" + year + "," + month + "," + day + ");}");
				}
			}, year, month, day);
			datePicker.show();
		}

		public void showSelectBox(String title, String options, final String callback) {
			new AlertDialog.Builder(activity).setTitle(title).setItems(makeCharSequenceArray(options), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					jsi.jsExec("function(){(" + callback + ")(" + which + ");}");
					dialog.dismiss();
				}
			}).show();
		}

		public void showRadioBox(String title, String options, int selected, final String callback) {
			new AlertDialog.Builder(activity).setTitle(title).setSingleChoiceItems(makeCharSequenceArray(options), selected, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					jsi.jsExec("function(){(" + callback + ")(" + which + ");}");
					dialog.dismiss();
				}
			}).show();
		}

		public void showMultiSelectBox(String title, String options, String selected, final String callback) {
			CharSequence[] seq = makeCharSequenceArray(options);
			new AlertDialog.Builder(activity).setTitle(title).setMultiChoiceItems(seq, makeBooleanArray(selected, seq.length), new DialogInterface.OnMultiChoiceClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which, boolean val) {
					jsi.jsExec("function(){(" + callback + ")(" + which + "," + (val ? "true" : "false") + ");}");
					dialog.dismiss();
				}
			}).show();
		}

		public void showNumberEditBox(final String title, final String defaultValue, final String callback) {
			showEditBoxWithInputType(title, defaultValue, callback, InputType.TYPE_CLASS_PHONE);
		}

		public void showTextEditBox(final String title, final String defaultValue, final String callback) {
			showEditBoxWithInputType(title, defaultValue, callback, InputType.TYPE_CLASS_TEXT);
		}

		/**
		 * Edit Box を表示する。
		 * @param title Dialog のタイトル
		 * @param defaultValue Edit Box のデフォルト値
		 * @param callback callback 関数 (js 文字列)
		 * @param inputType virtual keyboard の input type
		 */
		public void showEditBoxWithInputType(final String title, final String defaultValue, final String callback, int inputType) {
			final EditText editText = new EditText(activity);
			editText.setInputType(inputType);
			final AlertDialog alertDialog = new AlertDialog.Builder(activity)
			.setTitle(title)
			.setView(editText)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int arg1) {
					jsi.jsExec("function(){(" + callback + ")('" + editText.getText() + "');}");

					dialog.dismiss();
				}
			})
			.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int arg1) {
					dialog.dismiss();
				}
			})
			.create();
			editText.setOnFocusChangeListener(new OnFocusChangeListener() {

				@Override
				public void onFocusChange(View view, boolean hasFocus) {
					if (hasFocus) {
						alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
					}
				}
			});
			editText.setText(defaultValue);

			alertDialog.show();
		}

		/**
		 * トーストを表示する。
		 * @param text 表示するテキスト
		 */
		public void toast(String text) {
			Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
		}

		/**
		 * トーストを長く表示する。
		 * @param text 表示するテキスト
		 */
		public void toastLong(String text) {
			Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
		}

		public void log(String log) {
			Log.d(getString(R.string.app_name), log);
		}
		public void error(String log) {
			Log.e(getString(R.string.app_name), log);
		}
		public void warn(String log) {
			Log.w(getString(R.string.app_name), log);
		}
		public void info(String log) {
			Log.i(getString(R.string.app_name), log);
		}
		public void debug(String log) {
			Log.d(getString(R.string.app_name), log);
		}
		public void verbose(String log) {
			Log.v(getString(R.string.app_name), log);
		}

		public void finish() {
			activity.finish();
		}

		public void finish(String json) {
			Intent data = new Intent();
			data.putExtra("result", json);
			activity.setResult(RESULT_OK, data);
			activity.finish();
		}

		public void onDestroy() {
			this.webView = null;
			this.activity = null;
		}
	}
}
