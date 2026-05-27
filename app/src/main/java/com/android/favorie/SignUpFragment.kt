package com.android.favorie

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.favorie.network.RetrofitClient
import com.android.favorie.network.model.KakaoLoginRequest
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.launch

class SignUpFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<ImageView>(R.id.iv_kakao_signup_btn).setOnClickListener {
            startKakaoLogin()
        }
    }

    private fun startKakaoLogin() {
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e("SignUp", "카카오 로그인 실패", error)
            } else if (token != null) {
                authenticateWithServer(token.accessToken)
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(requireContext())) {
            UserApiClient.instance.loginWithKakaoTalk(requireContext()) { token, error ->
                if (error != null) {
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) return@loginWithKakaoTalk
                    UserApiClient.instance.loginWithKakaoAccount(requireContext(), callback = callback)
                } else if (token != null) {
                    authenticateWithServer(token.accessToken)
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(requireContext(), callback = callback)
        }
    }

    private fun authenticateWithServer(kakaoAccessToken: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.kakaoLogin(
                    KakaoLoginRequest(kakaoAccessToken = kakaoAccessToken)
                )
                if (response.isSuccessful) {
                    val body = response.body()!!
                    TokenManager.accessToken = body.accessToken
                    TokenManager.refreshToken = body.refreshToken
                    goToMain()
                } else {
                    Toast.makeText(requireContext(), "로그인 실패 (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("SignUp", "서버 인증 예외: ${e.message}", e)
                Toast.makeText(requireContext(), "서버 연결 실패: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun goToMain() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}
