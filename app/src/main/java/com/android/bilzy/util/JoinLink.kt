package com.android.bilzy.util

/**
 * 정산방 초대 딥링크(`bilzy://join/{settlement_id}`) 파싱·검증.
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

    /** 유효한 UUID 형식인지. */
    fun isValidId(id: String?): Boolean = id != null && UUID_REGEX.matches(id)

    /**
     * `bilzy://join/{uuid}` 문자열에서 유효한 settlement_id만 추출.
     * 스킴/호스트가 다르거나 id가 UUID가 아니면 null.
     */
    fun parse(raw: String?): String? {
        raw ?: return null
        val prefix = "$SCHEME://$HOST/"
        if (!raw.startsWith(prefix)) return null
        val id = raw.removePrefix(prefix).trim('/')
        return if (isValidId(id)) id else null
    }
}
