package com.widewine.drmkeyfetcher;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.OfflineLicenseHelper;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DashUtil;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.dash.manifest.DashManifest;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import java.util.HashMap;
import java.util.UUID;

public class DRMKeyFetcher {

    private DRMDefaultDrmSessionEventListener DRMDefaultDrmSessionEventListener;
    private Context context;
    private String licenseUrl;
    private String url;
    private OfflineLicenseHelper offlineLicenseHelper;
    private HashMap<String, String> optionalKeyRequestParameters = new HashMap<>();
    private UUID uuid;
    private SimpleExoPlayer siExoPlayer;

    public DRMKeyFetcher(Context context, UUID uuid, String licenseUrl, String url, DRMDefaultDrmSessionEventListener DRMDefaultDrmSessionEventListener){

        this.context  = context;
        this.uuid = uuid;
        this.licenseUrl = licenseUrl;
        this.url = url;
        this.DRMDefaultDrmSessionEventListener = DRMDefaultDrmSessionEventListener;

    }

    public HashMap<String, String> getOptionalKeyRequestParameters() {
        return optionalKeyRequestParameters;
    }

    public void setOptionalKeyRequestParameters(HashMap<String, String> optionalKeyRequestParameters) {
        this.optionalKeyRequestParameters = optionalKeyRequestParameters;
    }

    public void startFetchingKeys() {
        if (getMediaType(url) == C.TYPE_DASH) {
            byte[] offlineAssetKeyId = null;
            try {
                HttpDataSource.Factory licenseDataSourceFactory = new DefaultHttpDataSourceFactory(Util.getUserAgent(context, context.getPackageName()));
                DataSource dataSource = licenseDataSourceFactory.createDataSource();
                DashManifest dashManifest = DashUtil.loadManifest(dataSource,
                        Uri.parse(url));

                // to send content id and mode to drmsession manager
                HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(licenseUrl, licenseDataSourceFactory);

                FrameworkMediaDrm mediaDrm = FrameworkMediaDrm.newInstance(uuid);
                this.offlineLicenseHelper = new OfflineLicenseHelper(uuid, mediaDrm, drmCallback, optionalKeyRequestParameters);

                DrmInitData drmInitData = DashUtil.loadDrmInitData(dataSource, dashManifest.getPeriod(0));
                offlineAssetKeyId = offlineLicenseHelper.downloadLicense(drmInitData);
                if (offlineAssetKeyId != null) {
                    if (DRMDefaultDrmSessionEventListener !=null){
                        DRMDefaultDrmSessionEventListener.onDrmKeysLoaded(offlineAssetKeyId);
                    }
                }else {
                    if (DRMDefaultDrmSessionEventListener !=null){
                        DRMDefaultDrmSessionEventListener.onDrmKeysLoaded(offlineAssetKeyId);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                if (DRMDefaultDrmSessionEventListener !=null){
                    DRMDefaultDrmSessionEventListener.onDrmSessionManagerError(e);
                }
            }

            offlineLicenseHelper.release();
        }else if (getMediaType(url) == C.TYPE_HLS){
            setUpDummyExoPlayer();
        }else {
            if (DRMDefaultDrmSessionEventListener !=null){
                DRMDefaultDrmSessionEventListener.onDrmSessionManagerError(null);
            }
        }
    }


    private void setUpDummyExoPlayer(){

        try {
            DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            final TrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
            final TrackSelector trackSelector = new DefaultTrackSelector(trackSelectionFactory);;
            final DefaultLoadControl.Builder builder = new DefaultLoadControl.Builder();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        //Overriding DefaultDrmSessionManager to add the listener which will give keys on loaded
                        DRMDefaultDrmSessionManager defaultDrmSessionManager = getStreamDrmSessionManager();
                        if (defaultDrmSessionManager!=null) {
                            siExoPlayer = ExoPlayerFactory.newSimpleInstance(context, new DefaultRenderersFactory(context), trackSelector, builder.createDefaultLoadControl(), defaultDrmSessionManager);

                            // build MediaSource and feed to player
                            Uri uri = Uri.parse(url);
                            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, context.getPackageName()));
                            MediaSource mediaSource;
                            if (getMediaType(url) == C.TYPE_DASH) {
                                DashMediaSource.Factory factory = new DashMediaSource.Factory(new DefaultDashChunkSource.Factory(dataSourceFactory), dataSourceFactory);
                                mediaSource = factory.createMediaSource(uri);
                            } else {
                                mediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
                            }
                            siExoPlayer.setPlayWhenReady(false);
                            siExoPlayer.prepare(mediaSource);
                        }
                    }catch (Exception e){
                        if (DRMDefaultDrmSessionEventListener !=null){
                            DRMDefaultDrmSessionEventListener.onDrmSessionManagerError(e);
                        }
                        releasePlayer();
                    }
                }
            });
        }catch (Exception e){
            if (DRMDefaultDrmSessionEventListener !=null){
                DRMDefaultDrmSessionEventListener.onDrmSessionManagerError(e);
            }
            releasePlayer();
        }

    }

    private DRMDefaultDrmSessionManager<FrameworkMediaCrypto> getStreamDrmSessionManager() {

        try {
            HttpDataSource.Factory licenseDataSourceFactory = new DefaultHttpDataSourceFactory(Util.getUserAgent(context, context.getPackageName()));
            HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(licenseUrl, licenseDataSourceFactory);
            DRMDefaultDrmSessionManager<FrameworkMediaCrypto> ddsm;
            ddsm= DRMDefaultDrmSessionManager.newFrameworkInstance(uuid,  drmCallback, optionalKeyRequestParameters);
            ddsm.setDRMDefaultDrmSessionEventListener(DRMDefaultDrmSessionEventListener);
            ddsm.setMode(DRMDefaultDrmSessionManager.MODE_DOWNLOAD, null);
            return  ddsm;
        }catch (Exception e){
            if (DRMDefaultDrmSessionEventListener !=null){
                DRMDefaultDrmSessionEventListener.onDrmSessionManagerError(e);
            }
        }
        return null;
    }

    private void releasePlayer(){
        if (siExoPlayer!=null){
            siExoPlayer.release();
            siExoPlayer = null;
        }
    }

    public int getMediaType(String mSongUri){

        if (isNonEmptyString(mSongUri)){
            if (mSongUri.contains(".m3u8")){
                return C.TYPE_HLS;
            }else if (mSongUri.contains(".mpd")){
                return C.TYPE_DASH;
            }else {
                return C.TYPE_OTHER;
            }
        }
        return C.TYPE_OTHER;
    }

    public boolean isNonEmptyString(String str){
        return str != null && !str.equals("") && !str.trim().isEmpty();
    }

}
