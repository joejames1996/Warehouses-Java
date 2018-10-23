package com.softwire.training.shipit.dao;

import com.softwire.training.shipit.exception.InvalidStateException;
import com.softwire.training.shipit.model.Company;
import com.softwire.training.shipit.model.InboundOrderLine;
import com.softwire.training.shipit.model.Stock;

import java.util.List;
import java.util.Map;

public interface StockDAO
{
    List<Stock> getStock(int warehouseId);

    int getTrackedItemsCount();

    int getStockHeldSum();

    void addStock(int warehouseId, List<StockAlteration> lineItems) throws InvalidStateException;

    void removeStock(int warehouseId, List<StockAlteration> lineItems) throws InvalidStateException;

    Map<Integer, Stock> getStock(int warehouseId, List<Integer> productIds);

    Stock getStock(int warehouseId, Integer productId);
}
