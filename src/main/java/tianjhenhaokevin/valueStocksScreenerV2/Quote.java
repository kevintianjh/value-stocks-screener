package tianjhenhaokevin.valueStocksScreenerV2;

public class Quote {
    private String row;
    private String ticker;
    private String name;
    private String sector;
    private String industry;
    private String country;
    private double price;
    private IntrinsicValue intrinsicValue;
    private boolean ignore = false;

    public String getRow() {
        return row;
    }

    public void setRow(String row) {
        this.row = row;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public IntrinsicValue getIntrinsicValue() {
        return intrinsicValue;
    }

    public void setIntrinsicValue(IntrinsicValue intrinsicValue) {
        this.intrinsicValue = intrinsicValue;
    }

    public boolean isIgnore() {
        return ignore;
    }

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }
}