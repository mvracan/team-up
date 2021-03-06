package hr.foi.teamup;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

import hr.foi.air.teamup.Logger;
import hr.foi.air.teamup.ModularityCallback;
import hr.foi.air.teamup.SessionManager;
import hr.foi.air.teamup.nfcaccess.NfcForegroundDispatcher;
import hr.foi.air.teamup.nfcaccess.NfcNotAvailableException;
import hr.foi.air.teamup.nfcaccess.NfcNotEnabledException;
import hr.foi.air.teamup.prompts.AlertPrompt;
import hr.foi.air.teamup.prompts.InputPrompt;
import hr.foi.teamup.fragments.EmptyDataFragment;
import hr.foi.teamup.fragments.LocationFragment;
import hr.foi.teamup.fragments.TeamFragment;
import hr.foi.teamup.fragments.TeamHistoryFragment;
import hr.foi.teamup.maps.LocationCallback;
import hr.foi.teamup.maps.MapConfiguration;
import hr.foi.teamup.maps.MarkerClickHandler;
import hr.foi.teamup.model.Location;
import hr.foi.teamup.model.Person;
import hr.foi.teamup.model.Team;
import hr.foi.teamup.stomp.ListenerSubscription;
import hr.foi.teamup.stomp.OnStompCloseListener;
import hr.foi.teamup.stomp.Stomp;
import hr.foi.teamup.stomp.StompAuthentication;
import hr.foi.teamup.stomp.StompSocket;
import hr.foi.teamup.webservice.ServiceAsyncTask;
import hr.foi.teamup.webservice.ServiceCaller;
import hr.foi.teamup.webservice.ServiceParams;
import hr.foi.teamup.webservice.ServiceResponse;
import hr.foi.teamup.webservice.SimpleResponseHandler;

