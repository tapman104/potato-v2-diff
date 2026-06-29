package com.tapman104.mpvplayer.core.engine

object MpvConstants {
    // Properties
    const val PROP_PAUSE = "pause"
    const val PROP_TIME_POS = "time-pos"
    const val PROP_DURATION = "duration"
    const val PROP_DEMUXER_CACHE_TIME = "demuxer-cache-time"
    const val PROP_TRACK_LIST = "track-list"
    const val PROP_AUDIO_ID = "aid"
    const val PROP_SUBTITLE_ID = "sid"
    const val PROP_SPEED = "speed"
    const val PROP_HWDEC = "hwdec"
    const val PROP_VOLUME = "volume"
    const val PROP_MUTE = "mute"

    // Default values
    const val DEFAULT_SPEED = 1.0f
    const val DEFAULT_VOLUME = 100
    
    // MPV Format constants (mapped to is.xyz.mpv.MPVLib)
    const val MPV_FORMAT_NONE = 0
    const val MPV_FORMAT_STRING = 1
    const val MPV_FORMAT_OSD_STRING = 2
    const val MPV_FORMAT_FLAG = 3
    const val MPV_FORMAT_INT64 = 4
    const val MPV_FORMAT_DOUBLE = 5
    const val MPV_FORMAT_NODE = 6
    const val MPV_FORMAT_NODE_ARRAY = 7
    const val MPV_FORMAT_NODE_MAP = 8

    // Event constants
    const val MPV_EVENT_NONE = 0
    const val MPV_EVENT_SHUTDOWN = 1
    const val MPV_EVENT_LOG_MESSAGE = 2
    const val MPV_EVENT_GET_PROPERTY_REPLY = 3
    const val MPV_EVENT_SET_PROPERTY_REPLY = 4
    const val MPV_EVENT_COMMAND_REPLY = 5
    const val MPV_EVENT_START_FILE = 6
    const val MPV_EVENT_END_FILE = 7
    const val MPV_EVENT_FILE_LOADED = 8
    const val MPV_EVENT_IDLE = 9
    const val MPV_EVENT_TICK = 10
    const val MPV_EVENT_CLIENT_MESSAGE = 11
    const val MPV_EVENT_VIDEO_RECONFIG = 12
    const val MPV_EVENT_AUDIO_RECONFIG = 13
    const val MPV_EVENT_SEEK = 14
    const val MPV_EVENT_PLAYBACK_RESTART = 15
    const val MPV_EVENT_PROPERTY_CHANGE = 16
    const val MPV_EVENT_QUEUE_OVERFLOW = 17
    const val MPV_EVENT_HOOK = 18
}
