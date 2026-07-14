package com.linky.voiceclone.util

import android.media.MediaPlayer

/** Owns the active MediaPlayer so list items never leak or retain released players. */
object PlayerManager {
    private data class ActivePlayer(
        val source: String,
        val player: MediaPlayer,
        val onStopped: () -> Unit,
    )

    private var active: ActivePlayer? = null

    fun play(
        source: String,
        onStarted: () -> Unit,
        onStopped: () -> Unit,
        onError: (String) -> Unit = {},
    ) {
        stopCurrent(notify = true)

        val player = MediaPlayer()
        val entry = ActivePlayer(source, player, onStopped)
        active = entry

        player.setOnPreparedListener {
            if (active?.player !== player) {
                runCatching { player.release() }
                return@setOnPreparedListener
            }
            runCatching {
                player.start()
                onStarted()
            }.onFailure {
                finish(entry, notify = false)
                onError("音频播放失败")
            }
        }
        player.setOnCompletionListener {
            finish(entry, notify = true)
        }
        player.setOnErrorListener { _, _, _ ->
            finish(entry, notify = false)
            onError("无法播放该音频")
            true
        }

        runCatching {
            player.setDataSource(source)
            player.prepareAsync()
        }.onFailure {
            finish(entry, notify = false)
            onError("无法读取音频文件")
        }
    }

    fun stop(source: String? = null) {
        val current = active ?: return
        if (source == null || current.source == source) {
            stopCurrent(notify = true)
        }
    }

    fun release(source: String) {
        val current = active ?: return
        if (current.source == source) stopCurrent(notify = false)
    }

    fun isPlaying(source: String): Boolean = active?.source == source

    private fun stopCurrent(notify: Boolean) {
        val current = active ?: return
        active = null
        runCatching { current.player.stop() }
        runCatching { current.player.reset() }
        runCatching { current.player.release() }
        if (notify) current.onStopped()
    }

    private fun finish(entry: ActivePlayer, notify: Boolean) {
        if (active?.player !== entry.player) return
        active = null
        runCatching { entry.player.reset() }
        runCatching { entry.player.release() }
        if (notify) entry.onStopped()
    }
}
