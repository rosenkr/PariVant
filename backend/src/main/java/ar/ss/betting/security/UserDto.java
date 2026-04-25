package ar.ss.betting.security;

public record UserDto(
        String email,
        Role role
) {}
