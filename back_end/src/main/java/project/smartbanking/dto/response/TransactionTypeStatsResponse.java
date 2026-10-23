package project.smartbanking.dto.response;

import java.util.List;

import project.smartbanking.model.enums.TransactionType;

public record TransactionTypeStatsResponse(long total, List<Item> items) {

    public record Item(TransactionType type, long count, double percent) {
    }
}