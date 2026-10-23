package project.smartbanking.dto.response;

public record AccountLookupResponse(
        String accountNumber,
        String ownerName,
        String currency) {
}