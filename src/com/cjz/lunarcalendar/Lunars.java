package com.cjz.lunarcalendar;

public class Lunars {

    public static final int YEAR_START = LunarData.FIRST_YEAR + 1;
    public static final int YEAR_END = LunarData.LAST_YEAR;


    private static final int[] DAYS_AT_THE_END_OF_MONTH = new int[]{31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365};
    private static final int[] DAYS_AT_THE_END_OF_MONTH_LEAP;

    static {
        DAYS_AT_THE_END_OF_MONTH_LEAP = new int[12];
        DAYS_AT_THE_END_OF_MONTH_LEAP[0] = DAYS_AT_THE_END_OF_MONTH[0];
        for(int i = 1; i < DAYS_AT_THE_END_OF_MONTH.length; ++i){
            DAYS_AT_THE_END_OF_MONTH_LEAP[i] = DAYS_AT_THE_END_OF_MONTH[i] + 1;
        }
    }

    private static int[] END_OF_MONTH(int year){
        return isLeapYear(year) ? DAYS_AT_THE_END_OF_MONTH_LEAP : DAYS_AT_THE_END_OF_MONTH;
    }

    public static int getDayDiff(int fromY, int fromM, int fromD, int toY, int toM, int toD){
        int daysFrom = getDays(fromY, fromM, fromD);
        int daysTo = getDays(toY, toM, toD);

        int fromOffset = getDaysOfYear(fromY) - daysFrom;
        int toOffset = getDaysOfYear(toY) - daysTo;

        int years = toY - fromY;

        int sum = years * 365;

        if(years > 0) {
            for(int y = fromY + 1; y <= toY; ++y){
                if(isLeapYear(y)){
                    ++sum;
                }
            }
        }else if(years < 0) {
            for(int y = fromY; y > toY; --y) {
                if(isLeapYear(y)) {
                    --sum;
                }
            }
        }

        sum -= toOffset;
        sum += fromOffset;
        return sum;
    }


    public static boolean getLunarDate(int[] in, int[] out){
        int year = in[0];
        long data = 0, lastData = 0;
        LunarData.lock();
        try {
            data = LunarData.get(year);
            lastData = LunarData.get(year - 1);
        }finally {
            LunarData.unlock();
        }
        if(LunarData.isValid(data) && LunarData.isValid(lastData)){
            cast(in, data, lastData, out);
            return true;
        }
        return false;
    }

    public static boolean getSolarDate(int[] in, int[] out){
        final int ly = in[0], lm = in[1], ld = in[2];
        final long data;
        LunarData.lock();
        try{
            data = LunarData.get(ly);
        }finally {
            LunarData.unlock();
        }

        if(LunarData.isValid(data)){
            int lunarOffset = 0;

            int leapMonth = LunarData.leapMonth(data);
            boolean isLeap = lm > 12;
            final int lmon = isLeap ? lm - 12 : lm;

            if(isLeap && leapMonth != lmon){
                return false;
            }

            for(int i = 1; i < lmon; ++i){
                lunarOffset += 29 + LunarData.monthPattern(data, i);
            }
            lunarOffset += ld;
            if(isLeap){
                lunarOffset += 29 + LunarData.monthPattern(data, lmon);
            }else {
                if(leapMonth > 0 && leapMonth < lmon){
                    lunarOffset += 29 + LunarData.leapPattern(data);
                }
            }

            int days = getDays(ly, LunarData.month(data), LunarData.date(data));//农历1.1在公历当年是第几天

            int offset = days + lunarOffset - 1;
            return getDate(offset, ly, out);
        }
        return false;
    }

    public static boolean isYearSupported(int year){
        return year >= YEAR_START && year < YEAR_END;
    }

    public static boolean addLunarYear(int[] in, final int amount, int[] out){
        if(amount == 0){
            out[0] = in[0];
            out[1] = in[1];
            out[2] = in[2];
            return true;
        }
        final int ly = in[0], lm = in[1], ld = in[2];
        int ty = ly + amount, tm = lm, td = ld;
        if(!isYearSupported(ty)){
            return false;
        }
        long data = LunarData.getL(ty);
        if(tm > 12 && LunarData.leapMonth(data) != tm - 12){
            tm -= 12;
        }
        if(td > 29 && (tm > 12 && LunarData.leapPattern(data) == 0 || tm <= 12 && LunarData.monthPattern(data, tm) == 0)){
            td = 29;
        }

        out[0] = ty;
        out[1] = tm;
        out[2] = td;
        return true;
    }

