package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.PrincipalNameKeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Principal;

@SpringBootApplication
public class GatewayApplication {

	@Bean
	RedisRateLimiter redisRateLimiter() {
		return new RedisRateLimiter(5, 7);
	}

	@Bean
	MapReactiveUserDetailsService userDetailsService() {
		return new MapReactiveUserDetailsService(
			User.withDefaultPasswordEncoder().username("jlong").password("pw").roles("USER").build(),
			User.withDefaultPasswordEncoder().username("rgoers").password("pw").roles("USER", "ADMIN").build()
		);
	}

	@Bean
	SecurityWebFilterChain authorization(ServerHttpSecurity http) {
		return http
			.authorizeExchange(c -> c
				.pathMatchers("/proxy").authenticated()
				.anyExchange().permitAll()
			)
			.csrf(ServerHttpSecurity.CsrfSpec::disable)
			.httpBasic(Customizer.withDefaults())
			.build();
	}

	@Bean
	RouteLocator gatewayToHell(RouteLocatorBuilder rlb) {
		return rlb
			.routes()
			.route(rSpec ->
				rSpec
					.path("/proxy").and().host("*.spring.io")
					.filters(fSpec ->
						fSpec
							.setPath("/reservations")
							.addResponseHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
							.requestRateLimiter(config ->
								config
									.setRateLimiter(redisRateLimiter())
							)
					)
					.uri("http://localhost:8080")
			)
			.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

}
