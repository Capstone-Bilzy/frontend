package com.android.favorie

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.favorie.network.RetrofitClient
import com.android.favorie.network.model.KakaoLoginRequest
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivKakaoLogin = view.findViewById<ImageView>(R.id.iv_kakao_login_btn)
        val tvGoSignup = view.findViewById<TextView>(R.id.tv_go_to_signup)

        ivKakaoLogin.setOnClickListener {
            startKakaoLogin()
        }

        tvGoSignup.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.login_container, SignUpFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun startKakaoLogin() {
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e("KakaoLogin", "카카오 로그인 실패", error)
            } else if (token != null) {
                Log.d("KakaoLogin", "카카오 로그인 성공, 서버 인증 시작")
                authenticateWithServer(token.accessToken)
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(requireContext())) {
            UserApiClient.instance.loginWithKakaoTalk(requireContext()) { token, error ->
                if (error != null) {
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) return@loginWithKakaoTalk
                    UserApiClient.instance.loginWithKakaoAccount(requireContext(), callback = callback)
                } else if (token != null) {
                    Log.d("KakaoLogin", "카카오톡 로그인 성공, 서버 인증 시작")
                    authenticateWithServer(token.accessToken)
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(requireContext(), callback = callback)
        }
    }

    // 카카오 accessToken을 서버에 전달해서 JWT 발급
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
                    Log.d("KakaoLogin", "서버 인증 성공")
                    goToMain()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "(empty)"
                    Log.e("KakaoLogin", "서버 인증 실패: ${response.code()} ${response.message()} | body=$errorBody")
                    Toast.makeText(requireContext(), "로그인 실패 (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("KakaoLogin", "서버 인증 예외: ${e.message}", e)
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