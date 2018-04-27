package example.jni.com.coffeeseller.MachineConfig;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import example.jni.com.coffeeseller.bean.MachineConfig;
import example.jni.com.coffeeseller.contentprovider.Constance;
import example.jni.com.coffeeseller.contentprovider.ConstanceMethod;
import example.jni.com.coffeeseller.httputils.JsonUtil;
import example.jni.com.coffeeseller.httputils.OkHttpUtil;
import example.jni.com.coffeeseller.model.listeners.MsgTransListener;
import example.jni.com.coffeeseller.parse.ParseRQMsg;
import example.jni.com.coffeeseller.parse.PayResult;
import example.jni.com.coffeeseller.utils.MyLog;


public class QRMsger {
    private DealRecorder dealRecorder;

    final static String TAG = "QRMsger";

    public QRMsger() {

    }

    public QRMsger(DealRecorder dealRecorder) {

        init(dealRecorder);
    }

    void init(DealRecorder dealRecorder) {
        this.dealRecorder = dealRecorder;
    }


    public void reqQR(final MsgTransListener mListener, final String payType) {

        Runnable mRun = new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub

                Map<String, Object> params = ConstanceMethod.getParams();
                params.put("formulaID", 21);//dealRecorder.getFormulaID()
                params.put("cupNum", dealRecorder.getRqcup());

                MyLog.d(TAG, "reqQR RQ_URL = " + Constance.GET_QR);
                MyLog.d(TAG, "reqQR params = " + JsonUtil.mapToJson(params));

                String RESPONSE_TEXT = null;
                for (int i = 0; i < 3; i++) {

                    try {
                        RESPONSE_TEXT = OkHttpUtil.post(Constance.GET_QR, JsonUtil.mapToJson(params));
                    } catch (IOException e) {

                        e.printStackTrace();
                    }
                    if (!TextUtils.isEmpty(RESPONSE_TEXT)) {

                        break;
                    }
                }
                MyLog.W(TAG, "reqQR start recieve data---RESPONSE_TEXT = " + RESPONSE_TEXT);
                if (!TextUtils.isEmpty(RESPONSE_TEXT)) {
                    ParseRQMsg parseRQMsg = ParseRQMsg.parseRQMsg(RESPONSE_TEXT);//解析收到的应答数据

                    mListener.onMsgArrived(parseRQMsg);
                } else {

                    mListener.onMsgArrived(null);
                }
            }
        };

        Thread t = new Thread(mRun);
        t.start();
    }

    public void checkPay(final MsgTransListener mListener, final String order) {


        Runnable mRun = new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub

                Map<String, Object> params = ConstanceMethod.getParams();
                params.put("tradeCode", order);

                MyLog.d(TAG, "reqCheckPay RQ_URL = " + Constance.CHECK_PAY);
                MyLog.d(TAG, "reqCheckPay params = " + JsonUtil.mapToJson(params));

                String RESPONSE_TEXT = null;
                for (int i = 0; i < 3; i++) {

                    try {
                        RESPONSE_TEXT = OkHttpUtil.post(Constance.CHECK_PAY, JsonUtil.mapToJson(params));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (RESPONSE_TEXT != null) {

                        break;
                    }
                }
                MyLog.W(TAG, "check pay data" + RESPONSE_TEXT);

                if (RESPONSE_TEXT != null) {

                    PayResult msg = PayResult.getPayResult(RESPONSE_TEXT);
                    mListener.onMsgArrived(msg);
                } else {

                    mListener.onMsgArrived(null);
                }
            }
        };
        Thread t = new Thread(mRun);
        t.start();
    }

   /* public void refund(final MsgTransListener mListener, final String order, final int failCups) {

        Runnable mRun = new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                String RQ_URL = REFUND_URL + "imei=" + IMEI + "&order=" + order + "&failCups=" + failCups;
                String RESPONSE_TEXT = mInternetVisitor.getDatabyGet(RQ_URL);
                if (RESPONSE_TEXT != null) {

                    boolean refundResult = praseResultMsg(RESPONSE_TEXT);
                    mListener.onMsgArrived(refundResult);

                } else {

                    mListener.onMsgArrived(false);
                }
            }
        };
        Thread t = new Thread(mRun);
        t.start();
    }
*/

   /* public void closeDeal(final MsgTransListener mListener, final String order) {

        Runnable mRun = new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                String RQ_URL = CLOSEDEAL_URL + "order=" + order;
                String RESPONSE_TEXT = mInternetVisitor.getDatabyGet(RQ_URL);
                if (RESPONSE_TEXT != null) {

                    Boolean closeResult = praseResultMsg(RESPONSE_TEXT);
                    mListener.onMsgArrived(closeResult);

                } else {

                    mListener.onMsgArrived(false);
                }
            }
        };
        Thread t = new Thread(mRun);
        t.start();
    }*/



    /*private CheckQRPAYMsg praseQRPayMsg(String content) {

        CheckQRPAYMsg msg = new CheckQRPAYMsg();
        try {
            JSONObject json = new JSONObject(content);
            msg.setResult(json.getBoolean("result"));
            msg.setOrder(json.getString("order"));
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return msg;
    }*/

    /*private Boolean praseResultMsg(String content) {

        Boolean refundResult = false;
        try {
            JSONObject json = new JSONObject(content);
            refundResult = json.getBoolean("result");
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return refundResult;
    }*/
}