    public static boolean rollLunarYear(int[] in, final int amount, int[] out){
        if(amount == 0){
            out[0] = in[0];
            out[1] = in[1];
            out[2] = in[2];
            return true;
        }
        final int ly = in[0], lm = in[1], ld = in[2];
        int ty = ly + amount, tm = lm, td = ld;
        if(!isYearSupported(ty)){
            return false;
        }
        long data = LunarData.getL(ty);
        if(tm > 12 && LunarData.leapMonth(data) != tm - 12){//如果当年当月为闰月，而且不等于下一年的闰年（如果有）
            tm -= 12;
        }
        if(td > 29 && (tm > 12 && LunarData.leapPattern(data) == 0 || tm <= 12 && LunarData.monthPattern(data, tm) == 0)){
            if(tm < 12 || tm > 12 && tm < 24){//为了与jdk里的默认Calendar的行为保持一致
                ++tm;
                td = 1;
            }else {
                td = 29;
            }
        }

        out[0] = ty;
        out[1] = tm;
        out[2] = td;
        return true;
    }

    public static boolean addLunarMonth(int[] in, final int amount, int[] out){
        if(amount == 0){
            out[0] = in[0];
            out[1] = in[1];
            out[2] = in[2];
            return true;
        }
        final int ly = in[0], lm = in[1], ld = in[2];

        if(!isYearSupported(ly)){
            return false;
        }

        int ty = ly, tm = lm, td = ld;

        LunarData.lock();

        final int dir = amount > 0 ? 1 : -1;
        int count = 0;
        try{
            long data = LunarData.get(ty);
            int leapMonth = LunarData.leapMonth(data);
            if(tm > 12 && leapMonth != tm - 12){
                //log err
                return false;
            }
            if(dir > 0) {
                if(tm > 12){
                    count += 12 - (tm - 12);
                }else {
                    count += 12 - tm;
                    if(leapMonth >= tm){
                        ++count;
                    }
                }

            }else {
                if(tm > 12){
                    count += -(tm - 12);
                }else {
                    count += -(tm - 1);
                    if(leapMonth > 0 && leapMonth < tm){
                        --count;
                    }
                }

            }

            while(dir > 0 && count < amount || dir < 0 && count > amount){
                if(dir > 0){
                    ++ty;
                }else {
                    --ty;
                }
                if(!isYearSupported(ty)){
                    return false;
                }

                data = LunarData.get(ty);
                leapMonth = LunarData.leapMonth(data);

                if(dir > 0){
                    count += leapMonth > 0 ? 13 : 12;
                }else {
                    count -= leapMonth > 0 ? 13 : 12;
                }
            }

            if(dir > 0){

                tm = 12 + amount - count;

                if(leapMonth > 0){
                    if(leapMonth == tm){
                        tm += 12;
                    }else if(leapMonth > tm){
                        ++tm;
                    }
                }

            }else {
                tm = 1 + amount - count;
                if(leapMonth > 0){
                    if(leapMonth == tm - 1){
                        tm = leapMonth + 12;
                    }else if(leapMonth < tm) {
                        --tm;
                    }
                }
            }
            if(td > 29 && (tm > 12 && LunarData.leapPattern(data) == 0 || tm < 12 && LunarData.monthPattern(data, tm) == 0)){
                td = 29;
            }
        }finally {
            LunarData.unlock();
        }

        out[0] = ty;
        out[1] = tm;
        out[2] = td;

        return true;
    }


