package com.cjz.lunarcalendar;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;


/**
 * Created by cjz on 2019/11/21.
 */
public class LunarCalendar extends Calendar {
    private Calendar c;
    private Calendar worker;

    private ThreadLocal<int[]> ins;
    private ThreadLocal<int[]> outs;


    public LunarCalendar() {
        ins = new ThreadLocal<>();
        outs = new ThreadLocal<>();
        c = Calendar.getInstance();
        worker = Calendar.getInstance();
        setTimeInMillis(c.getTimeInMillis());

    }

    public LunarCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
        ins = new ThreadLocal<>();
        outs = new ThreadLocal<>();
        c = Calendar.getInstance(zone, aLocale);
        worker = Calendar.getInstance(zone, aLocale);
        setTimeInMillis(c.getTimeInMillis());

    }

    @Override
    public Object clone() {
        LunarCalendar cal = (LunarCalendar) super.clone();
        cal.ins = new ThreadLocal<>();
        cal.outs = new ThreadLocal<>();
        cal.c = (Calendar) c.clone();
        cal.worker = (Calendar) worker.clone();

        return cal;
    }

    private int[] in(){
        int[] in = ins.get();
        if(in == null){
            in = new int[3];
            ins.set(in);
        }
        return in;
    }

    private int[] out(){
        int[] out = outs.get();
        if(out == null){
            out = new int[3];
            outs.set(out);
        }
        return out;
    }

    @Override
    protected void computeTime() {
        int[] in = in();
        in[0] = fields[Calendar.YEAR];
        in[1] = fields[Calendar.MONTH] + 1;
        in[2] = fields[Calendar.DAY_OF_MONTH];

        int[] out = out();
        Lunars.getSolarDate(in, out);

        c.set(out[0], out[1] - 1, out[2], fields[Calendar.HOUR], fields[Calendar.MINUTE], fields[Calendar.SECOND]);
        c.clear(Calendar.MILLISECOND);

        time = c.getTimeInMillis() + fields[Calendar.MILLISECOND];
    }

    @Override
    public int get(int field) {
        return super.get(field);
    }

    @Override
    protected void computeFields() {
        c.setTimeInMillis(time);
        final int y = c.get(Calendar.YEAR);
        final int m = c.get(Calendar.MONTH) + 1;
        final int d = c.get(Calendar.DAY_OF_MONTH);

        int[] in = in();
        in[0] = y;
        in[1] = m;
        in[2] = d;
        int[] out = out();
        Lunars.getLunarDate(in, out);

        final int ly = out[0];
        final int lm = out[1];
        final int ld = out[2];

        int mask = 0;

        fields[YEAR] = ly;
        fields[MONTH] = lm - 1;
        fields[DAY_OF_MONTH] = ld;
        fields[DAY_OF_WEEK] = c.get(Calendar.DAY_OF_WEEK);
        fields[ERA] = ly > 0 ? GregorianCalendar.AD : GregorianCalendar.BC;

        mask |= YEAR | MONTH | DAY_OF_MONTH | DAY_OF_WEEK | ERA;

        in[0] = ly;
        in[1] = 1;
        in[2] = 1;
        if(Lunars.getSolarDate(in, out)) {
            int dayDiff = Lunars.getDayDiff(out[0], out[1], out[2], y, m, d);
            worker.set(out[0], out[1] - 1, out[2]);
            int dayOfWeek = worker.get(Calendar.DAY_OF_WEEK);
            int firstDayOfWeek = getFirstDayOfWeek();
            int front = (firstDayOfWeek - dayOfWeek + 7) % 7;

            int count = dayDiff + 1 - front;
            int weekCount = count / 7;
            int left = count % 7;
            if(left > 0){
                ++weekCount;
            }
            if(getMinimalDaysInFirstWeek() <= front){
                ++weekCount;
            }

            fields[WEEK_OF_YEAR] = weekCount;
            fields[DAY_OF_YEAR] = dayDiff + 1;

            mask |= WEEK_OF_YEAR | DAY_OF_YEAR;
        }else {
            //error
        }

        in[0] = ly;
        in[1] = lm;
        in[2] = 1;
        if(Lunars.getSolarDate(in, out)) {
            int dayDiff = Lunars.getDayDiff(out[0], out[1], out[2], y, m, d);
            worker.set(out[0], out[1] - 1, out[2]);
            int dayOfWeek = worker.get(Calendar.DAY_OF_WEEK);
            int firstDayOfWeek = getFirstDayOfWeek();
            int front = (firstDayOfWeek - dayOfWeek + 7) % 7;

            int count = dayDiff + 1 - front;
            int weekCount = count / 7;
            int left = count % 7;
            if(left > 0){
                ++weekCount;
            }
            if(getMinimalDaysInFirstWeek() <= front){
                ++weekCount;
            }

            fields[WEEK_OF_MONTH] = weekCount;
            fields[DAY_OF_WEEK_IN_MONTH] = (dayDiff + 1) / 7 + ((dayDiff + 1) % 7 != 0 ? 1 : 0);

            mask |= WEEK_OF_MONTH | DAY_OF_WEEK_IN_MONTH;
        }else {
            //error
        }

        fields[AM_PM] = c.get(AM_PM);
        fields[HOUR] = c.get(HOUR);
        fields[HOUR_OF_DAY] = c.get(HOUR_OF_DAY);
        fields[MINUTE] = c.get(MINUTE);
        fields[SECOND] = c.get(SECOND);
        fields[MILLISECOND] = c.get(MILLISECOND);
        fields[ZONE_OFFSET] = c.get(ZONE_OFFSET);
        fields[DST_OFFSET] = c.get(DST_OFFSET);

        mask |= AM_PM | HOUR | HOUR_OF_DAY | MINUTE | SECOND | MILLISECOND | ZONE_OFFSET | DST_OFFSET;



//        setFi

        setFieldsComputed(mask);

    }

