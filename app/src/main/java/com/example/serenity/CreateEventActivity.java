package com.example.serenity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.example.serenity.data.CalenderEventModel;
import com.example.serenity.utils.ColorUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

//@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CreateEventActivity extends AppCompatActivity {

    public static final int ACTION_DELETE = 1;
    public static final int ACTION_EDIT = 2;
    public static final int ACTION_CREATE = 3;

    private static final String INTENT_EXTRA_ACTION = "intent_extra_action";
    private static final String INTENT_EXTRA_EVENT = "intent_extra_event";
    private static final String INTENT_EXTRA_CALENDAR = "intent_extra_calendar";

    private static final int SET_DATE_AND_TIME_REQUEST_CODE = 200;

    private final static SimpleDateFormat dateFormat
            = new SimpleDateFormat("EEEE, dd/MM    HH:mm", Locale.getDefault());

    private CalenderEventModel mOriginalEvent;

    private Calendar mCalendar;
    private String mTitle;
    private boolean mIsComplete;
    private int mColor;

    private boolean isViewMode = true;

    private EditText mTitleView;
    private Switch mIsCompleteCheckBox;
    private TextView mDateTextView;
    private CardView mColorCardView;
    private View mHeader;


    public static Intent makeIntent(Context context, @NonNull Calendar calendar) {
        return new Intent(context, CreateEventActivity.class).putExtra(INTENT_EXTRA_CALENDAR, calendar);
    }

    public static Intent makeIntent(Context context, @NonNull CalenderEventModel event) {
        return new Intent(context, CreateEventActivity.class).putExtra(INTENT_EXTRA_EVENT, event);
    }

    public static CalenderEventModel extractEventFromIntent(Intent intent) {
        return intent.getParcelableExtra(INTENT_EXTRA_EVENT);
    }

    public static int extractActionFromIntent(Intent intent) {
        return intent.getIntExtra(INTENT_EXTRA_ACTION, 0);
    }

    public static Calendar extractCalendarFromIntent(Intent intent) {
        return (Calendar) intent.getSerializableExtra(INTENT_EXTRA_CALENDAR);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);

        extractDataFromIntentAndInitialize();

        initializeUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_event, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete: {
                delete(extractEventFromIntent(getIntent()));
                return true;
            }
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void extractDataFromIntentAndInitialize() {

        mOriginalEvent = extractEventFromIntent(getIntent());

        if (mOriginalEvent == null) {
            mCalendar = extractCalendarFromIntent(getIntent());
            if (mCalendar == null)
                mCalendar = Calendar.getInstance();
            mCalendar.set(Calendar.HOUR_OF_DAY, 8);
            mCalendar.set(Calendar.MINUTE, 0);
            mCalendar.set(Calendar.SECOND, 0);
            mCalendar.set(Calendar.MILLISECOND, 0);
            mColor = ColorUtils.mColors[0];
            mTitle = "";
            mIsComplete = false;
            isViewMode = false;
        }
        else {
            mCalendar = mOriginalEvent.getDate();
            mColor = mOriginalEvent.getColor();
            mTitle = mOriginalEvent.getTitle();
            mIsComplete = mOriginalEvent.isCompleted();
            isViewMode = true;
        }
    }

    private void initializeUI() {
        setContentView(R.layout.activity_create_event);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        mHeader = findViewById(R.id.ll_header);
        mHeader.setVisibility(View.VISIBLE);

        setupToolbar();

        View tvSave = mHeader.findViewById(R.id.tv_save);
        tvSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });

        View tvCancel = mHeader.findViewById(R.id.tv_cancel);
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onBackPressed();

                if (mOriginalEvent == null)
                    overridePendingTransition(R.anim.stay, R.anim.slide_out_down);
            }
        });

        mDateTextView = findViewById(R.id.tv_date);
        mDateTextView.setText(dateFormat.format(mCalendar.getTime()));
        mDateTextView.setOnClickListener(new View.OnClickListener() {

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View v) {
                Activity context = CreateEventActivity.this;
                Intent intent = SelectDateAndTimeActivity.makeIntent(context, mCalendar);

                startActivityForResult(intent,
                        SET_DATE_AND_TIME_REQUEST_CODE,
                        ActivityOptions.makeSceneTransitionAnimation(context).toBundle());
            }
        });

        mColorCardView = findViewById(R.id.cardView_event_color);
        mColorCardView.setCardBackgroundColor(mColor);
        mColorCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SelectColorDialog.Builder.instance(CreateEventActivity.this)
                        .setSelectedColor(mColor)
                        .setOnColorSelectedListener(new SelectColorDialog.OnColorSelectedListener() {
                            @Override
                            public void onColorSelected(int color) {
                                mColor = color;
                                mColorCardView.setCardBackgroundColor(mColor);
                            }
                        })
                        .create()
                        .show();
            }
        });
        mTitleView = findViewById(R.id.et_event_title);
        mTitleView.setText(mTitle);
        mIsCompleteCheckBox = findViewById(R.id.checkbox_completed);
        mIsCompleteCheckBox.setChecked(mIsComplete);

        if (isViewMode) {
            mIsCompleteCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    setupEditMode();
                    mIsCompleteCheckBox.setOnCheckedChangeListener(null);
                }
            });
            mTitleView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    setupEditMode();
                    mTitleView.setOnFocusChangeListener(null);
                }
            });
        }
        else {
            setupEditMode();
        }
    }

    private void setupEditMode() {
        if (isViewMode) {
            isViewMode = false;
            setupToolbar();
        }
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            if (isViewMode)
                getSupportActionBar().show();
            else
                getSupportActionBar().hide();
        }

        if (mHeader != null) {
            mHeader.setVisibility(isViewMode? View.GONE : View.VISIBLE);
        }
    }

    private void delete(CalenderEventModel event) {
        Log.e(getClass().getSimpleName(), "delete");
        Log.d("calendar event deletion", "delete: " + getClass().getSimpleName());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        final String uid = user.getUid();
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference ref = database.getReference("Calendar Events").child(uid);
        ref.child(event.getID()).removeValue();

        setResult(RESULT_OK, new Intent()
                .putExtra(INTENT_EXTRA_ACTION, ACTION_DELETE)
                .putExtra(INTENT_EXTRA_EVENT, mOriginalEvent));
        finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_out_down);

    }

    private void save() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        final String uid = user.getUid();
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference ref = database.getReference("Calendar Events").child(uid);
        String key = ref.push().getKey();

        int action = mOriginalEvent != null ? ACTION_EDIT : ACTION_CREATE;
        String id = mOriginalEvent != null ? mOriginalEvent.getID() : key;
        String rawTitle = mTitleView.getText().toString().trim();

        if(mOriginalEvent == null) {
            mOriginalEvent = new CalenderEventModel(
                    id,
                    rawTitle.isEmpty() ? null : rawTitle,
                    mCalendar,
                    mColor,
                    mIsCompleteCheckBox.isChecked()
            );
            Map<String, Object> childUpdate = new HashMap<>();
            childUpdate.put(key, mOriginalEvent.toFirebaseObject());
            ref.updateChildren(childUpdate, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                    if (databaseError == null) {
                        return;
                    }
                }
            });
        } else {
            mOriginalEvent.setmTitle(rawTitle.isEmpty() ? null : rawTitle);
            mOriginalEvent.setmDate(mCalendar);
            mOriginalEvent.setmColor(mColor);
            mOriginalEvent.setCompleted(mIsCompleteCheckBox.isChecked());

            ref.child(mOriginalEvent.getID()).setValue(mOriginalEvent.toFirebaseObject());
        }

        setResult(RESULT_OK, new Intent()
                .putExtra(INTENT_EXTRA_ACTION, action)
                .putExtra(INTENT_EXTRA_EVENT, mOriginalEvent));
        finish();

        if (action == ACTION_CREATE)
            overridePendingTransition(R.anim.stay, R.anim.slide_out_down);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SET_DATE_AND_TIME_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                mCalendar = SelectDateAndTimeActivity.extractCalendarFromIntent(data);
                mDateTextView.setText(dateFormat.format(mCalendar.getTime()));

                setupEditMode();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

   /* private static String generateID() {
        return Long.toString(System.currentTimeMillis());
    }*/
}
