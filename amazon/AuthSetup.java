package com.goertek.smartear.amazon;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener;
import com.amazon.identity.auth.device.authorization.api.AuthzConstants;
import com.amazon.identity.auth.device.shared.APIListener;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by almo.liu on 2016/11/16.
 */

public class AuthSetup {

    private static final String TAG = AuthSetup.class.getSimpleName();

    private static final String DEVICE_SERIAL_NUMBER = "deviceSerialNumber";
    private static final String ALEXA_ALL_SCOPE = "alexa:all";
    private static final String[] APP_SCOPES= { ALEXA_ALL_SCOPE };
    private static final String PRODUCT_ID = "productID";
    private static final String PRODUCT_INSTANCE_ATTRIBUTES = "productInstanceAttributes";


    private AmazonAuthorizationManager mAuthManager;

    private APIListener mAPIListener;
    private Context mContext;

    public AuthSetup(Context context, APIListener listener) {
        mAuthManager = new AmazonAuthorizationManager(context, Bundle.EMPTY);
        mAPIListener = listener;
        mContext = context;
    }

    public void loginInAmazon() {

        if (mAuthManager == null) {
            Toast.makeText(mContext, "mAuthManager == null", Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle options = new Bundle();
        JSONObject scopeData = new JSONObject();
        JSONObject productInfo = new JSONObject();
        JSONObject productInstanceAttributes = new JSONObject();

        try {
            productInstanceAttributes.put(DEVICE_SERIAL_NUMBER,AVSConstants.DeviceInfo.DEVICE_DSN);
            productInfo.put(PRODUCT_ID, AVSConstants.DeviceInfo.DEVICE_PRODUCT_ID);
            productInfo.put(PRODUCT_INSTANCE_ATTRIBUTES, productInstanceAttributes);
            scopeData.put(ALEXA_ALL_SCOPE, productInfo);
            options.putString(AuthzConstants.BUNDLE_KEY.SCOPE_DATA.val, scopeData.toString());
            mAuthManager.authorize(APP_SCOPES, options, new AuthorizeListener());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void accessAvs() {
            try {
                mAuthManager.getToken(APP_SCOPES, mAPIListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }
    private class AuthorizeListener implements AuthorizationListener {

        @Override
        public void onSuccess(Bundle response) {
            Log.i(TAG, "AuthorizeListener onSuccess");
            mAuthManager.getToken(APP_SCOPES, mAPIListener);
        }

        @Override
        public void onError(AuthError ae) {
            Log.e(TAG, "AuthorizeListener onError" + ae.getMessage());
            ae.printStackTrace();
        }

        @Override
        public void onCancel(Bundle cause) {

        }
    }

}
