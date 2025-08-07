package com.maxim;
import com.maxim.service.StockInfoStruct;
import com.maxim.service.Utils; // 工具模块
import com.maxim.service.DataLoader; // 数据导入模块
import com.maxim.service.StockKBarStruct;
import com.xxdb.DBConnection;
import com.xxdb.data.BasicTable;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class DataPrepare {
    private static final String DBName = "dfs://stock_cn/combination";
    private static final String TBName = "StockDailyKBar";
    private static final String HOST =  "172.16.0.184";
    private static final int PORT = 8001;
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "123456";
    public static void main(String[] args) throws IOException {
        // 创建DolphinDB连接对象 + 数据库连接池
        DBConnection conn = new DBConnection();
        String HOST = "183.134.101.138";
        int PORT = 8860;
        String USERNAME = "admin";
        String PASSWORD = "123456";
        conn.connect(HOST, PORT);
        conn.login(USERNAME, PASSWORD, true);
        String DBName = "dfs://MinuteKDB";
        String TBName = "stock_bar";
        String barSavePath = "D:\\Maxim\\BackTest\\JavaBackTest\\data\\stock_cn\\kbar";
        String infoSavePath = "D:\\Maxim\\BackTest\\JavaBackTest\\data\\stock_cn\\info";
        String start_date = "2023.02.01";
        String end_date = "2023.03.01";
        // 指定股票列表
        ArrayList<String> symbol_list = new ArrayList<>(
                Arrays.asList("000001","000002","000004","000005","000006","000007","000008","000009","000010")
        );

        // 定义KBar & Info结构体
        StockKBarStruct barStruct = new StockKBarStruct("SecurityID", "TradeDate", "TradeTime",
                "open", "high", "low", "close", "volume");
        StockInfoStruct infoStruct = new StockInfoStruct("SecurityID", "TradeDate",
                "OpenPrice", "HighestPrice", "LowestPrice", "ClosePrice");

        // 多线程获取KBar数据
        Utils.deleteFileDir(barSavePath); // 先删除上一次生成的全部文件
        ConcurrentHashMap<LocalDate, BasicTable> KBarMap = DataLoader.getStockKBar(conn, DBName, TBName, barSavePath, "minute",
                start_date, end_date, barStruct, symbol_list);
        conn.close();

        // 创建DolphinDB连接对象 + 数据库连接池
        DBConnection conn1 = new DBConnection();
        String HOST1 = "192.168.100.43";
        int PORT1 = 8700;
        String USERNAME1 = "admin";
        String PASSWORD1 = "123456";
        String DBName1 = "dfs://stockDayKDetail";
        String TBName1 = "stockDayK";
        conn1.connect(HOST1, PORT1);
        conn1.login(USERNAME1, PASSWORD1, true);

        // 多线程获取Info数据
        Utils.deleteFileDir(infoSavePath); // 先删除上一次生成的全部文件
        DataLoader.getStockInfo(conn1, DBName1, TBName1, infoSavePath, start_date, end_date, infoStruct, symbol_list);
        conn1.close();
    }
}