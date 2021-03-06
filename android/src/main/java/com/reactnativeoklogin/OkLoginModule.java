package com.reactnativeoklogin;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.*;
import com.facebook.react.module.annotations.ReactModule;

import ru.ok.android.sdk.Odnoklassniki;
import ru.ok.android.sdk.OkListener;

import android.app.Activity;
import android.content.Intent;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ru.ok.android.sdk.OkRequestMode;
import ru.ok.android.sdk.SharedKt;
import ru.ok.android.sdk.util.OkAuthType;

import static com.reactnativeoklogin.Helpers.*;


@ReactModule(name = OkLoginModule.NAME)
public class OkLoginModule extends ReactContextBaseJavaModule {
  private static final String LOG = "RNOdnoklassniki";
  private static final String E_LOGIN_ERROR = "E_LOGIN_ERROR";
  private static final String E_REQUEST_ERROR = "E_REQUEST_ERROR";
  private static final String E_GET_USER_FAILED = "E_GET_USER_FAILED";
  private static final String E_ACTIVITY_DOES_NOT_EXIST = "E_ACTIVITY_DOES_NOT_EXIST";


  private Odnoklassniki odnoklassniki;
  private String redirectUri;
  private Promise loginPromise;
  private Promise requestPromise;


  public static final String NAME = "OkLogin";

  public OkLoginModule(ReactApplicationContext reactContext) {
    super(reactContext);

  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void initialize(final String appId, final String appKey) {
    Log.d(LOG, "Inititalizing app " + appId + " with key " + appKey);
    odnoklassniki = Odnoklassniki.createInstance(getReactApplicationContext(), appId, appKey);
    redirectUri = "okauth://ok" + appId;
  }

  @ReactMethod
  public void request(final String method, ReadableMap params, final Promise promise) {
    requestPromise = promise;

    odnoklassniki.checkValidTokens(new OkListener() {
      @Override
      public void onSuccess(@NotNull JSONObject json) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            Map<String, String> paramsMap = new HashMap<>();
            @SuppressWarnings("unchecked")
            Iterator<Map.Entry<String, Object>> iterator = params.getEntryIterator();
            while (iterator.hasNext()) {
              String name = iterator.next().getKey();
              paramsMap.put(name, params.getString(name));
            }

            odnoklassniki.request(method, paramsMap, OkRequestMode.getDEFAULT(), new OkListener() {
              @Override
              public void onSuccess(@NotNull JSONObject requestData) {
                try {
                  WritableMap result = Arguments.createMap();
                  result.putString(SharedKt.PARAM_ACCESS_TOKEN, json.optString(SharedKt.PARAM_ACCESS_TOKEN));
                  result.putString(SharedKt.PARAM_SESSION_SECRET_KEY, json.optString(SharedKt.PARAM_SESSION_SECRET_KEY));

                  result.putMap("user", convertJsonToMap(requestData));

                  requestPromise.resolve(result);
                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }

              @Override
              public void onError(@Nullable String s) {
                Log.d(LOG, s);
                requestPromise.reject(E_REQUEST_ERROR, s);
              }
            });
          }
        }).start();
      }

      @Override
      public void onError(@Nullable String s) {
        Log.d(LOG, "Valid token wasn't found at login, requesting authorization");
        promise.reject(E_LOGIN_ERROR, s);
      }
    });


  }

  @ReactMethod
  public void login(final ReadableArray scope, final Promise promise) {


    int scopeSize = scope.size();
    final String[] scopeArray = new String[scopeSize];
    for (int i = 0; i < scopeSize; i++) {
      scopeArray[i] = scope.getString(i);
    }
    loginPromise = promise;
    odnoklassniki.checkValidTokens(new OkListener() {
      @Override
      public void onSuccess(JSONObject json) {
        Log.d(LOG, "Check valid token success");
        resolveWithCurrentUser(json.optString(SharedKt.PARAM_ACCESS_TOKEN), json.optString(SharedKt.PARAM_SESSION_SECRET_KEY));
      }

      @Override
      public void onError(String error) {
        Log.d(LOG, "Valid token wasn't found at login, requesting authorization");
        Activity activity = getCurrentActivity();

        if (activity == null) {
          promise.reject(E_ACTIVITY_DOES_NOT_EXIST, "Activity doesn't exist");
          return;
        }

        odnoklassniki.requestAuthorization(activity, redirectUri, OkAuthType.ANY, scopeArray);

        getReactApplicationContext().addActivityEventListener(new ActivityEventListener() {
          @Override
          public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
            if (odnoklassniki.isActivityRequestOAuth(requestCode)) {
              odnoklassniki.onAuthActivityResult(requestCode, resultCode, data, new OkListener() {
                @Override
                public void onSuccess(@NotNull JSONObject jsonObject) {
                  resolveWithCurrentUser(odnoklassniki.getMAccessToken(), odnoklassniki.getMSessionSecretKey());
                }

                @Override
                public void onError(@Nullable String s) {
                  Log.d(LOG, s);
                  promise.reject(E_LOGIN_ERROR, s);
                }
              });
            }
          }

          @Override
          public void onNewIntent(Intent intent) {

          }
        });
      }
    });
  }

  private void resolveWithCurrentUser(final String accessToken, final String sessionSecretKey) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          String userStr = odnoklassniki.request("users.getCurrentUser", null, OkRequestMode.getDEFAULT());

          WritableMap result = Arguments.createMap();
          result.putString(SharedKt.PARAM_ACCESS_TOKEN, accessToken);
          result.putString(SharedKt.PARAM_SESSION_SECRET_KEY, sessionSecretKey);
          result.putMap("user", convertJsonToMap(convertStringToJson(userStr)));
          loginPromise.resolve(result);
        } catch (Exception e) {
          loginPromise.reject(E_GET_USER_FAILED, "users.getLoggedInUser failed: " + e.getLocalizedMessage());
        }
      }
    }).start();
  }

  @ReactMethod
  public void logout(Promise promise) {
    Log.d(LOG, "Logout");
    odnoklassniki.clearTokens();
    promise.resolve(null);
  }


}
