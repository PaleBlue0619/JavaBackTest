package com.maxim.service.getdata;
// DolphinDB 模块
import com.maxim.pojo.kbar.KBar;
import com.maxim.pojo.kbar.StockBar;
import com.maxim.service.Utils;
import com.xxdb.DBConnection;
import com.xxdb.data.*;
// 目标数据结构模块
import com.maxim.service.struct.StockKBarStruct;
import com.maxim.service.struct.StockInfoStruct;
// 工具模块
import java.io.IOException;
import java.lang.Void;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.io.File;
import java.io.FileWriter;
import com.alibaba.fastjson2.JSONObject;
import com.xxdb.data.Vector;

public class fromDolphinDB{
    /*
    * 从DolphinDB中反序列化数据为指定Object
    * 设计: 传入DolphinDB配置&时间/标的范围->取BasicTable&希望装配的Bean对象
    * 每一行数据对应一个Java Bean
    * */

    public DBConnection conn;
    public String dbName;
    public String tbName;
    public Integer threadCount;

    public fromDolphinDB(){

    }
    public fromDolphinDB(DBConnection conn, String dbName, String tbName, Integer threadCount){
        this.conn = conn;       // DolphinDB连接
        this.dbName = dbName;
        this.tbName = tbName;
        this.threadCount = threadCount;
    }

