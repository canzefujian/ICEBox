package com.example.shareiceboxms.models.helpers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Administrator on 2017/12/20.
 */

public class SecondToDate {
    public static int TODAY_CODE = 0;
    public static int WEEK_CODE = 1;
    public static int MONTH_CODE = 2;
    public static int YEAR_CODE = 3;

    public static String[] formatLongToTimeStr(Long seconds) {
        String[] time = new String[4];
        int day = 0;
        int hour = 0;
        int minute = 0;
        int second;
        second = seconds.intValue();
        if (second > 60) {
            minute = second / 60;         //取整
            second = second % 60;         //取余
        }

        if (minute > 60) {
            hour = minute / 60;
            minute = minute % 60;
        }
        if (hour > 24) {
            day = hour / 24;
            hour = hour % 24;
        }
        time[0] = String.valueOf(day);
        time[1] = String.valueOf(hour);
        time[2] = String.valueOf(minute);
        time[3] = String.valueOf(second);
        return time;

    }

    /*
    * 获取日期数组参数
    * */
    public static String[] getDateParams(int code) {
        String[] date = getDay();
        switch (code) {
            case 0:
                break;
            case 1:
                date[0] = getWeek();
                break;
            case 2:
                date[0] = getTimeOfMonthStart();
                break;
            case 3:
                date[0] = getTimeOfYearStart();
                break;
        }
        return date;
    }

    public static String getDateUiShow(String[] time) {
        String[] dateShow = new String[2];
        dateShow[0] = time[0].replace(" 00:00", "");
        dateShow[1] = time[1].replace(" 23:59", "");
        return dateShow[0] + " 至 " + dateShow[1];
    }


    /*
       *今天日期
       **/
    public static String[] getDay() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date(System.currentTimeMillis());
        return new String[]{formatter.format(date) + " 00:00", formatter.format(date) + " 23:59"};
    }

    /*
    *本周开始日期
    **/
    public static String getWeek() {
        Calendar ca = Calendar.getInstance();
        ca.set(Calendar.HOUR_OF_DAY, 0);
        ca.clear(Calendar.MINUTE);
        ca.clear(Calendar.SECOND);
        ca.clear(Calendar.MILLISECOND);
        ca.set(Calendar.DAY_OF_WEEK, ca.getFirstDayOfWeek());
        int year = ca.get(Calendar.YEAR);
        int month = ca.get(Calendar.MONTH) + 1;
        int date = ca.get(Calendar.DAY_OF_MONTH);
        return year + "-" + month + "-" + date + " 00:00";
    }

    /*
    *本月开始日期
    * */
    public static String getTimeOfMonthStart() {
        Calendar ca = Calendar.getInstance();
        ca.set(Calendar.HOUR_OF_DAY, 0);
        ca.clear(Calendar.MINUTE);
        ca.clear(Calendar.SECOND);
        ca.clear(Calendar.MILLISECOND);
        ca.set(Calendar.DAY_OF_MONTH, 1);
        int year = ca.get(Calendar.YEAR);
        int month = ca.get(Calendar.MONTH) + 1;
        int date = ca.get(Calendar.DAY_OF_MONTH);
        return year + "-" + month + "-" + date + " 00:00";
    }

    /*
    * 本年的开始日期
    * */
    public static String getTimeOfYearStart() {
        Calendar ca = Calendar.getInstance();
        ca.set(Calendar.HOUR_OF_DAY, 0);
        ca.clear(Calendar.MINUTE);
        ca.clear(Calendar.SECOND);
        ca.clear(Calendar.MILLISECOND);
        ca.set(Calendar.DAY_OF_YEAR, 1);
        int year = ca.get(Calendar.YEAR);
        int month = ca.get(Calendar.MONTH) + 1;
        int date = ca.get(Calendar.DAY_OF_MONTH);
        return year + "-" + month + "-" + date + " 00:00";
    }
    /*
 * 将时间转换为时间戳
 */
    public static long dateToStamp(String s) throws ParseException {
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = simpleDateFormat.parse(s);
        long ts = date.getTime();
        return ts;
    }
}
