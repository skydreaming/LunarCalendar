package com.cjz.lunarcalendar;


/**
 * 表示左闭右开的整型区间
 * Created by cjz on 2016/11/22.
 */
public class Range implements Comparable<Range> {
    public int start = 0;
    public int end = 0;

    public Range() {
    }

    public Range( Range other){
        start = other.start;
        end = other.end;
    }

    public Range(int start, int end) {
        super();
        this.start = start;
        this.end = end;
    }

    public void set(int start, int end){
        this.start = start;
        this.end = end;
    }

    public boolean isRightTheRange(int start, int end){
        return this.start == start && this.end == end;
    }

    public boolean isWithinRange(int dot) {
        return dot >= start && dot < end;
    }

    public boolean contains(int start, int end){
        return this.start <= start && this.end >= end;
    }

    public int count(){
        return end - start;
    }

    public void move(int movement){
        end += movement;
        start += movement;
    }

    public void expand(int howMany){
        end += howMany;
    }

    public boolean mergeFrom(Range range){
        if(range == null){
            return false;
        }
        if(range.end < start || end < range.start){
            return false;
        }
        int minStart = range.start < start ? range.start : start;
        int maxEnd = range.end > end ? range.end : end;
        start = minStart;
        end = maxEnd;
        return true;
    }

    @Override
    public int compareTo(Range o) {
        if(o == null){
            return -1;
        }
        if(start < o.start){
            return -1;
        }
        if(start > o.start){
            return 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "[" + start +", " + end + ")";
    }
}
