package com.woozzu.android.widget;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

/**
 * Created by han on 16-2-11.
 */
public class ContactContentObservers extends ContentObserver {

    private static String TAG = "ContentObserver";
    public static final int MSG_CONTACT = 0x3;
    private Context mContext;
    private Handler mHandler;

    public ContactContentObservers(Context context, Handler handler) {
        super(handler);
        mContext = context;
        mHandler = handler;
    }

    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        Log.v(TAG, "the contacts has changed");
        //mHandler.obtainMessage()
        mHandler.obtainMessage(MSG_CONTACT, "ContactsChanged").sendToTarget();
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
    }
}
