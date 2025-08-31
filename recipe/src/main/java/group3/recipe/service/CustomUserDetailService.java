package group3.recipe.service;

import group3.recipe.entity.UserEntity;
import group3.recipe.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailService implements UserDetailsService {
  private final UserRepository userRepository;

  public CustomUserDetailService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    // DB에서 유저 검색
    UserEntity user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

    // Spring Security의 User 객체로 변환
    return org.springframework.security.core.userdetails.User.builder()
        .username(user.getEmail())          // 로그인 ID
        .password(user.getPassword())      // 암호화된 비밀번호
        .authorities(user.getRole())             // 권한 (ex: ROLE_USER, ROLE_ADMIN)
        .build();
  }
}