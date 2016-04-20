/*
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.incallui;

import android.net.Uri;
import android.provider.CallLog;
import android.content.Context;

import com.cyanogen.ambient.common.CyanogenAmbientUtil;
import com.cyanogen.ambient.common.api.AmbientApiClient;
import com.cyanogen.ambient.common.api.PendingResult;
import com.cyanogen.ambient.common.api.ResultCallback;
import com.cyanogen.ambient.common.ConnectionResult;
import com.cyanogen.ambient.common.CyanogenAmbientUtil;
import com.cyanogen.ambient.deeplink.DeepLink;
import com.cyanogen.ambient.deeplink.DeepLinkApi;
import com.cyanogen.ambient.deeplink.DeepLinkServices;
import com.cyanogen.ambient.deeplink.linkcontent.DeepLinkContentType;
import com.cyanogen.ambient.deeplink.applicationtype.DeepLinkApplicationType;

import java.util.List;

public class DeepLinkIntegrationManager {

    public static DeepLinkIntegrationManager getInstance() {
        if (sInstance == null) {
            sInstance = new DeepLinkIntegrationManager();
        }
        return sInstance;
    }

    private static DeepLinkIntegrationManager sInstance;
    private AmbientApiClient mAmbientApiClient;
    private DeepLinkApi mApi;
    private volatile boolean mConnected = false;

    public void setUp(Context ctx) {
        if(ambientIsAvailable(ctx)) {
            mApi = (DeepLinkApi) DeepLinkServices.API;
            mAmbientApiClient =
                    new AmbientApiClient.Builder(ctx).addApi(DeepLinkServices.API).build();

            mAmbientApiClient.registerConnectionFailedListener(
                    new AmbientApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult connectionResult) {
                            mConnected = false;
                        }
                    });

            mAmbientApiClient.registerDisconnectionListener(
                    new AmbientApiClient.OnDisconnectionListener() {
                        @Override
                        public void onDisconnection() {
                            mConnected = false;
                        }
                    });

            mAmbientApiClient
                    .registerConnectionCallbacks(new AmbientApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(android.os.Bundle bundle) {
                            mConnected = true;
                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                            mConnected = false;
                        }
                    });
            mAmbientApiClient.connect();
        }
    }

    public PendingResult<DeepLink.DeepLinkResultList> getPreferredLinksFor(
            ResultCallback<DeepLink.DeepLinkResultList> callback, DeepLinkContentType category,
            Uri uri) {
        PendingResult<DeepLink.DeepLinkResultList> result = null;
        if (mConnected) {
            result = mApi.getPreferredLinksForSingleItem(mAmbientApiClient,
                    DeepLinkApplicationType.NOTE, category, uri);
            result.setResultCallback(callback);
        }
        return result;
    }

    public PendingResult<DeepLink.DeepLinkResultList> getPreferredLinksForList(
            ResultCallback<DeepLink.DeepLinkResultList> callback, DeepLinkContentType category,
            List<Uri> uris) {
        PendingResult<DeepLink.DeepLinkResultList> result = null;
        if (mConnected) {
            result = mApi.getPreferredLinksForList(mAmbientApiClient,
                    DeepLinkApplicationType.NOTE, category, uris);
            result.setResultCallback(callback);
        }
        return result;
    }

    /**
     * Generate a uri which will identify the call for a given number and timestamp
     *
     * @param number - the phone number dialed
     * @param time   - the time the call occured
     * @return Uri identifying the call.
     */
    public static Uri generateCallUri(String number, long time) {
        return Uri.parse(CallLog.AUTHORITY + "." + number + "." + time);
    }

    public boolean isConnected() {
        return mConnected;
    }
    public boolean ambientIsAvailable(Context ctx) {
        return CyanogenAmbientUtil.isCyanogenAmbientAvailable(ctx) == CyanogenAmbientUtil.SUCCESS;
    }

    public void sendMetricsEventToAmbient() {

    }
}
