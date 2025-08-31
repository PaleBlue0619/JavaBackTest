package com.maxim.pojo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maxim.pojo.info.FutureInfo;
import com.maxim.pojo.info.StockInfo;
import com.maxim.pojo.kbar.FutureBar;
import com.maxim.pojo.kbar.StockBar;
import com.maxim.pojo.order.FutureOrder;
import com.maxim.pojo.order.StockOrder;
import com.maxim.pojo.position.FuturePosition;
import com.maxim.pojo.position.Position;
import com.maxim.pojo.position.StockPosition;
import com.maxim.pojo.record.FutureRecord;
import com.maxim.pojo.record.StockRecord;
import com.maxim.pojo.summary.FutureSummary;
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
    LocalDate startDate;
    LocalDate endDate;
    LocalDateTime startTimeStamp;
    LocalDateTime endTimeStamp;
    String startStrDate;
    String endStrDate;
    String startDotDate;
    String endDotDate;

    // 1.柜台类
    Integer orderNum = 0; // 订单编号(全局唯一值)
    Boolean runStock; // 策略中是否包含股票
    Boolean runFuture; // 策略中是否包含期货
    Boolean runOption; // 策略中是否包含期权

    // 股票类-基本信息
    Collection<LocalDate> stockDateList = new ArrayList<>();  // 股票标的的回测时间(天)
    Collection<LocalTime> stockTimeList = new ArrayList<>();   // 股票标的的回测时间(每天中的分钟)
    LinkedHashMap<Integer, HashMap<String, StockBar>> stockKDict = new LinkedHashMap<>(); // stock_k_dict -> minute -> symbol -> OHLC KBAR(MinFreq)
    HashMap<String, StockInfo> stockInfoDict = new HashMap<>(); // stock_info_dict -> symbol -> OHLC+startDate/endDate Info
    LinkedHashMap stockSignalDict = new LinkedHashMap<>();
    LinkedHashMap stockMacroDict = new LinkedHashMap<>();
    Collection<String> stockDotDateList = new ArrayList<>();
    Collection<String> stockStrDateList = new ArrayList<>();
    String stockKDataBase;
    String stockKTable;
    String stockCounterJson;
    String stockMacroJson;
    String stockInfoJson;
    String stockSignalJson;

    // 期货类-基本信息
    Collection<LocalDate> futureDateList = new ArrayList<>();  // 股票标的的回测时间(天)
    Collection<LocalTime> futureTimeList = new ArrayList<>();   // 股票标的的回测时间(每天中的分钟)
    LinkedHashMap<Integer, HashMap<String, FutureBar>> futureKDict = new LinkedHashMap<>(); // future_k_dict -> minute -> symbol -> OHLC KBAR(MinFreq)
    HashMap<String, FutureInfo> futureInfoDict = new HashMap<>();
    LinkedHashMap futureSignalDict = new LinkedHashMap<>();
    LinkedHashMap futureMacroDict = new LinkedHashMap<>();
    Collection<String> futureDotDateList = new ArrayList<>();
    Collection<String> futureStrDateList = new ArrayList<>();
    String futureKDataBase;
    String futureKTable;
    String futureMacroJson;
    String futureInfoJson;
    String futureSignalJson;

    // 股票类-柜台模块 // 注: LinkedHashMap底层双向链表, 按照插入顺序进行排序, 符合回测业务逻辑
    LinkedHashMap<Integer, StockOrder> stockCounter = new LinkedHashMap<>();
    Collection<StockRecord> stockRecord = new ArrayList<>();
    LinkedHashMap<String, ArrayList<StockPosition>> stockPosition = new LinkedHashMap<>(); // 当前股票持仓情况
    HashMap<String, StockSummary> stockSummary = new LinkedHashMap<>(); // 用于优化止盈止损

    // 期货类-柜台模块
    LinkedHashMap<Integer, FutureOrder> futureCounter = new LinkedHashMap<>();
    Collection<FutureRecord> futureRecord = new ArrayList<>();
    LinkedHashMap<String, ArrayList<FuturePosition>> futureLongPosition = new LinkedHashMap<>(); // 当前期货多头持仓情况
    LinkedHashMap<String, ArrayList<FuturePosition>> futureShortPosition = new LinkedHashMap<>(); // 当前期货空头持仓情况
    HashMap<String, FutureSummary> futureLongSummary = new LinkedHashMap<>(); // 用于优化期货多头止盈止损
    HashMap<String, FutureSummary> futureShortSummary = new LinkedHashMap<>(); // 用于优化期货空头止盈止损

    // 评价模块
    Double oriCash;      // 初始总资金
    Double oriStockCash; // 初始股票资金
    Double oriFutureCash; // 初始期货资金
    Double oriOptionCash; // 初始期权资金
    Double cash;         // 实时总资金
    Double stockCash;   // 实时股票资金
    Double futureCash;  // 实时期货资金
    Double optionCash;  // 实时期权资金
    Double profit = 0.0; // 平仓盈亏
    Double realTimeProfit = 0.0;  // 实时盈亏
    Double profitSettle = 0.0;
    Double stockProfit = 0.0;  // 平仓收益
    Double stockRealTimeProfit = 0.0; // 实时收益
    Double futureProfit = 0.0;
    Double futureRealTimeProfit = 0.0;
    Double optionProfit = 0.0;
    Double optionRealTimeProfit = 0.0;

    LinkedHashMap<LocalDate, Double> cashDict = new LinkedHashMap<>();
    LinkedHashMap<LocalDate, Double> profitDict = new LinkedHashMap<>();
    LinkedHashMap<LocalDate, Double> settleProfitDict = new LinkedHashMap<>();
    LinkedHashMap<LocalDate, Double> posDict = new LinkedHashMap<>(); // 用于记录整体仓位水平
    Collection<Object> stockOrderRecord = new ArrayList<>();
    Collection<Object> futureOrderRecord = new ArrayList<>();

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
        this.stockCounterJson = config.getStock_bar_json();

        // 初始化股票相关配置
        this.stockKDataBase = config.getStock_bar_database();
        this.stockKTable = config.getStock_bar_table();
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
            this.startTimeStamp = LocalDateTime.of(startDate, LocalTime.of(0, 0));
        }

        if (config.getEnd_date() != null) {
            LocalDate endDate = parseDateString(config.getEnd_date());
            this.endDotDate = formatter_dot.format(endDate);
            this.endTimeStamp = LocalDateTime.of(endDate, LocalTime.of(23, 59, 59));
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

    public LinkedHashMap<Integer, HashMap<String, StockBar>> getStockKDict() {
        return stockKDict;
    }

    public void setStockKDict(LinkedHashMap<Integer, HashMap<String, StockBar>> stockKDict) {
        this.stockKDict = stockKDict;
    }

    public LinkedHashMap getStockMacroDict() {
        return stockMacroDict;
    }

    public void setStockMacroDict(LinkedHashMap stockMacroDict) {
        this.stockMacroDict = stockMacroDict;
    }

    public HashMap<String, StockInfo> getStockInfoDict() {
        return stockInfoDict;
    }

    public void setStockInfoDict(HashMap<String, StockInfo> stockInfoDict) {
        this.stockInfoDict = stockInfoDict;
    }

    public LinkedHashMap getStockSignalDict() {
        return stockSignalDict;
    }

    public void setStockSignalDict(LinkedHashMap stockSignalDict) {
        this.stockSignalDict = stockSignalDict;
    }

    public Collection<LocalTime> getStockTimeList() {
        return stockTimeList;
    }

    public void setStockTimeList(Collection<LocalTime> stockTimeList) {
        this.stockTimeList = stockTimeList;
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
    // TODO: 评估这里是HashMap 还是应该是 LinkedHashMap, 因为order_id已经是同步自增的序号了，按理说LinkedHashMap会保持插入顺序, 问题是否需要排序呢

    public void setStockCounter(LinkedHashMap<Integer, StockOrder> stockCounter) {
        this.stockCounter = stockCounter;
    }

    public LinkedHashMap<String, ArrayList<StockPosition>> getStockPosition() {
        return stockPosition;
    }

    public void setStockPosition(LinkedHashMap<String, ArrayList<StockPosition>> stockPosition) {
        this.stockPosition = stockPosition;
    }

    public HashMap<String, StockSummary> getStockSummary() {
        return stockSummary;
    }

    public void setStockSummary(HashMap<String, StockSummary> stockSummary) {
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

    public String getFutureKDataBase() {
        return futureKDataBase;
    }

    public void setFutureKDataBase(String futureKDataBase) {
        this.futureKDataBase = futureKDataBase;
    }

    public Collection<LocalDate> getFutureDateList() {
        return futureDateList;
    }

    public void setFutureDateList(Collection<LocalDate> futureDateList) {
        this.futureDateList = futureDateList;
    }

    public Collection<LocalTime> getFutureTimeList() {
        return futureTimeList;
    }

    public void setFutureTimeList(Collection<LocalTime> futureTimeList) {
        this.futureTimeList = futureTimeList;
    }

    public LinkedHashMap<Integer, HashMap<String, FutureBar>> getFutureKDict() {
        return futureKDict;
    }

    public void setFutureKDict(LinkedHashMap<Integer, HashMap<String, FutureBar>> futureKDict) {
        this.futureKDict = futureKDict;
    }

    public HashMap<String, FutureInfo> getFutureInfoDict() {
        return futureInfoDict;
    }

    public void setFutureInfoDict(HashMap<String, FutureInfo> futureInfoDict) {
        this.futureInfoDict = futureInfoDict;
    }

    public LinkedHashMap getFutureSignalDict() {
        return futureSignalDict;
    }

    public void setFutureSignalDict(LinkedHashMap futureSignalDict) {
        this.futureSignalDict = futureSignalDict;
    }

    public LinkedHashMap getFutureMacroDict() {
        return futureMacroDict;
    }

    public void setFutureMacroDict(LinkedHashMap futureMacroDict) {
        this.futureMacroDict = futureMacroDict;
    }

    public Collection<String> getFutureDotDateList() {
        return futureDotDateList;
    }

    public void setFutureDotDateList(Collection<String> futureDotDateList) {
        this.futureDotDateList = futureDotDateList;
    }

    public Collection<String> getFutureStrDateList() {
        return futureStrDateList;
    }

    public void setFutureStrDateList(Collection<String> futureStrDateList) {
        this.futureStrDateList = futureStrDateList;
    }

    public String getFutureKTable() {
        return futureKTable;
    }

    public void setFutureKTable(String futureKTable) {
        this.futureKTable = futureKTable;
    }

    public String getFutureMacroJson() {
        return futureMacroJson;
    }

    public void setFutureMacroJson(String futureMacroJson) {
        this.futureMacroJson = futureMacroJson;
    }

    public String getFutureInfoJson() {
        return futureInfoJson;
    }

    public void setFutureInfoJson(String futureInfoJson) {
        this.futureInfoJson = futureInfoJson;
    }

    public String getFutureSignalJson() {
        return futureSignalJson;
    }

    public void setFutureSignalJson(String futureSignalJson) {
        this.futureSignalJson = futureSignalJson;
    }

    public Collection<StockRecord> getStockRecord() {
        return stockRecord;
    }

    public void setStockRecord(Collection<StockRecord> stockRecord) {
        this.stockRecord = stockRecord;
    }

    public void setStockSummary(LinkedHashMap<String, StockSummary> stockSummary) {
        this.stockSummary = stockSummary;
    }

    public LinkedHashMap<Integer, FutureOrder> getFutureCounter() {
        return futureCounter;
    }

    public void setFutureCounter(LinkedHashMap<Integer, FutureOrder> futureCounter) {
        this.futureCounter = futureCounter;
    }

    public Collection<FutureRecord> getFutureRecord() {
        return futureRecord;
    }

    public void setFutureRecord(Collection<FutureRecord> futureRecord) {
        this.futureRecord = futureRecord;
    }

    public LinkedHashMap<String, ArrayList<FuturePosition>> getFutureLongPosition() {
        return futureLongPosition;
    }

    public void setFutureLongPosition(LinkedHashMap<String, ArrayList<FuturePosition>> futureLongPosition) {
        this.futureLongPosition = futureLongPosition;
    }

    public LinkedHashMap<String, ArrayList<FuturePosition>> getFutureShortPosition() {
        return futureShortPosition;
    }

    public void setFutureShortPosition(LinkedHashMap<String, ArrayList<FuturePosition>> futureShortPosition) {
        this.futureShortPosition = futureShortPosition;
    }

    public HashMap<String, FutureSummary> getFutureLongSummary() {
        return futureLongSummary;
    }

    public void setFutureLongSummary(LinkedHashMap<String, FutureSummary> futureLongSummary) {
        this.futureLongSummary = futureLongSummary;
    }

    public HashMap<String, FutureSummary> getFutureShortSummary() {
        return futureShortSummary;
    }

    public void setFutureShortSummary(LinkedHashMap<String, FutureSummary> futureShortSummary) {
        this.futureShortSummary = futureShortSummary;
    }

    public Collection<Object> getFutureOrderRecord() {
        return futureOrderRecord;
    }

    public void setFutureOrderRecord(Collection<Object> futureOrderRecord) {
        this.futureOrderRecord = futureOrderRecord;
    }

    public Double getStockCash() {
        return stockCash;
    }

    public void setStockCash(Double stockCash) {
        this.stockCash = stockCash;
    }

    public void setFutureLongSummary(HashMap<String, FutureSummary> futureLongSummary) {
        this.futureLongSummary = futureLongSummary;
    }

    public void setFutureShortSummary(HashMap<String, FutureSummary> futureShortSummary) {
        this.futureShortSummary = futureShortSummary;
    }

    public Double getOriStockCash() {
        return oriStockCash;
    }

    public void setOriStockCash(Double oriStockCash) {
        this.oriStockCash = oriStockCash;
    }

    public Double getOriFutureCash() {
        return oriFutureCash;
    }

    public void setOriFutureCash(Double oriFutureCash) {
        this.oriFutureCash = oriFutureCash;
    }

    public Double getOriOptionCash() {
        return oriOptionCash;
    }

    public void setOriOptionCash(Double oriOptionCash) {
        this.oriOptionCash = oriOptionCash;
    }

    public Double getFutureCash() {
        return futureCash;
    }

    public void setFutureCash(Double futureCash) {
        this.futureCash = futureCash;
    }

    public Double getOptionCash() {
        return optionCash;
    }

    public void setOptionCash(Double optionCash) {
        this.optionCash = optionCash;
    }

    public Double getStockProfit() {
        return stockProfit;
    }

    public void setStockProfit(Double stockProfit) {
        this.stockProfit = stockProfit;
    }

    public Double getStockRealTimeProfit() {
        return stockRealTimeProfit;
    }

    public void setStockRealTimeProfit(Double stockRealTimeProfit) {
        this.stockRealTimeProfit = stockRealTimeProfit;
    }

    public Double getFutureProfit() {
        return futureProfit;
    }

    public void setFutureProfit(Double futureProfit) {
        this.futureProfit = futureProfit;
    }

    public Double getFutureRealTimeProfit() {
        return futureRealTimeProfit;
    }

    public void setFutureRealTimeProfit(Double futureRealTimeProfit) {
        this.futureRealTimeProfit = futureRealTimeProfit;
    }

    public Double getOptionProfit() {
        return optionProfit;
    }

    public void setOptionProfit(Double optionProfit) {
        this.optionProfit = optionProfit;
    }

    public Double getOptionRealTimeProfit() {
        return optionRealTimeProfit;
    }

    public void setOptionRealTimeProfit(Double optionRealTimeProfit) {
        this.optionRealTimeProfit = optionRealTimeProfit;
    }
}
