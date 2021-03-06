package hr.foi.teamup.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import hr.foi.air.teamup.SessionManager;
import hr.foi.teamup.R;
import hr.foi.teamup.adapters.PersonAdapter;
import hr.foi.teamup.model.Person;
import hr.foi.teamup.model.Team;

/**
 * fragment containing current team members list
 * Created by Tomislav Turek on 05.12.15..
 */
public class TeamFragment extends Fragment {

    ListView users;
    PersonAdapter adapter;
    TextView groupName;
    TextView groupCode;
    TextView counter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        View v = inflater.inflate(R.layout.fragment_team_current, container, false);
        users = (ListView) v.findViewById(R.id.current_team_list);

        groupName = (TextView) v.findViewById(R.id.group_name);
        groupCode = (TextView) v.findViewById(R.id.group_code);
        counter = (TextView) v.findViewById(R.id.counter);

        return v;
    }

    /**
     * called from outside activity to update the list of persons
     * @param list list of persons online
     */
    public void updateList(ArrayList<Person> list){
        if(list != null && isVisible()) {
            adapter = new PersonAdapter(getActivity(), R.layout.fragment_team_current, list);
            users.setAdapter(adapter);
            counter.setText(String.format(getString(R.string.counter), adapter.getCount()));
            Team t= SessionManager.getInstance(getActivity().getApplicationContext())
                    .retrieveSession(SessionManager.TEAM_INFO_KEY, Team.class);
            if(t != null) {
                groupName.setText(String.format(getString(R.string.group_name), t.getName()));
                groupCode.setText(String.format(getString(R.string.group_code), t.getPassword()));
            }
        }
    }
}
