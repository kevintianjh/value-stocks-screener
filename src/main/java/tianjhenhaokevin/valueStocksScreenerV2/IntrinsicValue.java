package tianjhenhaokevin.valueStocksScreenerV2;

public class IntrinsicValue {
    private String currency;
    private Double value;
    private Double growthRate;
    private String lastEpsYear;

    public IntrinsicValue() {}

    public IntrinsicValue(String currency, String lastEpsYear) {
        this.currency = currency;
        this.lastEpsYear = lastEpsYear;
    }

    public IntrinsicValue(String currency, Double value, Double growthRate,
                          String lastEpsYear) {
        this.currency = currency;
        this.value = value;
        this.growthRate = growthRate;
        this.lastEpsYear = lastEpsYear;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Double getGrowthRate() {
        return growthRate;
    }

    public void setGrowthRate(Double growthRate) {
        this.growthRate = growthRate;
    }

    public String getLastEpsYear() {
        return lastEpsYear;
    }

    public void setLastEpsYear(String lastEpsYear) {
        this.lastEpsYear = lastEpsYear;
    }
}