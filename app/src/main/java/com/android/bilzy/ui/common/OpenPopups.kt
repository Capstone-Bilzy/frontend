package com.android.bilzy.ui.common

import android.app.Dialog
import android.widget.PopupWindow
import java.util.Collections
import java.util.WeakHashMap

/**
 * 화면 위에 떠 있는 팝업(확인창·입력창·사진 확대·⋯ 메뉴)을 기억해 두었다가 한꺼번에 닫는다.
 * Dialog/PopupWindow는 Activity 창에 붙어 있어서, 로그인이 풀려 로그인 화면으로 넘어가도 남아 있을 수 있다.
 * 약한 참조라 닫힌 팝업은 따로 지우지 않아도 된다. 메인 스레드에서만 쓴다.
 */
object OpenPopups {
    private val dialogs: MutableSet<Dialog> = Collections.newSetFromMap(WeakHashMap())
    private val popups: MutableSet<PopupWindow> = Collections.newSetFromMap(WeakHashMap())

    fun track(dialog: Dialog): Dialog = dialog.also { dialogs.add(it) }
    fun track(popup: PopupWindow): PopupWindow = popup.also { popups.add(it) }

    fun dismissAll() {
        dialogs.toList().forEach { if (it.isShowing) runCatching { it.dismiss() } }
        popups.toList().forEach { if (it.isShowing) runCatching { it.dismiss() } }
        dialogs.clear()
        popups.clear()
    }
}
