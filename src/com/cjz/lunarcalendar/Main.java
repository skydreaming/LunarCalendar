package com.cjz.lunarcalendar;

import java.util.Calendar;

public class Main {
    public static void main(String[] args) {
        System.out.println(LunarData.FIRST_YEAR + "-" + LunarData.LAST_YEAR);
        LunarData.request(LunarData.FIRST_YEAR, LunarData.FIRST_YEAR +10);

        Calendar c = LunarCalendar.getInstance();

        System.out.println(c.get(Calendar.YEAR) + " " + c.get(Calendar.MONTH) + "  " + c.get(Calendar.DAY_OF_MONTH));

    }
}
