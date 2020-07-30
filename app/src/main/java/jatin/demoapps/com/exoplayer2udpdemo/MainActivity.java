package jatin.demoapps.com.exoplayer2udpdemo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory;
import com.google.android.exoplayer2.extractor.ts.TsExtractor;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
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

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;
import java.util.List;

import static com.google.android.exoplayer2.extractor.ts.TsExtractor.MODE_SINGLE_PMT;
import static com.google.android.exoplayer2.extractor.ts.TsExtractor.TS_STREAM_TYPE_H264;

public class MainActivity extends AppCompatActivity {
    PlayerView playerView;
    Button playVideoBtn;
    TextView videoURL;
    private SimpleExoPlayer player;
    private VLCVideoLayout playerVlcLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playerView = findViewById(R.id.player_view);
        playVideoBtn = findViewById(R.id.playVideo);
        playerVlcLayout = findViewById(R.id.player_viewTexture);
        Log.d("MainActivity", " view null: " + (playerVlcLayout==null));
        initVLC("rtp://@238.255.255.254:1234/");
       // videoURL = findViewById(R.id.url);
        //playVideoBtn.setOnClickListener(v -> );
        // playVideoBtn.setOnClickListener(v -> test(videoURL.getText().toString()));
    }

    private void initPlayer(String url) {
        if (url.isEmpty()) {
            url = "rtp://@238.255.255.254:1234/";
            Toast.makeText(this, "Please enter a url use default: " + url, Toast.LENGTH_SHORT).show();
        }
        Uri videoUri = Uri.parse(url);

        //Create a default TrackSelector
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory());
        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(getApplicationContext());
        /*If need the code can disable or enable codec*/
        renderersFactory.setMediaCodecSelector(new MediaCodecSelector() {
            @Nullable
            @Override
            public MediaCodecInfo getPassthroughDecoderInfo() throws MediaCodecUtil.DecoderQueryException {
                MediaCodecInfo selectedCodec = MediaCodecUtil.getPassthroughDecoderInfo();
                Log.d("MainActivity", "#getPassthroughDecoderInfo. Selects a decoder to instantiate for =AUDIO= passthrough.\n" +
                        " CodecInfo: " + logCodecInfo(selectedCodec));
                return selectedCodec;
            }

            @Override
            public List<MediaCodecInfo> getDecoderInfos(@NonNull String mimeType, boolean requiresSecureDecoder, boolean requiresTunnelingDecoder) throws MediaCodecUtil.DecoderQueryException {
                /*
                 * Android provides a list of codecs in priority order, and ExoPlayer picks the first usable decoder that supports playback of input media format.
                 * This is generally a hardware accelerated decoder if the device has one for the format. Please read MediaCodecUtil for details of how this works.
                 * Generally it's best to leave codec selection up to ExoPlayer. However, if you need to change the behavior you can implement a custom MediaCodecSelector.
                 * For example, you could put in logic that picks only decoders with names starting "OMX.google.", which should be software decoders.
                 * Different devices have different decoders so the logic would need to be robust to this. We also provide the vp9 extension, which provides a separate non-MediaCodec based
                 * renderer that wraps libvpx for VP9 decoding in software.
                 */
                List<MediaCodecInfo> priorityListCodecUse = MediaCodecUtil.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder);
                Log.d("MainActivity", "#getDecoderInfos. FirstCodecInPriorityList fo =VIDEO=." + "\n" +
                        " CodecInfo: " + (!priorityListCodecUse.isEmpty() ? logCodecInfo(priorityListCodecUse.get(0)) : null));
                return priorityListCodecUse;
            }
        });
        //Create the player
        player = ExoPlayerFactory.newSimpleInstance(this, renderersFactory, trackSelector);
        // set player in playerView
        playerView.setPlayer(player);
        playerView.requestFocus();
        player.addAnalyticsListener(new EventLogger(null));


        //Create default UDP Datasource
        DataSource.Factory factory = () -> new UdpDataSource(30000, 100000);
        ExtractorsFactory tsExtractorFactory = () -> new TsExtractor[]{new TsExtractor(TS_STREAM_TYPE_H264,
                new TimestampAdjuster(0), new DefaultTsPayloadReaderFactory())};
        MediaSource mediaSource = new ExtractorMediaSource(videoUri, factory, tsExtractorFactory, null, null);
        player.prepare(mediaSource);

        // start play automatically when player is ready.
        player.setPlayWhenReady(true);
    }

    private void test(String url) {
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
                return new UdpDataSource(3000, 100000);
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

    public static String logCodecInfo(@Nullable MediaCodecInfo selectedCodec) {
        if (selectedCodec!=null) {
            return "CodecName: " + selectedCodec.name + "\n" +
                    "CodecMineType: " + selectedCodec.mimeType + "\n" +
                    "CodecHasHardwareAccelerated: " + selectedCodec.hardwareAccelerated + "\n" +
                    "CodecSoftwareOnly: " + selectedCodec.softwareOnly + "\n" +
                    "CodecVendorDeveloper: " + selectedCodec.vendor + "\n";
        }
        return null;
    }


    /**
     * https://wiki.videolan.org/VLC_command-line_help
     * https://code.videolan.org/videolan/vlc-android/-/issues/1359
     * https://github.com/oaubert/python-vlc/issues/61
     */
    private void initVLC(String url){
        ArrayList<String> options = new ArrayList<>();
        options.add("--file-caching=5000");
        options.add("-vvv");

        LibVLC mLibVLC = new LibVLC(getApplicationContext(), options);

        MediaPlayer mMediaPlayer =  new MediaPlayer(mLibVLC);
        mMediaPlayer.attachViews(playerVlcLayout,null, false,false);

        Media media = new Media(mLibVLC, Uri.parse(url));
        media.setHWDecoderEnabled(true, false);
        media.addOption(":network-caching=1000");
        media.addOption(":clock-jitter=0");
        media.addOption(":clock-synchro=0");
        media.addOption(":live-caching=300");

        mMediaPlayer.setMedia(media);
        mMediaPlayer.play();
    }
}