    public static boolean rollLunarMonth(int[] in, final int amount, int[] out){
        if(amount == 0){
            out[0] = in[0];
            out[1] = in[1];
            out[2] = in[2];
            return true;
        }
        final int ly = in[0], lm = in[1], ld = in[2];
        if(!isYearSupported(ly)){
            return false;
        }

        int ty = ly, tm, td;

        final long data = LunarData.getL(ly);

        int leapMonth = LunarData.leapMonth(data);

        if(lm > 12 && leapMonth != lm - 12){
            //log err
            return false;
        }
        int mc = leapMonth > 0 ? 13 : 12;
        int amt = amount % mc;

        if(amt == 0){
            tm = lm;
            td = ld;
            out[0] = ty;
            out[1] = tm;
            out[2] = td;
            return true;
        }


        if(lm > 12){
            if(amt > 0){
                tm = (lm - 12 + amt) % 12;
            }else {
                tm = (lm - 12 + amt + 1 + 12) % 12;
            }
            if(tm == 0){
                tm = 12;
            }

        }else {

            if(leapMonth > 0){
                if (amt > 0) {
                    tm = (lm + amt) % 12;
                    if(tm == 0){
                        tm = 12;
                    }
                    if(tm == lm){
                        --tm;
                        if(tm == 0){
                            tm = 12;
                        }
                        if(tm == leapMonth){
                            tm += 12;
                        }
                    }else  if(leapMonth >= lm && (tm > leapMonth || tm < lm)
                            || leapMonth < lm && (tm > leapMonth && tm < lm)){
                        --tm;
                        if(tm == 0){
                            tm = 12;
                        }
                        if(tm == leapMonth){
                            tm += 12;
                        }
                    }
                }else {
                    tm = (lm + amt + 12) % 12;
                    if(tm == 0){
                        tm = 12;
                    }
                    if(tm == lm){
                        if(leapMonth == tm){
                            tm += 12;
                        }else {
                            --tm;
                        }
                    }else if(leapMonth < lm && (tm <= leapMonth || tm > lm) || leapMonth >= lm && (tm > lm && tm <= leapMonth)){
                        if(tm == leapMonth){
                            tm += 12;
                        }else {
                            ++tm;
                            if(tm > 12){
                                tm = 1;
                            }
                        }
                    }
                }
            }else {
                tm = (lm + amt + 12) % mc;
                if(tm == 0){
                    tm = 12;
                }
            }

        }

        td = ld;
        if(ld > 29 && (tm > 12 && LunarData.leapPattern(data) == 0 || tm <= 12 && LunarData.monthPattern(data, tm) == 0)){
            td = 29;
        }

        out[0] = ty;
        out[1] = tm;
        out[2] = td;

        return true;
    }

    public static boolean addLunarDay(int[] in, int amount, int[] out){
        if(amount == 0){
            out[0] = in[0];
            out[1] = in[1];
            out[2] = in[2];
            return true;
        }
        final int ly = in[0], lm = in[1], ld = in[2];
        if(!isYearSupported(ly)){
            return false;
        }
        int ty = ly, tm = lm, td = ld;
        long data;

        data = LunarData.getL(ty);
        if(!LunarData.isValid(data)){
            return false;
        }
        int dir = amount > 0 ? 1 : -1;

        if(amount > 0){


            //处理ty年tm月剩下的天数
            int endDayOfMonth;
            if(lm <= 12) {
                endDayOfMonth = LunarData.monthPattern(data, lm) + 29;

            }else {
                if(LunarData.leapMonth(data) != lm - 12){
                    //log err
                    return false;
                }
                endDayOfMonth = LunarData.leapPattern(data) + 29;

            }
            int remainings = endDayOfMonth - td;
            amount -= remainings;

            if(amount <= 0){
                td = endDayOfMonth + amount;

                out[0] = ty;
                out[1] = tm;
                out[2] = td;
                return true;
            }



            //处理ty年剩下的天数
            int m = tm;
            int dim = 0;
            while ((m = LunarData.nextMonthInYear(data, m)) > 0){
                dim = LunarData.daysInMonth(data, m);
                if(dim < 0){
                    return false;
                }
                amount -= dim;
                if(amount <= 0){
                    break;
                }
            }
            if(m < 0){
                return false;
            }
            if(amount <= 0){
                tm = m;
                td = dim + amount;

                out[0] = ty;
                out[1] = tm;
                out[2] = td;
                return true;
            }




            //以年为单位处理
            ++ty;
            data = LunarData.getL(ty);
            int dc = 0;

            while (true){
                if(!isYearSupported(ty)){
                    return false;
                }
                dc = LunarData.daysCount(data);
                if(dc >= amount){
                    break;
                }
                amount -= dc;
                ++ty;
                data = LunarData.getL(ty);
            }

            //处理最后一年或零头
            if(dc == amount){
                tm = LunarData.lastMonthInYear(data);
                td = LunarData.daysInMonth(data, tm);
                if(td == -1){
                    return false;
                }
            }else {
                tm = 1;
                dim = 0;
                do {
                    dim = LunarData.daysInMonth(data, tm);
                    amount -= dim;
                }while (amount > 0 && (tm = LunarData.nextMonthInYear(data, tm)) > 0);

                if(tm < 0){
                    return false;
                }

                dim += amount;

                td = dim;
            }


        }else {

            //处理ty年tm月剩下的天数
            int remaining = td - 1;
            amount += remaining;

            if(amount >= 0){
                td = 1 + amount;

                out[0] = ty;
                out[1] = tm;
                out[2] = td;
                return true;
            }



            //处理ty年剩下的天数
            int m = tm;
            int dim = 0;
            while ((m = LunarData.preMonthInYear(data, m)) > 0){
                dim = LunarData.daysInMonth(data, m);
                if(dim < 0){
                    return false;
                }
                amount += dim;
                if(amount >= 0){
                    break;
                }
            }
            if(m < 0){
                return false;
            }
            if(amount >= 0){
                tm = m;
                td = 1 + amount;

                out[0] = ty;
                out[1] = tm;
                out[2] = td;
                return true;
            }



            //以年为单位处理
            --ty;
            data = LunarData.getL(ty);

            int dc = 0;

            while (true){
                if(!isYearSupported(ty)){
                    return false;
                }
                dc = LunarData.daysCount(data);
                if(dc + amount >= 0){
                    break;
                }
                amount += dc;
                --ty;
                data = LunarData.getL(ty);
            }




            //处理最后一年或零头
            if(dc + amount == 0){
                tm = 1;
                td = 1;

            }else {
                tm = LunarData.lastMonthInYear(data);
                dim = 0;
                do{
                    dim = LunarData.daysInMonth(data, tm);
                    amount += dim;
                }while (amount < 0 && (tm = LunarData.preMonthInYear(data, tm)) > 0);

                if(tm < 0){
                    return false;
                }

                td = 1 + amount;
            }
        }


        out[0] = ty;
        out[1] = tm;
        out[2] = td;
        return true;
    }

