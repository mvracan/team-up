package hr.foi.teamup.handlers;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.Serializable;

import hr.foi.air.teamup.Logger;
import hr.foi.air.teamup.SessionManager;
import hr.foi.teamup.model.Team;
import hr.foi.teamup.webservice.ServiceAsyncTask;
import hr.foi.teamup.webservice.ServiceCaller;
import hr.foi.teamup.webservice.ServiceParams;
import hr.foi.teamup.webservice.ServiceResponse;

/**
 * Created by paz on 26.11.15..
 */
public class TeamCreateHandler extends ResponseHandler {

    public TeamCreateHandler(Context context, Serializable... args) {
        super(context, args);
    }

    @Override
    public boolean handleResponse(ServiceResponse response) {
        Team team = (Team) this.args[0];
        Logger.log(" TeamCreateHandler -- deserialized arguments: " + team.toString(), Log.DEBUG);

        if(response.getHttpCode() == 200) {
            Logger.log("TeamCreateHandler -- successfully created team", Log.DEBUG);
            // login

            SessionManager manager= SessionManager.getInstance(this.context);
            manager.createSession(team, SessionManager.TEAM_INFO_KEY);

            //TODO switch to teamactivity

            return true;
        } else {
            Logger.log("TeamCreateHandler -- creating team failed "
                    + response.getHttpCode(), Log.WARN);
            // show fail
            Toast.makeText(this.context,
                    "Team creation failed, please try again (" + response.getHttpCode() + ")",
                    Toast.LENGTH_LONG).show();
            return false;
        }
    }
}