//    private Method mSetFieldsComputed;
    private void setFieldsComputed(int mask){
        for(int i = 0; i < fields.length; ++i){
            if((mask & 1) == 1){
                isSet[i] = true;
            }
            mask >>>= 1;
        }
    }


    @Override
    public void setMinimalDaysInFirstWeek(int value) {
        boolean changed = getMinimalDaysInFirstWeek() != value;
        super.setMinimalDaysInFirstWeek(value);
        if(changed){
            Calendar cal = (Calendar) clone();

//            areFieldsSet = false;

            cal.setLenient(true);
            cal.clear(WEEK_OF_MONTH);
            cal.clear(WEEK_OF_YEAR);

            int weekOfMonth = cal.get(WEEK_OF_MONTH);
            if (fields[WEEK_OF_MONTH] != weekOfMonth) {
                fields[WEEK_OF_MONTH] = weekOfMonth;
            }

            int weekOfYear = cal.get(WEEK_OF_YEAR);
            if (fields[WEEK_OF_YEAR] != weekOfYear) {
                fields[WEEK_OF_YEAR] = weekOfYear;
            }
        }
    }


    @Override
    public void setFirstDayOfWeek(int value) {
        super.setFirstDayOfWeek(value);
    }

    private void invalidateWeekFields() {

    }

    @Override
    public void add(int field, int amount) {
        complete();

        if(field == Calendar.YEAR || field == Calendar.MONTH){
            int ly = fields[Calendar.YEAR];
            int lm = fields[Calendar.MONTH] + 1;
            int ld = fields[Calendar.DAY_OF_MONTH];

            int[] in = in();
            int[] out = out();
            in[0] = ly;
            in[1] = lm;
            in[2] = ld;

            if(field == Calendar.YEAR){
                if(!Lunars.addLunarYear(in, amount, out)){
                    //log err
                    return;
                }

            }else {
                if(!Lunars.addLunarMonth(in, amount, out)){
                    //log err
                    return;
                }
            }

            if(!Lunars.getSolarDate(out, in)){
                //log err
                return;
            }

            c.set(in[0], in[1] - 1, in[2]);
        } else {
            c.add(field, amount);
        }
        setTimeInMillis(c.getTimeInMillis());
    }

    @Override
    public void roll(int field, int amount) {
        if(field == Calendar.MONTH || field == Calendar.DAY_OF_MONTH || field == Calendar.YEAR){
            complete();

            int ly = fields[Calendar.YEAR];
            int lm = fields[Calendar.MONTH] + 1;
            int ld = fields[Calendar.DAY_OF_MONTH];

            int[] in = in();
            in[0] = ly;
            in[1] = lm;
            in[2] = ld;

            int[] out = out();
//            Lunars.getSolarDate(in, out);

            if(field == Calendar.MONTH) {
                if(!Lunars.rollLunarMonth(in, amount, out)){
                    //log err
                    return;
                }
            }else if(field == Calendar.YEAR){
                if(!Lunars.rollLunarYear(in, amount, out)){
                    //log err
                    return;
                }
            }else {
                if(!Lunars.rollLunarDay(in, amount, out)){
                    //log err
                    return;
                }
            }

            if(!Lunars.getSolarDate(out, in)){
                //log err
                return;
            }

            c.set(in[0], in[1] - 1, in[2]);
            setTimeInMillis(c.getTimeInMillis());
        }else {
            super.roll(field, amount);
        }
    }

    @Override
    public void roll(int field, boolean up) {

        if(field == Calendar.MONTH || field == Calendar.DAY_OF_MONTH || field == Calendar.YEAR){
            roll(field, up ? 1 : -1);
        }else {
            c.roll(field, up);
            setTimeInMillis(c.getTimeInMillis());

        }

    }

    @Override
    public int getMinimum(int field) {
        int min;
        if(field == Calendar.YEAR){
            min = LunarData.FIRST_YEAR + 1;
        }else {
            min = c.getMinimum(field);
        }
        return min;
    }

    @Override
    public int getMaximum(int field) {
        int max;
        if(field == Calendar.YEAR){
            max = LunarData.LAST_YEAR - 1;
        }else if(field == Calendar.MONTH){
            max = 23;
        }
        else if(field == Calendar.DAY_OF_MONTH){
            max = 30;
        }else {
            max = c.getMaximum(field);
        }
        return max;
    }

    @Override
    public int getGreatestMinimum(int field) {
        return c.getGreatestMinimum(field);
    }

    @Override
    public int getLeastMaximum(int field) {
        int leastMax;
        if(field == Calendar.YEAR){
            leastMax = LunarData.LAST_YEAR - 1;
        }else if(field == Calendar.MONTH){
            leastMax = 11;
        }else if(field == Calendar.DAY_OF_MONTH){
            leastMax = 29;
        }else {
            leastMax = c.getLeastMaximum(field);
        }
        return leastMax;
    }

    @Override
    public int getActualMinimum(int field) {
        return super.getActualMinimum(field);
    }

    @Override
    public int getActualMaximum(int field) {
        int year = get(Calendar.YEAR);
        long data = LunarData.getL(year);
        if(!LunarData.isValid(data)){
            //log
            return 0;
        }
        int actualMax;
        if(field == Calendar.YEAR){
            actualMax = LunarData.LAST_YEAR - 1;
        }else if(field == Calendar.MONTH){
            int i = LunarData.leapMonth(data);
            if(i > 0){
                actualMax = i + 12 - 1;
            }else {
                actualMax = 11;
            }
        }else if(field == Calendar.DAY_OF_MONTH){
            int i = get(Calendar.MONTH);
            if(i > 11){
                actualMax = 29 + LunarData.leapPattern(data);
            }else {
                actualMax = 29 + LunarData.monthPattern(data, i + 1);
            }
        }else {
            actualMax = c.getActualMaximum(field);
        }
        return actualMax;
    }

    @Override
    public String toString() {
        // NOTE: BuddhistCalendar.toString() interprets the string
        // produced by this method so that the Gregorian year number
        // is substituted by its B.E. year value. It relies on
        // "...,YEAR=<year>,..." or "...,YEAR=?,...".
        StringBuilder buffer = new StringBuilder(800);
        buffer.append(getClass().getName()).append('[');
        appendValue(buffer, "time", isTimeSet, time);
        buffer.append(",areFieldsSet=").append(areFieldsSet);
//        buffer.append(",areAllFieldsSet=").append(areAllFieldsSet);
        buffer.append(",lenient=").append(isLenient());
        buffer.append(",zone=").append(getTimeZone());
        appendValue(buffer, ",firstDayOfWeek", true, (long) getFirstDayOfWeek());
        appendValue(buffer, ",minimalDaysInFirstWeek", true, (long) getMinimalDaysInFirstWeek());
        for (int i = 0; i < FIELD_COUNT; ++i) {
            buffer.append(',');
            appendValue(buffer, FIELD_NAME[i], isSet(i), (long) fields[i]);
        }
        buffer.append(']');
        return buffer.toString();
    }

    private static final String[] FIELD_NAME = {
            "ERA", "YEAR", "MONTH", "WEEK_OF_YEAR", "WEEK_OF_MONTH", "DAY_OF_MONTH",
            "DAY_OF_YEAR", "DAY_OF_WEEK", "DAY_OF_WEEK_IN_MONTH", "AM_PM", "HOUR",
            "HOUR_OF_DAY", "MINUTE", "SECOND", "MILLISECOND", "ZONE_OFFSET",
            "DST_OFFSET"
    };

    private static void appendValue(StringBuilder sb, String item, boolean valid, long value) {
        sb.append(item).append('=');
        if (valid) {
            sb.append(value);
        } else {
            sb.append('?');
        }
    }

    private static Calendar createCalendar(TimeZone zone,
                                           Locale aLocale)
    {
        // BEGIN Android-changed: only support GregorianCalendar here
        return new LunarCalendar(zone, aLocale);
        // END Android-changed: only support GregorianCalendar here
    }

    public static Calendar getInstance()
    {
        return createCalendar(TimeZone.getDefault(), Locale.getDefault());
    }

    /**
     * Gets a calendar using the specified time zone and default locale.
     * The <code>Calendar</code> returned is based on the current time
     * in the given time zone with the default
     * {@link Locale.Category#FORMAT FORMAT} locale.
     *
     * @param zone the time zone to use
     * @return a Calendar.
     */
    public static Calendar getInstance(TimeZone zone)
    {
        return createCalendar(zone, Locale.getDefault());
    }

    /**
     * Gets a calendar using the default time zone and specified locale.
     * The <code>Calendar</code> returned is based on the current time
     * in the default time zone with the given locale.
     *
     * @param aLocale the locale for the week data
     * @return a Calendar.
     */
    public static Calendar getInstance(Locale aLocale)
    {
        return createCalendar(TimeZone.getDefault(), aLocale);
    }

    /**
     * Gets a calendar with the specified time zone and locale.
     * The <code>Calendar</code> returned is based on the current time
     * in the given time zone with the given locale.
     *
     * @param zone the time zone to use
     * @param aLocale the locale for the week data
     * @return a Calendar.
     */
    public static Calendar getInstance(TimeZone zone,
                                       Locale aLocale)
    {
        return createCalendar(zone, aLocale);
    }
}
