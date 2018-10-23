package com.softwire.training.shipit.model;

public class StockInboundOrder
{
    private Stock stock;
    private Product product;
    private Company company;

    public StockInboundOrder(Stock stock, Product product, Company company)
    {
        this.stock = stock;
        this.product = product;
        this.company = company;
    }

    public Stock getStock()
    {
        return stock;
    }

    public Product getProduct()
    {
        return product;
    }

    public Company getCompany()
    {
        return company;
    }
}
