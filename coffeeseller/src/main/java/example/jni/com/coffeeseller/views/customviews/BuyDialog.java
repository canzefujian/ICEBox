package example.jni.com.coffeeseller.views.customviews;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.support.annotation.StyleRes;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.alibaba.fastjson.JSONArray;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import cof.ac.inter.ContainerConfig;
import example.jni.com.coffeeseller.MachineConfig.DealRecorder;
import example.jni.com.coffeeseller.MachineConfig.QRMsger;
import example.jni.com.coffeeseller.R;
import example.jni.com.coffeeseller.bean.Coffee;
import example.jni.com.coffeeseller.bean.CoffeeFomat;
import example.jni.com.coffeeseller.bean.ReportBunker;
import example.jni.com.coffeeseller.bean.Step;
import example.jni.com.coffeeseller.contentprovider.Constance;
import example.jni.com.coffeeseller.contentprovider.ConstanceMethod;
import example.jni.com.coffeeseller.contentprovider.MaterialSql;
import example.jni.com.coffeeseller.contentprovider.SharedPreferencesManager;
import example.jni.com.coffeeseller.databases.DealOrderInfoManager;
import example.jni.com.coffeeseller.httputils.JsonUtil;
import example.jni.com.coffeeseller.httputils.OkHttpUtil;
import example.jni.com.coffeeseller.model.CheckCurMachineState;
import example.jni.com.coffeeseller.model.ChooseCup;
import example.jni.com.coffeeseller.model.ClearMachine;
import example.jni.com.coffeeseller.model.Help;
import example.jni.com.coffeeseller.model.MkCoffee;
import example.jni.com.coffeeseller.model.listeners.ChooseCupListenner;
import example.jni.com.coffeeseller.model.listeners.MkCoffeeListenner;
import example.jni.com.coffeeseller.utils.MyLog;
import example.jni.com.coffeeseller.utils.ScreenUtil;
import example.jni.com.coffeeseller.utils.TextUtil;
import example.jni.com.coffeeseller.views.fragments.BuyFragment;

/**
 * Created by WH on 2018/3/22.
 */

public class BuyDialog extends Dialog implements ChooseCupListenner, MkCoffeeListenner {
    private static String TAG = "BuyDialog";
    public static int VIEW_CHOOSE_CUP = 0;
    public static int VIEW_HELP = 1;
    public static int MK_COFFEE = 2;
    private static long TIME_BEFORE_MK_TO_CLEAR = 10 * 60 * 1000;
    private Context context;
    private Coffee coffee;
    private Handler handler;
    private ChooseCupListenner chooseCupListenner;
    private MkCoffeeListenner mkCoffeeListenner;
    private BuyFragment fragment;
    private Timer clearMachineTimer;
    private TimerTask clearMachineTimerTask;
    private int curViewId = -1;

    private boolean dialogDisCancle = false;

    private long lastClearMachineTime = 0;
    private long lastMkTime = 0;

