package project.smartbanking.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.smartbanking.model.User;
import project.smartbanking.repository.UserRepository;
import project.smartbanking.util.IdentifierUtils;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Dùng khi đăng nhập: identifier là email hoặc số điện thoại
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String normalized = IdentifierUtils.normalizeIdentifier(identifier);
        User user = userRepository.findByEmailOrPhone(normalized, normalized)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng"));
        return UserPrincipal.from(user);
    }

    // Dùng trong JwtAuthenticationFilter: subject của token là id người dùng
    @Transactional(readOnly = true)
    public UserPrincipal loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng"));
        return UserPrincipal.from(user);
    }
}