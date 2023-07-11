package com.goodjob.api.controller;

import com.goodjob.api.controller.member.MemberController;
import com.goodjob.core.domain.member.entity.Member;
import com.goodjob.core.domain.member.service.MemberService;
import com.goodjob.core.global.base.redis.RedisUt;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static com.goodjob.core.domain.member.constant.Membership.MENTOR;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberControllerTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisUt redisUt;

    private static Cookie getCookie(Cookie[] cookies, String refreshToken) {
        Cookie cookieName = Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(refreshToken))
                .findFirst()
                .orElse(null);

        return cookieName;
    }

    @Test
    @DisplayName("일반 회원가입 성공")
    void joinSuccess() throws Exception {
        // WHEN
        ResultActions resultActions = mockMvc
                .perform(post("/member/join")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("username", "tester1")
                        .param("password", "1234")
                        .param("confirmPassword", "1234")
                        .param("nickname", "tester1")
                        .param("email", "tester1@naver.com")
                )
                .andDo(print());

        // THEN
        resultActions
                .andExpect(status().is3xxRedirection())
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("join"))
                .andExpect(model().hasNoErrors())
                .andExpect(redirectedUrlPattern("/member/login**"));

        assertThat(memberService.findByUsername("tester1")).isPresent();
    }

    @Test
    @DisplayName("일반 회원가입 실패 - bindingError")
    void joinFail_BindingError() throws Exception {
        // WHEN
        ResultActions resultActions = mockMvc
                .perform(post("/member/join")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("username", "tes")
                        .param("password", "1234")
                        .param("confirmPassword", "1234")
                        .param("nickname", "tester1")
                        .param("email", "tester1@naver.com")
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().is2xxSuccessful())
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("join"))
                .andExpect(view().name("member/join"))
                .andExpect(model().hasErrors());

        assertThat(memberService.findByUsername("tes")).isEmpty();
    }

    @Test
    @DisplayName("일반 회원가입 실패 - 실패")
    void joinFail_joinError() throws Exception {
        // WHEN
        ResultActions resultActions = mockMvc
                .perform(post("/member/join")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("username", "test")
                        .param("password", "1234")
                        .param("confirmPassword", "1234")
                        .param("nickname", "tester1")
                        .param("email", "tester1@naver.com")
                )
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().is4xxClientError())
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("join"))
                .andExpect(view().name("common/js"))
                .andExpect(model().hasNoErrors());
    }

    @Test
    @DisplayName("로그인 성공")
    void loginSuccess() throws Exception {
        // WHEN
        ResultActions resultActions = mockMvc
                .perform(post("/member/login")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("username", "test")
                        .param("password", "1234")
                )
                .andDo(print());

        // THEN
        MvcResult mvcResult = resultActions
                .andExpect(status().is3xxRedirection())
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("login"))
                .andReturn();

        // 쿠키 검증
        MockHttpServletResponse response = mvcResult.getResponse();
        Cookie[] cookies = response.getCookies();
        assertThat(cookies).isNotNull();
        assertThat(cookies).hasSize(2);

        // 액세스 토큰 검증
        Cookie accessToken = getCookie(cookies, "accessToken");
        assertThat(accessToken).isNotNull();

        // 리프레시 토큰 검증
        Cookie refreshToken = getCookie(cookies, "refreshToken");
        assertThat(refreshToken).isNotNull();

        // 레디스 저장 확인 및 토큰 값 비교
        Member member = memberService.findByUsername("test").orElse(null);
        String value = redisUt.getValue(String.valueOf(member.getId()));

        assertThat(value).isEqualTo(refreshToken.getValue());
    }

    @Test
    @DisplayName("로그인 실패 - 아이디 틀림")
    void loginFail_InvalidUsername() throws Exception {
        // WHEN
        ResultActions resultActions = mockMvc
                .perform(post("/member/login")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("username", "tester1")
                        .param("password", "1234")
                )
                .andDo(print());

        // THEN
        resultActions
                .andExpect(status().is4xxClientError())
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("login"));
    }

    @Test
    @DisplayName("로그아웃")
    void logoutSuccess() throws Exception {
        // GIVEN
        // 쿠키, 레디스 설정을 위해 로그인처리 및 시큐리티에 User 객체 저장
        MvcResult loginResult = mockMvc
                .perform(post("/member/login")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("username", "test")
                        .param("password", "1234")
                )
                .andReturn();
        Member member = memberService.findByUsername("test").orElse(null);
        User user = new User(member.getUsername(), member.getPassword(), member.getAuthorities());
        // 스프링 시큐리티 객체에 저장할 authentication 객체 생성
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(user, null, member.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);

        MockHttpServletResponse loginResp = loginResult.getResponse();
        Cookie[] loginCookies = loginResp.getCookies();

        // WHEN
        ResultActions resultActions = mockMvc
                .perform(post("/member/logout")
                        .cookie(loginCookies)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andDo(print());

        // THEN
        MvcResult mvcResult = resultActions
                .andExpect(status().is3xxRedirection())
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("logout"))
                .andExpect(redirectedUrlPattern(("/**")))
                .andReturn();

        // 쿠키 검증
        MockHttpServletResponse response = mvcResult.getResponse();
        Cookie[] cookies = response.getCookies();

        int accessToken = getCookie(cookies, "accessToken").getMaxAge();
        int refreshToken = getCookie(cookies, "refreshToken").getMaxAge();
        assertThat(accessToken).isEqualTo(0);
        assertThat(refreshToken).isEqualTo(0);

        // 레디스 검증
        assertThat(redisUt.hasValue(String.valueOf(member.getId()))).isFalse();
    }

    @Test
    @DisplayName("멘토등록 성공")
    @WithUserDetails("test")
    void applyMentorSuccess() throws Exception {
        // WHEN
        ResultActions resultActions = mockMvc
                .perform(post("/member/applyMentor")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("isMentor", String.valueOf(true))
                )
                .andDo(print());

        // THEN
        resultActions
                .andExpect(status().is3xxRedirection())
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("applyMentor"))
                .andExpect(redirectedUrlPattern("/mentoring/list**"));

        Member member = memberService.findByUsername("test").orElse(null);

        assertThat(member.getMembership()).isEqualTo(MENTOR);
    }
}