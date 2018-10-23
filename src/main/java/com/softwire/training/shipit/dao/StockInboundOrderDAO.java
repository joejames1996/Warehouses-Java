package com.softwire.training.shipit.dao;

import com.softwire.training.shipit.exception.InvalidStateException;
import com.softwire.training.shipit.model.Company;
import com.softwire.training.shipit.model.InboundOrderLine;
import com.softwire.training.shipit.model.Stock;

import java.util.List;
import java.util.Map;

public interface StockInboundOrderDAO
{
    Map<Company, List<InboundOrderLine>> getStockForInboundOrder(Integer warehouseId);
}
