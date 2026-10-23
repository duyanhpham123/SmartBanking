package project.smartbanking.model.enums;

public enum BillProvider {
    ELECTRICITY("điện"),
    WATER("nước"),
    INTERNET("internet"),
    MOBILE("điện thoại di động");

    private final String label;

    BillProvider(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}