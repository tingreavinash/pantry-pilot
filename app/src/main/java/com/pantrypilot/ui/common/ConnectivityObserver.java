package com.pantrypilot.ui.common;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Observes network connectivity and exposes isOffline as LiveData.
 * Registered once in MainActivity / injected via Hilt.
 */
@Singleton
public class ConnectivityObserver {

    private final MutableLiveData<Boolean> _isOffline = new MutableLiveData<>(false);
    public final LiveData<Boolean> isOffline = _isOffline;

    @Inject
    public ConnectivityObserver(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Seed initial state
        Network active = cm.getActiveNetwork();
        if (active != null) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(active);
            _isOffline.postValue(caps == null ||
                    !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
        } else {
            _isOffline.postValue(true);
        }

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                _isOffline.postValue(false);
            }

            @Override
            public void onLost(Network network) {
                // Check if any network remains before declaring offline
                Network current = cm.getActiveNetwork();
                if (current == null) {
                    _isOffline.postValue(true);
                }
            }
        });
    }
}
