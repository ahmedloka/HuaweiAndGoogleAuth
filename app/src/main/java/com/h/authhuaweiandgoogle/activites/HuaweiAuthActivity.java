package com.h.authhuaweiandgoogle.activites;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.h.authhuaweiandgoogle.R;
import com.h.authhuaweiandgoogle.utilities.Constant;
import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.support.hwid.HuaweiIdAuthManager;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper;
import com.huawei.hms.support.hwid.result.AuthHuaweiId;
import com.huawei.hms.support.hwid.service.HuaweiIdAuthService;
import com.squareup.picasso.Picasso;

public class HuaweiAuthActivity extends AppCompatActivity implements View.OnClickListener {

    private AppCompatTextView mFamilyName;
    private AppCompatTextView mFullName;
    private AppCompatTextView mGivenName;
    private ImageView mProfileView;

    //Log tag
    public static final String TAG = "HuaweiIdActivity";
    private HuaweiIdAuthService mAuthManager;
    private HuaweiIdAuthParams mAuthParam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_huawei_auth);

        initViews();
    }

    private void initViews() {
        this.mGivenName = findViewById(R.id.givenName);
        this.mFamilyName = findViewById(R.id.familyName);
        this.mFullName = findViewById(R.id.fullName);
        this.mProfileView = findViewById(R.id.profileImage);

        findViewById(R.id.hwid_signin).setOnClickListener(this);
        findViewById(R.id.hwid_signout).setOnClickListener(this);
        findViewById(R.id.hwid_signInCode).setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Codelab Code
     * Pull up the authorization interface by getSignInIntent
     */
    private void signIn() {
        mAuthParam = new HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM)
                .setIdToken()
                .setAccessToken()
                .setEmail()
                .setProfile()
                .createParams();
        mAuthManager = HuaweiIdAuthManager.getService(this, mAuthParam);
        startActivityForResult(mAuthManager.getSignInIntent(), Constant.REQUEST_SIGN_IN_LOGIN);
    }

    private void signInCode() {
        mAuthParam = new HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM)
                .setProfile()
                .setEmail()
                .setProfile()
                .setAuthorizationCode()
                .createParams();
        mAuthManager = HuaweiIdAuthManager.getService(this, mAuthParam);
        startActivityForResult(mAuthManager.getSignInIntent(), Constant.REQUEST_SIGN_IN_LOGIN_CODE);
    }

    /**
     * Codelab Code
     * sign Out by signOut
     */
    private void signOut() {
        Task<Void> signOutTask = mAuthManager.signOut();
        signOutTask.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(HuaweiAuthActivity.this, "signOut Success", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "signOut Success");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.i(TAG, "signOut fail");
            }
        });
    }


    /**
     * Codelab Code
     * Silent SignIn by silentSignIn
     */
    private void silentSignIn() {
        Task<AuthHuaweiId> task = mAuthManager.silentSignIn();
        task.addOnSuccessListener(new OnSuccessListener<AuthHuaweiId>() {
            @Override
            public void onSuccess(AuthHuaweiId authHuaweiId) {
                Log.i(TAG, "silentSignIn success");
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                //if Failed use getSignInIntent
                if (e instanceof ApiException) {
                    ApiException apiException = (ApiException) e;
                    signIn();
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.hwid_signin:
                signIn();
                break;
            case R.id.hwid_signout:
                signOut();
                break;
            case R.id.hwid_signInCode:
                signInCode();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.REQUEST_SIGN_IN_LOGIN) {
            //login success
            //get user message by parseAuthResultFromIntent
            Task<AuthHuaweiId> authHuaweiIdTask = HuaweiIdAuthManager.parseAuthResultFromIntent(data);
            if (authHuaweiIdTask.isSuccessful()) {
                AuthHuaweiId huaweiAccount = authHuaweiIdTask.getResult();
                setUsersData(huaweiAccount);
                Toast.makeText(this, "SUCCESS", Toast.LENGTH_SHORT).show();
                Log.i(TAG, huaweiAccount.getDisplayName() + " signIn success ");
                Log.i(TAG, "AccessToken: " + huaweiAccount.getAccessToken());
                Log.i(TAG, "Profile Pic" + huaweiAccount.getAvatarUri());
            } else {
                Log.i(TAG, "signIn failed: " + ((ApiException) authHuaweiIdTask.getException()).getStatusCode());
            }
        }
        if (requestCode == Constant.REQUEST_SIGN_IN_LOGIN_CODE) {
            //login success
            Task<AuthHuaweiId> authHuaweiIdTask = HuaweiIdAuthManager.parseAuthResultFromIntent(data);
            if (authHuaweiIdTask.isSuccessful()) {
                AuthHuaweiId huaweiAccount = authHuaweiIdTask.getResult();
                setUsersData(huaweiAccount);
                Log.i(TAG, "signIn get code success.");
                Log.i(TAG, "ServerAuthCode: " + huaweiAccount.getAuthorizationCode());

                /**** english doc:For security reasons, the operation of changing the code to an AT must be performed on your server. The code is only an example and cannot be run. ****/
                /**********************************************************************************************/
            } else {
                Log.i(TAG, "signIn get code failed: " + ((ApiException) authHuaweiIdTask.getException()).getStatusCode());
            }
        }
    }

    private void setUsersData(AuthHuaweiId huaweiAccount) {
        if (huaweiAccount != null) {
            String fullName = huaweiAccount.getDisplayName();
            String givenName = huaweiAccount.getGivenName();
            String familyName = huaweiAccount.getFamilyName();
            String imageUrl = huaweiAccount.getAvatarUriString();
            if (!TextUtils.isEmpty(imageUrl)) {
                Picasso.with(this).load(imageUrl).placeholder(R.drawable.ic_account_circle_black_48dp).
                        into(this.mProfileView);
            }
            if (!TextUtils.isEmpty(fullName)) {
                this.mFullName.setText(fullName);
            }
            if (!TextUtils.isEmpty(givenName)) {
                this.mGivenName.setText(givenName);
            }
            if (!TextUtils.isEmpty(familyName)) {
                this.mFamilyName.setText(familyName);
            }
        }

    }
}
