package com.alpha.system.dto.response;

/** A single Excel row's outcome. Row numbers include the header. */
public record ImportRowResult(int row, String username, boolean success, String message) {
}
