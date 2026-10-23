package project.smartbanking.model.enums;

public enum PaymentMethod {
    BANK_TRANSFER("chuyển khoản ngân hàng"),
    ATM_CARD("thẻ ATM"),
    E_WALLET("ví điện tử");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}