    // DolphinDB -> BasicTable -> JavaBean
    // Step1. DolphinDB -> ConcurrentHashMap<LocalDate, BasicTable>
    public ConcurrentHashMap<LocalDate, BasicTable> toBasicTable(LocalDate start_date, LocalDate end_date, Collection<String> symbol_list,
                                                                 String timeCol, Boolean isTimeStampCol, String symbolCol, String...featureCols) throws IOException {
        /*
        * 将DolphinDB数据转换为BasicTable
        * 输入: DolphinDB连接, 数据库名, 表名, 起始时间, 结束时间, 标的列表, 时间列, 标的列, 特征列...
        * 输出: ConcurrentHashMap<LocalDate, BasicTable>
        * 注: 这里目前只能有一个时间列+一个symbol列去做范围限制,时间列必传,标的列可不传
        * */
        // 获取SQL脚本 & 时间信息
        BasicDateVector date_list = toBasicTableDateUtil(start_date, end_date, timeCol);
        String[] script = toBasicTableScriptUtil(symbol_list, timeCol, isTimeStampCol, symbolCol, featureCols);
        String script1 = script[0];
        String script3 = script[1];

        // 创建多线程结果接收集合
        ConcurrentHashMap<LocalDate, BasicTable> resultMap = new ConcurrentHashMap<>();
        Collection<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i=0; i<date_list.rows(); i++){  // 注: DolphinDB的JavaAPI向量/Table全部都是用rows获取维度的
            LocalDate tradeDate = date_list.getDate(i);
            String tradeDateStr = tradeDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
            String script2 = """ 
                from loadTable("%s","%s") where date(%s) == date(%s)
                """.formatted(this.dbName, this.tbName, timeCol, tradeDateStr); // 这里要将ISO标准时间转换为2020.01.01这样的dotType
            String finalScript = script1+script2+script3; // 这里要写在外边

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try{
                    // 获取结果
                    BasicTable data = (BasicTable) conn.run(finalScript, this.threadCount, this.threadCount);
                    System.out.println("Processing tradeDate: " + tradeDate);
                    // 保存到线程安全的集合
                    resultMap.put(tradeDate, data);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            });
            futures.add(future);
        }

        // 等待所有线程任务完成后, 返回结果
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return resultMap;
    }

    public ConcurrentHashMap<LocalDate, HashMap<String, BasicTable>> toBasicTableBySymbol(LocalDate start_date, LocalDate end_date, Collection<String> symbol_list,
                                                                                  String timeCol, Boolean isTimeStampCol, String symbolCol, String...featureCols) throws IOException {
        /*
         * 将DolphinDB数据转换为BasicTable, 同时每个日期内部按照标的进行分组(HashMap)
         * 输入: DolphinDB连接, 数据库名, 表名, 起始时间, 结束时间, 标的列表, 时间列, 标的列, 特征列...
         * 输出: ConcurrentHashMap<LocalDate, HashMap<String, BasicTable>>
         * */
        // 获取SQL脚本 & 时间信息
        BasicDateVector date_list = toBasicTableDateUtil(start_date, end_date, timeCol);
        String[] script = toBasicTableScriptUtil(symbol_list, timeCol, isTimeStampCol, symbolCol, featureCols);
        String script1 = script[0];
        String script3 = script[1];

        // 创建多线程结果接收集合
        ConcurrentHashMap<LocalDate, HashMap<String, BasicTable>> resultMap = new ConcurrentHashMap<>();
        Collection<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i=0; i<date_list.rows(); i++){  // 注: DolphinDB的JavaAPI向量/Table全部都是用rows获取维度的
            LocalDate tradeDate = date_list.getDate(i);
            String tradeDateStr = tradeDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
            String script2 = """ 
                from loadTable("%s","%s") where date(%s) == date(%s)
                """.formatted(this.dbName, this.tbName, timeCol, tradeDateStr); // 这里要将ISO标准时间转换为2020.01.01这样的dotType
            String finalScript = script1+script2+script3; // 这里要写在外边

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try{
                    // 获取结果
                    BasicTable data = (BasicTable) conn.run(finalScript, this.threadCount, this.threadCount);
                    System.out.println("Processing tradeDate: " + tradeDate);

                    // 获取所有唯一的标的列表
                    HashMap<String, Collection<Integer>> symbolList = new HashMap<>();
                    for (int j=0; j<data.rows(); j++){
                        String symbol = data.getColumn(symbolCol).getString(j);
                        if (!symbolList.containsKey(symbol)){
                            symbolList.put(symbol, new ArrayList<>());
                        }
                        symbolList.get(symbol).add(j);
                    }

                    // 保存结果至线程安全的集合
                    for (String symbol: symbolList.keySet()){
                        int[] indices = symbolList.get(symbol).stream().mapToInt(Integer::intValue).toArray();
                        BasicTable subData = (BasicTable) data.getSubTable(indices);
                        if (!(resultMap.containsKey(tradeDate))){
                            resultMap.put(tradeDate, new HashMap<>());
                        }
                        resultMap.get(tradeDate).put(symbol, subData);
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
            });
            futures.add(future);
        }

        // 等待所有线程任务完成后, 返回结果
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return resultMap;
    }

    public ConcurrentHashMap<LocalDate, TreeMap<LocalTime, BasicTable>> toBasicTableByTime(LocalDate start_date, LocalDate end_date, Collection<String> symbol_list,
                                                                                                 String dateCol, String timeCol, String symbolCol, String...featureCols) throws IOException {
        /*
         * 将DolphinDB数据转换为BasicTable, 同时每个时间段内部按照标的进行分组同时按照时间排序(TreeMap)
         * 输入: DolphinDB连接, 数据库名, 表名, 起始时间, 结束时间, 标的列表, 时间列, 标的列, 特征列...
         * 输出: ConcurrentHashMap<LocalDate, LinkedHashMap<LocalTime, BasicTable>>
         * */
        // 获取SQL脚本 & 时间信息
        BasicDateVector date_list = toBasicTableDateUtil(start_date, end_date, dateCol);
        String[] script = toBasicTableScriptUtil(symbol_list, dateCol, false, symbolCol, featureCols);  // 这里因为用到这个方法的场景对应的数据是一定是有分钟列的
        String script1 = script[0];
        String script3 = script[1];

        // 创建多线程结果接收集合
        ConcurrentHashMap<LocalDate, TreeMap<LocalTime, BasicTable>> resultMap = new ConcurrentHashMap<>();
        Collection<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i=0; i<date_list.rows(); i++){  // 注: DolphinDB的JavaAPI向量/Table全部都是用rows获取维度的
            LocalDate tradeDate = date_list.getDate(i);
            String tradeDateStr = tradeDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
            String script2 = """ 
                from loadTable("%s","%s") where date(%s) == date(%s)
                """.formatted(this.dbName, this.tbName, dateCol, tradeDateStr); // 这里要将ISO标准时间转换为2020.01.01这样的dotType
            String finalScript = script1+script2+script3; // 这里要写在外边

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try{
                    // 获取结果
                    BasicTable data = (BasicTable) conn.run(finalScript, this.threadCount, this.threadCount);
                    System.out.println("Processing tradeDate: " + tradeDate);

                    // 获取所有时间戳及其对应的行索引
                    HashMap<LocalTime, Collection<Integer>> timeDict = new HashMap<>();
                    for (int j=0; j<data.rows(); j++){
                        LocalTime time = LocalTime.parse(data.getColumn(timeCol).getString(j));
                        if (!timeDict.containsKey(time)){
                            timeDict.put(time, new ArrayList<>());
                        }
                        timeDict.get(time).add(j);
                    }

                    // 保存结果至线程安全的集合
                    for (LocalTime time: timeDict.keySet()){
                        int[] indices = timeDict.get(time).stream().mapToInt(Integer::intValue).toArray();
                        BasicTable subData = (BasicTable) data.getSubTable(indices);
                        if (!(resultMap.containsKey(tradeDate))){
                            resultMap.put(tradeDate, new TreeMap<>());
                        }
                        resultMap.get(tradeDate).put(time, subData);
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
            });
            futures.add(future);
        }

        // 等待所有线程任务完成后, 返回结果
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return resultMap;
    }

    // Step2. BasicTable -> JavaBean Your Needed
    // 这里一定是按照日期进行Map的
    public <T> ConcurrentHashMap<LocalDate, List<T>> toJavaBeans(ConcurrentHashMap<LocalDate, BasicTable> dataMap,
                                                             Class<?> clazz, HashMap<String, String> transMap) throws IOException {
        /* 将BasicTable转换为JavaBean
        * transMap: 键为DolphinDB列名, 值为JavaBean属性名
        * */

        // 创建多线程结果接收集合
        ConcurrentHashMap<LocalDate, List<T>> resultMap = new ConcurrentHashMap<>();
        Collection<CompletableFuture<Void>> futures = new ArrayList<>();

        // 多线程遍历所有日期的数据
        for (LocalDate tradeDate : dataMap.keySet()){
            BasicTable data = dataMap.get(tradeDate);  // 取出这个日期的数据
            List<T> beanList = new ArrayList<>();
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // 遍历每一行数据
                    for (int i = 0; i < data.rows(); i++) {
                        // 为每一行创建新的JavaBean实例
                        T instance = (T) clazz.getDeclaredConstructor().newInstance();

                        // 遍历transMap进行字段映射
                        for (Map.Entry<String, String> transEntry : transMap.entrySet()) {
                            String colName = transEntry.getKey();    // DolphinDB列名
                            String beanFieldName = transEntry.getValue(); // JavaBean属性名

                            // 检查BasicTable是否包含该列
                            if (data.getColumn(colName) != null) {
                                // 获取列数据的第i行
                                Entity columnData = data.getColumn(colName).get(i);
                                // 使用反射设置JavaBean属性
                                setBeanField(instance, beanFieldName, columnData);
                            }
                        }
                        // 将填充好的实例添加到列表中
                        beanList.add(instance);
                    }
                    System.out.println("processing JavaBean tradeDate"+tradeDate);
                    resultMap.put(tradeDate, beanList);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            futures.add(future);  // 添加线程任务
        }

        // 等待所有线程任务完成后, 返回结果
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return resultMap;
    }

    public <T> ConcurrentHashMap<LocalDate, HashMap<String, T>> toJavaBeansBySymbol(ConcurrentHashMap<LocalDate, BasicTable> dataMap, String symbolCol,
                                                                Class<?> clazz, HashMap<String, String> transMap) throws IOException {
        /* 将BasicTable转换为JavaBean
         * transMap: 键为DolphinDB列名, 值为JavaBean属性名
         * */

        // 创建多线程结果接收集合
        ConcurrentHashMap<LocalDate, HashMap<String, T>> resultMap = new ConcurrentHashMap<>();
        Collection<CompletableFuture<Void>> futures = new ArrayList<>();

        // 多线程遍历所有日期的数据
        for (LocalDate tradeDate : dataMap.keySet()){
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    BasicTable data = dataMap.get(tradeDate);  // 取出这个日期的数据
                    for (int i = 0; i < data.rows(); i++){
                        String symbol = data.getColumn(symbolCol).getString(i);
                        // 为每一行创建新的JavaBean实例
                        T instance = (T) clazz.getDeclaredConstructor().newInstance();

                        // 遍历transMap进行字段映射
                        for (Map.Entry<String, String> transEntry : transMap.entrySet()) {
                            String colName = transEntry.getKey();    // DolphinDB列名
                            String beanFieldName = transEntry.getValue(); // JavaBean属性名

                            // 检查BasicTable是否包含该列
                            if (data.getColumn(colName) != null) {
                                // 获取列数据的第i行
                                Entity columnData = data.getColumn(colName).get(i);
                                // 使用反射设置JavaBean属性
                                setBeanField(instance, beanFieldName, columnData);
                            }
                        }
                        resultMap.putIfAbsent(tradeDate, new HashMap<>());
                        resultMap.get(tradeDate).put(symbol, instance);
                    }
                    System.out.println("processing JavaBean tradeDate"+tradeDate);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            futures.add(future);  // 添加线程任务
        }

        // 等待所有线程任务完成后, 返回结果
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return resultMap;
    }

    public <T> ConcurrentHashMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>> toJavaBeansByTime(ConcurrentHashMap<LocalDate, BasicTable> dataMap,
                                                                                                      String symbolCol, String timeCol, Class<?> clazz, HashMap<String, String> transMap) throws IOException {
        /* 将BasicTable转换为JavaBean
         * transMap: 键为DolphinDB列名, 值为JavaBean属性名
         * */

        // 创建多线程结果接收集合
        ConcurrentHashMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>> resultMap = new ConcurrentHashMap<>();
        Collection<CompletableFuture<Void>> futures = new ArrayList<>();

        // 多线程遍历所有日期的数据
        for (LocalDate tradeDate : dataMap.keySet()) {
            resultMap.putIfAbsent(tradeDate, new TreeMap<>());
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    BasicTable data = dataMap.get(tradeDate);
                    // 获取所有时间戳及其对应的行索引
                    HashMap<LocalTime, Collection<Integer>> timeDict = new HashMap<>();
                    for (int j = 0; j < data.rows(); j++) {
                        LocalTime time = LocalTime.parse(data.getColumn(timeCol).getString(j));
                        if (!timeDict.containsKey(time)) {
                            timeDict.put(time, new ArrayList<>());
                        }
                        timeDict.get(time).add(j);
                    }
                    for (LocalTime time : timeDict.keySet()) {
                        resultMap.get(tradeDate).putIfAbsent(time, new HashMap<>());

                        int[] indices = timeDict.get(time).stream().mapToInt(i -> i).toArray();
                        BasicTable df = (BasicTable) data.getSubTable(indices);
                        // 遍历这一分钟的所有行(每行对应一个标的)
                        for (int i = 0; i < df.rows(); i++) {
                            // 为每一行创建新的JavaBean实例
                            T instance = (T) clazz.getDeclaredConstructor().newInstance();

                            String symbol = df.getColumn(symbolCol).getString(i);
                            for (Map.Entry<String, String> transEntry : transMap.entrySet()) {
                                String colName = transEntry.getKey();
                                String beanFieldName = transEntry.getValue();
                                Entity columnData = df.getColumn(colName).get(i);
                                setBeanField(instance, beanFieldName, columnData);
                            }
                            resultMap.get(tradeDate).get(time).put(symbol, instance);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            futures.add(future);
        }

        // 等待所有线程任务完成后, 返回结果
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return resultMap;
    }

    public <T> ConcurrentHashMap<LocalDate, HashMap<String, T>> toJavaBeanBySymbol(ConcurrentHashMap<LocalDate, HashMap<String, BasicTable>> dataMap, String symbolCol,
                                                             Class<?> clazz, HashMap<String, String> transMap) throws IOException {
        /* 将BasicTable转换为JavaBean, 按照标的列(symbolCol列)进行Map
        * transMap: 键为DolphinDB列名, 值为JavaBean属性名
        * */
        ConcurrentHashMap<LocalDate, HashMap<String, T>> resultMap = new ConcurrentHashMap<>();
        Collection<CompletableFuture<Void>> futures = new ArrayList<>();

        // 多线程遍历所有日期的数据
        for (LocalDate tradeDate : dataMap.keySet()){
            HashMap<String, BasicTable> dataDict = dataMap.get(tradeDate);
            HashMap<String, T> partMap = new HashMap<>();
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // 遍历所有的标的列表
                    for (String symbol: dataDict.keySet()){
                        BasicTable data = dataDict.get(symbol);
                        // 为每一个标的创建一个新的JavaBean实例
                        T instance = (T) clazz.getDeclaredConstructor().newInstance();

                        // 遍历transMap进行字段映射
                        for (Map.Entry<String, String> transEntry : transMap.entrySet()) {
                            String colName = transEntry.getKey();    // DolphinDB列名
                            String beanFieldName = transEntry.getValue(); // JavaBean属性名

                            // 检查BasicTable是否包含该列
                            if (data.getColumn(colName) != null) {
                                // 获取列数据的第i行
                                Entity columnData = data.getColumn(colName).get(0);
                                // 使用反射设置JavaBean属性
                                setBeanField(instance, beanFieldName, columnData);
                            }
                        }
                        partMap.put(symbol, instance);
                    }
                    resultMap.put(tradeDate, partMap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            futures.add(future);
        }
        // 等待所有线程任务完成后, 返回结果
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return resultMap;
    }

    public <T> ConcurrentHashMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>> toJavaBeanByTime(ConcurrentHashMap<LocalDate, TreeMap<LocalTime, BasicTable>> dataMap, String symbolCol,
                                                                                                     Class<?> clazz, HashMap<String, String> transMap) throws IOException {
        /* 将BasicTable转换为JavaBean
         * transMap: 键为DolphinDB列名, 值为JavaBean属性名
         * */

        // 创建多线程结果接收集合
        ConcurrentHashMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>> resultMap = new ConcurrentHashMap<>();
        Collection<CompletableFuture<Void>> futures = new ArrayList<>();

        // 多线程遍历所有日期的数据
        for (LocalDate tradeDate : dataMap.keySet()){
            resultMap.putIfAbsent(tradeDate, new TreeMap<>());

            Collection<LocalTime> time_list = dataMap.get(tradeDate).keySet();
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    for (LocalTime tradeTime: time_list){
                        resultMap.get(tradeDate).putIfAbsent(tradeTime, new HashMap<>());

                        BasicTable data = dataMap.get(tradeDate).get(tradeTime);
                        // 遍历每一行数据(每行对应一个标的)
                        for (int i = 0; i < data.rows(); i++){
                            // 为每一行创建一个新的JavaBean实例
                            T instance = (T) clazz.getDeclaredConstructor().newInstance();
                            String symbol = data.getColumn(symbolCol).get(i).getString(); // 获取当前行的标的代码
                            // 遍历transMap进行字段映射
                            for (Map.Entry<String, String> transEntry : transMap.entrySet()) {
                                String colName = transEntry.getKey();
                                String beanFieldName = transEntry.getValue();

                                // 检查BasicTable是否包含该列
                                if (data.getColumn(colName) != null) {
                                    Entity columnData = data.getColumn(colName).get(i);
                                    setBeanField(instance, beanFieldName, columnData); // 利用反射设置JavaBean属性
                                }
                            }
                            resultMap.get(tradeDate).get(tradeTime).put(symbol, instance);
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            });
            futures.add(future);
        }
        // 等待所有线程任务完成后, 返回结果
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return resultMap;
    }

    /*工具方法*/
    public String[] toBasicTableScriptUtil(Collection<String> symbol_list, String timeCol, Boolean isTimestampCol, String symbolCol, String...featureCols){
        /* 生成SQL字符串的逻辑
         * 转换字符串为DolphinDB List str, 并拼接SQL脚本
         * isTimeStampCol: 表示输入的timeCol是否为时间戳列
         * isTimeStampCol=True: 需要拆分为TradeDate和TradeTime[这里列名写死, 叫你不听话增加我的开发难度]
         * isTimeStampCol=False: 说明用户认为原来的列里面有timeCol, 后续处理能够正常处理
        * */
        if (featureCols == null){
            throw new NullPointerException("特征列不允许为空");
        }
        String script1;
        String feature_list_str = Utils.arrayToDolphinDBStrSplit(List.of(featureCols));  // 这里用List.of(),将String[]转换为Collection<String>
        if (!isTimestampCol){ // 说明是日期列+分钟列的组合<推荐, 棒棒哒>
            script1 = "select %s,%s,%s ".formatted(symbolCol, timeCol, feature_list_str);
            System.out.println("featureList: " + feature_list_str);
        }else{ // 拆成日期列TradeDate+分钟列TradeTime
            script1 = "select %s, %s.date() as `TradeDate, %s.time() as `TradeTime,%s ".formatted(symbolCol, timeCol, timeCol, feature_list_str);
        }

        String script3 = "";
        if (symbol_list != null){
            String symbol_list_str = Utils.arrayToDolphinDBString(symbol_list);
            script3 = "and %s in ".formatted(symbolCol)+symbol_list_str;
            System.out.println("symbolList: " + symbol_list_str);
        }else{
            script3 = "";
            System.out.println("symbolList: null");
        }
        return new String[]{script1, script3};
    }

    public BasicDateVector toBasicTableDateUtil(LocalDate start_date, LocalDate end_date, String timeCol) throws IOException {
        // LocalDate -> String Dot
        String startDotDate = start_date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        String endDotDate = end_date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));

        BasicDateVector date_list = (BasicDateVector) conn.run("""
                t = select count(*) as count from loadTable("%s", "%s") where date(%s) between date(%s) and date(%s) group by date(%s) as %s; 
                exec %s from t
                """.formatted(this.dbName, this.tbName, timeCol, startDotDate, endDotDate, timeCol, timeCol, timeCol));
        System.out.println("dateList: " + date_list.getString());
        return date_list;
    }


    // 利用DolphinDB 中的类型反射设置JavaBean属性
    // 辅助方法：使用反射设置JavaBean属性
    private void setBeanField(Object instance, String fieldName, Entity columnData) throws Exception {
    Class<?> clazz = instance.getClass();

    // 查找字段
    java.lang.reflect.Field field = null;
    try {
        field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
    } catch (NoSuchFieldException e) {
        // 如果找不到字段，尝试通过setter方法
        handleWithSetter(clazz, instance, fieldName, columnData);
        return;
    }

    // 获取目标字段类型并进行类型转换
    Class<?> fieldType = field.getType();
    Object value = convertToFieldType(columnData, fieldType);

    try {
        field.set(instance, value);
    } catch (IllegalArgumentException e) {
        System.err.println("Warning: Cannot set field '" + fieldName + "' to field type " + fieldType.getSimpleName());
    }
}

    private Object convertToFieldType(Entity entity, Class<?> fieldType) {
        try {
            if (fieldType == String.class) {
                return entity.getString();
            } else if (fieldType == double.class || fieldType == Double.class) {
                if (entity instanceof BasicDouble) {
                    return ((BasicDouble) entity).getDouble();
                } else if (entity instanceof BasicInt) {
                    return (double) ((BasicInt) entity).getInt();
                } else {
                    String str = entity.getString();
                    return str.isEmpty() ? 0.0 : Double.parseDouble(str);
                }
            } else if (fieldType == int.class || fieldType == Integer.class) {
                if (entity instanceof BasicInt) {
                    return ((BasicInt) entity).getInt();
                } else if (entity instanceof BasicDouble) {
                    return (int) ((BasicDouble) entity).getDouble();
                } else {
                    String str = entity.getString();
                    return str.isEmpty() ? 0 : Integer.parseInt(str);
                }
            } else if (fieldType == LocalDate.class && entity instanceof BasicDate) {
                return ((BasicDate) entity).getDate();
            } else if (fieldType == LocalTime.class && entity instanceof BasicTime) {
                return ((BasicTime) entity).getTime();
            } else {
                // 其他情况尝试通用转换
                return convertGeneric(entity, fieldType);
            }
        } catch (Exception e) {
            System.err.println("Conversion error: " + e.getMessage());
            return getDefaultFieldValue(fieldType);
        }
    }

    private Object convertGeneric(Entity entity, Class<?> fieldType) {
        String strValue = entity.getString();
        if (strValue.isEmpty()) {
            return getDefaultFieldValue(fieldType);
        }

        try {
            if (fieldType == double.class || fieldType == Double.class) {
                return Double.parseDouble(strValue);
            } else if (fieldType == int.class || fieldType == Integer.class) {
                return Integer.parseInt(strValue);
            } else if (fieldType == long.class || fieldType == Long.class) {
                return Long.parseLong(strValue);
            } else if (fieldType == LocalDate.class && entity instanceof BasicDate){
                return ((BasicDate) entity).getDate();
            } else if (fieldType == LocalTime.class && entity instanceof BasicTime){
                return ((BasicTime) entity).getTime();
            } else {
                return strValue;
            }
        } catch (NumberFormatException e) {
            return strValue; // 如果无法转换数字，返回原始字符串
        }
    }

    private Object getDefaultFieldValue(Class<?> fieldType) {
        if (fieldType == double.class || fieldType == Double.class) return 0.0;
        if (fieldType == int.class || fieldType == Integer.class) return 0;
        if (fieldType == long.class || fieldType == Long.class) return 0L;
        if (fieldType == float.class || fieldType == Float.class) return 0.0f;
        if (fieldType == boolean.class || fieldType == Boolean.class) return false;
        return null;
    }

    private void handleWithSetter(Class<?> clazz, Object instance, String fieldName, Entity columnData) {
        // 简化处理setter方法
        System.err.println("Setter method handling not implemented for field: " + fieldName);
    }

}
