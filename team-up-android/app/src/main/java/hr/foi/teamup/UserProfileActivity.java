package hr.foi.teamup;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import java.util.Arrays;
import java.util.List;

import hr.foi.air.teamup.Input;
import hr.foi.air.teamup.Logger;
import hr.foi.air.teamup.SessionManager;
import hr.foi.teamup.handlers.UpdateHandler;
import hr.foi.teamup.model.Credentials;
import hr.foi.teamup.model.Person;
import hr.foi.teamup.webservice.ServiceAsyncTask;
import hr.foi.teamup.webservice.ServiceCaller;
import hr.foi.teamup.webservice.ServiceParams;

public class UserProfileActivity extends AppCompatActivity {

    Button change;
    EditText firstName;
    EditText lastName;
    EditText username;
    EditText password;
    EditText confirmPassword;
    Input passwordInput;
    Input confirmPasswordInput;
    Person user;
    List<Input> inputs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // session
        user= SessionManager.getInstance(getApplicationContext())
                .retrieveSession(SessionManager.PERSON_INFO_KEY, Person.class);

        // binding
        firstName = (EditText) findViewById(R.id.firstNameInput);
        username=(EditText) findViewById(R.id.usernameInput);
        username.setClickable(false);
        lastName = (EditText) findViewById(R.id.lastNameInput);
        password = (EditText) findViewById(R.id.passwordInput);
        confirmPassword = (EditText) findViewById(R.id.confirmPasswordInput);
        change = (Button) findViewById(R.id.submitButton);

        // input fill
        firstName.setText(user.getName());
        lastName.setText(user.getSurname());
        username.setText(user.getCredentials().getUsername());
        password.setText(user.getCredentials().getPassword());

        change.setOnClickListener(onChange);

        // password inputs
        passwordInput = new Input(password, Input.PASSWORD_PATTERN, getString(R.string.password_error));
        confirmPasswordInput = new Input(confirmPassword,
                Input.PASSWORD_PATTERN, getString(R.string.confirm_password_error));

        // input validation
        inputs = Arrays.asList(
                new Input(firstName, Input.TEXT_MAIN_PATTERN, getString(R.string.first_name_error)),
                new Input(lastName, Input.TEXT_MAIN_PATTERN, getString(R.string.last_name_error)),
                new Input(password, Input.PASSWORD_PATTERN, getString(R.string.password_error)),
                new Input(confirmPassword, Input.PASSWORD_PATTERN, getString(R.string.confirm_password_error))
        );

        // hide keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /**
     * invoked when submit button is clicked
     */
    View.OnClickListener onChange = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Logger.log("Initiated user update", getClass().getName());

            String firstNameValue = firstName.getText().toString();
            String lastNameValue = lastName.getText().toString();
            String passwordValue = password.getText().toString();

            if (Input.validate(inputs) && passwordInput.equals(confirmPasswordInput)) {

                Logger.log("Fetching user from session", getClass().getName());

                Logger.log("User fetched from session " + user.toString(), getClass().getName(), Log.DEBUG);
                user.setName(firstNameValue);
                user.setSurname(lastNameValue);
                Credentials changedPassword = new Credentials(user.getCredentials().getUsername(), passwordValue);
                user.setCredentials(changedPassword);

                Logger.log("Calling web service", getClass().getName());

                UpdateHandler updateHandler = new UpdateHandler(UserProfileActivity.this, user);
                new ServiceAsyncTask(updateHandler).execute(new ServiceParams(
                        getString(hr.foi.teamup.webservice.R.string.person_path) + user.getIdPerson(),
                        ServiceCaller.HTTP_PUT, user));
            }
        }


    };

}
