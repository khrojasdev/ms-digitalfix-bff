package cl.duoc.digitalfix.bff.controller.dto;

import java.util.List;

public record UserProfileDTO(
        String oid,
        String email,
        String name,
        String companyId,
        String companyName,
        List<String> roles
) {}