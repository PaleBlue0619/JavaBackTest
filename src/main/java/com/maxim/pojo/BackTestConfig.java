package com.maxim.pojo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maxim.pojo.info.StockInfo;
import com.maxim.pojo.kbar.StockBar;
import com.maxim.pojo.order.StockOrder;
import com.maxim.pojo.position.Position;
import com.maxim.pojo.record.StockRecord;
import com.maxim.pojo.summary.StockSummary;
import com.xxdb.DBConnection;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.time.*;
import java.util.HashMap;
import java.util.LinkedHashMap;

// JavaBean
public class BackTestConfig {
    // 单例实例
    private static volatile BackTestConfig instance;

    // 基本对象
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    DateTimeFormatter formatter_dot = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    DBConnection session;
    Config config; // Json格式配置文件

    // 数据库连接信息
    String HOST;
    int PORT;
    String USERNAME;
    String PASSWORD;

    // 回测模块
    // 0.时间类
    Collection<LocalDate> dateList = new ArrayList<>(); // Date(2024.01.01)
    Collection<String> dotDateList = new ArrayList<>(); // "2024.01.01"
    Collection<String> strDateList = new ArrayList<>(); // "20240101"
    LocalDate currentDate;
    Integer currentMinute = 915;
    LocalDateTime currentTimeStamp;
    String currentStrDate; // 20240101
    String currentDotDate; // 2024.01.01
    String startDotDate;
    String endDotDate;
    LocalDateTime startDateTime; // 开始时间 LocalDateTime
    LocalDateTime endDateTime;   // 结束时间 LocalDateTime

    // 1.柜台类
    Integer orderNum = 0; // 订单编号(全局唯一值)
    Boolean runStock; // 策略中是否包含股票
    Boolean runFuture; // 策略中是否包含期货
    Boolean runOption; // 策略中是否包含期权
    String stockCounterJson;

    // 股票类
    LinkedHashMap<Integer, HashMap<String, StockBar>> stockKDict = new LinkedHashMap<>(); // stock_k_dict -> minute -> symbol -> OHLC KBAR(MinFreq)
    HashMap<String, StockInfo> stockInfoDict = new HashMap<>(); // stock_info_dict -> symbol -> OHLC+startDate/endDate Info
    LinkedHashMap stockSignalDict = new LinkedHashMap<>();
    LinkedHashMap stockTimeStampDict = new LinkedHashMap<>();
    Collection<LocalDate> stockDateList = new ArrayList<>();
    LinkedHashMap stockMacroDict = new LinkedHashMap<>();
    Collection<String> stockDotDateList = new ArrayList<>();
    Collection<String> stockStrDateList = new ArrayList<>();
    String stockKDataBase;
    String stockKTable;
    String stockSignalDatabase;
    String stockSignalTable;
    String stockMacroJson;
    String stockInfoJson;
    String stockSignalJson;

    // 柜台模块
    LinkedHashMap<Integer, StockOrder> stockCounter = new LinkedHashMap<>();
    Collection<StockRecord> stockRecord = new ArrayList<>();
//    Collection<String> stockRecord_state = new ArrayList<>(); // 股票成交记录
//    Collection<String> stockRecord_reason = new ArrayList<>();
//    Collection<LocalDate> stockRecord_date = new ArrayList<>();
//    Collection<Integer> stockRecord_minute = new ArrayList<>();
//    Collection<String> stockRecord_symbol = new ArrayList<>();
//    Collection<Double> stockRecord_price = new ArrayList<>();
//    Collection<Double> stockRecord_pnl = new ArrayList<>();
    LinkedHashMap<String, ArrayList<Position>> stockPosition = new LinkedHashMap<>(); // 当前股票持仓情况
    LinkedHashMap<String, StockSummary> stockSummary = new LinkedHashMap<>(); // 用于优化止盈止损

