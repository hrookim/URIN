package com.dongpop.urin.global.oauth.service;

import com.dongpop.urin.domain.member.repository.Member;
import com.dongpop.urin.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        Long id = (Long) attributes.get("id");
        Map<String, String> properties = (Map<String, String>) attributes.get("properties");
        String memberName = properties.get("nickname");
        String profileImage = properties.get("profile_image");

        String email = "";
        Map<String, Object> kakao_account = (Map<String, Object>) attributes.get("kakao_account");
        if ((Boolean) kakao_account.get("has_email")) {
            email = (String) kakao_account.get("email");
        }
        String identifier = email + id;
        log.info("id = {}, memberName = {}, profile_image = {}", id, memberName, profileImage);

        String finalEmail = email;
        Member findMember = memberRepository.findByIdentifier(identifier)
                .orElseGet(() -> {
                    log.info("Execute Join Member");
                    return memberRepository.save(Member.builder()
                            .identifier(identifier)
                            .memberName(memberName)
                            .nickname(memberName)
                            .password(passwordEncoder.encode(identifier))
                            .role("ROLE_USER")
                            .email(finalEmail)
                            .build()
                    );
                });

        return oAuth2User;
    }

}
