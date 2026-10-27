package com.android.bilzy.ui.common

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RawRes

/**
 * `res/raw`의 모션 영상을 TextureView에 한 번 재생한다. 첫 프레임이 그려지기 전에는 뷰를 투명하게 둬서
 * 검은 화면이 깜빡이지 않게 한다. 재생이 끝나거나 실패하면 [onEnd]를 한 번 부른다.
 * 반환된 함수를 onDestroyView에서 불러 플레이어를 해제할 것.
 */
fun TextureView.playMotionOnce(@RawRes resId: Int, onEnd: () -> Unit): () -> Unit {
    var player: MediaPlayer? = null
    var surface: Surface? = null
    var ended = false
    fun finish() {
        if (ended) return
        ended = true
        onEnd()
    }
    fun start(texture: SurfaceTexture) {
        val s = Surface(texture).also { surface = it }
        player = MediaPlayer().apply {
            try {
                setDataSource(context, Uri.parse("android.resource://${context.packageName}/$resId"))
                setSurface(s)
                setVolume(0f, 0f)
                setOnInfoListener { _, what, _ ->
                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) alpha = 1f
                    false
                }
                setOnCompletionListener { finish() }
                setOnErrorListener { _, _, _ -> finish(); true }
                setOnPreparedListener { it.start() }
                prepareAsync()
            } catch (e: Exception) {
                finish()
            }
        }
    }

    alpha = 0f
    if (isAvailable) {
        surfaceTexture?.let { start(it) }
    } else {
        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) = start(st)
            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) = Unit
            override fun onSurfaceTextureDestroyed(st: SurfaceTexture) = true
            override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
        }
    }
    return {
        ended = true
        player?.release()
        player = null
        surface?.release()
        surface = null
    }
}
