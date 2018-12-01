/**
 * SettingsFragment.java
 * Implements the main fragment of the main activity
 * This fragment implements a RecyclerView list of radio stations and a bottom sheet player
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-18 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package com.mjhram.radioalarm.fragments;

import android.app.Activity;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mjhram.radioalarm.MainActivity;
import com.mjhram.radioalarm.PlayerService;
import com.mjhram.radioalarm.R;
import com.mjhram.radioalarm.adapter.CollectionAdapter;
import com.mjhram.radioalarm.adapter.CollectionViewModel;
import com.mjhram.radioalarm.core.Station;
import com.mjhram.radioalarm.helpers.DialogRename;
import com.mjhram.radioalarm.helpers.ImageHelper;
import com.mjhram.radioalarm.helpers.LogHelper;
import com.mjhram.radioalarm.helpers.NightModeHelper;
import com.mjhram.radioalarm.helpers.PermissionHelper;
import com.mjhram.radioalarm.helpers.SleepTimerService;
import com.mjhram.radioalarm.helpers.StationContextMenu;
import com.mjhram.radioalarm.helpers.StationFetcher;
import com.mjhram.radioalarm.helpers.StationListHelper;
import com.mjhram.radioalarm.helpers.StorageHelper;
import com.mjhram.radioalarm.helpers.TransistorKeys;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


/**
 * SettingsFragment class
 */
public final class SettingsFragment extends Fragment implements TransistorKeys {

    private TabLayout tabLayout;
    private AnimatedVectorDrawable animation;
    private Drawable deactivated;
    private int position;

    /* Define log tag */
    private static final String LOG_TAG = SettingsFragment.class.getSimpleName();


    /* Main class variables */
    private Activity mActivity;
    private StorageHelper mStorageHelper;
    private CollectionAdapter mCollectionAdapter = null;
    private View mRootView;
    private View mPlayerSheet;
    private ImageView mPlayerStationImage;
    private TextView mPlayerStationName;
    private TextView mPlayerStationMetadata;
    private ImageButton mPlayerPlaybackButton;
    private ImageButton mPlayerSheetSleepTimerButton;
    private ImageButton mPlayerSheetStationOptionsButton;
    private ImageButton mPlayerSheetMetadataCopyButton;
    private ImageButton mPlayerSheetStreamUrlCopyButton;
    private TextView mPlayerSheetMetadata;
    private TextView mPlayerSheetStreamUrl;
    private TextView mStationDataSheetMimeType;
    private TextView mStationDataSheetChannelCount;
    private TextView mStationDataSheetSampleRate;
    private ConstraintLayout mOnboardingLayout;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private CollectionViewModel mCollectionViewModel;
    private BottomSheetBehavior mPlayerBottomSheetBehavior;
    private BroadcastReceiver mSleepTimerStartedReceiver;
    private String mCurrentStationUrl;
    private Station mCurrentStation = null;
    private Station mPlayerServiceStation;
    private Uri mNewStationUri;
    private boolean mSleepTimerRunning;
    private String mSleepTimerNotificationMessage;
    private Snackbar mSleepTimerNotification;


