package com.test.security.auth;
import com.test.security.user.Role;
import  com.test.security.user.User;
import com.test.security.config.JwtService;
import com.test.security.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Collections;
import com.test.security.otp.OtpRequest;
import com.test.security.otp.OtpService;

@RequiredArgsConstructor
@Service
@Slf4j
public class AuthenticationService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final EmailService emailService;

    public AuthenticationResponse register(RegisterRequest request) {


        User user = repository.findByEmail(request.getEmail()).get();

        user.setAddresses(request.getAddresses());
        user.setMobile(request.getMobile());
        user.setFirstname(request.getFirstname());
        user.setLastname(request.getLastname());
        repository.save(user);
        var jwtToken = jwtService.generateToken(user, Collections.singleton(user.getRole()));
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .roles(Collections.singleton(user.getRole()))
                .userId(user.getId())
                .build();
    }

     public AuthenticationResponse authenticate(authenticationRequest request) {
          try {
              authenticationManager.authenticate(
                      new UsernamePasswordAuthenticationToken(
                              request.getEmail(),
                              request.getPassword()
                      )
              );
              var user = repository.findByEmail(request.getEmail())
                      .orElseThrow();
              var jwtToken = jwtService.generateToken(user, Collections.singleton(user.getRole()));
              return AuthenticationResponse.builder()
                      .token(jwtToken)
                      .roles(Collections.singleton(user.getRole()))
                      .userId(user.getId())
                      .build();
          }catch (AuthenticationException e){
              //log the error
              log.error("Authentication failed for user: {}", request.getEmail());
              throw e;
          }
     }

    public void requestOtp(OtpRequest request) {
        String otp = otpService.generateOtp(request.getEmail());
    }

    public AuthenticationResponse verifyOtpAndAuthenticate(OtpRequest request) {
        if (!otpService.verifyOtp(request.getEmail(), request.getOtp())) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        // Check if user exists
        User user = repository.findByEmail(request.getEmail())
                .orElseGet(() -> createNewUser(request.getEmail()));

        //authenticate this user
        var auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        var jwtToken = jwtService.generateToken(user, Collections.singleton(user.getRole()));
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .roles(Collections.singleton(user.getRole()))
                .userId(user.getId())
                .build();
    }

    private User createNewUser(String email) {
        
        User newUser = User.builder()
                .email(email)
                .role(Role.ROLE_USER)
                .build();
        
        return repository.save(newUser);
    }
}
