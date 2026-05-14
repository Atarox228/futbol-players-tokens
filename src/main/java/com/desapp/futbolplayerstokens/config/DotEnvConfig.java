package com.desapp.futbolplayerstokens.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DotEnvConfig {
    public DotEnvConfig() {
        Dotenv.configure().ignoreIfMissing().load();
    }
}