    /* Constructor (default) */
    public SettingsFragment() {
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get activity and application contexts
        mActivity = getActivity();

        // get notification message
        mSleepTimerNotificationMessage = mActivity.getString(R.string.snackbar_message_timer_set) + " ";

        // load  state
        loadAppState(mActivity);

        // create collection adapter
        mCollectionAdapter = new CollectionAdapter(mActivity, mCurrentStationUrl);

        // initialize broadcast receivers
        initializeBroadcastReceivers();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // inflate root view from xml
        mRootView = inflater.inflate(R.layout.fragment_main, container, false);

        if (getActivity() != null) {
            tabLayout = (TabLayout) getActivity().findViewById(R.id.tabs);
        }

        deactivated = getResources().getDrawable(R.drawable.vd_pathmorph_arrowoverflow_overflow_deactivated);
        animation = (AnimatedVectorDrawable) getResources().getDrawable(R.drawable.avd_pathmorph_arrowoverflow_overflow_to_arrow);

        tabLayout.getTabAt(position).setIcon(deactivated);

        // get needed references to views
        mOnboardingLayout = mRootView.findViewById(R.id.onboarding_layout);
        mRecyclerView = mRootView.findViewById(R.id.list_recyclerview);
        mSwipeRefreshLayout = mRootView.findViewById(R.id.swipeRefreshLayout);
        mPlayerStationImage = mRootView.findViewById(R.id.player_station_image);
        mPlayerStationName = mRootView.findViewById(R.id.player_station_name);
        mPlayerStationMetadata = mRootView.findViewById(R.id.player_station_metadata);
        mPlayerPlaybackButton = mRootView.findViewById(R.id.player_playback_button);
        mPlayerSheet = mRootView.findViewById(R.id.player_sheet);
        mPlayerSheetSleepTimerButton = mRootView.findViewById(R.id.player_sheet_timer_button);
        mPlayerSheetStationOptionsButton = mRootView.findViewById(R.id.player_sheet_station_options_button);
        mPlayerSheetMetadataCopyButton = mRootView.findViewById(R.id.player_sheet_metadata_copy_button);
        mPlayerSheetStreamUrlCopyButton = mRootView.findViewById(R.id.player_sheet_stream_url_copy_button);
        mPlayerSheetMetadata = mRootView.findViewById(R.id.player_sheet_p_metadata);
        mPlayerSheetStreamUrl = mRootView.findViewById(R.id.player_sheet_p_stream_url);
        mStationDataSheetMimeType = mRootView.findViewById(R.id.player_sheet_p_mime);
        mStationDataSheetChannelCount = mRootView.findViewById(R.id.player_sheet_p_channels);
        mStationDataSheetSampleRate = mRootView.findViewById(R.id.player_sheet_p_samplerate);

        // set up RecyclerView
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mLayoutManager = new LinearLayoutManager(mActivity) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return true;
            }
        };
        mRecyclerView.setLayoutManager(mLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(), mLayoutManager.getOrientation());
        mRecyclerView.addItemDecoration(dividerItemDecoration);
        mRecyclerView.setAdapter(mCollectionAdapter);

        // set up and show station data sheet
        mPlayerBottomSheetBehavior = BottomSheetBehavior.from(mPlayerSheet);
        minimizePlayer();

        // attach listeners to buttons
        setAdapterListeners();

        // observe changes in LiveData
        mCollectionViewModel = ViewModelProviders.of((AppCompatActivity) mActivity).get(CollectionViewModel.class);
        mCollectionViewModel.getStationList().observe((LifecycleOwner) mActivity, createStationListObserver());
        mCollectionViewModel.getPlayerServiceStation().observe((LifecycleOwner) mActivity, createStationObserver());

        return mRootView;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (tabLayout == null || deactivated == null || animation == null) {
            return;
        }

        if (tabLayout.getTabAt(position) == null) {
            return;
        }

        if (isVisibleToUser) {
            // start animation
            tabLayout.getTabAt(position).setIcon(animation);
            animation.start();
        } else {
            // deactivated icon
            animation.stop();
            tabLayout.getTabAt(position).setIcon(deactivated);
        }
    }

    public void setPosition(int position) {
        this.position = position;
    }


    @Override
    public void onResume() {
        super.onResume();

        // switch volume control from ringtone to music
        mActivity.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // refresh app state
        loadAppState(mActivity);

        // handles the activity's intent
        Intent intent = mActivity.getIntent();
        if (ACTION_SHOW_PLAYER.equals(intent.getAction())) {
            handleShowPlayer(intent);
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            handleStreamingLink(intent);
        }

        // show notification bar if timer is running
        if (mSleepTimerRunning) {
            showSleepTimerNotification(-1);
        }

        /*Bundle extras = intent.getExtras();
        if(extras != null) {
            Boolean message = extras.getBoolean("alarm");
            if(message != null) {
                //it is alarm
                //tabLayout.getTabAt(1).select();
                ArrayList<Station> newStationList = StationListHelper.loadStationListFromStorage(mActivity);
                if(newStationList.size() > 0) {
                    startPlayback(newStationList.get(0));
                }
            }
        }*/
    }


    @Override
    public void onPause() {
        super.onPause();
        // save state
        saveAppState(mActivity);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        // unregister Broadcast Receivers
        unregisterBroadcastReceivers();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_REQUEST_STATION_FETCHER_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted - fetch station from given Uri
                    fetchNewStation(mNewStationUri);
                } else {
                    // permission denied
                    Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_permission_denied) + " READ_EXTERNAL_STORAGE", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }


    /* Updates the player UI after delete */
    public void updatePlayerAfterDelete(Station station) {
        mCurrentStation = station;
        setupPlayer(station);
    }


    /* Minimizes player sheet if necessary - used by onBackPressed */
    public boolean minimizePlayer() {
        if (mPlayerBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            return false;
        } else {
            mPlayerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            return true;
        }
    }


    /* Refreshes list of stations - used by pull to refresh */
    private void refreshList() {
        // manually refresh list of stations (force reload) - useful when editing playlist files manually outside of Transistor
        ArrayList<Station> newStationList = StationListHelper.loadStationListFromStorage(mActivity);
        mCollectionViewModel.getStationList().setValue(newStationList);

        // notify user
        Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_list_refreshed), Toast.LENGTH_LONG).show();
    }


    /* Handles tap on streaming link */
    private void handleStreamingLink(Intent intent) {
        mNewStationUri = intent.getData();

        // clear the intent
        intent.setAction("");

        // check for null and type "http"
        if (mNewStationUri != null && mNewStationUri.getScheme().startsWith("http")) {
            // download and add new station
            fetchNewStation(mNewStationUri);
        } else if (mNewStationUri != null && mNewStationUri.getScheme().startsWith("content")) {
            // check for read permission
            if (PermissionHelper.requestReadExternalStorage(mActivity, mRootView, PERMISSION_REQUEST_STATION_FETCHER_READ_EXTERNAL_STORAGE)) {
                // read and add new station
                fetchNewStation(mNewStationUri);
            }
        }
        // unsuccessful - log failure
        else {
            LogHelper.i(LOG_TAG, "Received an empty intent");
        }
    }


    /* start player service using intent */
    private void startPlayback(Station station) {
        Intent intent = new Intent(mActivity, PlayerService.class);
        intent.setAction(ACTION_PLAY);
        intent.putExtra(EXTRA_STATION, station);
        mActivity.startService(intent);
        mPlayerServiceStation = station;
        LogHelper.v(LOG_TAG, "Starting player service.");
    }


    /* Stop player service using intent */
    private void stopPlayback() {
        Intent intent = new Intent(mActivity, PlayerService.class);
        intent.setAction(ACTION_STOP);
        mActivity.startService(intent);
        LogHelper.v(LOG_TAG, "Stopping player service.");
    }


    /* Handles intent to show player from notification or from shortcut */
    private void handleShowPlayer(Intent intent) {
        Station station = null;
        boolean startPlayback = false;

        // CASE: user tapped on notification
        if (intent.hasExtra(EXTRA_STATION)) {
            // get station from notification
            station = intent.getParcelableExtra(EXTRA_STATION);
            mPlayerServiceStation = station;
            startPlayback = false;
        }
        // CASE: playback requested via homescreen shortcut
        else if (intent.hasExtra(EXTRA_STREAM_URI)) {
            // get Uri of station from home screen shortcut
            station = StationListHelper.findStation(mCollectionAdapter.getStationList(), Uri.parse(intent.getStringExtra(EXTRA_STREAM_URI)));
            startPlayback = true;
        }
        // CASE: transistor received a last station intent
        else if (intent.hasExtra(EXTRA_LAST_STATION)) {
            // try to get last station from SharedPreferences
            String stationUrlLastString = PreferenceManager.getDefaultSharedPreferences(mActivity).getString(PREF_STATION_URL_LAST, null);
            loadAppState(mActivity);
            if (stationUrlLastString != null) {
                station = StationListHelper.findStation(mCollectionAdapter.getStationList(), Uri.parse(stationUrlLastString));
            }
            startPlayback = true;
        }

        // clear the intent, show player and start playback if requested
        if (station != null) {
            intent.setAction("");
            setupPlayer(station);
            if (startPlayback) {
                startPlayback(station);
            }
        } else {
            Toast.makeText(mActivity, getString(R.string.toastalert_station_not_found), Toast.LENGTH_LONG).show();
        }


    }


    /* Attaches tap listeners to buttons */
    private void setAdapterListeners() {
        // tap on station list
        mCollectionAdapter.setCollectionAdapterListener(new CollectionAdapter.CollectionAdapterListener() {
            @Override
            public void itemSelected(Station station, boolean isLongPress) {
                if (isLongPress) {
                    togglePlayback(station, true);
                }
                setupPlayer(station);
                Animation animation = AnimationUtils.loadAnimation(mActivity, R.anim.wiggle);
                mPlayerStationImage.startAnimation(animation);
            }

            @Override
            public void jumpToPosition(int position) {
                mLayoutManager.scrollToPosition(position);
            }
        });

        // playback button
        mPlayerPlaybackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                togglePlayback(mCurrentStation, false);
            }
        });

        // expand player sheet
        mPlayerSheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPlayerBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    mPlayerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    mPlayerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });

        // sleep timer tap
        mPlayerSheetSleepTimerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleSleepTimerTap();
            }
        });

        // attach listener for option menu
        mPlayerSheetStationOptionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StationContextMenu.show(mActivity, view, mCurrentStation);
            }
        });

        // clipboard copy
        mPlayerSheetMetadataCopyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(COPY_STATION_METADATA);
            }
        });
        mPlayerSheetMetadata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyToClipboard(COPY_STATION_METADATA);
            }
        });
        mPlayerSheetStreamUrlCopyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(COPY_STREAM_URL);
            }
        });
        mPlayerSheetStreamUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyToClipboard(COPY_STREAM_URL);
            }
        });


        mPlayerStationImage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                longPressFeedback(R.string.toastmessage_long_press_change_icon);
                // get system picker for images
                ((MainActivity)mActivity).pickImage(mCurrentStation);
                return true;
            }
        });


        mPlayerStationName.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                longPressFeedback(R.string.toastmessage_long_press_change_name);
                // construct and run rename dialog
                DialogRename.show(mActivity, mCurrentStation);
                return true;
            }
        });


        // secret night mode switch - pre-Android P (28)
        if (Build.VERSION.SDK_INT < 28) {
            mPlayerSheetStationOptionsButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    longPressFeedback(R.string.toastmessage_long_press_night_mode_switch);
                    NightModeHelper.switchToOpposite(mActivity);
                    mActivity.recreate();
                    return true;
                }
            });
        }

        mPlayerSheetStationOptionsButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                longPressFeedback(R.string.toastmessage_long_press_night_mode_switch);
                NightModeHelper.switchToOpposite(mActivity);
                mActivity.recreate();
                return true;
            }
        });

        // swipe to refresh
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshList();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });

    }


    /* Handles playback button taps */
    private void togglePlayback(Station station, boolean isLongPress) {
        if (station.getPlaybackState() != PLAYBACK_STATE_STOPPED) {
            // stop player service using intent
            stopPlayback();
            // if long press -> inform user
            if (isLongPress) {
                longPressFeedback(R.string.toastmessage_long_press_playback_stopped);
            }
        } else {
            // start player service using intent
            startPlayback(station);
            mPlayerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            // if long press -> inform user
            if (isLongPress) {
                longPressFeedback(R.string.toastmessage_long_press_playback_started);
            }
        }
    }


    /* Setup player visually */
    private void setupPlayer(@Nullable Station station) {
        if (isAdded()) {
            // update current station
            mCurrentStation = station;

            if (station != null) {
                // show player & hide onboarding
                if (mPlayerBottomSheetBehavior.getPeekHeight() == 0) {
                    mPlayerBottomSheetBehavior.setPeekHeight(convertDpToPx(PLAYER_SHEET_PEEK_HEIGHT));
                    mOnboardingLayout.setVisibility(View.GONE);
                }

                // setup all views
                setupStationMainViews(station);
                setupStationPlaybackButtonState(station);
                setupStationMetadataViews(station);
                setupExtendedMetaDataViews(station);

            } else {
                // hide player & show onboarding
                mPlayerBottomSheetBehavior.setPeekHeight(0);
                minimizePlayer();
                mOnboardingLayout.setVisibility(View.VISIBLE);
            }
        }
    }


    /*  Setup station name, image and stream url */
    private void setupStationMainViews(Station station) {
        ImageHelper imageHelper = new ImageHelper(station, mActivity);
        Bitmap stationImage = imageHelper.createCircularFramedImage(192);
        mPlayerStationName.setText(station.getStationName());
        mPlayerStationName.setContentDescription(station.getStationName()); // for accessibility apps like TalkBack
        mPlayerStationImage.setImageBitmap(stationImage);
        mPlayerSheetStreamUrl.setText(station.getStreamUri().toString());
        mPlayerSheetStreamUrl.setContentDescription(station.getStreamUri().toString()); // for accessibility apps like TalkBack
    }


    /* Setup playback button state */
    private void setupStationPlaybackButtonState(Station station) {
        if (isAdded()) {
            // toggle views needed for active playback
            switch (station.getPlaybackState()) {
                case PLAYBACK_STATE_STOPPED: {
                    mPlayerPlaybackButton.setImageResource(R.drawable.ic_play_arrow_white_36dp);
                    break;
                }
                case PLAYBACK_STATE_LOADING_STATION: {
                    mPlayerPlaybackButton.setImageResource(R.drawable.ic_stop_white_36dp);
                    break;
                }
                case PLAYBACK_STATE_STARTED: {
                    mPlayerPlaybackButton.setImageResource(R.drawable.ic_stop_white_36dp);
                    break;
                }
            }
        }
    }


    /* Initiates the rotation animation of the playback button  */
    private void animatePlaybackButtonStateTransition(Station station) {
        if (isAdded()) {
            // toggle views needed for active playback
            switch (station.getPlaybackState()) {
                case PLAYBACK_STATE_STOPPED: {
                    Animation rotateCounterClockwise = AnimationUtils.loadAnimation(mActivity, R.anim.rotate_counterclockwise_fast);
                    rotateCounterClockwise.setAnimationListener(createAnimationListener(station));
                    mPlayerPlaybackButton.startAnimation(rotateCounterClockwise);
                    break;
                }
                case PLAYBACK_STATE_LOADING_STATION: {
                    Animation rotateClockwise = AnimationUtils.loadAnimation(mActivity, R.anim.rotate_clockwise_slow);
                    rotateClockwise.setAnimationListener(createAnimationListener(station));
                    mPlayerPlaybackButton.startAnimation(rotateClockwise);
                    break;
                }
                case PLAYBACK_STATE_STARTED: {
                    setupStationPlaybackButtonState(station);
                    break;
                }
            }
        }
    }


    /* Creates AnimationListener for playback button */
    private Animation.AnimationListener createAnimationListener(final Station station) {
        return new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                // set up button symbol and playback indicator afterwards
                setupStationPlaybackButtonState(station);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        };
    }


    /* Setup station now playing metadata views */
    private void setupStationMetadataViews(Station station) {
        String stationMetadata = station.getMetadata();
        if (isAdded() && stationMetadata != null) {
            if (stationMetadata.equals("")) {
                stationMetadata = mActivity.getString(R.string.player_sheet_p_no_data);
                mPlayerStationMetadata.setVisibility(View.GONE);
            } else {
                mPlayerStationMetadata.setVisibility(View.VISIBLE);
            }
            mPlayerStationMetadata.setText(stationMetadata);
            mPlayerStationMetadata.setContentDescription(stationMetadata); // for accessibility apps like TalkBack
            mPlayerStationMetadata.setSelected(true); // triggers the marquee
            mPlayerSheetMetadata.setText(stationMetadata);
            mPlayerSheetMetadata.setContentDescription(stationMetadata); // for accessibility apps like TalkBack
            mPlayerSheetMetadata.setSelected(true); // triggers the marquee
        }
    }


    /* Setup extended metadata information */
    private void setupExtendedMetaDataViews(Station station) {

        // fill and show mime type
        String mimeType = station.getMimeType();
        if (!mimeType.equals("")) {
            mStationDataSheetMimeType.setText(mimeType);
            mStationDataSheetMimeType.setContentDescription(mimeType); // for accessibility apps like TalkBack
        } else {
            mStationDataSheetMimeType.setText(R.string.player_sheet_p_no_data);
            mStationDataSheetMimeType.setContentDescription(getString(R.string.player_sheet_p_no_data)); // for accessibility apps like TalkBack
        }

        // fill and show channel count
        int channelCount = station.getChannelCount();
        if (channelCount > 0) {
            String channelCountString = String.valueOf(channelCount);
            mStationDataSheetChannelCount.setText(channelCountString);
            mStationDataSheetChannelCount.setContentDescription(channelCountString); // for accessibility apps like TalkBack
        } else {
            mStationDataSheetChannelCount.setText(R.string.player_sheet_p_no_data);
            mStationDataSheetChannelCount.setContentDescription(getString(R.string.player_sheet_p_no_data)); // for accessibility apps like TalkBack
        }

        // fill and show sample rate
        int sampleRate = station.getSampleRate();
        if (sampleRate > 0) {
            String sampleRateString = String.valueOf(sampleRate);
            mStationDataSheetSampleRate.setText(sampleRateString);
            mStationDataSheetSampleRate.setContentDescription(sampleRateString); // for accessibility apps like TalkBack
        } else {
            mStationDataSheetSampleRate.setText(R.string.player_sheet_p_no_data);
            mStationDataSheetSampleRate.setContentDescription(getString(R.string.player_sheet_p_no_data)); // for accessibility apps like TalkBack
        }

//        // fill and show bit rate
//        if (station.getBitrate() > 0) {
//            mStationDataSheetBitRate.setText(String.valueOf(station.getBitrate()));
//        } else {
//            mStationDataSheetBitRate.setText(R.string.player_sheet_p_no_data);
//        }

    }


    /* Handles tap timer icon in actionbar */
    private void handleSleepTimerTap() {
        // set duration
        long duration = FIFTEEN_MINUTES;

        // CASE: No station is playing, no timer is running
        if (!PlayerService.isPlaybackRunning() && !mSleepTimerRunning) {
            // unable to start timer
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_timer_start_unable), Toast.LENGTH_SHORT).show();
        }
        // CASE: A station is playing, no sleep timer is running
        else if (PlayerService.isPlaybackRunning() && !mSleepTimerRunning) {
            startSleepTimer(duration);
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_timer_activated), Toast.LENGTH_SHORT).show();
        }
        // CASE: A station is playing, Sleep timer is running
        else if (PlayerService.isPlaybackRunning() && mSleepTimerRunning) {
            startSleepTimer(duration);
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_timer_duration_increased) + " [+" + getReadableTime(duration) + "]", Toast.LENGTH_SHORT).show();
        }
    }


    /* Starts timer service and notification */
    private void startSleepTimer(long duration) {
        // start sleep timer service using intent
        Intent intent = new Intent(mActivity, SleepTimerService.class);
        intent.setAction(ACTION_TIMER_START);
        intent.putExtra(EXTRA_TIMER_DURATION, duration);
        mActivity.startService(intent);

        // show timer notification
        if (mSleepTimerNotification == null || !mSleepTimerNotification.isShown()) {
            showSleepTimerNotification(duration);
        }
        mSleepTimerRunning = true;
        LogHelper.v(LOG_TAG, "Starting timer service and notification.");
    }


    /* Stops timer service and notification */
    private void stopSleepTimer() {
        // stop sleep timer service using intent
        Intent intent = new Intent(mActivity, SleepTimerService.class);
        intent.setAction(ACTION_TIMER_STOP);
        mActivity.startService(intent);

        // cancel notification
        if (mSleepTimerNotification != null && mSleepTimerNotification.isShown()) {
            mSleepTimerNotification.dismiss();
        }
        mSleepTimerRunning = false;
        LogHelper.v(LOG_TAG, "Stopping timer service and notification.");
        Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_timer_cancelled), Toast.LENGTH_SHORT).show();
    }


    /* Shows notification for a running sleep timer */
    private void showSleepTimerNotification(long remainingTime) {

        // set snackbar message
        String message;
        if (remainingTime > 0) {
            message = mSleepTimerNotificationMessage + getReadableTime(remainingTime);
        } else {
            message = mSleepTimerNotificationMessage;
        }

        // show snackbar notification
        mSleepTimerNotification = Snackbar.make(mRootView, message, Snackbar.LENGTH_INDEFINITE);
        mSleepTimerNotification.setAction(R.string.dialog_generic_button_cancel, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // stop sleep timer service using intent
                Intent intent = new Intent(mActivity, SleepTimerService.class);
                intent.setAction(ACTION_TIMER_STOP);
                mActivity.startService(intent);
                mSleepTimerRunning = false;
                // notify user
                Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_timer_cancelled), Toast.LENGTH_SHORT).show();
                LogHelper.v(LOG_TAG, "Sleep timer cancelled.");
            }
        });
        mSleepTimerNotification.addCallback(new Snackbar.Callback() {
            @Override
            public void onShown(Snackbar sb) {
                super.onShown(sb);
                // make room for notification
                int notificationHeight = mSleepTimerNotification.getView().getHeight();
                mPlayerBottomSheetBehavior.setPeekHeight(convertDpToPx(PLAYER_SHEET_PEEK_HEIGHT) + notificationHeight);
                mStationDataSheetSampleRate.setPadding(0,0,0,notificationHeight);
            }
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                super.onDismissed(transientBottomBar, event);
                // reset peek height
                mPlayerBottomSheetBehavior.setPeekHeight(convertDpToPx(PLAYER_SHEET_PEEK_HEIGHT));
                mStationDataSheetSampleRate.setPadding(0,0,0,0);
            }
        });
        // stretch Snackbar
        (mSleepTimerNotification.getView()).getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        mSleepTimerNotification.show();
    }


    /* Translates milliseconds into minutes and seconds */
    private String getReadableTime(long remainingTime) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(remainingTime),
                TimeUnit.MILLISECONDS.toSeconds(remainingTime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(remainingTime)));
    }


    /* Loads app state from preferences */
    private void loadAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mCurrentStationUrl = settings.getString(PREF_STATION_URI_SELECTED, null);
        mSleepTimerRunning = settings.getBoolean(PREF_TIMER_RUNNING, false);
        LogHelper.v(LOG_TAG, "Loading state.");
    }


    /* Saves app state to SharedPreferences */
    private void saveAppState(Context context) {
        if (mCurrentStation != null) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(PREF_STATION_URI_SELECTED, mCurrentStation.getStreamUri().toString());
            editor.apply();
            LogHelper.v(LOG_TAG, "Saving state.");
        }
    }


    /* Fetch new station with given Uri */
    private void fetchNewStation(Uri stationUri) {
        // download and add new station
        StationFetcher stationFetcher = new StationFetcher(mActivity, StorageHelper.getCollectionDirectory(mActivity), stationUri, null);
        stationFetcher.execute();
    }


    /* Find station in list for given Uri */
    private Station findStationByUri(Uri streamUri, ArrayList<Station> stationList) {
        for (Station station :  stationList) {
            if (station.getStreamUri().equals(streamUri)) {
                // found matching station
                return station;
            }
        }
        // default return
        return null;
    }


    /* Copy station data to system clipboard */
    private void copyToClipboard(int contentType) {

        String notificationText = null;
        String clipboardText = null;
        ClipData clip;

        // set clip text and notification text
        switch (contentType) {
            case COPY_STATION_ALL:
                if (mCurrentStation.getMetadata() != null) {
                    clipboardText = mCurrentStation.getStationName() +  " - " +  mCurrentStation.getMetadata() + " (" +  mCurrentStation.getStreamUri().toString() + ")";
                } else {
                    clipboardText = mCurrentStation.getStationName() + " (" + mCurrentStation.getStreamUri().toString() + ")";
                }
                notificationText = mActivity.getString(R.string.toastmessage_station_copied);
                break;

            case COPY_STATION_METADATA:
                clipboardText = mPlayerSheetMetadata.getText().toString();
                notificationText = mActivity.getString(R.string.toastmessage_copied_to_clipboard_metadata);
                break;

            case COPY_STREAM_URL:
                clipboardText = mPlayerSheetStreamUrl.getText().toString();
                notificationText = mActivity.getString(R.string.toastmessage_copied_to_clipboard_url);
                break;
        }

        // create clip and copy to clipboard - and notify user
        if (clipboardText != null && notificationText != null) {
            clip = ClipData.newPlainText("simple text", clipboardText);
            ClipboardManager cm = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(clip);
            Toast.makeText(mActivity, notificationText, Toast.LENGTH_SHORT).show();
        }

    }


    /* Inform user and give haptic feedback (vibration) */
    private void longPressFeedback(int stringResource) {
        // inform user
        Toast.makeText(mActivity, stringResource, Toast.LENGTH_LONG).show();
        // vibrate 50 milliseconds
        Vibrator v = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(50);
//            v.vibrate(VibrationEffect.createOneShot(50, DEFAULT_AMPLITUDE)); // todo check if there is a support library vibrator
    }


    /* Creates an observer for collection of stations stored as LiveData */
    private Observer<ArrayList<Station>> createStationListObserver() {
        return new Observer<ArrayList<Station>>() {
            @Override
            public void onChanged(@Nullable ArrayList<Station> newStationList) {
                if (newStationList.size() == 0) {
                    // hide player
                    setupPlayer(null);
                } else if (mCurrentStation == null) {
                    // restore last station
                    if (mCurrentStationUrl != null) {
                        mCurrentStation = findStationByUri(Uri.parse(mCurrentStationUrl), newStationList);
                    } else {
                        mCurrentStation = newStationList.get(0);
                    }
                    // update current station from player service - if necessary
                    if (mPlayerServiceStation == null) {
                        mPlayerServiceStation = PlayerService.getCurrentStation();
                        if (mPlayerServiceStation != null && mPlayerServiceStation.getStreamUri().equals(mCurrentStation.getStreamUri())) {
                            mCurrentStation = mPlayerServiceStation;
                        }
                    }
                    // setup and show player
                    setupPlayer(mCurrentStation);
                }
            }
        };
    }


    /* Creates an observer for station from player service stored as LiveData */
    private Observer<Station> createStationObserver() {
        return new Observer<Station>() {
            @Override
            public void onChanged(@Nullable Station newStation) {
                // check if the station currently selected has been changed
                if (mCurrentStation != null && newStation!= null &&
                        mCurrentStation.getStreamUri().equals(newStation.getStreamUri())) {

                    String newName = newStation.getStationName();
                    long newImageSize = newStation.getStationImageSize();
                    String newMetaData = newStation.getMetadata();

                    String oldName = mCurrentStation.getStationName();
                    long oldImageSize = mCurrentStation.getStationImageSize();
                    String oldMetaData = mCurrentStation.getMetadata();

                    // CASE: NAME
                    if (!(newName.equals(oldName))) {
                        mPlayerStationName.setText(newStation.getStationName());
                        minimizePlayer();
                    }
                    // CASE: IMAGE
                    else if (newImageSize != oldImageSize) {
                        ImageHelper imageHelper = new ImageHelper(newStation, mActivity);
                        Bitmap stationImage = imageHelper.createCircularFramedImage(192);
                        mPlayerStationImage.setImageBitmap(stationImage);
                        minimizePlayer();
                    }
                    // CASE: METADATA
                    else if (!(newMetaData.equals(oldMetaData))) {
                        setupStationMetadataViews(newStation);
                    }
                    // CASE: PLAYBACK STATE
                    if (mCurrentStation.getPlaybackState() != newStation.getPlaybackState()) {
                        animatePlaybackButtonStateTransition(newStation);
                        setupStationMetadataViews(newStation);
                        setupExtendedMetaDataViews(newStation);
                    }
                    // update this station
                    mCurrentStation = newStation;
                }

                // check if the station currently used by player service has been changed
                if (mPlayerServiceStation != null && newStation!= null &&
                        mPlayerServiceStation.getStreamUri().equals(newStation.getStreamUri())) {
                    // stop sleep timer - if necessary
                    if (mSleepTimerRunning && newStation.getPlaybackState() == PLAYBACK_STATE_STOPPED) {
                        stopSleepTimer();
                    }
                    // update station currently used by player service
                    mPlayerServiceStation = newStation;
                }
            }
        };
    }


    /* Initializes broadcast receivers for onCreate */
    private void initializeBroadcastReceivers() {
        // RECEIVER: sleep timer service sends updates
        mSleepTimerStartedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // get duration from intent
                long remaining = intent.getLongExtra(EXTRA_TIMER_REMAINING, 0);
                if (mSleepTimerNotification != null && remaining > 0) {
                    // update existing notification
                    mSleepTimerNotification.setText(mSleepTimerNotificationMessage + getReadableTime(remaining));
                } else if (mSleepTimerNotification != null) {
                    // cancel notification
                    mSleepTimerNotification.dismiss();
                    mSleepTimerRunning = false;
                }
            }
        };
        IntentFilter sleepTimerIntentFilter = new IntentFilter(ACTION_TIMER_RUNNING);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(mSleepTimerStartedReceiver, sleepTimerIntentFilter);
    }


    /* Unregisters broadcast receivers */
    private void unregisterBroadcastReceivers() {
        mCollectionAdapter.unregisterBroadcastReceivers(mActivity);
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(mSleepTimerStartedReceiver);
    }


    /* Convert device pixel to real pixel */
    private int convertDpToPx(int dp) {
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mActivity.getResources().getDisplayMetrics());
        return px;
    }

}