    // 评价模块
    Double cash;
    Double oriCash;
    Double profit = 0.0;
    Double profitSettle = 0.0;
    LinkedHashMap<LocalDate, Double> cashDict = new LinkedHashMap<>();
    LinkedHashMap<LocalDate, Double> profitDict = new LinkedHashMap<>();
    LinkedHashMap<LocalDate, Double> settleProfitDict = new LinkedHashMap<>();
    LinkedHashMap<LocalDate, Double> posDict = new LinkedHashMap<>(); // 用于记录整体仓位水平
    Collection<Object> stockOrderRecord = new ArrayList<>();

    /**
     * 获取单例实例（懒加载）
     * @return BackTestConfig单例实例
     */
    public static BackTestConfig getInstance() {
        if (instance == null) {
            synchronized (BackTestConfig.class) {
                if (instance == null) {
                    try {
                        instance = new BackTestConfig();
                    } catch (Exception e) {
                        throw new RuntimeException("初始化BackTestConfig失败", e);
                    }
                }
            }
        }
        return instance;
    }

    /**
     * 通过JSON配置字符串初始化单例实例
     * @param conf JSON配置字符串
     * @return BackTestConfig单例实例
     */
    public static BackTestConfig getInstance(String conf) throws ParseException, JsonProcessingException {
        if (instance == null) {
            synchronized (BackTestConfig.class) {
                if (instance == null) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Config config = objectMapper.readValue(conf, Config.class);
                    instance = new BackTestConfig();
                    instance.initializeFromConfig(config);
                }
            }
        }
        return instance;
    }

    public void BackTestConfigInit(){
        // 默认初始化
        if (currentDate != null) {
            cashDict.put(currentDate, oriCash);
            profitDict.put(currentDate, 0.0);
            settleProfitDict.put(currentDate, 0.0);
            posDict.put(currentDate, 0.0);
        }
    }

    /**
     * 根据Config对象初始化BackTestConfig的所有属性
     * @param config 配置对象
     */
    public void initializeFromConfig(Config config) throws ParseException {
        this.config = config;

        // 初始化数据库连接信息（如果Config中有相关配置）
        // 如果Config中没有数据库连接信息，可以使用默认值或通过构造函数设置
        this.HOST = config.getHost() != null ? config.getHost() : "localhost"; // 默认值
        this.PORT = config.getPort() != 0 ? config.getPort() : 8848; // 默认值
        this.USERNAME = config.getUsername() != null ? config.getUsername() : "admin"; // 默认值
        this.PASSWORD = config.getPassword() != null ? config.getPassword() : "123456"; // 默认值

        // 初始化回测模块配置
        this.runStock = config.getRun_stock() != null ? config.getRun_stock() : false;
        this.runFuture = config.getRun_future() != null ? config.getRun_future() : false;
        this.runOption = config.getRun_option() != null ? config.getRun_option() : false;
        this.stockCounterJson = config.getStock_counter_json();

        // 初始化股票相关配置
        this.stockKDataBase = config.getStock_K_database();
        this.stockKTable = config.getStock_K_table();
        this.stockSignalDatabase = config.getStock_signal_database();
        this.stockSignalTable = config.getStock_signal_table();
        this.stockMacroJson = config.getStock_macro_json();
        this.stockInfoJson = config.getStock_info_json();
        this.stockSignalJson = config.getStock_signal_json();

        // 初始化时间相关属性
        if (config.getStart_date() != null) {
            LocalDate startDate = parseDateString(config.getStart_date());
            this.currentDate = startDate;
            this.currentTimeStamp = LocalDateTime.of(startDate, LocalTime.of(0, 0));
            this.currentStrDate = formatter.format(currentTimeStamp);
            this.currentDotDate = formatter_dot.format(currentTimeStamp);
            this.startDotDate = formatter_dot.format(startDate);
            this.startDateTime = startDate.atStartOfDay(); // 设置开始时间
        }

        if (config.getEnd_date() != null) {
            LocalDate endDate = parseDateString(config.getEnd_date());
            this.endDotDate = formatter_dot.format(endDate);
            this.endDateTime = endDate.atStartOfDay(); // 设置结束时间
        }

        // 设置初始现金
        this.cash = config.getCash() != null ? config.getCash() : 10000000.0;
        this.oriCash = this.cash;

        // 默认初始化
        BackTestConfigInit();
    }

    /**
     * 解析日期字符串，支持多种格式
     * @param dateStr 日期字符串
     * @return LocalDate对象
     */
    public LocalDate parseDateString(String dateStr) throws ParseException {
        try {
            if (dateStr.contains(".")) {
                return sdf.parse(dateStr).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else {
                // 处理yyyyMMdd格式
                DateTimeFormatter yyyymmddFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                return LocalDate.parse(dateStr, yyyymmddFormatter);
            }
        } catch (Exception e) {
            throw new ParseException("无法解析日期字符串: " + dateStr, 0);
        }
    }

    // 提供获取Config对象的方法，以便其他类可以访问原始配置
    public Config getConfig() {
        return this.config;
    }

    public String getUSERNAME() {
        return USERNAME;
    }

    public void setUSERNAME(String USERNAME) {
        this.USERNAME = USERNAME;
    }

    public static void setInstance(BackTestConfig instance) {
        BackTestConfig.instance = instance;
    }

    public SimpleDateFormat getSdf() {
        return sdf;
    }

    public void setSdf(SimpleDateFormat sdf) {
        this.sdf = sdf;
    }

    public DateTimeFormatter getFormatter() {
        return formatter;
    }

    public void setFormatter(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    public DateTimeFormatter getFormatter_dot() {
        return formatter_dot;
    }

    public void setFormatter_dot(DateTimeFormatter formatter_dot) {
        this.formatter_dot = formatter_dot;
    }

    public DBConnection getSession() {
        return session;
    }

    public void setSession(DBConnection session) {
        this.session = session;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public String getHOST() {
        return HOST;
    }

    public void setHOST(String HOST) {
        this.HOST = HOST;
    }

    public int getPORT() {
        return PORT;
    }

    public void setPORT(int PORT) {
        this.PORT = PORT;
    }

    public String getPASSWORD() {
        return PASSWORD;
    }

    public void setPASSWORD(String PASSWORD) {
        this.PASSWORD = PASSWORD;
    }

    public Collection<LocalDate> getDateList() {
        return dateList;
    }

    public void setDateList(Collection<LocalDate> dateList) {
        this.dateList = dateList;
    }

    public Collection<String> getDotDateList() {
        return dotDateList;
    }

    public void setDotDateList(Collection<String> dotDateList) {
        this.dotDateList = dotDateList;
    }

    public Collection<String> getStrDateList() {
        return strDateList;
    }

    public void setStrDateList(Collection<String> strDateList) {
        this.strDateList = strDateList;
    }

    public LocalDate getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(LocalDate currentDate) {
        this.currentDate = currentDate;
    }

    public Integer getCurrentMinute() {
        return currentMinute;
    }

    public void setCurrentMinute(Integer currentMinute) {
        this.currentMinute = currentMinute;
    }

    public LocalDateTime getCurrentTimeStamp() {
        return currentTimeStamp;
    }

    public void setCurrentTimeStamp(LocalDateTime currentTimeStamp) {
        this.currentTimeStamp = currentTimeStamp;
    }

    public String getCurrentStrDate() {
        return currentStrDate;
    }

    public void setCurrentStrDate(String currentStrDate) {
        this.currentStrDate = currentStrDate;
    }

    public String getCurrentDotDate() {
        return currentDotDate;
    }

    public void setCurrentDotDate(String currentDotDate) {
        this.currentDotDate = currentDotDate;
    }

    public String getStartDotDate() {
        return startDotDate;
    }

    public void setStartDotDate(String startDotDate) {
        this.startDotDate = startDotDate;
    }

    public String getEndDotDate() {
        return endDotDate;
    }

    public void setEndDotDate(String endDotDate) {
        this.endDotDate = endDotDate;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }

    public Boolean getRunStock() {
        return runStock;
    }

    public void setRunStock(Boolean runStock) {
        this.runStock = runStock;
    }

    public Boolean getRunFuture() {
        return runFuture;
    }

    public void setRunFuture(Boolean runFuture) {
        this.runFuture = runFuture;
    }

    public Boolean getRunOption() {
        return runOption;
    }

    public void setRunOption(Boolean runOption) {
        this.runOption = runOption;
    }

    public String getStockCounterJson() {
        return stockCounterJson;
    }

    public void setStockCounterJson(String stockCounterJson) {
        this.stockCounterJson = stockCounterJson;
    }

    public HashMap getStockKDict() {
        return stockKDict;
    }

    public void setStockKDict(LinkedHashMap stockKDict) {
        this.stockKDict = stockKDict;
    }

    public LinkedHashMap getStockMacroDict() {
        return stockMacroDict;
    }

    public void setStockMacroDict(LinkedHashMap stockMacroDict) {
        this.stockMacroDict = stockMacroDict;
    }

    public HashMap getStockInfoDict() {
        return stockInfoDict;
    }

    public void setStockInfoDict(HashMap stockInfoDict) {
        this.stockInfoDict = stockInfoDict;
    }

    public LinkedHashMap getStockSignalDict() {
        return stockSignalDict;
    }

    public void setStockSignalDict(LinkedHashMap stockSignalDict) {
        this.stockSignalDict = stockSignalDict;
    }

    public LinkedHashMap getStockTimeStampDict() {
        return stockTimeStampDict;
    }

    public void setStockTimeStampDict(LinkedHashMap stockTimeStampDict) {
        this.stockTimeStampDict = stockTimeStampDict;
    }

    public Collection<LocalDate> getStockDateList() {
        return stockDateList;
    }

    public void setStockDateList(Collection<LocalDate> stockDateList) {
        this.stockDateList = stockDateList;
    }

    public Collection<String> getStockDotDateList() {
        return stockDotDateList;
    }

    public void setStockDotDateList(Collection<String> stockDotDateList) {
        this.stockDotDateList = stockDotDateList;
    }

    public Collection<String> getStockStrDateList() {
        return stockStrDateList;
    }

    public void setStockStrDateList(Collection<String> stockStrDateList) {
        this.stockStrDateList = stockStrDateList;
    }

    public String getStockKDataBase() {
        return stockKDataBase;
    }

    public void setStockKDataBase(String stockKDataBase) {
        this.stockKDataBase = stockKDataBase;
    }

    public String getStockKTable() {
        return stockKTable;
    }

    public void setStockKTable(String stockKTable) {
        this.stockKTable = stockKTable;
    }

    public String getStockSignalDatabase() {
        return stockSignalDatabase;
    }

    public void setStockSignalDatabase(String stockSignalDatabase) {
        this.stockSignalDatabase = stockSignalDatabase;
    }

    public String getStockSignalTable() {
        return stockSignalTable;
    }

    public void setStockSignalTable(String stockSignalTable) {
        this.stockSignalTable = stockSignalTable;
    }

    public String getStockMacroJson() {
        return stockMacroJson;
    }

    public void setStockMacroJson(String stockMacroJson) {
        this.stockMacroJson = stockMacroJson;
    }

    public String getStockInfoJson() {
        return stockInfoJson;
    }

    public void setStockInfoJson(String stockInfoJson) {
        this.stockInfoJson = stockInfoJson;
    }

    public String getStockSignalJson() {
        return stockSignalJson;
    }

    public void setStockSignalJson(String stockSignalJson) {
        this.stockSignalJson = stockSignalJson;
    }

    public LinkedHashMap<Integer, StockOrder> getStockCounter() {
        return stockCounter;
    }

    public void setStockCounter(LinkedHashMap<Integer, StockOrder> stockCounter) {
        this.stockCounter = stockCounter;
    }

//    public Collection<String> getStockRecord_state() {
//        return stockRecord_state;
//    }
//
//    public void setStockRecord_state(Collection<String> stockRecord_state) {
//        this.stockRecord_state = stockRecord_state;
//    }
//
//    public Collection<String> getStockRecord_reason() {
//        return stockRecord_reason;
//    }
//
//    public void setStockRecord_reason(Collection<String> stockRecord_reason) {
//        this.stockRecord_reason = stockRecord_reason;
//    }
//
//    public Collection<LocalDate> getStockRecord_date() {
//        return stockRecord_date;
//    }
//
//    public void setStockRecord_date(Collection<LocalDate> stockRecord_date) {
//        this.stockRecord_date = stockRecord_date;
//    }
//
//    public Collection<Integer> getStockRecord_minute() {
//        return stockRecord_minute;
//    }
//
//    public void setStockRecord_minute(Collection<Integer> stockRecord_minute) {
//        this.stockRecord_minute = stockRecord_minute;
//    }
//
//    public Collection<String> getStockRecord_symbol() {
//        return stockRecord_symbol;
//    }
//
//    public void setStockRecord_symbol(Collection<String> stockRecord_symbol) {
//        this.stockRecord_symbol = stockRecord_symbol;
//    }
//
//    public Collection<Double> getStockRecord_price() {
//        return stockRecord_price;
//    }
//
//    public void setStockRecord_price(Collection<Double> stockRecord_price) {
//        this.stockRecord_price = stockRecord_price;
//    }
//
//    public Collection<Double> getStockRecord_pnl() {
//        return stockRecord_pnl;
//    }
//
//    public void setStockRecord_pnl(Collection<Double> stockRecord_pnl) {
//        this.stockRecord_pnl = stockRecord_pnl;
//    }

    public LinkedHashMap<String, ArrayList<Position>> getStockPosition() {
        return stockPosition;
    }

    public void setStockPosition(LinkedHashMap<String, ArrayList<Position>> stockPosition) {
        this.stockPosition = stockPosition;
    }

    public LinkedHashMap getStockSummary() {
        return stockSummary;
    }

    public void setStockSummary(LinkedHashMap stockSummary) {
        this.stockSummary = stockSummary;
    }

    public Double getCash() {
        return cash;
    }

    public void setCash(Double cash) {
        this.cash = cash;
    }

    public Double getOriCash() {
        return oriCash;
    }

    public void setOriCash(Double oriCash) {
        this.oriCash = oriCash;
    }

    public Double getProfit() {
        return profit;
    }

    public void setProfit(Double profit) {
        this.profit = profit;
    }

    public Double getProfitSettle() {
        return profitSettle;
    }

    public void setProfitSettle(Double profitSettle) {
        this.profitSettle = profitSettle;
    }

    public LinkedHashMap<LocalDate, Double> getCashDict() {
        return cashDict;
    }

    public void setCashDict(LinkedHashMap<LocalDate, Double> cashDict) {
        this.cashDict = cashDict;
    }

    public LinkedHashMap<LocalDate, Double> getProfitDict() {
        return profitDict;
    }

    public void setProfitDict(LinkedHashMap<LocalDate, Double> profitDict) {
        this.profitDict = profitDict;
    }

    public LinkedHashMap<LocalDate, Double> getSettleProfitDict() {
        return settleProfitDict;
    }

    public void setSettleProfitDict(LinkedHashMap<LocalDate, Double> settleProfitDict) {
        this.settleProfitDict = settleProfitDict;
    }

    public LinkedHashMap<LocalDate, Double> getPosDict() {
        return posDict;
    }

    public void setPosDict(LinkedHashMap<LocalDate, Double> posDict) {
        this.posDict = posDict;
    }

    public Collection<Object> getStockOrderRecord() {
        return stockOrderRecord;
    }

    public void setStockOrderRecord(Collection<Object> stockOrderRecord) {
        this.stockOrderRecord = stockOrderRecord;
    }
}
