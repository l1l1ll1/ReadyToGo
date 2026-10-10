package kr.ac.baekseok.readytogo.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * 계정에 딸린 사용자 정보.
 *
 * 지금은 닉네임 하나뿐이지만 Auth와 따로 두는 이유: 닉네임은 **우리가 저장하는 값**이고,
 * 이름·이메일·사진은 Google이 주는 값이다. 한 곳에 섞으면 어느 쪽이 바뀔 수 있는 값인지
 * 구분되지 않고, 나중에 프로필 항목이 늘 때마다 AuthRepository가 커진다.
 */
interface UserProfileRepository {

    /**
     * 지금 로그인한 계정의 닉네임. 정하지 않았으면 null.
     *
     * [Flow]인 이유: 프로필에서 바꾸면 홈 인사말이 바로 따라와야 한다.
     * 로그아웃하거나 계정을 바꾸면 null로 돌아간다.
     */
    fun observeNickname(): Flow<String?>

    suspend fun setNickname(nickname: String): Result<Unit>

    /** 이 계정의 프로필 문서를 지운다. 탈퇴에만 쓴다 — 일정·즐겨찾기와 같은 규칙이다. */
    suspend fun delete(): Result<Unit>
}
