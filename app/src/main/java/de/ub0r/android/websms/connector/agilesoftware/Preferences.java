/*
 * Copyright (C) 2010 Felix Bechstein
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

import android.os.Process;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import de.ub0r.android.websms.connector.common.ConnectorPreferenceActivity;
import de.ub0r.android.websms.connector.common.Log;

/**
 * Preferences.
 * 
 * @author flx
 */
public final class Preferences extends ConnectorPreferenceActivity implements
		OnPreferenceClickListener {
	/** TAG  for output. */
	private static final String TAG = "agilesoftware.pref";

	/** Preference key: enabled. */
	static final String PREFS_ENABLED = "enable_agilesoftware";
	/** Preference's name: user's password. */
	static final String PREFS_PASSWORD = "password_agilesoftware";
	/** Preference's name: user's login. */
	static final String PREFS_USER = "user_agilesoftware";
	/** Preference's name: user's login. */
	static final String PREFS_SENDER_NUMBER = "sender_number_agilesoftware";

	/** Base referral URL. */
	private static final String REF_URL = "http://it.agiletelecom.com/prova-gratis-agile-telecom?REF=";
	/** Ids of referrals. */
	private static final String[] REF_IDS = new String[] { "Carlo"};

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.connector_agilesoftware_prefs);
		this.findPreference("new_account").setOnPreferenceClickListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onPreferenceClick(final Preference preference) {
		final int i = (int) Math.floor(Math.random() * REF_IDS.length);
		final String url = REF_URL + REF_IDS[i];
		final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		Log.i(TAG + "[" +  Process.myTid() + "]", "Referral URL: " + url);
		this.startActivity(intent);
		return true;
	}
}