    public BuyDialog(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public BuyDialog(Context context, @StyleRes int themeResId) {
        super(context, themeResId);
        this.context = context;
        init();
    }

    public void init() {

        /*initView();*/
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setOnCancleTouchListenner(false);
        initData();
    }

    private void initView() {

        Window window = this.getWindow();
        WindowManager.LayoutParams wl = window.getAttributes();
        window.setTitle(null);
        window.setWindowAnimations(R.style.dialogWindowAnim);
        window.setBackgroundDrawableResource(android.R.color.transparent);
        wl.width = ScreenUtil.getScreenWidth(context) / 2;
        wl.height = WindowManager.LayoutParams.WRAP_CONTENT;
        wl.alpha = 0.9f;
        window.setAttributes(wl);

    }

    private void initData() {
        handler = new Handler();
        chooseCupListenner = this;
        mkCoffeeListenner = this;
    }

    public void setInitView(int viewID) {
        if (viewID == VIEW_CHOOSE_CUP) {
            final ChooseCup chooseCup = new ChooseCup(context, coffee, chooseCupListenner, handler);
            setContentView(chooseCup.getView());
            setOnCancleTouchListenner(true);
            setOnDismissListener(new OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (!dialogDisCancle) {
                        chooseCupListenner.cancle(chooseCup.getOrder());
                        chooseCup.stopTimer();
                    }
                }
            });

            curViewId = VIEW_CHOOSE_CUP;
            MyLog.W(TAG, "choose cup view is added");

        } else if (viewID == VIEW_HELP) {
            Help help = new Help(context, this);
            setContentView(help.getView());

            setOnCancleTouchListenner(true);

            curViewId = VIEW_HELP;

            MyLog.W(TAG, "help view is added");
        }
    }

    public void setInitData(BuyFragment buyFragment, Coffee coffee) {
        this.fragment = buyFragment;
        this.coffee = coffee;
        setInitView(VIEW_CHOOSE_CUP);

        MyLog.W(TAG, coffee.getName() + " has been selected !");
    }

    public int getCurViewId() {
        return curViewId;
    }

    public void showDialog() {
        if (!isShowing())
            show();

        initView();
    }

    public void disDialog() {

        stopTaskClearMachine();
        curViewId = -1;
        if (isShowing()) {
            dismiss();
        }

    }

    public void setOnCancleTouchListenner(boolean canCancle) {
        setCanceledOnTouchOutside(canCancle);
    }


    /*
* 清洗机器
* */
    public void clearMachine(final List<ContainerConfig> containerConfigs) {
        stopTaskClearMachine();
        MyLog.d(TAG, "clearMachine");
        lastClearMachineTime = System.currentTimeMillis();
        if (clearMachineTimer == null) {
            clearMachineTimer = new Timer();
        }
        if (clearMachineTimerTask == null) {
            clearMachineTimerTask = new TimerTask() {
                @Override
                public void run() {

                    if (CheckCurMachineState.getInstance().isCupShelfRightPlaceClearMachineTest()) {
//                        boolean isResultSuccess = ClearMachine.clearMachineByHotWater(100, 0);
                        boolean isOver = ClearMachine.clearMachineAllModule(containerConfigs);
                        if (System.currentTimeMillis() - lastClearMachineTime > 50 * 1000) {

                            MyLog.d(TAG, "clearMachine over 50s");
                            stopTaskClearMachine();
                            return;
                        }
                        if (isOver) {
                            MyLog.d(TAG, "clearMachine success!");
                            stopTaskClearMachine();
                        } else {
                            MyLog.d(TAG, "clearMachine failed!");
                        }
                    }
                }
            };
        }
        clearMachineTimer.schedule(clearMachineTimerTask, 0, 2000);
    }

    public void stopTaskClearMachine() {

        if (clearMachineTimer != null) {

            clearMachineTimer.cancel();
        }
        clearMachineTimer = null;
        clearMachineTimerTask = null;
    }


    //更新数据库原料表
    private String updateMaterial(DealRecorder dealRecorder) {


        List<ReportBunker> bunkers = new ArrayList<>();
        if (coffee == null || coffee.getStepList() == null || coffee.getStepList().size() <= 0) {
            return "";
        }
        if (dealRecorder == null || dealRecorder.getContainerConfigs() == null || dealRecorder.getContainerConfigs().size() <= 0) {
            return "";
        }
        MaterialSql materialSql = new MaterialSql(context);

        int waterUseTotal = 0;
        String waterMaterialId = materialSql.getMaterialIDByContainerID("7");
        ;
        for (int i = 0; i < coffee.getStepList().size(); i++) {
            Step step = coffee.getStepList().get(i);


            if (step != null)//&& step.getContainerConfig().getWater_capacity() == 0

                if (step.getMaterial() != null) {


                    if (step.getContainerConfig().getWater_capacity() != 0 || TextUtils.equals(waterMaterialId, step.getMaterial().getMaterialID() + "")) {

                        //更新用水量
                        waterUseTotal += step.getContainerConfig().getWater_capacity();

                    }


                    MyLog.d(TAG, "materialID is " + step.getMaterial().getMaterialID());

                    String sqlRestMaterial = materialSql.getStorkByMaterialID(step.getMaterial().getMaterialID() + "");

                    float sqlRestMaterialInt = Float.parseFloat(sqlRestMaterial);

                    if (step.getContainerConfig().getMaterial_time() == 0) {
                        continue;
                    }
                    float mkUseMaterialInt = dealRecorder.getContainerConfigs().get(i).getMaterial_time() / step.getContainerConfig().getMaterial_time() * step.getAmount();

                    if (mkUseMaterialInt == 0) {
                        continue;
                    }

                    Log.e(TAG, " materialID is  " + step.getMaterial().getMaterialID() + " stock is " + sqlRestMaterial + ",used= " + mkUseMaterialInt);

                    Long stock = Long.parseLong((int) (sqlRestMaterialInt - mkUseMaterialInt) + "");

                    boolean isUpdateSuccess = materialSql.updateMaterialStockByMaterialId(step.getMaterial().getMaterialID() + "", stock + "");

                    MyLog.W(TAG, "update material is " + isUpdateSuccess + ", materialId=" + step.getMaterial().getMaterialID()
                            + ", usedMaterial = " + mkUseMaterialInt + " sqlRestMaterial= " + sqlRestMaterialInt + ", stock=" + (sqlRestMaterialInt - mkUseMaterialInt));

                    ReportBunker reportBunker = new ReportBunker();
                    int bunkerId = Integer.parseInt(materialSql.getBunkerIDByMaterialD(step.getMaterial().getMaterialID() + ""));
                    reportBunker.setBunkerID(bunkerId);
                    reportBunker.setUnit(Math.round(mkUseMaterialInt) + "");
                    reportBunker.setMaterialStock(Math.round((sqlRestMaterialInt - mkUseMaterialInt)) + "");

                    bunkers.add(reportBunker);
                }

        }
/*
* 更新用水量
* */
        if (waterUseTotal != 0 && waterMaterialId != null && !TextUtils.isEmpty(waterMaterialId)) {

            String sqlRestWaterMaterial = materialSql.getStorkByMaterialID(waterMaterialId + "");

            float sqlRestWaterMaterialInt = Float.parseFloat(sqlRestWaterMaterial);


            int waterStock = (int) (sqlRestWaterMaterialInt - waterUseTotal / 10);

            boolean isUpdateSuccess = materialSql.updateMaterialStockByMaterialId(waterMaterialId + "", waterStock + "");

            MyLog.d(TAG, "用水量更新：materialID is" + waterMaterialId + " , rest= "
                    + sqlRestWaterMaterialInt + ", used= " + waterUseTotal / 10
                    + " , stock= " + waterStock + ", isUpdateSuccess= " + isUpdateSuccess);


            ReportBunker reportBunker = new ReportBunker();
            int bunkerId = Integer.parseInt(materialSql.getBunkerIDByMaterialD(waterMaterialId + ""));
            reportBunker.setBunkerID(bunkerId);
            reportBunker.setUnit(Math.round(waterUseTotal) + "");
            reportBunker.setMaterialStock(Math.round(waterStock) + "");

            bunkers.add(reportBunker);

            //更新杯子

            String cupBunkerId = materialSql.getBunkerIDByContainerID("8");
            String cupNum = materialSql.getStorkByBunkersID(cupBunkerId);
            if (!TextUtils.isEmpty(cupNum)) {
                int cupNumInt = Integer.parseInt(cupNum);
                if (cupNumInt > 0) {
                    materialSql.updateContact(cupBunkerId, "", "", "", "", "", (cupNumInt - 1) + "", "", "", "", "");
                }
            }
        }




        /*
        * 更新数据库本地交易数据库bunker
        * */
        JSONArray jsonArray = (JSONArray) com.alibaba.fastjson.JSONObject.toJSON(bunkers);
        String array = jsonArray.toString();
        dealRecorder.setBunkers(array);

        DealOrderInfoManager.getInstance(context).update(dealRecorder);

        MyLog.W(TAG, "updateMaterial is over");
        return array;
    }


    private void cancleOrder(String order) {
        Map<String, Object> params = ConstanceMethod.getParams();
        params.put("tradeCode", order);

        MyLog.d(TAG, "cancleOrder RQ_URL = " + Constance.TRADE_CLOSE);
        MyLog.d(TAG, "cancleOrder params = " + JsonUtil.mapToJson(params));

        String RESPONSE_TEXT = null;
        try {
            RESPONSE_TEXT = OkHttpUtil.post(Constance.TRADE_CLOSE, JsonUtil.mapToJson(params));
        } catch (IOException e) {
            e.printStackTrace();
        }

        MyLog.W(TAG, "cancleOrder data" + RESPONSE_TEXT);

        if (RESPONSE_TEXT != null) {


        } else {

        }
    }

    @Override
    public void cancle(final String order) {
        //通知服务器取消订单

        if (TextUtils.isEmpty(order)) {

        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    cancleOrder(order);
                }
            }).start();
        }


        MyLog.d(TAG, "choosecup is cancle");

        disDialog();
    }

    @Override
    public void paying() {

    }

    @Override
    public void hasPay(final CoffeeFomat coffeeFomat, final DealRecorder dealRecorder) {


        if (!isShowing()) {
            return;
        }

        //本地保存交易记录
        DealOrderInfoManager.getInstance(context).addToTable(dealRecorder);

        final CoffeeFomat fomat = coffeeFomat;

        handler.post(new Runnable() {
            @Override
            public void run() {

                dialogDisCancle = true;

                setOnCancleTouchListenner(false);

                MkCoffee mkCoffee = new MkCoffee(context, fomat, dealRecorder, mkCoffeeListenner, handler);
                setContentView(mkCoffee.getView());

                //上次制作后长时间没有制作就清洗机器
                if (System.currentTimeMillis() - lastMkTime > TIME_BEFORE_MK_TO_CLEAR) {
                    //清洗机器
                    boolean isOver = ClearMachine.clearMachineAllModule(coffeeFomat.getContainerConfigs());

                    mkCoffee.startMkCoffee();
                }else{
                    mkCoffee.startMkCoffee();
                }

                curViewId = MK_COFFEE;

            }
        });
    }

    @Override
    public void getMkResult(final DealRecorder dealRecorder, final boolean success, final boolean isCalculateMaterial) {

        lastMkTime = System.currentTimeMillis();

        MyLog.d(TAG, "getMkResult been called!");
        final DealRecorder recorder = dealRecorder;

        dialogDisCancle = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
//                List<ReportBunker> bunkers = new ArrayList<ReportBunker>();
                String bunkers = "";
                if (isCalculateMaterial) {

                    MyLog.d(TAG, "isCalculateMaterial= " + isCalculateMaterial);

                    //更新数据库原料表
                    bunkers = updateMaterial(recorder);

                }

                //上报交易结果给服务器

                QRMsger qrMsger = new QRMsger();
                DealRecorder newDealRecorder = qrMsger.reportTradeToServer(recorder, bunkers);


                //更新本地交易记录
                DealOrderInfoManager.getInstance(context).update(newDealRecorder);

                if (!success && !isCalculateMaterial) {

                } else {

                    //清洗机器
                    clearMachine(dealRecorder.getContainerConfigs());
                }

            }
        }).start();


        //更新BuyFragment ui
        if (fragment != null) {
            fragment.updateUi();
        }


        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                disDialog();
            }
        }, 3000);

    }


}
