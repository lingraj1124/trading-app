package com.example.webhook.model;

public class DhanOrderRequest {
    private String correlationId;
    private String transactionType;
    private String exchangeSegment;
    private String productType;
    private String orderType;
    private String validity;
    private String securityId;
    private int quantity;
    private int disclosedQuantity;
    private double price;
    private double triggerPrice;
    private boolean afterMarketOrder;
    private String amoTime;
    private double boProfitValue;
    private double boStopLossValue;

    // Getters and Setters (you can generate via IDE)
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getExchangeSegment() { return exchangeSegment; }
    public void setExchangeSegment(String exchangeSegment) { this.exchangeSegment = exchangeSegment; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }

    public String getValidity() { return validity; }
    public void setValidity(String validity) { this.validity = validity; }

    public String getSecurityId() { return securityId; }
    public void setSecurityId(String securityId) { this.securityId = securityId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getDisclosedQuantity() { return disclosedQuantity; }
    public void setDisclosedQuantity(int disclosedQuantity) { this.disclosedQuantity = disclosedQuantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getTriggerPrice() { return triggerPrice; }
    public void setTriggerPrice(double triggerPrice) { this.triggerPrice = triggerPrice; }

    public boolean isAfterMarketOrder() { return afterMarketOrder; }
    public void setAfterMarketOrder(boolean afterMarketOrder) { this.afterMarketOrder = afterMarketOrder; }

    public String getAmoTime() { return amoTime; }
    public void setAmoTime(String amoTime) { this.amoTime = amoTime; }

    public double getBoProfitValue() { return boProfitValue; }
    public void setBoProfitValue(double boProfitValue) { this.boProfitValue = boProfitValue; }

    public double getBoStopLossValue() { return boStopLossValue; }
    public void setBoStopLossValue(double boStopLossValue) { this.boStopLossValue = boStopLossValue; }
}
