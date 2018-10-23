package com.softwire.training.shipit.dao.impl;

import com.softwire.training.shipit.dao.StockAlteration;
import com.softwire.training.shipit.dao.StockDAO;
import com.softwire.training.shipit.dao.StockInboundOrderDAO;
import com.softwire.training.shipit.exception.InvalidStateException;
import com.softwire.training.shipit.model.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
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

public class StockInboundOrderDAOImpl implements StockInboundOrderDAO
{
    private static final ParameterizedRowMapper<StockInboundOrder> MAPPER = new ParameterizedRowMapper<StockInboundOrder>()
    {
        public StockInboundOrder mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            Stock stock = new Stock(rs.getInt("w_id"),
                                    rs.getInt("p_id"),
                                    rs.getInt("hld"));

            Product product = new Product(  rs.getInt("p_id"),
                                            rs.getString("gtin_cd"),
                                            rs.getString("gcp_cd"),
                                            rs.getString("gtin_nm"),
                                            rs.getFloat("m_g"),
                                            rs.getInt("l_th"),
                                            rs.getInt("ds") == 1,
                                            rs.getInt("min_qt"));

            Company company = new Company(  rs.getString("gcp_cd"),
                                            rs.getString("gln_nm"),
                                            rs.getString("gln_addr_02"),
                                            rs.getString("gln_addr_03"),
                                            rs.getString("gln_addr_04"),
                                            rs.getString("gln_addr_postalcode"),
                                            rs.getString("gln_addr_city"),
                                            rs.getString("contact_tel"),
                                            rs.getString("contact_mail"));

            return new StockInboundOrder(stock, product, company);
        }
    };

    private SimpleJdbcTemplate simpleJdbcTemplate;

    public void setDataSource(DataSource dataSource)
    {
        simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    public Map<Company, List<InboundOrderLine>> getStockForInboundOrder(Integer warehouseId)
    {
        String sql = "SELECT * FROM stock " +
                "JOIN gtin ON stock.p_id = gtin.p_id " +
                "JOIN gcp ON gtin.gcp_cd = gcp.GCP_CD " +
                "WHERE stock.w_id = ? " +
                "AND stock.hld < gtin.l_th " +
                "AND gtin.ds != 1;";
        List<StockInboundOrder> inboundOrders = simpleJdbcTemplate.query(sql, MAPPER, warehouseId);

        Map<Company, List<InboundOrderLine>> orderMap = new HashMap<Company, List<InboundOrderLine>>(inboundOrders.size());

        for(StockInboundOrder order : inboundOrders)
        {
            int orderQuantity = NumberUtils.max(order.getProduct().getLowerThreshold() * 3 - order.getStock().getHeld(), order.getProduct().getMinimumOrderQuantity());

            if(!orderMap.containsKey(order.getCompany()))
            {
                orderMap.put(order.getCompany(), new ArrayList<InboundOrderLine>());
            }

            orderMap.get(order.getCompany()).add(new InboundOrderLine(order.getProduct().getGtin(), order.getProduct().getName(), orderQuantity));
        }

        return orderMap;
    }
}
