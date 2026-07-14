package com.linky.voiceclone.util

import android.media.MediaPlayer

object PlayerManager {
    private var currentPlayer: MediaPlayer? = null
    private var onStopped: (() -> Unit)? = null

    /** 点击播放时调用。自动停掉上一个。 */
    fun play(player: MediaPlayer, onStop: () -> Unit) {
        // 停掉之前的
        if (currentPlayer != player) {
            stopCurrent()
        }
        currentPlayer = player
        onStopped = onStop
        player.setOnCompletionListener {
            onStop()
            currentPlayer = null
            onStopped = null
        }
        player.start()
    }

    fun pause(player: MediaPlayer) {
        try { player.pause(); player.seekTo(0) } catch (_: Exception) {}
        if (currentPlayer == player) {
            currentPlayer = null
            onStopped = null
        }
    }

    private fun stopCurrent() {
        currentPlayer?.let {
            try { it.pause(); it.seekTo(0) } catch (_: Exception) {}
        }
        onStopped?.invoke()
        currentPlayer = null
        onStopped = null
    }
}
