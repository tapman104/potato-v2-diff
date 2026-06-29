package com.tapman104.mpvplayer.core.engine

import `is`.xyz.mpv.MPVLib

object MpvPropertyObserver {
    fun registerObservers() {
        // Observe status properties
        MPVLib.observeProperty(MpvConstants.PROP_PAUSE, MpvConstants.MPV_FORMAT_FLAG)
        MPVLib.observeProperty(MpvConstants.PROP_TIME_POS, MpvConstants.MPV_FORMAT_DOUBLE)
        MPVLib.observeProperty(MpvConstants.PROP_DURATION, MpvConstants.MPV_FORMAT_DOUBLE)
        MPVLib.observeProperty(MpvConstants.PROP_DEMUXER_CACHE_TIME, MpvConstants.MPV_FORMAT_DOUBLE)
        
        // Observe track structures
        MPVLib.observeProperty(MpvConstants.PROP_TRACK_LIST, MpvConstants.MPV_FORMAT_NODE)
        MPVLib.observeProperty(MpvConstants.PROP_AUDIO_ID, MpvConstants.MPV_FORMAT_INT64)
        MPVLib.observeProperty(MpvConstants.PROP_SUBTITLE_ID, MpvConstants.MPV_FORMAT_INT64)
        
        // Observe playback parameters
        MPVLib.observeProperty(MpvConstants.PROP_SPEED, MpvConstants.MPV_FORMAT_DOUBLE)
        MPVLib.observeProperty(MpvConstants.PROP_HWDEC, MpvConstants.MPV_FORMAT_STRING)
        MPVLib.observeProperty(MpvConstants.PROP_VOLUME, MpvConstants.MPV_FORMAT_DOUBLE)
    }
}