public class TeamActivity extends NfcForegroundDispatcher implements NavigationView.OnNavigationItemSelectedListener,
        LocationCallback {

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawer;
    private Person client;
    private double teamRadius;
    private StompAuthentication authentication;
    private StompSocket socket;
    private String teamId;
    private static TeamFragment teamFragment = new TeamFragment();
    private EmptyDataFragment emptyFragment;
    private static LocationFragment locationFragment = new LocationFragment();
    private NavigationView navigationView;
    private Person panicPerson;
    private MapConfiguration mapConfiguration;
    private Toolbar mToolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team);

        // stomp dependencies
        authentication = new StompAuthentication();
        socket = new StompSocket();
        socket.setOnStompCloseListener(listener);

        // instantiate location listeners
        mapConfiguration = new MapConfiguration(this, this);
        mapConfiguration.buildGoogleApiClient();
        mapConfiguration.createLocationRequest();

        // navigation menu
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mDrawer = (DrawerLayout) findViewById(R.id.drawer);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawer, mToolbar,
                R.string.drawer_open, R.string.drawer_close);
        mDrawer.setDrawerListener(mDrawerToggle);

        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        // panic button
        FloatingActionButton panicButton = (FloatingActionButton) findViewById(R.id.panic_button);
        panicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                socket.send("/app/team/" + teamId + "/panic/" + client.getIdPerson(), null);
            }
        });

        // log out button
        Button logOut = (Button) findViewById(R.id.log_out_button);
        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.log("Logging out user...");
                signOut();
            }
        });

        // set main fragments
        emptyFragment = new EmptyDataFragment();
        locationFragment.setCallback(callbackMarkerClick);

        Team t = SessionManager.getInstance(this).retrieveSession(SessionManager.TEAM_INFO_KEY, Team.class);
        if(t == null)
            exchangeFragments(emptyFragment);
        else
            teamId = String.valueOf(t.getIdTeam());

        // get current user
        client = SessionManager.getInstance(getApplicationContext())
                .retrieveSession(SessionManager.PERSON_INFO_KEY, Person.class);

        // gets form cookie for stomp authentication
        authentication.authenticate(client);

        getCurrentTeam();

        // starts nfc foreground dispatcher
        try {
            startNfcAdapter();
            setNfcDispatchCallback(callback);
        } catch (NfcNotAvailableException e) {
            Toast.makeText(this, getString(R.string.nfc_not_available), Toast.LENGTH_LONG).show();
        } catch (NfcNotEnabledException e) {
            Toast.makeText(this, getString(R.string.nfc_not_enabled), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        socket.send("/app/updateLocation", location);
    }

    /**
     * initiates stomp by adding channels and subscribing
     */
    private void initiateStomp(String message) {
        teamId = message;
        new ServiceAsyncTask(null).execute(new ServiceParams("/team/" + teamId
                + "/person/" + client.getIdPerson(), ServiceCaller.HTTP_POST, null));
        exchangeFragments(teamFragment);
        socket.addSubscriptionChannel(subscriptionUserLost, getString(R.string.user_channel));
        socket.addSubscriptionChannel(subscription, getString(R.string.group_channel) + teamId);
        socket.subscribe(authentication.getCookie());
        getCurrentTeam();
    }

    /**
     * executes when stomp socket gets closed
     */
    OnStompCloseListener listener = new OnStompCloseListener() {
        @Override
        public void onClose() {
            SessionManager.getInstance(getApplicationContext()).destroySession(SessionManager.TEAM_INFO_KEY);
            new ServiceAsyncTask(null).execute(new ServiceParams(getString(hr.foi.teamup.webservice.R.string.team_path)
                    + teamId + "/leave/" + client.getIdPerson(),
                    ServiceCaller.HTTP_POST, null));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    exchangeFragments(emptyFragment);
                    setNavigationMenuItems(R.menu.menu);
                    SessionManager.getInstance(getApplicationContext()).destroySession(SessionManager.TEAM_INFO_KEY);
                }
            });
            teamId = null;
        }
    };

    /**
     * gets current team
     */
    private void getCurrentTeam() {
        ServiceParams params = new ServiceParams(
                getString(hr.foi.teamup.webservice.R.string.team_person_path) + client.getIdPerson(),
                ServiceCaller.HTTP_POST, null
        );
        new ServiceAsyncTask(activeTeamHandler).execute(params);
    }

    /**
     * executes when server returns if active team is present
     */
    SimpleResponseHandler activeTeamHandler = new SimpleResponseHandler() {
        @Override
        public boolean handleResponse(ServiceResponse response) {
            if (response.getHttpCode() == 200) {

                // convert json to person object
                Team team = new Gson().fromJson(response.getJsonResponse(), Team.class);
                // save team to session
                SessionManager.getInstance(getApplicationContext())
                        .createSession(team, SessionManager.TEAM_INFO_KEY);

                // call parent implementation
                teamRadius = team.getRadius();

                setNavigationMenuItems(R.menu.team_exist_menu);
                if(!StompSocket.isActive())
                    initiateStomp(String.valueOf(team.getIdTeam()));

                return true;
            } else {

                setNavigationMenuItems(R.menu.menu);
                exchangeFragments(emptyFragment);
                return false;
            }
        }
    };

    /**
     * callback that every module can use
     * gets team id and subscribes him to team channel
     */
    ModularityCallback callback = new ModularityCallback() {
        @Override
        public void onAction(String message) {
            new ServiceAsyncTask(new SimpleResponseHandler() {
                @Override
                public boolean handleResponse(ServiceResponse response) {
                    if(response.getHttpCode() == 200) initiateStomp(response.getJsonResponse());
                    return true;
                }
            }).execute(new ServiceParams(getString(R.string.team_path)
                    + client.getIdPerson() + "/code/" + message, ServiceCaller.HTTP_GET, null));
        }
    };

    /**
     * stomp message callback
     */
    ListenerSubscription subscription = new ListenerSubscription() {
        @Override
        public void onMessage(Map<String, String> headers, String body) {
            Logger.log(body);

            if(body.equals(Stomp.SOCKET_FINISH)) {
                socket.close();
                return;
            }

            Type listType = new TypeToken<ArrayList<Person>>() {
            }.getType();

            final ArrayList<Person> persons = new Gson().fromJson(body, listType);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    teamFragment.updateList(persons);
                    locationFragment.setUserLocations(persons, teamRadius);
                }
            });
        }
    };

    /**
     * stomp panic message callback
     */
    ListenerSubscription subscriptionUserLost = new ListenerSubscription() {
        @Override
        public void onMessage(Map<String, String> headers, String body) {

            Logger.log(body);
            panicPerson = new Gson().fromJson(body, Person.class);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    // make him red if fragment is visible
                    if (locationFragment.isVisible()) {
                        locationFragment.paintPerson(panicPerson, BitmapDescriptorFactory.HUE_RED);
                    }

                    // vibrate
                    Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(2000);

                    // issue notification
                    NotificationCompat.Builder mBuilder = setNotification(setNotificationMessage(client, panicPerson));

                    int mNotificationId = 1;
                    NotificationManager mNotifyMgr =
                            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    mNotifyMgr.notify(mNotificationId, mBuilder.build());

                }
            });
        }
    };

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void setNavigationMenuItems(int menuId) {
        navigationView.getMenu().clear();
        navigationView.inflateMenu(menuId);
    }

    @Override
    public void onBackPressed() {
        mDrawer.closeDrawers();
        signOut();
    }

    /**
     * ask for sign out
     */
    private void signOut() {
        DialogInterface.OnClickListener signOutListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SessionManager.getInstance(getApplicationContext()).destroyAll();
                dialog.dismiss();
                if (socket != null) socket.close();
                for (int i = 0; i < getFragmentManager().getBackStackEntryCount(); ++i) {
                    getFragmentManager().popBackStack();
                }
                TeamActivity.super.onBackPressed();
            }
        };
        AlertPrompt signOutPrompt = new AlertPrompt(this);
        signOutPrompt.prepare(R.string.log_out_question, signOutListener,
                R.string.log_out, null, R.string.cancel);
        signOutPrompt.showPrompt();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_team_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.open_map) {
            if(teamId != null && !TextUtils.isEmpty(teamId)){
                exchangeFragments(locationFragment);
            } else if(!((LocationManager)this.getSystemService(Context.LOCATION_SERVICE))
                    .isProviderEnabled(LocationManager.GPS_PROVIDER)){
                Toast.makeText(this,
                        getString(R.string.location_not_enabled), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this,
                        getString(R.string.please_join), Toast.LENGTH_LONG).show();
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * exchanges fragments
     * @param fragment fragment that goes in foreground
     */
    private void exchangeFragments(Fragment fragment) {
        exchangeFragments(fragment, R.string.app_name);
    }

    /**
     * exchanges fragments with optional toolbar title
     * @param fragment fragment that goes in foreground
     * @param toolbarTitle title shown in toolbar
     */
    private void exchangeFragments(Fragment fragment, int toolbarTitle) {
        mToolbar.setTitle(toolbarTitle);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_frame, fragment);
        transaction.commit();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        // handling navigation items
        if (menuItem.getItemId() == R.id.profile) {
            startActivity(new Intent(getApplicationContext(), UserProfileActivity.class));
        } else if (menuItem.getItemId() == R.id.code) {
            new InputPrompt(this).prepare(R.string.join_group, callback, R.string.join, R.string.cancel).showPrompt();
        } else if (menuItem.getItemId() == R.id.nfc) {
            startActivity(new Intent(this, BeamActivity.class));
        } else if (menuItem.getItemId() == R.id.history) {
            exchangeFragments(new TeamHistoryFragment(), R.string.history);
        } else if (menuItem.getItemId() == R.id.new_group) {
            startActivity(new Intent(getApplicationContext(), CreateTeamActivity.class));
        } else if (menuItem.getItemId() == R.id.home) {
            if(SessionManager.getInstance(this)
                    .retrieveSession(SessionManager.TEAM_INFO_KEY, Team.class) != null)
                exchangeFragments(teamFragment);
            else
                exchangeFragments(emptyFragment);
        } else if (menuItem.getItemId() == R.id.leave_group) {
            socket.close();
        }

        mDrawer.closeDrawers();
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapConfiguration.getGoogleApiClient() != null && mapConfiguration.getGoogleApiClient().isConnected())
            mapConfiguration.stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapConfiguration.getGoogleApiClient() != null && mapConfiguration.getGoogleApiClient().isConnected()) {
            mapConfiguration.startLocationUpdates();
        }
    }

    /**
     * sets the notification message (panic or client)
     * @param client user using app
     * @param panic user that panics
     * @return notification message
     */
    protected String setNotificationMessage(Person client, Person panic){

        if(client.getIdPerson() == panic.getIdPerson())
            return "Please come back to group area!";

        return "User " + panic.getName() + " is panicking, go find this person!";

    }

    /**
     * handles red marker click
     */
    MarkerClickHandler callbackMarkerClick = new MarkerClickHandler(){

        @Override
        public void onMarkerClick(String... args){
            String username = args[0];
            if(socket != null)
                socket.send("/app/team/"+teamId+"/calmUser",username);

        }
    };

    /**
     * creates notification
     * @param notification notification text
     * @return notification builder
     */
    protected NotificationCompat.Builder setNotification(String notification){

        return new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(notification);
    }


}
