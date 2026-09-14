package com.android.bilzy.util

import android.net.Uri

/**
 * 정산방 초대 딥링크(`bilzy://join/{settlement_id}?token={jwt}`) 파싱·검증.
 *
 * QR 스캔과 외부 딥링크 양쪽에서 공용으로 쓴다. settlement_id는 백엔드가 UUID로 발급하므로,
 * 외부에서 들어온 값은 **반드시 UUID 형식인지 검증**한 뒤에만 사용한다(주입/오용 방어).
 */
object JoinLink {

    const val SCHEME = "bilzy"
    const val HOST = "join"

    private val UUID_REGEX = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    )

    /** 파싱된 초대 링크. token은 서명+만료 검증용(신규 참여자만 필요, 없을 수도 있음). */
    data class ParsedInvite(val settlementId: String, val token: String?)

    /** 유효한 UUID 형식인지. */
    fun isValidId(id: String?): Boolean = id != null && UUID_REGEX.matches(id)

    /**
     * `bilzy://join/{uuid}?token={jwt}` 문자열에서 settlement_id와 token을 추출.
     * 스킴/호스트가 다르거나 id가 UUID가 아니면 null. token 쿼리 파라미터는 선택적.
     */
    fun parse(raw: String?): ParsedInvite? {
        raw ?: return null
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
        if (uri.scheme != SCHEME || uri.host != HOST) return null
        val id = uri.lastPathSegment
        if (!isValidId(id)) return null
        val token = uri.getQueryParameter("token")
        return ParsedInvite(settlementId = id!!, token = token)
    }
}
