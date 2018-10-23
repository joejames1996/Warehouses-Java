package com.softwire.training.shipit.dao.impl;

import com.softwire.training.shipit.dao.StockAlteration;
import com.softwire.training.shipit.dao.StockDAO;
import com.softwire.training.shipit.exception.InvalidStateException;
import com.softwire.training.shipit.model.Company;
import com.softwire.training.shipit.model.InboundOrderLine;
import com.softwire.training.shipit.model.Stock;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockDAOImpl implements StockDAO
{
    private static final ParameterizedRowMapper<Stock> MAPPER = new ParameterizedRowMapper<Stock>()
    {
        public Stock mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            return new Stock(
                    rs.getInt("w_id"),
                    rs.getInt("p_id"),
                    rs.getInt("hld")
            );
        }
    };

    private SimpleJdbcTemplate simpleJdbcTemplate;


    public void setDataSource(DataSource dataSource)
    {
        simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    public List<Stock> getStock(int warehouseId)
    {
        String sql = "SELECT p_id, w_id, hld FROM stock WHERE w_id = ?";
        return simpleJdbcTemplate.query(sql, MAPPER, warehouseId);
    }

    public void addStock(int warehouseId, List<StockAlteration> lineItems) throws InvalidStateException
    {
        List<Object[]> args = new ArrayList<Object[]>();
        for (StockAlteration orderLine : lineItems)
        {
            args.add(new Object[]{
                    orderLine.getProductId(),
                    warehouseId,
                    orderLine.getQuantity(),
                    orderLine.getQuantity()
            });
        }

        String sql = "INSERT INTO stock (p_id, w_id, hld) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE hld = hld + ?";

        int[] rowsUpdated = simpleJdbcTemplate.batchUpdate(sql, args);

        List<String> errors = new ArrayList<String>();
        for (int i = 0; i < rowsUpdated.length; i++)
        {
            if (rowsUpdated[i] == 0)
            {
                errors.add(String.format("Product %s in warehouse %s was unexpectedly not updated (rows updated returned %s)",
                        args.get(i)[1], warehouseId, rowsUpdated[i]));
            }
        }
        if (errors.size() > 0)
        {
            throw new InvalidStateException(errors.toString());
        }
    }

    public Map<Integer, Stock> getStock(int warehouseId, List<Integer> productIds)
    {
        String sql = String.format("SELECT p_id, w_id, hld FROM stock WHERE w_id = %s AND p_id IN (%s)",
                warehouseId, StringUtils.join(productIds, ','));
        List<Stock> stock = simpleJdbcTemplate.query(sql, MAPPER);

        Map<Integer, Stock> stockById = new HashMap<Integer, Stock>(stock.size());
        for (Stock item : stock)
        {
            stockById.put(item.getProductId(), item);
        }

        return stockById;
    }

    public Stock getStock(int warehouseId, Integer productId)
    {
        String sql = String.format("SELECT p_id, w_id, hld FROM stock WHERE w_id = %s AND p_id = %s",
                warehouseId, productId);
        try
        {
            return simpleJdbcTemplate.queryForObject(sql, MAPPER);
        }
        catch (EmptyResultDataAccessException e)
        {
            return null;
        }
    }

    public void removeStock(final int warehouseId, final List<StockAlteration> lineItems) throws InvalidStateException
    {
        String sql = String.format("UPDATE stock SET hld = hld - ? WHERE w_id = %s AND p_id = ?", warehouseId);

        List<Object[]> args = new ArrayList<Object[]>();
        for (StockAlteration lineItem : lineItems)
        {
            args.add(new Object[]{lineItem.getQuantity(), lineItem.getProductId()});
        }

        int[] results = StockDAOImpl.this.simpleJdbcTemplate.batchUpdate(sql, args);
        for (int i = 0; i < results.length; i++)
        {
            if (results[i] != 1)
            {
                throw new InvalidStateException(String.format(
                        "Expected stock row to be updated, but wasn't.  p_id: %s, w_id: %s, hld: %s",
                        args.get(i)[1], warehouseId, args.get(i)[0]));
            }
        }
    }

    public int getTrackedItemsCount()
    {
        String sql = "SELECT COUNT(*) FROM stock";
        return simpleJdbcTemplate.queryForInt(sql);
    }

    public int getStockHeldSum()
    {
        String sql = "SELECT SUM(hld) FROM stock";
        return simpleJdbcTemplate.queryForInt(sql);
    }
}