    public static boolean addLunarDay2(int[] in, int amount, int[] out){
        if(amount == 0){
            out[0] = in[0];
            out[1] = in[1];
            out[2] = in[2];
            return true;
        }
        final int ly = in[0], lm = in[1], ld = in[2];
        if(!isYearSupported(ly)){
            return false;
        }
        int ty = ly, tm = lm, td = ld;
        long data;

        data = LunarData.getL(ty);
        if(!LunarData.isValid(data)){
            return false;
        }
        int dir = amount > 0 ? 1 : -1;


        //处理ty年tm月剩下的天数
        int endDayOfMonth;
        if(dir > 0){
            if(lm <= 12) {
                endDayOfMonth = LunarData.monthPattern(data, lm) + 29;

            }else {
                if(LunarData.leapMonth(data) != lm - 12){
                    //log err
                    return false;
                }
                endDayOfMonth = LunarData.leapPattern(data) + 29;

            }
        }else {
            endDayOfMonth = 1;
        }

        int remainings = endDayOfMonth - td;
        amount -= remainings;

        if(dir > 0 && amount <= 0 || dir < 0 && amount >= 0){
            td = endDayOfMonth + amount;

            out[0] = ty;
            out[1] = tm;
            out[2] = td;
            return true;
        }


        //处理ty年剩下的天数
        //处理ty年剩下的天数
        int m = tm;
        int dim = 0;
        while ((m = dir > 0 ? LunarData.nextMonthInYear(data, m) : LunarData.preMonthInYear(data, m)) > 0){
            dim = LunarData.daysInMonth(data, m);
            if(dim < 0){
                return false;
            }
            if(dir > 0) {
                amount -= dim;
                if (amount <= 0) {
                    break;
                }
            }else {
                amount += dim;
                if(amount >= 0){
                    break;
                }
            }
        }
        if(m < 0){
            return false;
        }

        int eom = dir > 0 ? dim : 1;
        if(dir > 0 && amount <= 0 || dir < 0 && amount >= 0){
            tm = m;
            td = eom + amount;

            out[0] = ty;
            out[1] = tm;
            out[2] = td;
            return true;
        }



        //以年为单位处理
        if(dir > 0) {
            ++ty;
        }else {
            --ty;
        }
        data = LunarData.getL(ty);
        int dc = 0;

        while (true){
            if(!isYearSupported(ty)){
                return false;
            }
            dc = LunarData.daysCount(data);
            if(dir > 0 && dc >= amount || dir < 0 && dc + amount >= 0){
                break;
            }
            if (dir > 0) {
                amount -= dc;
                ++ty;
            }else {
                amount += dc;
                --ty;
            }

            data = LunarData.getL(ty);
        }



        //处理最后一年或零头
        if(dir > 0 && dc == amount){
            tm = LunarData.lastMonthInYear(data);
            td = LunarData.daysInMonth(data, tm);
            if(td == -1){
                return false;
            }
        }else if(dir < 0 && dc + amount == 0){
            tm = 1;
            td = 1;
        }
        else {
            tm = dir > 0 ? 1 : LunarData.lastMonthInYear(data);
            dim = 0;
            do {
                dim = LunarData.daysInMonth(data, tm);
                if(dir > 0) {
                    amount -= dim;
                }else {
                    amount += dim;
                }
            }while (dir > 0 && amount > 0 && (tm = LunarData.nextMonthInYear(data, tm)) > 0 || dir < 0 && amount < 0 && (tm = LunarData.preMonthInYear(data, tm)) > 0);

            if(tm < 0){
                return false;
            }

            td = dir > 0 ? dim + amount : 1 + amount;
        }

        out[0] = ty;
        out[1] = tm;
        out[2] = td;
        return true;
    }

