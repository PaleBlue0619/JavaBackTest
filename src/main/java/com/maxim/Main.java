package com.maxim;

import java.util.ArrayList;
import java.util.Iterator;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
//        System.out.printf("Hello and welcome!");
//
//        for (int i = 1; i <= 5; i++) {
//            //TIP Press <shortcut actionId="Debug"/> to start debugging your code. We have set one <icon src="AllIcons.Debugger.Db_set_breakpoint"/> breakpoint
//            // for you, but you can always add more by pressing <shortcut actionId="ToggleLineBreakpoint"/>.
//            System.out.println("i = " + i);
//        }
        ArrayList<String> L = new ArrayList<>();
        L.add("c"+1);
        L.add("c"+2);
        L.add("c"+3);

//        Iterator<String> iterator = L.iterator();
//
//        while (iterator.hasNext()){
//            String i = iterator.next();
//            System.out.println(i);
//            iterator.remove(); // 安全移除元素
//            // System.out.println(iterator.next());
//        }

         int i = 0;
         while (i < L.size()) {
            String item = L.get(i);
            System.out.println(item);

            // 根据条件判断是否移除元素
            if (true) {
                L.remove(i);
                // 不增加i，因为删除元素后下一个元素会自动移到当前位置
            } else {
                i++; // 只有不删除元素时才增加索引
            }
         }


//        for (int i=0; i<L.size(); i++){
//            System.out.println(i);
//            L.remove(i);
//        }

    }


}