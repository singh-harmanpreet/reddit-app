package in.harmanpreet.learn.reddit.service;

import in.harmanpreet.learn.reddit.constants.EmailTexts;
import in.harmanpreet.learn.reddit.dto.RegisterRequest;
import in.harmanpreet.learn.reddit.exception.RedditException;
import in.harmanpreet.learn.reddit.model.NotificationEmail;
import in.harmanpreet.learn.reddit.model.User;
import in.harmanpreet.learn.reddit.model.VerificationToken;
import in.harmanpreet.learn.reddit.repository.UserRepository;
import in.harmanpreet.learn.reddit.repository.VerificationTokenRepository;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final MailService mailService;

    @Transactional
    public void signup(RegisterRequest registerRequest) {
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setCreated(Instant.now());
        user.setEnabled(false);

        userRepository.save(user);

        String token = generateVerificationToken(user);
        mailService.sendMail(new NotificationEmail(
                "Please activate your account",
                user.getEmail(),
                EmailTexts.ACTIVATION_EMAIL_BODY + token));
    }

    private String generateVerificationToken(User user) {
        String token = UUID.randomUUID().toString();

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);

        verificationTokenRepository.save(verificationToken);

        return token;
    }

    public void activateAccount(String token) {
        Optional<VerificationToken> verificationToken = verificationTokenRepository.findByToken(token);
        verificationToken.orElseThrow(() -> new RedditException("Invalid verification token!"));

        fetchUserAndEnable(verificationToken.get());
    }

    @Transactional
    private void fetchUserAndEnable(VerificationToken verificationToken) {
        String username = verificationToken.getUser().getUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RedditException("User not found with username: " + username));
        user.setEnabled(true);
        userRepository.save(user);
    }
}
