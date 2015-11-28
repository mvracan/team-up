package hr.foi.teamup;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import hr.foi.air.teamup.Input;
import hr.foi.air.teamup.Logger;
import hr.foi.air.teamup.SessionManager;
import hr.foi.teamup.handlers.TeamCreateHandler;
import hr.foi.teamup.model.Person;
import hr.foi.teamup.model.Team;
import hr.foi.teamup.webservice.ServiceAsyncTask;
import hr.foi.teamup.webservice.ServiceCaller;
import hr.foi.teamup.webservice.ServiceParams;

/**
 * Created by paz on 26.11.15..
 */
public class CreateTeamActivity extends AppCompatActivity implements Serializable {

    Button submit;
    EditText name;
    EditText teamDesc;

    EditText radius;
    List<Input> inputs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_team);


        name = (EditText) findViewById(R.id.teamName);
        teamDesc = (EditText) findViewById(R.id.teamDesc);
        radius = (EditText) findViewById(R.id.teamRadius);

        submit = (Button) findViewById(R.id.submitButton);
        submit.setOnClickListener(onSubmit);

        Logger.log("Radius : " +radius);

        inputs = Arrays.asList(
                new Input(name, Input.TEXT_MAIN_PATTERN, getString(R.string.team_name_error)),
                new Input(teamDesc, Input.TEXT_MAIN_PATTERN, getString(R.string.team_desc_error)),
                new Input(radius, Input.RADIUS_PATTERN,getString(R.string.team_radius_error))
        );

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    View.OnClickListener onSubmit = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Logger.log("CreateTeamActivity -- initiated creation of group");

            Logger.log("Radius : " +radius.getText().toString());

            if(Input.validate(inputs)){

                Logger.log("CreateTeamActivity -- creating new group and sending object to service");

                SessionManager manager = SessionManager.getInstance(getApplicationContext());
                Person creator = manager.retrieveSession(SessionManager.PERSON_INFO_KEY, Person.class);

                UUID uuid = UUID.randomUUID();

                List<Person> members= new ArrayList<Person>();
                members.add(creator);

                Team team =new Team(
                        0,
                        name.getText().toString(),
                        teamDesc.getText().toString(),
                        uuid.toString(),
                        Double.parseDouble( radius.getText().toString()),
                        uuid.toString(),
                        creator,
                        members);



                TeamCreateHandler teamCreateHandler = new TeamCreateHandler(CreateTeamActivity.this, team);

                new ServiceAsyncTask(teamCreateHandler).execute(new ServiceParams(
                        getString(hr.foi.teamup.webservice.R.string.team_create_path),
                        ServiceCaller.HTTP_POST, team));

            }
        }
    };

}