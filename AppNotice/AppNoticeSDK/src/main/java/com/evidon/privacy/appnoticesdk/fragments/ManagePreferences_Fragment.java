package com.evidon.privacy.appnoticesdk.fragments;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.evidon.privacy.appnoticesdk.AppNotice;
import com.evidon.privacy.appnoticesdk.AppNotice_Activity;
import com.evidon.privacy.appnoticesdk.R;
import com.evidon.privacy.appnoticesdk.adapter.ManagePreferences_ViewPager_Adapter;
import com.evidon.privacy.appnoticesdk.model.AppNoticeData;
import com.evidon.privacy.appnoticesdk.utils.AppData;

/**
 *
 */
public class ManagePreferences_Fragment extends Fragment {
    private ManagePreferences_ViewPager_Adapter managePreferences_viewPager_adapter;
    private AppNotice_Activity appNotice_activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.evidon_fragment_manage_preferences, container, false);
        appNotice_activity = (AppNotice_Activity) getActivity();

        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.show();
        }

        AppNoticeData appNoticeData = AppNoticeData.getInstance(appNotice_activity);
        boolean showOptionalTab = appNoticeData.hasOptionalTrackers();
        boolean showEssentailTab = appNoticeData.hasEssentialTrackers();
        boolean showWebTab = getResources().getBoolean(R.bool.evidon_show_web_tab);
        ManagePreferences_ViewPager_Adapter.setActiveTabs(showOptionalTab, showEssentailTab, showWebTab);

        TabLayout tab_layout = (TabLayout) view.findViewById(R.id.tab_layout);
        Resources resources = getActivity().getResources();
        if (showOptionalTab) {
            tab_layout.addTab(tab_layout.newTab().setText(resources.getString(R.string.evidon_preferences_optional_title)));
        }
        if (showEssentailTab) {
            tab_layout.addTab(tab_layout.newTab().setText(resources.getString(R.string.evidon_preferences_essential_title)));
        }
        if (showWebTab) {
            tab_layout.addTab(tab_layout.newTab().setText(resources.getString(R.string.evidon_preferences_webbased_title)));
        }

        int tabCount = tab_layout.getTabCount();
        if (tabCount <= 1) {
            // This will force the single tab to be left aligned...and look more like a header row
            tab_layout.setTabMode(TabLayout.MODE_SCROLLABLE);
        }

        managePreferences_viewPager_adapter = new ManagePreferences_ViewPager_Adapter(getChildFragmentManager(), tabCount);

        final ViewPager view_pager = (ViewPager) view.findViewById(R.id.view_pager);
        view_pager.setAdapter(managePreferences_viewPager_adapter);
        view_pager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tab_layout));

        tab_layout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                view_pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LinearLayout explicitButtonLayout = (LinearLayout)getView().findViewById(R.id.explicit_button_layout);

        if (AppNotice_Activity.isConsentActive) {
            if (AppNotice.isImpliedMode) {
                // If implied mode, hide the explicit button layout
                explicitButtonLayout.setVisibility(View.GONE);

                // If implied mode, show the snackbar
                CoordinatorLayout coordinatorlayout = (CoordinatorLayout)getView().findViewById(R.id.coordinatorLayout);
                Snackbar snackbar = Snackbar
                        .make(coordinatorlayout, R.string.evidon_preferences_ready_message, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.evidon_preferences_continue_button, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Send notice for this event
                                AppNoticeData.sendNotice(AppNoticeData.pingEvent.IMPLIED_CONTINUE);
                                appNotice_activity.handleTrackerStateChanges();

                                // Let the calling class know the selected option
                                AppNoticeData appNoticeData = AppNoticeData.getInstance(appNotice_activity);

                                if (AppNotice_Activity.appNotice_callback != null) {
                                    AppNotice_Activity.appNotice_callback.onOptionSelected(true, appNoticeData.getTrackerHashMap(true));
                                }

                                // Close this fragment
                                AppNotice_Activity.isConsentActive = false;
                                appNotice_activity.finish();
                            }
                        });

                snackbar.show();
            } else {
                // If explicit mode, show the explicit button layout
                explicitButtonLayout.setVisibility(View.VISIBLE);

                // Watch for button clicks.
                AppCompatButton preferences_button_accept = (AppCompatButton)getView().findViewById(R.id.preferences_button_accept);
                preferences_button_accept.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        // Send notice for this event
                        AppNoticeData.sendNotice(AppNoticeData.pingEvent.EXPLICIT_ACCEPT);
                        appNotice_activity.handleTrackerStateChanges();

                        // Remember in a persistent way that the explicit notice has been accepted
                        AppData.setBoolean(AppData.APPDATA_EXPLICIT_ACCEPTED, true);

                        // Let the calling class know the selected option
                        AppNoticeData appNoticeData = AppNoticeData.getInstance(appNotice_activity);
                        if (AppNotice_Activity.appNotice_callback != null && !appNotice_activity.isFinishing()) {
                            AppNotice_Activity.appNotice_callback.onOptionSelected(true, appNoticeData.getTrackerHashMap(true));
                        }

                        // Close this fragment
                        AppNotice_Activity.isConsentActive = false;
                        appNotice_activity.finish();
                    }
                });

                AppCompatButton preferences_button_decline = (AppCompatButton)getView().findViewById(R.id.preferences_button_decline);
                preferences_button_decline.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        // Send notice for this event
                        AppNoticeData.sendNotice(AppNoticeData.pingEvent.EXPLICIT_DECLINE);

                        ExplicitDecline_Fragment fragment = new ExplicitDecline_Fragment();
                        FragmentManager fragmentManager = appNotice_activity.getSupportFragmentManager();
                        FragmentTransaction ft = fragmentManager.beginTransaction();
                        ft.replace(R.id.appnotice_fragment_container, fragment, AppNotice_Activity.FRAGMENT_TAG_EXPLICIT_DECLINE);
                        ft.addToBackStack(AppNotice_Activity.FRAGMENT_TAG_EXPLICIT_DECLINE);
                        ft.commit();
                    }
                });
            }
        } else {
            // If not in a consent flow, hide the explicit button layout
            explicitButtonLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        getActivity().setTitle(R.string.evidon_preferences_header);
    }

    public void onBackPressed() {
        final AppNotice_Activity appNotice_activity = (AppNotice_Activity) getActivity();
        appNotice_activity.handleTrackerStateChanges();
    }

}
