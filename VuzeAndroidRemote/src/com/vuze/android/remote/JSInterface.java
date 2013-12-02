/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

package com.vuze.android.remote;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.aelitis.azureus.util.JSONUtils;
import com.google.analytics.tracking.android.MapBuilder;
import com.vuze.android.remote.activity.LoginActivity;
import com.vuze.android.remote.activity.MetaSearch;
import com.vuze.android.remote.dialog.DialogFragmentOpenTorrent;

public class JSInterface
{
	private FragmentActivity activity;

	private JSInterfaceListener listener;

	private String rpcRoot;

	private RemoteProfile remoteProfile;

	public JSInterface(FragmentActivity activity, WebView myWebView,
			JSInterfaceListener listener) {
		this.activity = activity;
		this.listener = listener;
		this.setRpcRoot(rpcRoot);
	}

	@JavascriptInterface
	public void updateSessionProperties(String json) {
		Map<?, ?> map = JSONUtils.decodeJSON(json);
		listener.sessionPropertiesUpdated(map);
	}

	@JavascriptInterface
	public void showOpenTorrentDialog() {
		DialogFragmentOpenTorrent dlg = new DialogFragmentOpenTorrent();
		dlg.show(activity.getSupportFragmentManager(), "OpenTorrentDialog");
	}

	@JavascriptInterface
	public boolean showConfirmDeleteDialog(String name, final long torrentID) {
		listener.openDeleteTorrentDialog(name, torrentID);
		return true;
	}

	@JavascriptInterface
	public boolean executeSearch(String search) {
		Intent myIntent = new Intent(Intent.ACTION_SEARCH);
		myIntent.setClass(activity, MetaSearch.class);
		if (remoteProfile.getRemoteType() == RemoteProfile.TYPE_LOOKUP) {
			Bundle bundle = new Bundle();
			bundle.putString("com.vuze.android.remote.searchsource", rpcRoot);
			bundle.putString("com.vuze.android.remote.ac", remoteProfile.getAC());
			myIntent.putExtra(SearchManager.APP_DATA, bundle);
		}
		myIntent.putExtra(SearchManager.QUERY, search);

		activity.startActivity(myIntent);
		return true;
	}

	@SuppressWarnings({
		"rawtypes",
		"unchecked"
	})
	@JavascriptInterface
	public void selectionChanged(String selectedTorrentsJSON,
			boolean haveActiveSel, boolean havePausedSel) {
		List selectedTorrents = JSONUtils.decodeJSONList(selectedTorrentsJSON);
		if (selectedTorrents == null) {
			selectedTorrents = new ArrayList(0);
		}
		listener.selectionChanged(selectedTorrents, haveActiveSel, havePausedSel);
	}

	@JavascriptInterface
	public void updateSpeed(long downSpeed, long upSpeed) {
		listener.updateSpeed(downSpeed, upSpeed);
	}

	@JavascriptInterface
	public void updateTorrentStates(boolean haveActive, boolean havePaused,
			boolean haveActiveSel, boolean havePausedSel) {
		listener.updateTorrentStates(haveActive, havePaused, haveActiveSel,
				havePausedSel);
	}

	@JavascriptInterface
	public void updateTorrentCount(long total) {
		listener.updateTorrentCount(total);
	}

	@JavascriptInterface
	public void logout() {
		if (activity.isFinishing()) {
			if (AndroidUtils.DEBUG) {
				System.err.println("activity finishing.. can't log out");
			}
			return;
		}

		if (AndroidUtils.DEBUG) {
			System.out.println("logging out " + activity.toString());
		}

		Intent myIntent = new Intent(activity.getIntent());
		myIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		myIntent.setClass(activity, LoginActivity.class);

		activity.startActivity(myIntent);
		activity.finish();
	}

	@JavascriptInterface
	public void uiReady() {
		listener.uiReady();
	}

	@JavascriptInterface
	public void cancelGoBack(boolean cancel) {
		listener.cancelGoBack(cancel);
	}

	@JavascriptInterface
	public boolean handleConnectionError(final long errNo, final String errMsg,
			final String status) {
		if (AndroidUtils.DEBUG) {
			System.out.println(remoteProfile.getAC() + "/hCE: " + errNo + ";"
					+ errMsg);
		}

		if (status.equals("timeout")
				|| (!AndroidUtils.isOnline(activity) && status.equals("error"))) {
			// ignore timeout for now :(
			// TODO: Don't ignore
			return true;
		}
		AndroidUtils.showError(activity, errMsg, true);
		return true;
	}

	@JavascriptInterface
	public boolean handleTapHold() {
		return true;
	}

	@JavascriptInterface
	public void slowAjax(String id) {
		if (AndroidUtils.DEBUG) {
			System.out.println("Slow Ajax for " + id);
		}
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (activity.isFinishing()) {
					return;
				}
				activity.setProgressBarIndeterminateVisibility(true);
			}
		});
	}

	@JavascriptInterface
	public void slowAjaxDone(String id, long ms) {
		if (AndroidUtils.DEBUG) {
			System.out.println("Slow Ajax Done for " + id + " after " + ms + "ms");
		}
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (activity.isFinishing()) {
					return;
				}
				activity.setProgressBarIndeterminateVisibility(false);
			}
		});
	}

	@JavascriptInterface
	public void torrentInfoShown(String id, String page) {
		VuzeEasyTracker.getInstance(activity).send(
				MapBuilder.createEvent("uiAction", "ViewShown", page, null).build());
	}

	public String getRpcRoot() {
		return rpcRoot;
	}

	public void setRpcRoot(String rpcRoot) {
		this.rpcRoot = rpcRoot;
	}

	public void setRemoteProfile(RemoteProfile remoteProfile) {
		this.remoteProfile = remoteProfile;
	}
}
