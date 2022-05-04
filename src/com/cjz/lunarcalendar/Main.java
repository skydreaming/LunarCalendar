package com.cjz.lunarcalendar;

import java.util.Calendar;

public class Main {
    public static void main(String[] args) {
        LunarData.request(LunarData.FIRST_YEAR, LunarData.LAST_YEAR);

        Calendar c = LunarCalendar.getInstance();
        System.out.println(c.getMinimalDaysInFirstWeek());
//        c.setMinimalDaysInFirstWeek(4);


        System.out.println(c.get(Calendar.YEAR) + " " + c.get(Calendar.MONTH) + "  " + c.get(Calendar.DAY_OF_MONTH));
    }
}
