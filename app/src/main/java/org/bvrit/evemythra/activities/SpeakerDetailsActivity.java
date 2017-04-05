package org.bvrit.evemythra.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.design.widget.AppBarLayout;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import org.bvrit.evemythra.R;
import org.bvrit.evemythra.adapters.SessionsListAdapter;
import org.bvrit.evemythra.api.Urls;
import org.bvrit.evemythra.data.Session;
import org.bvrit.evemythra.data.Speaker;
import org.bvrit.evemythra.databinding.ActivitySpeakersBinding;
import org.bvrit.evemythra.dbutils.DbSingleton;
import org.bvrit.evemythra.utils.SpeakerIntent;

import java.util.List;


/**
 * Created by MananWason on 30-06-2015.
 */
public class SpeakerDetailsActivity extends AppCompatActivity implements AppBarLayout.OnOffsetChangedListener {

    private SessionsListAdapter sessionsListAdapter;

    private GridLayoutManager gridLayoutManager;

    private Speaker selectedSpeaker;

    private List<Session> mSessions;

    private String speaker;

    private CustomTabsClient customTabsClient;

    private CustomTabsServiceConnection customTabsServiceConnection;

    private boolean isHideToolbarView = false;

    private static final int spearkerWiseSessionList = 2;


    ActivitySpeakersBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_speakers);
        final DbSingleton dbSingleton = DbSingleton.getInstance();
        speaker = getIntent().getStringExtra(Speaker.SPEAKER);
        setSupportActionBar(binding.toolbarSpeakers);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.collapsingToolbar.setTitle(" ");
        selectedSpeaker = dbSingleton.getSpeakerbySpeakersname(speaker);

        binding.appbar.addOnOffsetChangedListener(this);

        loadSpeakerImage();

        binding.speakerDetailsTitle.setText(selectedSpeaker.getName());
        binding.speakerDetailsDesignation.setText(String.format("%s %s", selectedSpeaker.getPosition(), selectedSpeaker.getOrganisation()));

        boolean customTabsSupported;
        Intent customTabIntent = new Intent("android.support.customtabs.action.CustomTabsService");
        customTabIntent.setPackage("com.android.chrome");
        customTabsServiceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                customTabsClient = client;
                customTabsClient.warmup(0L);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                //do nothing
            }
        };
        customTabsSupported = bindService(customTabIntent, customTabsServiceConnection, Context.BIND_AUTO_CREATE);

        final SpeakerIntent speakerIntent;
        if (customTabsClient != null) {
            speakerIntent = new SpeakerIntent(selectedSpeaker, getApplicationContext(), this,
                    customTabsClient.newSession(new CustomTabsCallback()), customTabsSupported);
        } else {
            speakerIntent = new SpeakerIntent(selectedSpeaker, getApplicationContext(), this, customTabsSupported);
        }

        if (!TextUtils.isEmpty(selectedSpeaker.getLinkedin())) {
            speakerIntent.clickedImage(binding.imageViewLinkedin);
        } else {
            binding.imageViewLinkedin.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(selectedSpeaker.getTwitter())) {
            speakerIntent.clickedImage(binding.imageViewTwitter);
        } else {
            binding.imageViewTwitter.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(selectedSpeaker.getGithub())) {
            speakerIntent.clickedImage(binding.imageViewGithub);
        } else {
            binding.imageViewGithub.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(selectedSpeaker.getFacebook())) {
            speakerIntent.clickedImage(binding.imageViewFb);
        } else {
            binding.imageViewFb.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(selectedSpeaker.getWebsite())) {
            speakerIntent.clickedImage(binding.imageViewWeb);
        } else {
            binding.imageViewWeb.setVisibility(View.GONE);
        }

        binding.speakerBio.setText(Html.fromHtml(selectedSpeaker.getShortBiography()));
        binding.speakerBio.setMovementMethod(LinkMovementMethod.getInstance());

        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        float width = displayMetrics.widthPixels / displayMetrics.density;
        int spanCount = (int) (width / 250.00);

        binding.recyclerViewSpeakers.setHasFixedSize(true);
        gridLayoutManager = new GridLayoutManager(this, spanCount);
        binding.recyclerViewSpeakers.setLayoutManager(gridLayoutManager);

        mSessions = dbSingleton.getSessionbySpeakersName(speaker);
        sessionsListAdapter = new SessionsListAdapter(this, mSessions,spearkerWiseSessionList);
        binding.recyclerViewSpeakers.setNestedScrollingEnabled(false);
        binding.recyclerViewSpeakers.setAdapter(sessionsListAdapter);
        binding.recyclerViewSpeakers.setItemAnimator(new DefaultItemAnimator());
        if (!mSessions.isEmpty()) {
            binding.txtNoSessions.setVisibility(View.GONE);
            binding.recyclerViewSpeakers.setVisibility(View.VISIBLE);
        } else {
            binding.txtNoSessions.setVisibility(View.VISIBLE);
            binding.recyclerViewSpeakers.setVisibility(View.GONE);
        }
    }


    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }

    private static int getDarkColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
    }

    private void loadSpeakerImage() {
        if (TextUtils.isEmpty(selectedSpeaker.getPhoto()) || !isNetworkConnected()) {
            binding.progressBar.setVisibility(View.GONE);
            return;
        }

        final Context context = this;

        final Palette.PaletteAsyncListener paletteAsyncListener = new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                Palette.Swatch swatch = palette.getDarkVibrantSwatch();

                int backgroundColor = ContextCompat.getColor(context, R.color.color_primary);

                if(swatch != null) {
                    backgroundColor = swatch.getRgb();
                }

                binding.collapsingToolbar.setBackgroundColor(backgroundColor);
                binding.collapsingToolbar.setStatusBarScrimColor(getDarkColor(backgroundColor));
                binding.collapsingToolbar.setContentScrimColor(backgroundColor);

                sessionsListAdapter.setColor(backgroundColor);
            }
        };

        Target imageTarget = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                binding.progressBar.setVisibility(View.GONE);

                binding.speakerImage.setImageBitmap(bitmap);

                Palette.from(bitmap).generate(paletteAsyncListener);
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                binding.progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                // No action to be done on preparation of loading
            }
        };

        Picasso.with(SpeakerDetailsActivity.this)
                .load(Uri.parse(selectedSpeaker.getPhoto()))
                .into(imageTarget);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share_speakers_url:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.subject));
                StringBuilder message = new StringBuilder();
                message.append(String.format("%s %s %s %s\n\n",
                        selectedSpeaker.getName(),
                        getResources().getString(R.string.message_1),
                        getResources().getString(R.string.app_name),
                        getResources().getString(R.string.message_2)));
                for (Session m : mSessions) {
                    message.append(m.getTitle())
                            .append(",");
                }
                message.append(String.format("\n\n%s (%s)\n%s",
                        getResources().getString(R.string.message_3),
                        Urls.APP_LINK,
                        selectedSpeaker.getPhoto()));
                sendIntent.putExtra(Intent.EXTRA_TEXT, message.toString());
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, selectedSpeaker.getEmail()));
                return true;
            default:
                //do nothing
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_speakers_activity, menu);

        Target imageTarget = new Target() {
            @Override
            public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        int shareColor;

                        shareColor = Color.WHITE;

                        Drawable shareDrawable = VectorDrawableCompat.create(getApplicationContext().getResources(), R.drawable.ic_share_white_24dp, null);
                        if(shareDrawable != null) shareDrawable.mutate().setColorFilter(shareColor, PorterDuff.Mode.MULTIPLY);

                        menu.getItem(0).setIcon(shareDrawable);

                        Drawable backDrawable = VectorDrawableCompat.create(getApplicationContext().getResources(), R.drawable.ic_arrow_back_white_24dp, null);
                        if(backDrawable != null) backDrawable.mutate().setColorFilter(shareColor, PorterDuff.Mode.MULTIPLY);

                    }
                });
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                Drawable shareDrawable = VectorDrawableCompat.create(getApplicationContext().getResources(), R.drawable.ic_share_white_24dp, null);
                if(shareDrawable != null) shareDrawable.clearColorFilter();

                Drawable backDrawable = VectorDrawableCompat.create(getApplicationContext().getResources(), R.drawable.ic_arrow_back_white_24dp, null);
                if(backDrawable != null) backDrawable.clearColorFilter();
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                //This method is intentionally empty, because it is required to use Target, which is abstract
            }
        };

        Picasso.with(SpeakerDetailsActivity.this)
                .load(Uri.parse(selectedSpeaker.getPhoto()))
                .into(imageTarget);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();

        Drawable shareDrawable = VectorDrawableCompat.create(getApplicationContext().getResources(), R.drawable.ic_share_white_24dp, null);
        if(shareDrawable != null) shareDrawable.clearColorFilter();

        Drawable backDrawable = VectorDrawableCompat.create(getApplicationContext().getResources(), R.drawable.ic_arrow_back_white_24dp, null);
        if(backDrawable != null) backDrawable.clearColorFilter();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(customTabsServiceConnection);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        float width = displayMetrics.widthPixels / displayMetrics.density;
        int spanCount = (int) (width / 250.00);
        gridLayoutManager.setSpanCount(spanCount);
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {

        int maxScroll = appBarLayout.getTotalScrollRange();
        float percentage = (float) Math.abs(offset) / (float) maxScroll;

        if (percentage == 1f && isHideToolbarView) {
            //Collapsed
            if (selectedSpeaker.getOrganisation().isEmpty()) {
                binding.speakerDetailsHeader.setVisibility(View.GONE);
                binding.collapsingToolbar.setTitle(selectedSpeaker.getName());
                isHideToolbarView = !isHideToolbarView;
            } else {
                binding.speakerDetailsHeader.setVisibility(View.VISIBLE);
                binding.collapsingToolbar.setTitle(" ");
                binding.speakerDetailsDesignation.setMaxLines(1);
                binding.speakerDetailsDesignation.setEllipsize(TextUtils.TruncateAt.END);
                isHideToolbarView = !isHideToolbarView;
            }
        } else if (percentage < 1f && !isHideToolbarView) {
            //Not Collapsed
            binding.speakerDetailsHeader.setVisibility(View.VISIBLE);
            binding.collapsingToolbar.setTitle(" ");
            binding.speakerDetailsDesignation.setMaxLines(3);
            isHideToolbarView = !isHideToolbarView;
        }
    }

}
