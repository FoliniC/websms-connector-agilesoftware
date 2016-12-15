/*
 * Copyright (C) 2010-2011 Felix Bechstein
 * 
 * This file is part of WebSMS.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.websms.connector.agilesoftware;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.Buffer;
import java.security.cert.CertificateException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Process;
import android.preference.PreferenceManager;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import de.ub0r.android.websms.connector.common.BasicConnector;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorService;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.Log;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.WebSMSException;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import static android.R.string.copy;

/**
 * AsyncTask to manage IO to agilesoftware.com API.
 * 
 * @author flx
 */
public final class Connectoragilesoftware extends BasicConnector {
	/** TAG for output. */
	private static final String TAG = "agilesoftware";
	/** SubConnectorSpec ID: with sender. */
	private static final String ID_W_SENDER = "w_sender";
	/** SubConnectorSpec ID: without sender. */
	private static final String ID_WO_SENDER = "wo_sender";
	/** Preference's name: hide with sender subcon. */
	private static final String PREFS_HIDE_W_SENDER = "hide_withsender";
	/** Preference's name: hide without sender subcon. */
	private static final String PREFS_HIDE_WO_SENDER = "hide_nosender";

	/** agilesoftware Gateway URL. */
	private static final String URL = "https://secure.agiletelecom.com/securesend_v1.aspx";
	/** Ad unitid. */
	private static final String AD_UNITID = "a14dbba90186ed3";

