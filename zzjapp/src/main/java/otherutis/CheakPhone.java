package otherutis;

import android.util.Log;

/**
 * Created by WH on 2017/8/7.
 */

public class CheakPhone {

    //中国移动手机号段： 134 135 136 137 138 139 150 151 152 157 158 159 182 183 184 187 188 1705 178 147
//    中国联通手机号段： 130 131 132 155 156 185 186 1709 176 145
//    中国电信手机号段： 133 1349 153 180 181 189 1700 177


    static String YD = "(134[0-9]{8})|(134[0-9]{8})|(136[0-9]{8})|(137[0-9]{8})|(139[0-9]{8})|(150[0-9]{8})|(151[0-9]{8})|(152[0-9]{8})|(157[0-9]{8})|(158[0-9]{8})|(159[0-9]{8})|(182[0-9]{8})|(183[0-9]{8})|(184[0-9]{8})|(187[0-9]{8})|(188[0-9]{8})"
            + "|(1705[0-9]{7})|(178[0-9]{8})|(147[0-9]{8})";
    static String LT = "(130[0-9]{8})|(131[0-9]{8})|(132[0-9]{8})|(155[0-9]{8})|(156[0-9]{8})|(185[0-9]{8})|(186[0-9]{8})|(1709[0-9]{7})|(176[0-9]{8})|(145[0-9]{8})";
    static String DX = "(133[0-9]{8})|(1349[0-9]{7})|(153[0-9]{8})|(180[0-9]{8})|(181[0-9]{8})|(189[0-9]{8})|(1700[0-9]{7})|(177[0-9]{8})";
    //    static String YD = "^[1]{1}((([3]{1}[4-9]{1})|([5]{1}[012789]{1})|([8]{1}[23478]{1})|([4]{1}[7]{1}))[0-9]{8})|(705[0-9]{7})|(78[0-9]{8})$";
//    static String LT = "^[1]{1}(([3]{1}[0-2]{1})|(4{1}[5]{1})|([5]{1}[56]{1})|([7]{1}6|)|([8]{1}[56]{1}))[0-9]{8}$|(1709)[0-9]{7}";
//    static String DX = "^[1]{1}(([3]{1}([3]{1}|49)|([5]{1}[3]{1})|([8]{1}[019]{1}))[0-9]{8}$(|(1700[0-9]{7})|(177[0-9]]{8})|(1349[0-9]{7})";

    public static boolean isLegalPhone(String phone) {
        Log.d("debug", phone);
        phone = phone.trim();
        if (phone.length() == 11 && phone.matches(YD) || phone.matches(LT) || phone.matches(DX)) {
            return true;
        } else {
            return false;
        }
    }
}
