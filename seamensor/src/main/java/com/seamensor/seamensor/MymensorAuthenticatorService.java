package com.seamensor.seamensor;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MymensorAuthenticatorService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        MymensorAccAuthenticator authenticator = new MymensorAccAuthenticator(this);
        return authenticator.getIBinder();
    }
}
