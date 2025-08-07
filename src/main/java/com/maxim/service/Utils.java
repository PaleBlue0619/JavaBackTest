package com.maxim.service;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Utils{
    public static boolean deleteFileDir(String fileDir){
        // 删除文件夹
        File file = new File(fileDir);
        if (file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteFileDir(f.getAbsolutePath());
                    } else {
                        f.delete();
                    }
                }
            }
            return file.delete();
        }
        return false;
    }
    public static String arrayToDolphinDBString(ArrayList<String> array) {
        /*
        输入：String[] arr = {"c1","c2"}
        输出：["c1","c2"]
        */
        String[] arr = new String[array.size()];
        for (int i=0; i<array.size(); i++){
            arr[i] = array.get(i);
        }
        return "[" + Arrays.stream(arr)
                .map(s -> "'" + s + "'")
                .collect(Collectors.joining(", ")) + "]";
    }

    public static ArrayList<Integer> getMinuteList(String marketType) {
        ArrayList<Integer> minute_list = new ArrayList<>();
        if (marketType.toLowerCase().equals("sse") || marketType.toLowerCase().equals("szse")) {
            // 添加上午交易时间 9:30-11:30
            for (int hour = 9; hour <= 11; hour++) {
                int startMinute = (hour == 9) ? 30 : 0;
                int endMinute = (hour == 11) ? 30 : 59;

                for (int minute = startMinute; minute <= endMinute; minute++) {
                    minute_list.add(hour * 100 + minute);
                }
            }

            // 添加下午交易时间 13:00-15:00
            for (int hour = 13; hour < 15; hour++) {
                for (int minute = 0; minute <= 59; minute++) {
                    minute_list.add(hour * 100 + minute);
                }
            }

            // 单独添加15:00
            minute_list.add(1500);

        }
        return minute_list;
    }
}