    @Override
    protected void doSend(final Context context, Intent intent) throws IOException {
        //super.doSend(context, intent);
//        GitHubService gitHubService = GitHubService.retrofit.create(GitHubService.class);
//        Call<List<Contributor>> call = gitHubService.repoContributors("square", "retrofit");
//        List<Contributor> result = call.execute().body();
        AgileTelecomService agileTelecomService = AgileTelecomService.retrofit.create(AgileTelecomService.class);

        ConnectorService smsData = (ConnectorService)context;

        Call call = null   ;
        ///////////
        try {
            ConnectorSpec e = ConnectorSpec.fromIntent(intent);
            final ConnectorCommand command = new ConnectorCommand(intent);
            String[] r = command.getRecipients();
            final ConnectorSpec origSpecs = new ConnectorSpec(intent);
            final ConnectorSpec specs = this.getSpec(context);
			String userName = null;
			userName  = getUsername(context, command, specs);
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(context);
            String recipient = null;
            if (true) {
                recipient = Utils.getRecipientsNumber(r[0]);
            }
            else
                recipient = "+1111111111";


            String serviceUserName = p.getString(Preferences.PREFS_USER, "");
			String servicePassword = p.getString(Preferences.PREFS_PASSWORD, "");
			String senderNumber = p.getString(Preferences.PREFS_SENDER_NUMBER, "");

            call = agileTelecomService.sendSMS( command.getText(), recipient, senderNumber, "H" , "file.sms", serviceUserName, servicePassword, "1234", "");
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {

                    try {
                        String responseBody = response.body().string();
                        int tokenPosition = responseBody.indexOf("+Ok");

                        if (tokenPosition > -1) {
                            // response ok
                            String amountString = responseBody.substring(tokenPosition + "+Ok ".length(),tokenPosition + "+Ok ".length() +"0000".length());
                            amountString = String.valueOf(Double.parseDouble(amountString)/1000).replace(".",",") + "€";

                            specs.setBalance(amountString);

                            final NotificationManager nm = (NotificationManager) context
                                    .getSystemService(Context.NOTIFICATION_SERVICE);
                            nm.notify(ConnectorService.NOTIFICATION_PENDING,
                                    ConnectorService.getNotification(context, command));
                            try {
                                setResultCode(Activity.RESULT_OK);
                            } catch (Exception e) {
                                Log.w(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "not an ordered boradcast", e);
                            }
                            Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "Message sent. Remaining credit: " + amountString);
                            showToast(context, "Message sent. Remaining credit: " +  amountString);
                        }
                        else
                        {   tokenPosition = responseBody.indexOf("-Err");
                            String errorMessage = null;
                            if (tokenPosition > -1 ) {
                                switch (responseBody.substring(tokenPosition + "-Err".length() + 1, tokenPosition + "-Err".length() + "000".length())) {
                                    case "001":
                                        errorMessage = "Username e/o password incorretti";
                                        break;
                                    case "002":
                                        errorMessage = "Credito esaurito";
                                        break;
                                    case "004":
                                        errorMessage = "Numero del destinatario non corretto";
                                        break;
                                    case "005":
                                        errorMessage = "Parametro smsNUMBER mancante";
                                        break;
                                    case "006":
                                        errorMessage = "Parametro smsTEXT mancante";
                                        break;
                                    case "007":
                                        errorMessage = "Messaggio non abilitato";
                                        break;
                                    case "008":
                                        errorMessage = "Errore del server nell'accettazione del messaggio";
                                        break;
                                    case "009":
                                        errorMessage = "Client time-out";
                                        break;
                                    case "011":
                                        errorMessage = "Parametro smsUser mancante";
                                        break;
                                    case "012":
                                        errorMessage = "Parametro smsPassword mancante";
                                        break;
                                    case "013":
                                        errorMessage = "Parametro smsNumber mancante";
                                        break;
                                    case "090":
                                        errorMessage = "Troppe sessioni attive";
                                        break;
                                    default:
                                        errorMessage = "Error code unknown " + responseBody.substring(tokenPosition + "-Err".length() + 1);
                                }
                                showToast(context, errorMessage);
                                specs.setErrorMessage(errorMessage);
                            }

                            else
                                Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", String.format("Call response error : %s", response.body().string()));
                        }
                    } catch (IOException e1) {
                        specs.setErrorMessage(context, e1);
                        Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", String.format("response error: %s", e1.toString()));
                    }  catch (Exception e2) {
                        specs.setErrorMessage(context, e2);
                        Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", String.format("response error: %s", e2.toString()));
                    }

                    sendInfo(context, null, null);
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "Call ko");
                    specs.setErrorMessage("Call ko sendig SMS: " + t.toString());
                    showToast(context, "Call ko sendig SMS: " + t.toString());
                    sendInfo(context, null, null);
                }
            });
        } catch (WebSMSException var8) {
            Log.e("IO", "error starting service", var8);
        } catch (Exception e) {
            Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "Call queued error:  " + e.getMessage());
        };       /////////

        //retrofit2.Response response = call.execute();



        Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "Call queued");
        //return response.body().string();
        //List<Contributor> result = call.execute().body();
    }

    @Override
    protected void onNewRequest(final Context context, final ConnectorSpec regSpec, ConnectorCommand command) {
        Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "onNewRequest");

        try {
            getBalance(context, regSpec);
        } catch (IOException ioe)
        {
            Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "Get balance error:  " + ioe.getMessage());
        }

        super.onNewRequest(context, regSpec, command);
    }

    final void getBalance(final Context context, final ConnectorSpec connectorSpec) throws IOException {
        Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  " [" +  Process.myTid() + "]", "Start ");
        AgileTelecomService agileTelecomService = AgileTelecomService.retrofit.create(AgileTelecomService.class);
        Call call = null   ;
        ///////////
        try {
            final SharedPreferences p = PreferenceManager
                    .getDefaultSharedPreferences(context);

            final ConnectorSpec specs = this.getSpec(context);

            String serviceUserName = p.getString(Preferences.PREFS_USER, "");
            String servicePassword = p.getString(Preferences.PREFS_PASSWORD, "");

            call = agileTelecomService.sendSMS( "", "+1111111111", "", "H" , "file.sms", serviceUserName, servicePassword, "1234", "");
            agileTelecomService.checkCredit(serviceUserName, servicePassword);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {

                    try {
                        String responseBody = response.body().string();
                        //Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  " [" +  Process.myTid() + "]", "Response received " + responseBody);
                        int tokenPosition = responseBody.indexOf("+Ok");

                        if (tokenPosition > -1) {
                            // response ok
                            String amountString = responseBody.substring(tokenPosition + "+Ok ".length(),tokenPosition + "+Ok ".length() +"0000".length());
                            amountString = String.valueOf(Double.parseDouble(amountString)/1000).replace(".",",") + "€";

                            specs.setBalance(amountString);

                            Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "Remaining credit: " + amountString);
                            showToast(context, "Remaining credit: " +  amountString);
                        }
                        else
                        {   tokenPosition = responseBody.indexOf("-Err");
                            String errorMessage = null;
                            if (tokenPosition > -1 ) {
                                switch (responseBody.substring(tokenPosition + "-Err".length() + 1, tokenPosition + "-Err".length() + "000".length())) {
                                    case "001":
                                        errorMessage = "Username e/o password incorretti";
                                        break;
                                    default:
                                        errorMessage = "Error code unknown " + responseBody.substring(tokenPosition + "-Err".length() + 1);
                                }
                                showToast(context, errorMessage);
                            }

                            else
                                Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", String.format("Call response error : %s", response.body().string()));
                        }
                    } catch (IOException e1) {
                        Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", String.format("response error: %s", e1.toString()));
                    }  catch (Exception e2) {
                        Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", String.format("response error: %s", e2.toString()));
                    }

                    sendInfo(context, null, null);
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "Call ko " + t.toString());
                    showToast(context, "Call ko retrieving credit: " + t.toString());
                    connectorSpec.setBalance("error retrieving credit " + t.toString());
                    sendInfo(context, null, null);
                }
            });
        } catch (WebSMSException var8) {
            Log.e("IO", "error starting service", var8);
        } catch (Exception e) {
            Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "Call queued error:  " + e.getMessage());
        };       /////////

        //retrofit2.Response response = call.execute();



        Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "Call get credit queued");
        //return response.body().string();
        //List<Contributor> result = call.execute().body();
    }

    @Override
	public ConnectorSpec initSpec(final Context context) {
        Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "Started ");
		final String name = context
				.getString(R.string.connector_agilesoftware_name);
		ConnectorSpec c = new ConnectorSpec(name);
		c.setAuthor(// .
		context.getString(R.string.connector_agilesoftware_author));
        c.setBalance("");
		c.setAdUnitId(AD_UNITID);
		c.setCapabilities(ConnectorSpec.CAPABILITIES_UPDATE
				| ConnectorSpec.CAPABILITIES_SEND
				| ConnectorSpec.CAPABILITIES_PREFS);
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (!p.getBoolean(PREFS_HIDE_WO_SENDER, false)) {
			c.addSubConnector(ID_WO_SENDER,
					context.getString(R.string.wo_sender), 0);
		}
		if (!p.getBoolean(PREFS_HIDE_W_SENDER, false)) {
			c.addSubConnector(ID_W_SENDER,
					context.getString(R.string.w_sender), 0);
		}
        Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "Ended ");
		return c;
	}

	@Override
	public ConnectorSpec updateSpec(final Context context,
			final ConnectorSpec connectorSpec) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (p.getBoolean(Preferences.PREFS_ENABLED, false)) {

            if (p.getString(Preferences.PREFS_PASSWORD, "").length() > 0) {
				connectorSpec.setReady();
			} else {
				connectorSpec.setStatus(ConnectorSpec.STATUS_ENABLED);
			}
		} else {
			connectorSpec.setStatus(ConnectorSpec.STATUS_INACTIVE);
		}
		return connectorSpec;
	}

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "OnReceive");
        super.onReceive(context, intent);
    }

    @Override
    public IBinder peekService(Context myContext, Intent service) {
        Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "peekService");
        return super.peekService(myContext, service);
    }

    /**
	 * Check return code from agilesoftware.com.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param ret
	 *            return code
	 * @return true if no error code
	 */
	private static boolean checkReturnCode(final Context context, final int ret) {
		Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "ret=" + ret);
		switch (ret) {
		case 100:
			return true;
		case 10:
			throw new WebSMSException(context, R.string.error_agilesoftware_10);
		case 20:
			throw new WebSMSException(context, R.string.error_agilesoftware_20);
		case 30:
			throw new WebSMSException(context, R.string.error_agilesoftware_30);
		case 31:
			throw new WebSMSException(context, R.string.error_agilesoftware_31);
		case 40:
			throw new WebSMSException(context, R.string.error_agilesoftware_40);
		case 50:
			throw new WebSMSException(context, R.string.error_agilesoftware_50);
		case 60:
			throw new WebSMSException(context, R.string.error_agilesoftware_60);
		case 70:
			throw new WebSMSException(context, R.string.error_agilesoftware_70);
		case 71:
			throw new WebSMSException(context, R.string.error_agilesoftware_71);
		case 80:
			throw new WebSMSException(context, R.string.error_agilesoftware_80);
		case 90:
			throw new WebSMSException(context, R.string.error_agilesoftware_90);
		default:
			throw new WebSMSException(context, R.string.error, " code: " + ret);
		}
	}

	@Override
	protected String getParamUsername() {
		return "user";
	}

	@Override
	protected String getParamPassword() {
		return "password";
	}

	@Override
	protected String getParamRecipients() {
		return "to";
	}

	@Override
	protected String getParamSender() {
		return null;
	}

	@Override
	protected String getParamText() {
		return "message";
	}

	@Override
	protected String getUsername(final Context context,
			final ConnectorCommand command, final ConnectorSpec cs) {
		return Utils.international2oldformat(Utils.getSender(context,
				command.getDefSender()));
	}

	@Override
	protected String getPassword(final Context context,
			final ConnectorCommand command, final ConnectorSpec cs) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		return Utils.md5(p.getString(Preferences.PREFS_PASSWORD, ""));
	}

	@Override
	protected String getRecipients(final ConnectorCommand command) {
		return Utils.joinRecipientsNumbers(
				Utils.national2international(command.getDefPrefix(),
						command.getRecipients()), ";", true);
	}

	@Override
	protected String getSender(final Context context,
			final ConnectorCommand command, final ConnectorSpec cs) {
		return null;
	}

	@Override
	protected boolean usePost(final ConnectorCommand command) {
		return false;
	}

	/*@Override
	protected void addExtraArgs(final Context context,
			final ConnectorCommand command, final ConnectorSpec cs,
			final ArrayList<BasicNameValuePair> d) {
		boolean sendWithSender = false;
        Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "addExtraArgs");
		final String sub = command.getSelectedSubConnector();
		if (sub != null && sub.equals(ID_W_SENDER)) {
			sendWithSender = true;
		}
		Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "send with sender = " + sendWithSender);
		if (sendWithSender) {
			d.add(new BasicNameValuePair("from", "1"));
		}
	}*/

    /*@Override
    protected HttpEntity addHttpEntity(Context context, ConnectorCommand command, ConnectorSpec cs) {
        Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "addHttpEntity");
        return super.addHttpEntity(context, command, cs);
    }*/

    @Override
    protected void doUpdate(Context context, Intent intent) throws IOException {
        Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "doUpdate");
        //super.doUpdate(context, intent);
    }

    @Override
	protected void parseResponse(final Context context,
			final ConnectorCommand command, final ConnectorSpec cs,
			final String htmlText) {
        Log.d(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]" + "[" +  Process.myTid() + "]", "parseResponse");
		if (htmlText == null || htmlText.length() == 0) {
			throw new WebSMSException(context, R.string.error_service);
		}
		String[] lines = htmlText.split("\n");
		int l = lines.length;
		if (command.getType() == ConnectorCommand.TYPE_SEND) {
			try {
				final int ret = Integer.parseInt(lines[0].trim());
				checkReturnCode(context, ret);
				if (l > 1) {
					cs.setBalance(lines[l - 1].trim());
				}
			} catch (NumberFormatException e) {
				Log.e(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "could not parse ret", e);
				throw new WebSMSException(e.getMessage());
			}
		} else {
			cs.setBalance(lines[l - 1].trim());
		}
	}
