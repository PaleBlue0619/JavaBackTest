package com.maxim.pojo;

// Java Bean
public class Config {
    // Basic Config
    private String data_type;
    private String start_date;
    private String end_date;
    private String strategy_name;
    private Double cash;
    private Integer seed;
    private Boolean run_stock;
    private Boolean run_future;
    private Boolean run_option;

    // STOCK DATABASE
    private String stock_K_database;
    private String stock_K_table;
    private String stock_counter_json;
    private String stock_signal_database;
    private String stock_signal_table;
    private String stock_signal_json;
    private String stock_info_json;
    private String stock_macro_json;

    // FUTURE DATABASE
    private String future_K_database;
    private String future_K_table;
    private String future_counter_json;
    private String future_signal_database;
    private String future_signal_table;
    private String future_signal_json;
    private String future_info_json;
    private String future_macro_json;

    // OPTION DATABASE
    private String option_K_database;
    private String option_K_table;
    private String option_counter_json;
    private String option_signal_database;
    private String option_signal_table;
    private String option_signal_json;
    private String option_info_json;
    private String option_macro_json;

    // Getters
    public String getHost() { return "localhost"; } // 如果需要默认值
    public int getPort() { return 8848; } // 如果需要默认值
    public String getUsername() { return "admin"; } // 如果需要默认值
    public String getPassword() { return "123456"; } // 如果需要默认值

    public String getData_type() { return data_type; }
    public String getStart_date() { return start_date; }
    public String getEnd_date() { return end_date; }
    public String getStrategy_name() { return strategy_name; }
    public Double getCash() { return cash; }
    public Integer getSeed() { return seed; }
    public Boolean getRun_stock() { return run_stock; }
    public Boolean getRun_future() { return run_future; }
    public Boolean getRun_option() { return run_option; }

    // STOCK GETTERS
    public String getStock_K_database() { return stock_K_database; }
    public String getStock_K_table() { return stock_K_table; }
    public String getStock_counter_json() { return stock_counter_json; }
    public String getStock_signal_database() { return stock_signal_database; }
    public String getStock_signal_table() { return stock_signal_table; }
    public String getStock_signal_json() { return stock_signal_json; }
    public String getStock_info_json() { return stock_info_json; }
    public String getStock_macro_json() { return stock_macro_json; }

    // FUTURE GETTERS
    public String getFuture_K_database() { return future_K_database; }
    public String getFuture_K_table() { return future_K_table; }
    public String getFuture_counter_json() { return future_counter_json; }
    public String getFuture_signal_database() { return future_signal_database; }
    public String getFuture_signal_table() { return future_signal_table; }
    public String getFuture_signal_json() { return future_signal_json; }
    public String getFuture_info_json() { return future_info_json; }
    public String getFuture_macro_json() { return future_macro_json; }

    // OPTION GETTERS
    public String getOption_K_database() { return option_K_database; }
    public String getOption_K_table() { return option_K_table; }
    public String getOption_counter_json() { return option_counter_json; }
    public String getOption_signal_database() { return option_signal_database; }
    public String getOption_signal_table() { return option_signal_table; }
    public String getOption_signal_json() { return option_signal_json; }
    public String getOption_info_json() { return option_info_json; }
    public String getOption_macro_json() { return option_macro_json; }
}
