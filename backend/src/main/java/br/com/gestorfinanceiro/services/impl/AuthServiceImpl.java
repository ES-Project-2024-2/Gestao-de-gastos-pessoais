package br.com.gestorfinanceiro.services.impl;

import br.com.gestorfinanceiro.exceptions.auth.UserOperationException;
import br.com.gestorfinanceiro.exceptions.auth.login.EmailNotFoundException;
import br.com.gestorfinanceiro.exceptions.auth.login.InvalidPasswordException;
import br.com.gestorfinanceiro.exceptions.auth.register.EmailAlreadyExistsException;
import br.com.gestorfinanceiro.exceptions.auth.register.UsernameAlreadyExistsException;
import br.com.gestorfinanceiro.models.UserEntity;
import br.com.gestorfinanceiro.repositories.UserRepository;
import br.com.gestorfinanceiro.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserEntity register(@Valid UserEntity userEntity) {
        // Verifica se o e-mail já está cadastrado
        if (userRepository.findByEmail(userEntity.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException(userEntity.getEmail());
        }
        // Verifica se o username já está cadastrado
        if (userRepository.findByUsername(userEntity.getUsername()).isPresent()) {
            throw new UsernameAlreadyExistsException(userEntity.getUsername());
        }

        try {
            userEntity.setPassword(passwordEncoder.encode(userEntity.getPassword()));
            return userRepository.save(userEntity);
        } catch (Exception e) {
            throw new UserOperationException("Erro ao registrar usuario. Por favor, tente novamente", e);
        }
    }


    @Override
    public UserEntity login(String email, String password) {
        try {
            UserEntity userFoundByEmail = userRepository.findByEmail(email).orElseThrow(() -> new EmailNotFoundException(email));

            // Verifica se a senha informada é a mesma que a senha cadastrada
            if (passwordEncoder.matches(password, userFoundByEmail.getPassword())) {
                return userFoundByEmail;
            }
            // Senão encontrar a senha é inválida
            throw new InvalidPasswordException();

        } catch (EmailNotFoundException e) {
            throw new EmailNotFoundException("Email não encontrado. Por favor, verifique se o email está correto.");
        } catch (InvalidPasswordException e) {
            throw new InvalidPasswordException();
        } catch (Exception e) {
            throw new UserOperationException("Erro ao logar usuario. Por favor, tente novamente", e);
        }
    }

    @Override
    public UserEntity findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new EmailNotFoundException(email));
    }
}