    public static boolean rollLunarDay(int[] in, int amount, int[] out){
        if(amount == 0){
            out[0] = in[0];
            out[1] = in[1];
            out[2] = in[2];
            return true;
        }
        final int ly = in[0], lm = in[1], ld = in[2];
        if(!isYearSupported(ly)){
            return false;
        }
        int ty = ly, tm = lm, td = ld + amount;

        if(td <= 0 || td > 29){
            final long data = LunarData.getL(ty);
            if(tm > 12 && LunarData.leapMonth(data) != tm - 12){
                //log err
                return false;
            }
            int days = LunarData.daysInMonth(data, tm);
            if(days < 0){
                return false;
            }

            td = td % days;
            if(td < 0){
                td += days;
            }else if(td == 0){
                td = days;
            }

        }

        out[0] = ty;
        out[1] = tm;
        out[2] = td;
        return true;
    }

    public static int getMonth(int days, int year){
        int[] ints = END_OF_MONTH(year);
        for(int i = 0; i < ints.length; ++i){
            if(ints[i] >= days){
                return i + 1;
            }
        }
        return -1;
    }

    public static int getDaysOfYear(int year){
        return isLeapYear(year) ? 366 : 365;
    }

    public static boolean getDate(int days, int year, int[] out){
        int daysOfYear;
        while (days > (daysOfYear = getDaysOfYear(year))){
            days -= daysOfYear;
            ++year;
        }
        int[] ints = END_OF_MONTH(year);
        int i = 0;
        for(; i < ints.length; ++i){
            if(ints[i] >= days){
                break;
            }
        }
        if(i < ints.length){
            int month = i + 1;
            int daysOfMonth = getDaysOfMonth(year, month);
            int day = daysOfMonth + days - ints[i];
            out[0] = year;
            out[1] = month;
            out[2] = day;
            return true;
        }

        return false;
    }

//    public static int getDays(int year, int month, int day){
//        int daysAtTheEndOf = getDaysAtTheEndOf(year, month);
//        int daysOfMonth = getDaysOfMonth(year, month);
//        return daysAtTheEndOf + (day - daysOfMonth);
//    }

    public static int getDaysOfMonth(int year, int month){
        int v = -1;
        switch (month){
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
                v = 31;
                break;
            case 4:
            case 6:
            case 9:
            case 11:
                v = 30;
                break;
            case 2:
                v = isLeapYear(year) ? 29 : 28;
                break;
        }
        return v;
    }

    public static int getDaysAtTheEndOf(int year, int month){
        if(month < 1 || month > 12){
            return -1;
        }
        int[] arr = isLeapYear(year) ? DAYS_AT_THE_END_OF_MONTH_LEAP : DAYS_AT_THE_END_OF_MONTH;
        return arr[month];
    }

    /**
     * 该月份之前的天数
     * @param month
     * @return
     */
    private static int addDays(int month) {
        switch (month) {
            case 1:
                return 0;
            case 2:
                return 31;
            case 3:
                return 59;
            case 4:
                return 90;
            case 5:
                return 120;
            case 6:
                return 151;
            case 7:
                return 181;
            case 8:
                return 212;
            case 9:
                return 243;
            case 10:
                return 273;
            case 11:
                return 304;
            case 12:
                return 334;
            default:
                throw new RuntimeException("错误");
        }
    }

