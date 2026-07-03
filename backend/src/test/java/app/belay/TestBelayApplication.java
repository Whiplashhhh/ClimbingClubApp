package app.belay;

import org.springframework.boot.SpringApplication;

public class TestBelayApplication {

    public static void main(String[] args) {
        SpringApplication.from(BelayApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
