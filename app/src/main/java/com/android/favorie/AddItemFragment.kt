package com.android.favorie

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.favorie.network.KakaoLocalClient
import com.android.favorie.network.RetrofitClient
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class AddItemFragment : Fragment() {

    private val categoryColors = mapOf(
        "MUSIC"   to "#E2DD64",
        "FASHION" to "#1E88E5",
        "MOOD"    to "#C026D3",
        "BOOK"    to "#00C853",
        "MOVIE"   to "#FF2D3D",
        "SPACE"   to "#FF6D00"
    )

    private var uploadedImageUrl: String? = null
    private var prefillExternalImageUrl: String? = null
    private var recommendationId: Long = -1L
    private var isUploading = false

    private lateinit var ivPreview: ImageView
    private lateinit var ivImagePreviewFull: ImageView
    private lateinit var ivNeonUploadBg: ImageView
    private lateinit var tvUploadHint: TextView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        uploadImage(uri)
    }

    companion object {
        fun newInstance(category: String? = null) = AddItemFragment().apply {
            category?.let { arguments = Bundle().apply { putString("category", it) } }
        }

        fun newInstancePrefilled(
            category: String,
            title: String,
            externalId: String?,
            imageUrl: String?,
            address: String? = null,
            latitude: Double? = null,
            longitude: Double? = null,
            recommendationId: Long = -1L
        ) = AddItemFragment().apply {
            arguments = Bundle().apply {
                putString("category",           category)
                putString("prefill_title",      title)
                externalId?.let { putString("prefill_external_id", it) }
                imageUrl?.let   { putString("prefill_image_url",   it) }
                address?.let    { putString("prefill_address",     it) }
                latitude?.let   { putDouble("prefill_latitude",    it) }
                longitude?.let  { putDouble("prefill_longitude",   it) }
                putLong("recommendation_id", recommendationId)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchIcon: ImageView  = view.findViewById(R.id.iv_search_icon)
        val etSearch: EditText     = view.findViewById(R.id.et_search)
        val etComment: EditText    = view.findViewById(R.id.et_comment)
        val spinner: Spinner       = view.findViewById(R.id.spinner_category)
        val addButton: View        = view.findViewById(R.id.btn_add_item)
        val ivNeonShadow: ImageView = view.findViewById(R.id.iv_neon_shadow)
        val searchContainer: View  = view.findViewById(R.id.layout_search_bar_container)
        val layoutImageUpload: View = view.findViewById(R.id.layout_image_upload)

        ivPreview         = view.findViewById(R.id.iv_preview)
        ivImagePreviewFull = view.findViewById(R.id.iv_image_preview_full)
        ivNeonUploadBg    = view.findViewById(R.id.iv_neon_upload_bg)
        tvUploadHint      = view.findViewById(R.id.tv_upload_hint)

        // ── pre-fill 상태 읽기 ──────────────────────────────────────────────
        val prefillTitle      = arguments?.getString("prefill_title")
        val prefillExternalId = arguments?.getString("prefill_external_id")
        val prefillImageUrl   = arguments?.getString("prefill_image_url")
        val prefillAddress    = arguments?.getString("prefill_address")
        val prefillLatitude   = if (arguments?.containsKey("prefill_latitude") == true) arguments?.getDouble("prefill_latitude") else null
        val prefillLongitude  = if (arguments?.containsKey("prefill_longitude") == true) arguments?.getDouble("prefill_longitude") else null
        val isPrefilled       = prefillTitle != null
        recommendationId      = if (arguments?.containsKey("recommendation_id") == true) arguments?.getLong("recommendation_id") ?: -1L else -1L

        // ── 카테고리 스피너 ──────────────────────────────────────────────────
        val categories = arrayOf("MUSIC", "MOVIE", "BOOK", "SPACE", "FASHION", "MOOD")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        spinner.adapter = adapter

        val initialCategory = arguments?.getString("category") ?: "MUSIC"
        val initialIndex = categories.indexOf(initialCategory).takeIf { it >= 0 } ?: 0
        spinner.setSelection(initialIndex)
        applyCategory(initialCategory, addButton, ivNeonShadow, searchContainer, etSearch, searchIcon)

        if (isPrefilled) {
            // 추천 아이템 자동입력 모드: 카테고리·제목 고정
            spinner.isEnabled = false
            spinner.alpha = 0.5f
            etSearch.setText(prefillTitle)
            etSearch.isEnabled = false
            etSearch.alpha = 0.7f
            searchIcon.visibility = View.GONE

            if (prefillImageUrl != null) {
                prefillExternalImageUrl = prefillImageUrl
                Glide.with(this)
                    .load(prefillImageUrl)
                    .centerCrop()
                    .into(ivImagePreviewFull)
                ivImagePreviewFull.visibility = View.VISIBLE
                ivPreview.visibility          = View.GONE
                tvUploadHint.visibility       = View.GONE
            }
        } else {
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                    resetImageUpload()
                    etSearch.text.clear()
                    applyCategory(categories[position], addButton, ivNeonShadow, searchContainer, etSearch, searchIcon)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        layoutImageUpload.setOnClickListener {
            if (!isUploading) pickImageLauncher.launch("image/*")
        }

        // skipCommentCheck=true: 검색 아이콘 클릭 시 코멘트 없이 검색 허용
        val doSearch = { skipCommentCheck: Boolean ->
            val query    = etSearch.text.toString().trim()
            val comment  = etComment.text.toString().trim()
            val category = spinner.selectedItem.toString()

            val needsSearch = category in listOf("MOVIE", "BOOK", "SPACE", "MUSIC")
            val needsTitle  = category in listOf("FASHION", "MOOD")
            val hasImage    = uploadedImageUrl != null || prefillExternalImageUrl != null

            when {
                needsSearch && query.isEmpty() -> {
                    Toast.makeText(requireContext(), "검색어를 입력해주세요", Toast.LENGTH_SHORT).show()
                }
                needsTitle && query.isEmpty() -> {
                    Toast.makeText(requireContext(), "타이틀을 입력해주세요", Toast.LENGTH_SHORT).show()
                }
                !skipCommentCheck && comment.length < 10 -> {
                    Toast.makeText(requireContext(), "코멘트를 10자 이상 입력해주세요", Toast.LENGTH_SHORT).show()
                }
                !skipCommentCheck && category in listOf("FASHION", "MOOD", "SPACE") && !hasImage -> {
                    Toast.makeText(requireContext(), "이미지를 추가해주세요", Toast.LENGTH_SHORT).show()
                }
                isPrefilled -> {
                    // 추천 아이템 자동입력: 검색 없이 바로 결과 화면으로
                    val effectiveImageUrl = uploadedImageUrl ?: prefillExternalImageUrl
                    navigateToResult(category, query, comment, prefillExternalId, effectiveImageUrl, prefillAddress, prefillLatitude, prefillLongitude)
                }
                else -> {
                    when (category) {
                        "MOVIE"  -> searchMovies(query, comment)
                        "BOOK"   -> searchBooks(query, comment)
                        "SPACE"  -> searchPlaces(query, comment)
                        "MUSIC"  -> searchMusics(query, comment)
                        else     -> navigateToResult(category, query, comment, null, uploadedImageUrl)
                    }
                }
            }
        }

        searchIcon.setOnClickListener { doSearch(true) }   // 코멘트 없이 검색 가능
        addButton.setOnClickListener  { doSearch(false) }  // 코멘트 필수

        // 코멘트 입력 시 키보드 위로 스크롤
        etComment.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                (view as? NestedScrollView)?.post {
                    (view as NestedScrollView).fullScroll(View.FOCUS_DOWN)
                }
            }
        }
    }

    private fun applyCategory(
        category: String,
        addButton: View,
        ivNeonShadow: ImageView,
        searchContainer: View,
        etSearch: EditText,
        searchIcon: ImageView
    ) {
        val colorInt = Color.parseColor(categoryColors[category] ?: "#FFFFFF")
        addButton.backgroundTintList = ColorStateList.valueOf(colorInt)
        ivNeonShadow.imageTintList   = ColorStateList.valueOf(colorInt)
        searchContainer.visibility   = View.VISIBLE

        when (category) {
            "FASHION", "MOOD" -> {
                etSearch.hint    = "Title..."
                searchIcon.visibility = View.GONE
            }
            else -> {
                etSearch.hint    = "Search..."
                searchIcon.visibility = View.VISIBLE
            }
        }

        when (category) {
            "FASHION" -> {
                ivNeonUploadBg.setImageResource(R.drawable.ic_neon_blue)
                ivNeonUploadBg.visibility = View.VISIBLE
            }
            "MOOD" -> {
                ivNeonUploadBg.setImageResource(R.drawable.ic_neon_purple)
                ivNeonUploadBg.visibility = View.VISIBLE
            }
            else -> ivNeonUploadBg.visibility = View.GONE
        }
    }

    private fun uploadImage(uri: android.net.Uri) {
        isUploading = true
        tvUploadHint.text = "업로드 중..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val mimeType = requireContext().contentResolver.getType(uri) ?: "image/jpeg"
                val ext = when (mimeType) {
                    "image/png"  -> "png"
                    "image/webp" -> "webp"
                    else         -> "jpg"
                }
                val bytes = withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openInputStream(uri)?.readBytes()
                }
                if (bytes == null) {
                    Toast.makeText(requireContext(), "이미지를 읽을 수 없습니다", Toast.LENGTH_SHORT).show()
                    resetUploadHint()
                    return@launch
                }

                val filename = "item_${System.currentTimeMillis()}.$ext"
                val presignedResp = RetrofitClient.api.getPresignedUrl(filename)
                if (!presignedResp.isSuccessful) {
                    Toast.makeText(requireContext(), "업로드 URL 발급 실패", Toast.LENGTH_SHORT).show()
                    resetUploadHint()
                    return@launch
                }

                val urls = presignedResp.body()!!
                val success = withContext(Dispatchers.IO) {
                    try {
                        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                        val request = Request.Builder()
                            .url(urls.uploadUrl)
                            .put(requestBody)
                            .build()
                        OkHttpClient().newCall(request).execute().isSuccessful
                    } catch (_: Exception) { false }
                }

                if (success) {
                    uploadedImageUrl = urls.objectUrl
                    Glide.with(this@AddItemFragment)
                        .load(uri)
                        .centerCrop()
                        .into(ivImagePreviewFull)
                    ivImagePreviewFull.visibility = View.VISIBLE
                    ivPreview.visibility  = View.GONE
                    tvUploadHint.visibility = View.GONE
                } else {
                    Toast.makeText(requireContext(), "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                    resetUploadHint()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "오류: ${e.message}", Toast.LENGTH_SHORT).show()
                resetUploadHint()
            } finally {
                isUploading = false
            }
        }
    }

    private fun resetUploadHint() {
        tvUploadHint.text = "[+ Image / Default Cover]"
    }

    private fun resetImageUpload() {
        uploadedImageUrl = null
        ivImagePreviewFull.visibility = View.GONE
        ivPreview.visibility  = View.VISIBLE
        tvUploadHint.visibility = View.VISIBLE
        tvUploadHint.text = "[+ Image / Default Cover]"
    }

    private fun searchMovies(query: String, comment: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.searchMovies(query)
                if (response.isSuccessful) {
                    val results = response.body()?.results ?: emptyList()
                    if (results.isEmpty()) {
                        Toast.makeText(requireContext(), "검색 결과가 없습니다", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val titles = results.map { "${it.title} (${it.releaseYear ?: "-"})" }.toTypedArray()
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("영화 검색 결과")
                        .setItems(titles) { _, index ->
                            val selected = results[index]
                            navigateToResult("MOVIE", selected.title, comment, selected.tmdbId.toString(), selected.imageUrl)
                        }
                        .show()
                } else {
                    Toast.makeText(requireContext(), "검색 실패 (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchBooks(query: String, comment: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.searchBooks(query)
                if (response.isSuccessful) {
                    val results = response.body()?.results ?: emptyList()
                    if (results.isEmpty()) {
                        Toast.makeText(requireContext(), "검색 결과가 없습니다", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val titles = results.map { "${it.title} - ${it.author ?: "저자 미상"}" }.toTypedArray()
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("도서 검색 결과")
                        .setItems(titles) { _, index ->
                            val selected = results[index]
                            navigateToResult("BOOK", selected.title, comment, selected.bookId, selected.imageUrl)
                        }
                        .show()
                } else {
                    Toast.makeText(requireContext(), "검색 실패 (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchMusics(query: String, comment: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.searchMusics(query)
                if (response.isSuccessful) {
                    val results = response.body()?.results ?: emptyList()
                    if (results.isEmpty()) {
                        Toast.makeText(requireContext(), "검색 결과가 없습니다", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val titles = results.map { "${it.title} - ${it.artist ?: "아티스트 미상"}" }.toTypedArray()
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("음악 검색 결과")
                        .setItems(titles) { _, index ->
                            val selected = results[index]
                            navigateToResult("MUSIC", selected.title, comment, selected.spotifyId, selected.imageUrl)
                        }
                        .show()
                } else {
                    Toast.makeText(requireContext(), "검색 실패 (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchPlaces(query: String, comment: String) {
        val restApiKey = getString(R.string.kakao_rest_api_key)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = KakaoLocalClient.service.searchPlaces(
                    authorization = "KakaoAK $restApiKey",
                    query = query
                )
                if (response.isSuccessful) {
                    val places = response.body()?.documents ?: emptyList()
                    if (places.isEmpty()) {
                        Toast.makeText(requireContext(), "검색 결과가 없습니다", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val items = places.map {
                        val addr = it.roadAddressName.ifEmpty { it.addressName }
                        "${it.placeName}\n$addr"
                    }.toTypedArray()
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("장소 검색 결과")
                        .setItems(items) { _, index ->
                            val selected = places[index]
                            val address = selected.roadAddressName.ifEmpty { selected.addressName }
                            navigateToResult(
                                category   = "SPACE",
                                title      = selected.placeName,
                                comment    = comment,
                                externalId = selected.id,
                                imageUrl   = uploadedImageUrl,
                                address    = address,
                                latitude   = selected.y.toDoubleOrNull(),
                                longitude  = selected.x.toDoubleOrNull()
                            )
                        }
                        .show()
                } else {
                    Toast.makeText(requireContext(), "장소 검색 실패 (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToResult(
        category: String,
        title: String,
        comment: String,
        externalId: String?,
        imageUrl: String?,
        address: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        val resultFragment = SearchResultFragment()
        resultFragment.arguments = Bundle().apply {
            putString("category",      category)
            putString("search_query",  title)
            putString("user_comment",  comment)
            putString("external_id",   externalId)
            putString("image_url",     imageUrl)
            putString("address",       address)
            latitude?.let  { putDouble("latitude",  it) }
            longitude?.let { putDouble("longitude", it) }
            if (recommendationId > 0L) putLong("recommendation_id", recommendationId)
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, resultFragment)
            .addToBackStack(null)
            .commit()
    }
}
