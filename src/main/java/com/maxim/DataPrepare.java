package com.maxim;
import com.maxim.pojo.emun.AssetType;
import com.maxim.pojo.emun.DataFreq;
import com.maxim.service.DataLoader;
import com.xxdb.DBConnection;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

public class DataPrepare {
    public static void main(String[] args) throws IOException {
        DataLoader loader = new DataLoader("172.16.0.184", 8001,
                "maxim", "dyJmoc-tiznem-1figgu", 8);
        DBConnection conn = new DBConnection();
        conn.connect("172.16.0.184", 8001, "maxim", "dyJmoc-tiznem-1figgu");
        conn.login("maxim", "dyJmoc-tiznem-1figgu", false);
        LocalDate start_date = LocalDate.parse("2020-01-01");
        LocalDate end_date = LocalDate.parse("2025-10-01");
        Collection<String> symbol_list = Arrays.asList("000001.SZ", "000002.SZ");
        HashMap<String, String> transMap = new HashMap<>();
        // Case-1 股票K线异步多线程保存json
//        loader.KBarToJsonAsync(AssetType.STOCK, DataFreq.MINUTE,"dfs://MinKDB", "Min1K",
//                "tradeDate", "tradeTime", "code",
//                new HashMap<String, String>() {{ // 映射关系: DolphinDB -> JavaBean
//                    put("code", "symbol");
//                    put("tradeDate", "tradeDate");
//                    put("tradeTime", "tradeTime");
//                    put("open", "open");
//                    put("high", "high");
//                    put("low", "low");
//                    put("close", "close");
//                    put("volume", "volume");
//                }},
//                "D:\\BackTest\\JavaBackTest\\data\\stock_cn\\kbar\\", false,
//                start_date, end_date, symbol_list, "open", "high", "low", "close", "volume");
        // Case-2 股票K线信息异步多线程保存json
        loader.InfoToJsonAsync(AssetType.STOCK,"dfs://Info",
                "StockInfo",
                "TradeDate", "symbol" ,
                new HashMap<String, String>() {{ // 映射关系: DolphinDB -> JavaBean
                    put("TradeDate", "tradeDate");
                    put("symbol", "symbol");
                    put("open", "open");
                    put("high", "high");
                    put("low", "low");
                    put("close", "close");
                    put("startDate", "start_date");
                    put("endDate","end_date");
                }},
                "D:\\BackTest\\JavaBackTest\\data\\stock_cn\\info\\", false,
                start_date, end_date, symbol_list, "open", "high", "low", "close", "startDate", "endDate");

//        // Case-2 期货K线异步多线程保存json
//        symbol_list = Arrays.asList("AL", "AU");
//        loader.KBarToJsonAsync(AssetType.FUTURE, DataFreq.DAY,"dfs://DayKDB", "o_tushare_futures_daily",
//                "trade_date", null, "ts_code_body",
//                new HashMap<String, String>() {{ // 映射关系: DolphinDB -> JavaBean
//                    put("ts_code_body", "symbol");
//                    put("trade_date", "tradeDate");
//                    put("open", "open");
//                    put("high", "high");
//                    put("low", "low");
//                    put("close", "close");
//                    put("volume", "volume");
//                    put("pre_settle", "pre_settle");
//                    put("oi", "open_interest");
//                }},
//                "D:\\BackTest\\JavaBackTest\\data\\future_cn\\kbar\\", false,
//                start_date, end_date, symbol_list, "open", "high", "low", "close", "volume", "pre_settle", "oi");
    }
}