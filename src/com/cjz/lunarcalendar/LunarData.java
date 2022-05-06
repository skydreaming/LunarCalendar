package com.cjz.lunarcalendar;


import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by cjz on 2019/11/20.
 * pattern:
 *  _________________________________________________________________________________________________
 * |         |             |              |        |                  |        |         |          |
 * | valid:1 | day count:9 | leap month:4 | leap:1 | month pattern:12 | date:5 | month:4 | year: 28 |
 * |_________|_____________|______________|________|__________________|________|_________|__________|
 */
public class LunarData {
    private static final int HEADER_LEN = 8;

    private static final int AUTO_LOAD_RADIUS = 50;


    //[FIRST_YEAR, LAST_YEAR)
    public static int FIRST_YEAR = -1;
    public static int LAST_YEAR = -1;
    private static Range SUPPORTED_RANGE;

    private static final Object lock = new Object();
    private static final ReentrantReadWriteLock readWriteLock;
    private static final ReentrantReadWriteLock.ReadLock rL;
    private static final ReentrantReadWriteLock.WriteLock wL;
    private static long[] datas;
    private static Range dataRange = new Range(0, 0);

    private static OpenFile openFile = new JavaOpenFile();

    static {
        DataInputStream in = null;
        try {

            in = new DataInputStream(new BufferedInputStream(openFile(), 8));
            FIRST_YEAR = in.readInt();
            LAST_YEAR = in.readInt();
            SUPPORTED_RANGE = new Range(FIRST_YEAR, LAST_YEAR);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(in != null){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }

    static {
        readWriteLock = new ReentrantReadWriteLock(true);
        rL = readWriteLock.readLock();
        wL = readWriteLock.writeLock();
    }

    public static boolean isSupported(int startYear, int endYear){
        return SUPPORTED_RANGE.contains(startYear, endYear);
    }


    public static boolean request(int startYear, int endYear){
        if(!SUPPORTED_RANGE.contains(startYear, endYear)){
            return false;
        }
        synchronized (lock) {
            if (dataRange.contains(startYear, endYear)) {
                return true;
            }
            try {
                readFile(startYear, endYear);
            } catch (RuntimeException e) {
                return false;
            }
            return true;
        }
    }

    public static long get(int year){
        if(!SUPPORTED_RANGE.isWithinRange(year)){
            return 0;
        }
        synchronized (lock) {
            if (!dataRange.isWithinRange(year)) {
                int start = Math.min(dataRange.start, year - AUTO_LOAD_RADIUS);
                int end = Math.max(dataRange.end, year + 1 + AUTO_LOAD_RADIUS);
                boolean b = readFile(start, end);
                if (!b) {
                    return 0;
                }
            }
            int index = year - dataRange.start;
            return datas[index];
        }
    }

    public static long getL(int year){
        synchronized (lock) {
            return get(year);
        }
    }

    private static boolean readFile(int startYear, int endYear){
//        wL.lock();
        DataInputStream in = null;
        try {

            int start, end;
            if (datas == null) {
                start = startYear;
                end = endYear;
            } else {
                start = Math.min(dataRange.start, startYear);
                end = Math.max(dataRange.end, endYear);
            }

            if(start < FIRST_YEAR){
                start = FIRST_YEAR;
            }
            if(end > LAST_YEAR){
                end = LAST_YEAR;
            }

            int offset, count;
            offset = start - FIRST_YEAR;
            count = end - start;


            InputStream inputStream = openFile();
            in = new DataInputStream(new BufferedInputStream(inputStream));

            int skip = HEADER_LEN + offset * 8;
            if(in.skip(skip) != skip){
                return false;
//                throw new RuntimeException("Error");
            }

            synchronized (lock) {
                datas = new long[count];
                dataRange = new Range(start, end);
                for (int i = 0; i < count; ++i) {
                    datas[i] = in.readLong();
                }
            }


        } catch (IOException e) {
            //log
            return false;
        } finally {
            if(in != null){
                try {
                    in.close();
                } catch (IOException e) {
                    //log
                }
            }
//            wL.unlock();
        }
        return true;
    }

    private static InputStream openFile() throws IOException {
        return openFile.openFile();
    }




    public static final int YEAR_OFFSET = 0;
    public static final long YEAR_MASK = 0xfffffffL;

    public static final int MONTH_OFFSET = 28;
    public static final long MONTH_MASK = 0xfL;//4

    public static final int DATE_OFFSET = 32;
    public static final long DATE_MASK = 0B11111L;//5

    public static final int MONTH_PATTERN_OFFSET = 37;
    public static final long MONTH_PATTERN_MASK = 0xfffL;//12

    public static final int LEAP_PATTERN_OFFSET = 49;
    public static final long LEAP_PATTERN_MASK = 0x1L;//1

    public static final int LEAP_MONTH_OFFSET = 50;
    public static final long LEAP_MONTH_MASK = 0xfL;//4

    public static final int DAYS_COUNT_OFFSET = 54;
    public static final int DAYS_COUNT_MASK = 0x1ff;//9

    public static final int VALIDATION_OFFSET = 63;
    public static final int VALIDATION_MASK = 1;

    public static int year(long v){
        return (int) (v >>> YEAR_OFFSET & YEAR_MASK);
    }

    public static int month(long v){
        return (int) (v >>> MONTH_OFFSET & MONTH_MASK);
    }

    public static int date(long v){
        return (int) (v >>> DATE_OFFSET & DATE_MASK);
    }

    public static int monthPattern(long v, int month){
        return ((v >>> MONTH_PATTERN_OFFSET) & (1 << (12 - month))) != 0 ? 1 : 0;
    }

    public static int leapPattern(long v){
        return (int) (v >>> LEAP_PATTERN_OFFSET & LEAP_PATTERN_MASK);
    }

    public static int leapMonth(long v){
        return (int) (v >>> LEAP_MONTH_OFFSET & LEAP_MONTH_MASK);
    }


    public static boolean isValid(long v){
        return (v >>> VALIDATION_OFFSET & VALIDATION_MASK) != 0;
    }

    public static int daysCount(long v){
        return (int) (v >>> DAYS_COUNT_OFFSET & DAYS_COUNT_MASK);
    }

    public static int daysCount(int year){
        long v = getL(year);
        return (int) (v >>> DAYS_COUNT_OFFSET & DAYS_COUNT_MASK);
    }

    public static int monthDaysCount(long v, int month){
        return 29 + monthPattern(v, month);
    }

    public static int leapMonthDaysCount(long v){
        return leapMonth(v) > 0 ? 29 + leapPattern(v) : 0;
    }

    public static int daysInMonth(long v, int month){
        int lm = leapMonth(v);
        if(month > 12 && month - 12 != lm){
            return -1;//err
        }
        if(month > 12){
            return leapPattern(v) + 29;
        }else {
            return monthPattern(v, month) + 29;
        }
    }

    public static int daysInMonth(int year, int month){
        long v = getL(year);
        if(!isValid(v)){
            return -1;
        }
        return daysInMonth(v, month);
    }

    public static int nextMonthInYear(long v, int curMonth){
        int lm = leapMonth(v);
        if(curMonth > 12 && curMonth - 12 != lm){
            return -1;//err
        }
        if(curMonth == 12 && lm < 12 || lm == 12 && curMonth == 24){
            return 0;
        }

        if(curMonth <= 12){
            if(curMonth == lm){
                return lm + 12;
            }
            return curMonth + 1;
        }else {
            return curMonth - 12 + 1;
        }
    }

    public static int preMonthInYear(long v, int curMonth){
        int lm = leapMonth(v);
        if(curMonth > 12 && curMonth - 12 != lm){
            return -1;
        }
        if(curMonth > 12){
            return curMonth - 12;
        }else if(lm > 0 && curMonth - 1 == lm){
            return lm + 12;
        }
        return curMonth - 1;
    }

    public static int lastMonthInYear(long v){
        if(!isValid(v)){
            return -1;
        }
        int lm = leapMonth(v);
        return lm == 12 ? 24 : 12;
    }

    public static int lastMonthInYear(int year){
        long v = getL(year);
        return lastMonthInYear(v);
    }
}