/*
    @Override
    protected void parseResponseCode(Context context, HttpResponse response) {
        Log.e(TAG + "." + new Object(){}.getClass().getEnclosingMethod().getName() +  "  [" +  Process.myTid() + "]", "parseResponseCode");
        super.parseResponseCode(context, response);
    }*/
}

interface GitHubService {
	@GET("repos/{owner}/{repo}/contributors")
    Call<List<Contributor>> repoContributors(
			@Path("owner") String owner,
			@Path("repo") String repo);



	Retrofit retrofit = new Retrofit.Builder()
			.baseUrl("https://api.github.com/")
            .client(UnsafeOkHttpClient.getUnsafeOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
			.build();
}


interface AgileTelecomService {
    @FormUrlEncoded
    @POST("securesend_v1.aspx")
    Call<ResponseBody> sendSMS(
            @Field("smsTEXT") String smsTEXT,
            @Field("smsNUMBER") String smsNUMBER,
            @Field("smsSENDER") String smsSENDER,
            @Field("smsGATEWAY") String smsGATEWAY,
            @Field("smsTYPE") String smsType,
            @Field("smsUSER") String smsUSER,
            @Field("smsPASSWORD") String smsPASSWORD,
            @Field("smsDELIVERY") String smsDELIVERY,
            @Field("smsDELAYED") String smsDELAYED);

    @GET("credit.aspx")
    Call<ResponseBody> checkCredit(
            @Query("smsUSER") String smsUSER,
            @Query("smsPASSWORD") String smsPASSWORD);

    Gson gson = new GsonBuilder()
            .setLenient()
            .create();

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://secure.agiletelecom.com/")
            .client(UnsafeOkHttpClient.getUnsafeOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();
}

