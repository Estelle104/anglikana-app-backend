package com.anglikana.backoffice;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


public class PasswordEncoderTest {

    @Test
    void testerBCrypt() {

        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        String motDePasse = "rasoa123";

        String hash = encoder.encode(motDePasse);

        System.out.println("Mot de passe : " + motDePasse);
        System.out.println("Hash BCrypt  : " + hash);

        System.out.println(
                "Correct : " +
                encoder.matches(motDePasse, hash)
        );

        System.out.println(
                "Incorrect : " +
                encoder.matches("bonjour123", hash)
        );
    }
}