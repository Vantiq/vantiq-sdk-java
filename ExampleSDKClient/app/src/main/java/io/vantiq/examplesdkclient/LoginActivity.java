package io.vantiq.examplesdkclient;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import io.vantiq.client.BaseResponseHandler;
import io.vantiq.client.Vantiq;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask authTask = null;

    // UI references.
    private EditText serverView;
    private EditText usernameView;
    private EditText passwordView;
    private View     progressView;
    private View     loginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Add logo image
        ImageView logo = (ImageView) findViewById(R.id.logo);
        logo.setImageResource(R.drawable.vantiq);

        // Setup widgets
        this.serverView = (EditText) findViewById(R.id.server);
        this.serverView.setText("https://dev.vantiq.com");

        this.usernameView = (EditText) findViewById(R.id.username);
        this.passwordView = (EditText) findViewById(R.id.password);

        this.passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        this.loginFormView = findViewById(R.id.login_form);
        this.progressView = findViewById(R.id.login_progress);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid username, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (this.authTask != null) {
            return;
        }

        // Reset errors.
        this.usernameView.setError(null);
        this.passwordView.setError(null);

        // Store values at the time of the login attempt.
        String server   = this.serverView.getText().toString();
        String username = this.usernameView.getText().toString();
        String password = this.passwordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid URL
        if (TextUtils.isEmpty(server)) {
            this.serverView.setError(getString(R.string.error_field_required));
            focusView = this.serverView;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            this.passwordView.setError(getString(R.string.error_invalid_password));
            focusView = this.passwordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(username)) {
            this.usernameView.setError(getString(R.string.error_field_required));
            focusView = this.usernameView;
            cancel = true;
        } else if (!isUsernameValid(username)) {
            this.usernameView.setError(getString(R.string.error_invalid_username));
            focusView = this.usernameView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);

            // Create Vantiq instance
            Vantiq vantiq = new Vantiq(server);
            ((ExampleApplication) getApplication()).setVantiq(vantiq);

            // Authenticate to the server
            this.authTask = new UserLoginTask(vantiq, username, password);
            this.authTask.execute((Void) null);
        }
    }

    private boolean isUsernameValid(String username) {
        return username != null && username.length() > 0;
    }

    private boolean isPasswordValid(String password) {
        return password != null && password.length() > 0;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            loginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            progressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Navigate to the main activity on authentication
     */
    protected void onLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    /**
     * Simply raise an alert if failed to login
     */
    protected void onLoginFailure() {
        new AlertDialog.Builder(this)
            .setMessage(R.string.error_login_failed)
            .setTitle(R.string.title_login_alert)
            .setPositiveButton(R.string.button_ok, null)
            .show();
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends VantiqAsyncTask<Void, Void, Boolean> {

        private final String username;
        private final String password;
        private boolean done = false;
        private boolean result = false;

        UserLoginTask(Vantiq vantiq, String username, String password) {
            super(vantiq);
            this.username = username;
            this.password = password;
        }

        @Override
        protected void doRequest(Vantiq vantiq, BaseResponseHandler handler) {
            vantiq.authenticate(this.username, this.password, handler);
        }

        @Override
        protected Boolean prepareResult(BaseResponseHandler handler) {
            return (handler.getStatusCode() == 200);
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            LoginActivity.this.authTask = null;
            showProgress(false);

            if (success) {
                onLogin();
            } else {
                onLoginFailure();
            }
        }

        @Override
        protected void onCancelled() {
            LoginActivity.this.authTask = null;
            showProgress(false);
        }
    }
}

