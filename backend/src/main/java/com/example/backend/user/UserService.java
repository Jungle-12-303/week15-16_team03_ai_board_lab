package com.example.backend.user;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public UserService(UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public UserResponse signUp(UserSignUpRequest request){
        if(userRepository.existsByLoginId(request.getLoginId())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용중인 아이디입니다.");
        }

        User user = new User(
            request.getLoginId(),
            request.getPassword(),
            request.getNickname()
        );

        User savedUser = userRepository.save(user);

        return new UserResponse(
            savedUser.getId(), 
            savedUser.getLoginId(), 
            savedUser.getNickname()
        );
    }

    public AuthResponse login(UserLoginRequest request) {
        User user = userRepository.findByLoginId(request.getLoginId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "존재하지 않는 아이디입니다."));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.");
        }

        String token = jwtTokenProvider.createToken(user.getLoginId());

        return new AuthResponse(token, user.getNickname(), user.getLoginId());
    }
}
