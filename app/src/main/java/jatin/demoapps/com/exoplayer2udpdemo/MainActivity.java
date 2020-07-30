package jatin.demoapps.com.exoplayer2udpdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory;
import com.google.android.exoplayer2.extractor.ts.TsExtractor;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.UdpDataSource;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.TimestampAdjuster;
import com.google.android.exoplayer2.util.Util;

import static com.google.android.exoplayer2.extractor.ts.TsExtractor.MODE_SINGLE_PMT;

public class MainActivity extends AppCompatActivity {
    PlayerView playerView;
    Button playVideoBtn;
    TextView videoURL;
    private SimpleExoPlayer player;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playerView = findViewById(R.id.player_view);
        playVideoBtn = findViewById(R.id.playVideo);
        videoURL = findViewById(R.id.url);
       // playVideoBtn.setOnClickListener(v -> initPlayer(videoURL.getText().toString()));
        playVideoBtn.setOnClickListener(v -> test(videoURL.getText().toString()));
    }

    private void initPlayer(String url){
        if (url.isEmpty())
            Toast.makeText(this,"Please enter a url",Toast.LENGTH_SHORT).show();
        Uri videoUri = Uri.parse(url);

        //Create a default TrackSelector
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory());

        //Create the player
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);

        // set player in playerView
        playerView.setPlayer(player);
        playerView.requestFocus();
        player.addAnalyticsListener(new EventLogger(null));


        //Create default UDP Datasource
        DataSource.Factory factory = () -> new UdpDataSource(30000, 100000);
        ExtractorsFactory tsExtractorFactory = () -> new TsExtractor[]{new TsExtractor(MODE_SINGLE_PMT,
                new TimestampAdjuster(0), new DefaultTsPayloadReaderFactory())};
        MediaSource mediaSource = new ExtractorMediaSource(videoUri, factory, tsExtractorFactory,null,null);
        player.prepare(mediaSource);

        // start play automatically when player is ready.
        player.setPlayWhenReady(true);
    }

    private void test(String url){
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);

        LoadControl loadControl = new DefaultLoadControl(
                new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE));


        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);


        Uri uri =
                Uri.parse
                        (url);

        final DefaultBandwidthMeter bandwidthMeterA = new DefaultBandwidthMeter();

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "teveolauncher"), bandwidthMeterA);

        DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

        DataSource.Factory udsf = new UdpDataSource.Factory() {
            @Override
            public DataSource createDataSource() {
                return new UdpDataSource( 3000, 100000);
            }
        };
        ExtractorsFactory tsExtractorFactory = new ExtractorsFactory() {
            @Override
            public Extractor[] createExtractors() {
                return new TsExtractor[]{new TsExtractor(MODE_SINGLE_PMT,
                        new TimestampAdjuster(0), new DefaultTsPayloadReaderFactory())};
            }
        };



        MediaSource videoSource = new ExtractorMediaSource
                (uri, udsf, tsExtractorFactory, null, null);

        playerView.setPlayer(player);
        player.prepare(videoSource);
        player.setPlayWhenReady(true);
    }
}
