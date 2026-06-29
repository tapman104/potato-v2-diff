package com.tapman104.mpvplayer.core.engine

import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode
import android.util.Log

class MpvEventDispatcher : MPVLib.EventObserver {
    private val TAG = "MpvEventDispatcher"
    private val listeners = ArrayList<MpvEventListener>()

    fun addListener(listener: MpvEventListener) {
        synchronized(listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }
    }

    fun removeListener(listener: MpvEventListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    override fun eventProperty(name: String) {
        synchronized(listeners) {
            for (listener in listeners) {
                listener.onPropertyChange(name, null)
            }
        }
    }

    override fun eventProperty(name: String, value: Long) {
        synchronized(listeners) {
            for (listener in listeners) {
                listener.onPropertyChange(name, value)
            }
        }
    }

    override fun eventProperty(name: String, value: Boolean) {
        synchronized(listeners) {
            for (listener in listeners) {
                listener.onPropertyChange(name, value)
            }
        }
    }

    override fun eventProperty(name: String, value: String) {
        synchronized(listeners) {
            for (listener in listeners) {
                listener.onPropertyChange(name, value)
            }
        }
    }

    override fun eventProperty(name: String, value: Double) {
        synchronized(listeners) {
            for (listener in listeners) {
                listener.onPropertyChange(name, value)
            }
        }
    }

    override fun eventProperty(name: String, value: MPVNode) {
        synchronized(listeners) {
            for (listener in listeners) {
                listener.onPropertyChange(name, value)
            }
        }
    }

    override fun event(eventId: Int, eventNode: MPVNode) {
        synchronized(listeners) {
            Log.d(TAG, "Received MPV event: $eventId")
            when (eventId) {
                MpvConstants.MPV_EVENT_FILE_LOADED -> {
                    for (listener in listeners) {
                        listener.onFileLoaded()
                    }
                }
                MpvConstants.MPV_EVENT_PLAYBACK_RESTART -> {
                    for (listener in listeners) {
                        listener.onPlaybackStarted()
                    }
                }
                MpvConstants.MPV_EVENT_END_FILE -> {
                    val reason = try {
                        eventNode.get("reason")?.asInt()?.toInt() ?: 0
                    } catch (e: Exception) {
                        0
                    }
                    for (listener in listeners) {
                        listener.onPlaybackStopped(reason)
                    }
                }
                else -> {
                    // Other events
                }
            }
        }
    }
}
