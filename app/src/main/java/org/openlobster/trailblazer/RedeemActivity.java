package org.openlobster.trailblazer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
//import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


/**
 * A login screen that offers login via email/password.
 */
public class RedeemActivity extends Activity {

    ScanDB scandb;
    boolean mRedeemStatus;
    int     mPoints;
    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    //private UserLoginTask mAuthTask = null;

    // UI references.
    private TextView mRedeemStatusView;
    private EditText mRedeemCommandView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redeem);
        // Set up the login form.
        mRedeemStatusView = (TextView) findViewById(R.id.redeem_status);
        scandb = new ScanDB(this);
        mRedeemStatus = scandb.getRedeem();
        mPoints = scandb.getPoints();
        if (mRedeemStatus)
            mRedeemStatusView.setText("已領取禮物");
        else
            mRedeemStatusView.setText("現有分數 " + mPoints );
        //mRedeemStatusView.setText("Redeem!");

        mRedeemCommandView = (EditText) findViewById(R.id.redeem_command);
//        mRedeemCommandView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
//                if (id == R.id.login || id == EditorInfo.IME_NULL) {
//                    attemptLogin();
//                    return true;
//                }
//                return false;
//            }
//        });


        Button mRedeemButton = (Button) findViewById(R.id.redeem_button);
        mRedeemButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRedeem();
            }
        });

        //mLoginFormView = findViewById(R.id.login_form);
        //mProgressView = findViewById(R.id.login_progress);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptRedeem() {
//        if (mAuthTask != null) {
//            return;
//        }

        // Reset errors.
//        mEmailView.setError(null);
        mRedeemCommandView.setError(null);

        // Store values at the time of the login attempt.
        //String email = mEmailView.getText().toString();
        String redeemCommand = mRedeemCommandView.getText().toString();

        if (redeemCommand.equals("cuhk")) {
            scandb.setRedeem();
        }

        if (redeemCommand.equals("462689")) {
            scandb.resetRedeem();
        }

        if (redeemCommand.equals("19890604")) {
            scandb.clearAll();
        }

        finish();
        startActivity(getIntent());
    }

//    private boolean isEmailValid(String email) {
//        //TODO: Replace this with your own logic
//        return email.contains("@");
//    }

//    private boolean isPasswordValid(String password) {
//        //TODO: Replace this with your own logic
//        return password.length() > 4;
//    }


}