    /**
     * 判断是否为闰年
     * @param year
     * @return
     */
    private static boolean isLeapYear(int year) {
        if (year % 172800 == 0 || year % 400 == 0 && year % 3200 != 0 || year % 4 == 0 && year % 100 != 0)
            return true;
        return false;
    }

    /**
     * 一年中的第几天 1.1是第一天
     * @param year
     * @param month
     * @param day
     * @return
     */
    private static int getDays(int year, int month, int day) {
        int sum = addDays(month) + day;
        if (isLeapYear(year) && month > 2) {
            sum++;
        }
        return sum;
    }

    private static int getDays(long v){
        int year = LunarData.year(v);
        int month = LunarData.month(v);
        int date = LunarData.date(v);
        return getDays(year, month, date);
    }


    private static void cast(int[] in, long data, long lastData, int[] out){
        final int y = in[0], m = in[1], d = in[2];
        int numStart = getDays(data);//当年春节对应的公历日期，在当年是第几天
        int numNow = getDays(y, m, d);
        int dif = numNow - numStart;//当前日期相对天数,相对新年 新年为0天

        final int leapMonth = LunarData.leapMonth(data);
        final int lastYearLeapMonth = LunarData.leapMonth(lastData);

        final int lastFirstMonth, lastSecondMonth;//去年最后两个月
        final int lastFirst, lastSecond;//去年 最后两个农历月的天数，可能是闰月
        if(lastYearLeapMonth == 12){//去年的闰月是12月
            lastFirst = 29 + LunarData.leapPattern(lastData);//闰12月
            lastSecond = 29 + LunarData.monthPattern(lastData, 12);//正常12月
            lastFirstMonth = lastSecondMonth = 12;
        }else if(lastYearLeapMonth == 11){//去年的闰月是11月
            lastFirst = 29 + LunarData.monthPattern(lastData, 12);
            lastSecond = 29 + LunarData.leapPattern(lastData);
            lastFirstMonth = 12;
            lastSecondMonth = 11;
        }else {
            lastFirst = 29 + LunarData.monthPattern(lastData, 12);
            lastSecond = 29 + LunarData.monthPattern(lastData, 11);
            lastFirstMonth = 12;
            lastSecondMonth = 11;
        }


        int offset = -lastFirst - lastSecond;

        int ly, lm = -1, ld = -1;

        boolean found = false;

        offset += lastSecond;
        if(offset >= dif){
            found = true;
            if(offset == dif){
                lm = lastYearLeapMonth == 12 ? lastFirstMonth + 12 : lastFirstMonth;
                ld = 1;
            }else {
                lm = lastYearLeapMonth == 11 ? lastSecondMonth + 12 : lastSecondMonth;
                ld = lastSecond - (offset - dif) + 1;
            }

        }

        if(!found) {
            offset += lastFirst;
            if (offset >= dif) {
                found = true;
                if(offset == dif){
                    lm = 1;
                    ld = 1;
                }else {
                    lm = lastYearLeapMonth == 12 ? lastFirstMonth + 12 : lastFirstMonth;
                    ld = lastFirst - (offset - dif) + 1;
                }
            }
        }

        if(!found) {

            int leapMonthCount = 0;
            int lastMonthCount = 0;
            int i = 0;
            while (offset < dif) {
                ++i;
                lastMonthCount = 29 + LunarData.monthPattern(data, i);
                offset += lastMonthCount;
                if(i == leapMonth){
                    if(offset >= dif){
                        break;
                    }
                    lastMonthCount = 29 + LunarData.leapPattern(data);
                    leapMonthCount = lastMonthCount;
                    offset += leapMonthCount;
                }
            }


            if(offset == dif){
                lm = i + 1;
                if(i == leapMonth && leapMonthCount == 0){
                    lm = i + 12;
                }
                ld = 1;
            }else {
                lm = i == leapMonth && leapMonthCount != 0 ? i + 12 : i;
                ld = lastMonthCount - (offset - dif) + 1;
            }
        }

        ly = dif >= 0 ? y : y - 1;
        if(out != null && out.length >= 3){
            out[0] = ly;
            out[1] = lm;
            out[2] = ld;
        }
    }



}
