package com.ghostery.privacy.appnoticesdk.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatCallback;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatTextView;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Switch;

import com.ghostery.privacy.appnoticesdk.R;
import com.ghostery.privacy.appnoticesdk.callbacks.AppNotice_Callback;
import com.ghostery.privacy.appnoticesdk.model.AppNoticeData;
import com.ghostery.privacy.appnoticesdk.model.Tracker;
import com.ghostery.privacy.appnoticesdk.utils.Session;

import java.util.ArrayList;

/**
 * An fragmentActivity representing a list of Trackers. This fragmentActivity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the fragmentActivity presents a list of items, which when touched,
 * lead to a {@link TrackerDetailActivity} representing
 * item details. On tablets, the fragmentActivity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p/>
 * The fragmentActivity makes heavy use of fragments. The list of items is a
 * {@link TrackerListFragment} and the item details
 * (if present) is a {@link TrackerDetailFragment}.
 * <p/>
 * This fragmentActivity also implements the required
 * {@link TrackerListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class TrackerListActivity extends AppCompatActivity implements TrackerListFragment.Callbacks, AppCompatCallback {

    private ArrayList<Tracker> trackerArrayList;
    private ArrayList<Tracker> trackerArrayListClone;
    private AppNoticeData appNoticeData;

    /**
     * Whether or not the fragmentActivity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ghostery_activity_tracker_list);
        Session.set(Session.APPNOTICE_ALL_BTN_SELECT, false);    // "All" not clicked yet
        Session.set(Session.APPNOTICE_NONE_BTN_SELECT, false);   // "None" not clicked yet

        appNoticeData = (AppNoticeData)Session.get(Session.APPNOTICE_DATA);
        trackerArrayList = appNoticeData.trackerArrayList;
        trackerArrayListClone = appNoticeData.getTrackerListClone(); // Get a copy of the current tracker list so it can be compared on save

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
//            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
//            actionBar.setCustomView(R.layout.ghostery_action_bar_layout);
            actionBar.setDisplayHomeAsUpEnabled(true);
//            actionBar.setHomeButtonEnabled(true);
             actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.ghostery_header_background_color)));

            // If there is header text in the JSON, use it. Else use the default.
            if (appNoticeData != null)
                actionBar.setTitle(appNoticeData.getManage_preferences_header());
        }

        setAllNoneControlState();

		AppCompatTextView manage_preferences_description = (AppCompatTextView)findViewById(R.id.manage_preferences_description);
        if (manage_preferences_description != null) {
            AppNoticeData appNoticeData = AppNoticeData.getInstance(this);
            String manage_preferences_description_text = appNoticeData.getManage_preferences_description();
            manage_preferences_description.setText(manage_preferences_description_text);
        }

        if (findViewById(R.id.tracker_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // fragmentActivity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((TrackerListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.tracker_list))
                    .setActivateOnItemClick(true);
        }

        // TODO: If exposing deep links into your app, handle intents here.
    }

    @Override
    public void onResume()
    {
        super.onResume();
        ((TrackerListFragment) getSupportFragmentManager().findFragmentById(R.id.tracker_list)).refresh();
        setAllNoneControlState();
    }

    /**
     * Callback method from {@link TrackerListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(int uId) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this fragmentActivity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putInt(TrackerDetailFragment.ARG_ITEM_ID, uId);
            TrackerDetailFragment fragment = new TrackerDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.tracker_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail fragmentActivity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, TrackerDetailActivity.class);
            detailIntent.putExtra(TrackerDetailFragment.ARG_ITEM_ID, uId);
            startActivityForResult(detailIntent, 0);
        }
    }

    @Override
    public void onBackPressed() {
        saveTrackerStates();
        sendOptInOutNotices();    // Send opt-in/out ping-back
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Bundle arguments = new Bundle();

        int i = item.getItemId();
        if (item.getItemId() == android.R.id.home) {
            saveTrackerStates();
            sendOptInOutNotices();    // Send opt-in/out ping-back

            this.finish();  // Or onBackPressed();
        }

//        DetailFragment fragment = new DetailFragment();
//        fragment.setArguments(arguments);
//        getSupportFragmentManager().beginTransaction().replace(R.id.item_detail_container, fragment).commit();

        return true;
    }

    private void sendOptInOutNotices() {
        // Opt-in/out ping-back parameters
        boolean allBtnSelected = (boolean)Session.get(Session.APPNOTICE_ALL_BTN_SELECT, false);
        boolean noneBtnSelected = (boolean)Session.get(Session.APPNOTICE_NONE_BTN_SELECT, false);
        int pingBackCount = 0;      // Count the ping-backs

        // Send opt-in/out ping-back for each changed non-essential tracker
        for (int i = 0; i < trackerArrayList.size(); i++) {
            Tracker tracker = trackerArrayList.get(i);
            Tracker trackerClone = trackerArrayListClone.get(i);

            // If the tracker is non-essential and is changed...
            if (!tracker.isEssential() && (tracker.isOn() != trackerClone.isOn())) {
                boolean optOut = tracker.isOn() == false;
                boolean uniqueVisit = ((allBtnSelected == false && noneBtnSelected == false) || pingBackCount == 0);
                boolean firstOptOut = pingBackCount == 0;
                boolean selectAll = ((allBtnSelected == true || noneBtnSelected == true) && pingBackCount == 0);

                AppNoticeData.sendOptInOutNotice(tracker.getTrackerId(), optOut, uniqueVisit, firstOptOut, selectAll);    // Send opt-in/out ping-back
                pingBackCount++;
            }
        }
    }

    public void saveTrackerStates() {
        appNoticeData.saveTrackerStates();

        // If trackers have been changed and a consent dialog is not showing, send an updated tracker state hashmap to the calling app
        int trackerStateChangeCount = appNoticeData.getTrackerStateChangeCount(trackerArrayListClone);
        if (trackerStateChangeCount > 0 && !(boolean)Session.get(Session.APPNOTICE_PREF_OPENED_FROM_DIALOG, false)) {
            AppNotice_Callback appNotice_callback = (AppNotice_Callback)Session.get(Session.APPNOTICE_CALLBACK);
            appNotice_callback.onTrackerStateChanged(appNoticeData.getTrackerHashMap(true));
        }
    }


    public void onClick(View view) {
        RadioButton rbAll = (AppCompatRadioButton) findViewById(R.id.rb_all);
        RadioButton rbNone = (AppCompatRadioButton) findViewById(R.id.rb_none);

        if (view.getId() == R.id.rb_all) {
            appNoticeData.setTrackerOnOffState(true);
            rbAll.setChecked(true);
            rbNone.setChecked(false);
            Session.set(Session.APPNOTICE_ALL_BTN_SELECT, true);    // If they selected "All", remember it.
            Session.set(Session.APPNOTICE_NONE_BTN_SELECT, false);  // If they selected "None", remember that "None" wasn't the last set state.
        } else if (view.getId() == R.id.rb_none) {
            appNoticeData.setTrackerOnOffState(false);
            rbAll.setChecked(false);
            rbNone.setChecked(true);
            Session.set(Session.APPNOTICE_NONE_BTN_SELECT, true);   // If they selected "None", remember it.
            Session.set(Session.APPNOTICE_ALL_BTN_SELECT, false);   // If they selected "None", remember that "All" wasn't the last set state.
        }

        ((TrackerListFragment) getSupportFragmentManager().findFragmentById(R.id.tracker_list)).refresh();
    }

    public void onClickDescription(View view) {
        String manage_preferences_description_text = appNoticeData.getManage_preferences_description();

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(appNoticeData.getManage_preferences_header());
        alertDialog.setMessage(manage_preferences_description_text);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, appNoticeData.getClose_button(),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    public void onOptInOutClick(View view) {
        Boolean isOn = ((Switch)view).isChecked();
        int uId = (int)view.getTag();
        appNoticeData.setTrackerOnOffState(uId, isOn);
        Session.set(Session.APPNOTICE_ALL_BTN_SELECT, false);   // If they changed the state of a tracker, remember that "All" wasn't the last set state.
        Session.set(Session.APPNOTICE_NONE_BTN_SELECT, false);  // If they changed the state of a tracker, remember that "None" wasn't the last set state.

        ((TrackerListFragment) getSupportFragmentManager().findFragmentById(R.id.tracker_list)).refresh();
        setAllNoneControlState();
    }

    private void setAllNoneControlState() {
        int nonEssentialTrackerCount = appNoticeData.getNonEssentialTrackerCount();
        RadioButton rbAll = (AppCompatRadioButton) findViewById(R.id.rb_all);
        RadioButton rbNone = (AppCompatRadioButton) findViewById(R.id.rb_none);

        if (nonEssentialTrackerCount > 0) {
            int trackerOnOffStates = appNoticeData.getTrackerOnOffStates();
            if (trackerOnOffStates == 1) {              // All on
                rbAll.setChecked(true);
                rbNone.setChecked(false);
            } else if (trackerOnOffStates == -1) {      // None on
                rbAll.setChecked(false);
                rbNone.setChecked(true);
            } else {                                    // Some on, some off
                rbAll.setChecked(false);
                rbNone.setChecked(false);
            }
        } else {
            // Set both to unchecked and disabled
            rbAll.setChecked(false);
            rbNone.setChecked(false);
            rbAll.setEnabled(false);
            rbNone.setEnabled(false);
        }
    }

    @Override
    public void onSupportActionModeStarted(ActionMode mode) {
        //let's leave this empty, for now
    }

    @Override
    public void onSupportActionModeFinished(ActionMode mode) {
        // let's leave this empty, for now
    }
}