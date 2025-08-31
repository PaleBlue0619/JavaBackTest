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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.io.File;
import java.io.FileWriter;
import com.alibaba.fastjson2.JSONObject;

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

    public static void main(String[] args) throws IOException {
        // 测试类
        String HOST = "172.16.0.184";
        int PORT = 8001;
        String USERNAME = "maxim";
        String PASSWORD = "dyJmoc-tiznem-1figgu";
        DBConnection conn = new DBConnection();
        conn.connect(HOST, PORT, USERNAME, PASSWORD);
        conn.login(USERNAME, PASSWORD, true);

        fromDolphinDB test = new fromDolphinDB(conn, "dfs://MinKDB", "Min1K", 4);
        String[] symbol_list = new String[]{"000001.SZ", "000002.SZ", "000004.SZ", "000005.SZ", "000006.SZ", "000007.SZ", "000008.SZ", "000009.SZ", "000010.SZ", "000011.SZ", "000012.SZ", "000013.SZ", "000014.SZ", "000015.SZ", "000016.SZ", "000017.SZ", "000018.SZ"};
        ConcurrentHashMap<LocalDate, BasicTable> data = test.toBasicTable(
                LocalDate.parse("2023.02.01", DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                LocalDate.parse("2023.02.03", DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                List.of(symbol_list),
                 "tradeDate", "code", "tradeTime","open", "high", "low", "close", "volume");
        HashMap<String, String> transMap = new HashMap<>();
        List<String> featureName = Arrays.asList(new String[]{"code","tradeDate","tradeTime","open", "high", "low", "close","volume"});
        List<String> structName = Arrays.asList(new String[]{"symbol","tradeDate","tradeTime","open", "high", "low", "close","volume"});
        for (int i=0; i<featureName.size(); i++){
            transMap.put(featureName.get(i), structName.get(i));
        }
        ConcurrentHashMap<LocalDate, List<KBar>> result =
                test.toJavaBean(data, KBar.class, transMap);
        for (LocalDate date: result.keySet()){
            List<KBar> kbarList = result.get(date);
            for (KBar kbar: kbarList){
                System.out.println(kbar.symbol + " " + kbar.tradeDate + " " + kbar.tradeTime + " " + kbar.open + " " + kbar.high + " " + kbar.low + " " + kbar.close + " " + kbar.volume);
            }
        }
    }


    // DolphinDB -> BasicTable -> JavaBean
    // Step1. DolphinDB -> ConcurrentHashMap<LocalDate, BasicTable>
    public ConcurrentHashMap<LocalDate, BasicTable> toBasicTable(LocalDate start_date, LocalDate end_date, Collection<String> symbol_list,
                                                                 String dateCol, String symbolCol, String...featureCols) throws IOException {
        /*
        * 将DolphinDB数据转换为BasicTable
        * 输入: DolphinDB连接, 数据库名, 表名, 起始时间, 结束时间, 标的列表, 时间列, 标的列, 特征列...
        * 输出: ConcurrentHashMap<LocalDate, BasicTable>
        * 注: 这里目前只能有一个时间列+一个symbol列去做范围限制,时间列必传,标的列可不传
        * */

        // 转换字符串为DolphinDB List str, 并拼接SQL脚本
        if (featureCols == null){
            throw new NullPointerException("特征列不允许为空");
        }
        String feature_list_str = Utils.arrayToDolphinDBStrSplit(List.of(featureCols));  // 这里用List.of(),将String[]转换为Collection<String>
        String script1 = "select %s,%s,%s ".formatted(symbolCol, dateCol, feature_list_str);
        System.out.println("featureList: " + feature_list_str);

        String script3 = "";
        if (symbol_list != null){
            String symbol_list_str = Utils.arrayToDolphinDBString(symbol_list);
            script3 = "and %s in ".formatted(symbolCol)+symbol_list_str;
            System.out.println("symbolList: " + symbol_list_str);
        }else{
            script3 = "";
            System.out.println("symbolList: null");
        }

        // 获取所有时间
        // LocalDate -> String Dot
        String startDotDate = start_date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        String endDotDate = end_date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));

        BasicDateVector date_list = (BasicDateVector) conn.run("""
                t = select count(*) as count from loadTable("%s", "%s") where %s between date(%s) and date(%s) group by %s order by %s; 
                exec %s from t
                """.formatted(this.dbName, this.tbName, dateCol, startDotDate, endDotDate, dateCol, dateCol, dateCol));
        System.out.println("dateList: " + date_list.getString());

        // 创建多线程结果接收集合
        ConcurrentHashMap<LocalDate, BasicTable> resultMap = new ConcurrentHashMap<>();
        Collection<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i=0; i<date_list.rows(); i++){  // 注: DolphinDB的JavaAPI向量/Table全部都是用rows获取维度的
            LocalDate tradeDate = date_list.getDate(i);
            String tradeDateStr = tradeDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
            String script2 = """ 
                from loadTable("%s","%s") where %s == date(%s)
                """.formatted(this.dbName, this.tbName, dateCol, tradeDateStr); // 这里要将ISO标准时间转换为2020.01.01这样的dotType
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

    // Step2. BasicTable -> JavaBean Your Needed
    // 这里一定是按照日期进行Map的
    public <T> ConcurrentHashMap<LocalDate, List<T>> toJavaBean(ConcurrentHashMap<LocalDate, BasicTable> dataMap,
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

    /*独有工具方法*/
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
