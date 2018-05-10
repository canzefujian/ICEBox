package example.jni.com.coffeeseller.model;

import android.content.Context;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import cof.ac.inter.CoffMsger;
import cof.ac.inter.ContainerConfig;
import cof.ac.inter.MachineState;
import cof.ac.inter.MajorState;
import cof.ac.inter.Result;
import cof.ac.inter.StateEnum;
import cof.ac.util.DataSwitcher;
import example.jni.com.coffeeseller.MachineConfig.CoffeeMakeState;
import example.jni.com.coffeeseller.MachineConfig.DealRecorder;
import example.jni.com.coffeeseller.MachineConfig.QRMsger;
import example.jni.com.coffeeseller.R;
import example.jni.com.coffeeseller.bean.CoffeeFomat;
import example.jni.com.coffeeseller.bean.CoffeeMakeStateRecorder;
import example.jni.com.coffeeseller.databases.DealOrderInfoManager;
import example.jni.com.coffeeseller.model.listeners.MkCoffeeListenner;
import example.jni.com.coffeeseller.model.listeners.MsgTransListener;
import example.jni.com.coffeeseller.utils.MyLog;
import example.jni.com.coffeeseller.utils.TextUtil;
import example.jni.com.coffeeseller.utils.Waiter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by WH on 2018/4/25.
 */

public class MkCoffee {
    private static String TAG = "MkCoffee";
    private long MAX_TOTAL_MK_TIME = 180000;
    private long MAX_STATE_TIME = 5000;
    private long MK_TIP_TIME = 180000;
    private int MAX_PROGRESS = 100;
    private int MAX_MAKING_PROGRESS = 92;
    private int CONTAIN_MAKING_PROGRESS_TIME = 550;
    private Context context;
    private MakingViewHolder makingViewHolder;
    private CoffMsger coffMsger;
    private CoffeeMakeStateRecorder coffeeMakeStateRecorder;

    private CoffeeFomat coffeeFomat;
    private DealRecorder dealRecorder;
    private MkCoffeeListenner mkCoffeeListenner;
    private Handler handler;
    private CountDownTimer mkCountDownTimer;
    private Timer clearTimer;
    private TimerTask clearTimerTask;

    private boolean isStartMaking = false;
    private boolean makeSuccess = false;
    private boolean isCalculateMaterial = true;
    private boolean isSendMkingComdSuccess = false;
    private boolean isMkingOver = false;

    private long lastStateTime;
    private long totalMakingTime = 0;
    private StringBuffer buffer;

    public MkCoffee(Context context, CoffeeFomat coffeeFomat, DealRecorder dealRecorder, MkCoffeeListenner mkCoffeeListenner, Handler handler) {
        this.context = context;
        this.coffeeFomat = coffeeFomat;
        this.dealRecorder = dealRecorder;
        this.mkCoffeeListenner = mkCoffeeListenner;
        this.handler = handler;
        init();
    }

    public void init() {

        initView();
        initData();
    }

    private void initView() {
        if (makingViewHolder == null) {
            makingViewHolder = new MakingViewHolder();
        }
    }

    public void initData() {

        if (coffeeFomat != null && dealRecorder != null) {

            dealRecorder.getContainerConfigs().clear();
            dealRecorder.getContainerConfigs().addAll(coffeeFomat.getContainerConfigs());


            /*
            * 便于查看步骤
            * */
            int waterTotal = 0;
            for (int i = 0; i < dealRecorder.getContainerConfigs().size(); i++) {
                ContainerConfig containerConfig = dealRecorder.getContainerConfigs().get(i);

                if (containerConfig.getWater_capacity() != 0) {
                    waterTotal += containerConfig.getWater_capacity();
                    if (containerConfig.getMaterial_time() == 0) {
                        CONTAIN_MAKING_PROGRESS_TIME += 5 * 1000;//出水运作5s

                    } else {
                        CONTAIN_MAKING_PROGRESS_TIME += 5 * 1000;//出水运作5s
                        CONTAIN_MAKING_PROGRESS_TIME += containerConfig.getMaterial_time() * 10 + 10 * 1000;//每个步骤机器运作大概8s
                    }

                } else {
                    CONTAIN_MAKING_PROGRESS_TIME += containerConfig.getMaterial_time() * 10 + 10 * 1000;//每个步骤机器运作大概8s
                }
                MyLog.d(TAG, "getContainer=" + containerConfig.getContainer());
                MyLog.d(TAG, "getWater_capacity=" + containerConfig.getWater_capacity());
                MyLog.d(TAG, "getMaterial_time=" + containerConfig.getMaterial_time());
                MyLog.d(TAG, "getWater_type=" + containerConfig.getWater_type());
                MyLog.d(TAG, "getContainer_id=" + containerConfig.getContainer_id());
                MyLog.d(TAG, "getRotate_speed=" + containerConfig.getRotate_speed());
                MyLog.d(TAG, "getStir_speed=" + containerConfig.getStir_speed());
                MyLog.d(TAG, "getWater_interval=" + containerConfig.getWater_interval());
            }
            CONTAIN_MAKING_PROGRESS_TIME += waterTotal * 10 / 10;

            //    CONTAIN_MAKING_PROGRESS_TIME += 30 * 1000;

            MyLog.d(TAG, "CONTAIN_MAKING_PROGRESS_TIME= " + CONTAIN_MAKING_PROGRESS_TIME);

            String tasteNameAndRadio = "";
            for (int i = 0; i < coffeeFomat.getTasteNameRatio().size(); i++) {
                tasteNameAndRadio += (coffeeFomat.getTasteNameRatio().get(i) + ",");
            }
            dealRecorder.setTasteRadio(tasteNameAndRadio);
            dealRecorder.setRqTempFormat(coffeeFomat.getWaterType() + "");
            DealOrderInfoManager.getInstance(context).update(dealRecorder);
        }

        coffMsger = CoffMsger.getInstance();
        buffer = new StringBuffer();

        MyLog.W(TAG, "enter mkCoffee page");

        makingViewHolder.mCoffeeName.setText(coffeeFomat.getCoffeeName());


        countDownTime();
        //    startMkCoffee();


    }

    public View getView() {
        return makingViewHolder.view;
    }

    public void startMkCoffee() {
        if (coffeeMakeStateRecorder == null) {
            coffeeMakeStateRecorder = new CoffeeMakeStateRecorder();
        }
        coffeeMakeStateRecorder.init();
        if (coffeeFomat != null && coffeeFomat.getContainerConfigs().size() >= 0) {
            buffer.setLength(0);

            //     countDownTime();

//            isStartMkCoffee();

            mkCoffee();
        } else {

            buffer.append("\n");
            buffer.append("没有设置配方");


            showErr(true);

            if (mkCoffeeListenner != null) {
                dealRecorder.setMakeSuccess(false);
                mkCoffeeListenner.getMkResult(dealRecorder, false, isCalculateMaterial);
                stopCountDownTimer();
            }

            MyLog.W(TAG, "containerConfigs is null");
        }
    }

    /*
   * 倒计时检测
   * */
    private void countDownTime() {

        stopCountDownTimer();

        mkCountDownTimer = new CountDownTimer(MK_TIP_TIME, 1000) {
            @Override
            public void onTick(final long millisUntilFinished) {

                makingViewHolder.mMkTimerCount.setText(millisUntilFinished / 1000 + " s");

                if (!isSendMkingComdSuccess) {
                    makingViewHolder.mErrTip.setVisibility(View.VISIBLE);
                    makingViewHolder.mProgressBarLayout.setVisibility(GONE);
                    makingViewHolder.mErrTip.setText(TextUtil.textPointNum("清洗中", (int) (millisUntilFinished / 1000 % 3)));
                } else {
                    makingViewHolder.mErrTip.setVisibility(View.GONE);
                    makingViewHolder.mProgressBarLayout.setVisibility(VISIBLE);
                }
            }

            @Override
            public void onFinish() {

            }
        };
        mkCountDownTimer.start();
    }


    private void stopCountDownTimer() {
        if (mkCountDownTimer != null) {
            mkCountDownTimer.cancel();
            mkCountDownTimer = null;
        }
    }

    public void updateProgress() {

        if (coffeeMakeStateRecorder.state == CoffeeMakeState.COFFEE_ISMAKING && !isStartMaking) {

            isStartMaking = true;
            updateProgressAnim();

        } else if (coffeeMakeStateRecorder.state == CoffeeMakeState.COFFEE_DOWN_POWER && !isStartMaking) {

            isStartMaking = true;
            updateProgressAnim();

        } else if (coffeeMakeStateRecorder.state == CoffeeMakeState.COFFEEFINISHED_CUPNOTAKEN && isStartMaking) {

            isStartMaking = false;
            makeSuccess = true;
            updateProgressAnim();

        }
    }

    public void stopClearTimerTask() {

        if (clearTimer != null) {

            clearTimer.cancel();
        }
        clearTimer = null;
        clearTimerTask = null;
    }

    private void isStartMkCoffee() {

        if (clearTimer == null) {

            clearTimer = new Timer(true);
        }
        if (clearTimerTask == null) {

            clearTimerTask = new TimerTask() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub

                    MachineState machineState = coffMsger.getLastMachineState();
                    MajorState majorState = machineState.getMajorState();

                    CheckCurMachineState.getInstance().checkCurState(majorState);

                    if (CheckCurMachineState.getInstance().isClearing()) {
                        makingViewHolder.mErrTip.setVisibility(View.VISIBLE);
                        makingViewHolder.mProgressBarLayout.setVisibility(View.GONE);
                        makingViewHolder.mErrTip.setText("清洗中，请稍后...");
                    } else {
                        MyLog.d(TAG, " isClearing =false");
                        makingViewHolder.mErrTip.setVisibility(View.GONE);
                        makingViewHolder.mProgressBarLayout.setVisibility(View.VISIBLE);
                        stopClearTimerTask();
                        mkCoffee();
                    }
                }
            };

            clearTimer.schedule(clearTimerTask, 0, 1000);//交易之后20秒进行查询交易状态,每隔5秒查询一次
        }


    }


    /*
    * 制作咖啡
    * */
    private void mkCoffee() {

        MyLog.W(TAG, "start making");

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                buffer.setLength(0);
                buffer.append("提示：");
                MachineState machineState = null;
                lastStateTime = System.currentTimeMillis();
                totalMakingTime = System.currentTimeMillis();
                while (true) {

                    machineState = coffMsger.getLastMachineState();

                    if (isMkTimeOut()) {

                        if (coffeeMakeStateRecorder.state == null
                                || coffeeMakeStateRecorder.state == CoffeeMakeState.COFFEEFINISHED_CUPNOTAKEN
                                || coffeeMakeStateRecorder.state == CoffeeMakeState.COFFEEFINISHED_CUPISTAKEN) {
                            MyLog.W(TAG, " make successed but time is too long ");

                   /*         if (mkCoffeeListenner != null) {
                                dealRecorder.setMakeSuccess(true);
                                mkCoffeeListenner.getMkResult(dealRecorder, false, isCalculateMaterial);
                                stopCountDownTimer();
                            }*/
                        } else {
                            if (coffeeMakeStateRecorder.state == CoffeeMakeState.COFFEE_DOWN_CUP) {

                                MyLog.W(TAG, " make failed and time is too long,down cup state is remaining ");

                                //   if (mkCoffeeListenner != null) {
                                dealRecorder.setMakeSuccess(false);
                                isCalculateMaterial = false;
                                if (!isMkingOver) {
                                    mkCoffeeListenner.getMkResult(dealRecorder, false, isCalculateMaterial);
                                }
                                stopCountDownTimer();
                                break;
                                //      }

                            } else if (coffeeMakeStateRecorder.state == CoffeeMakeState.COFFEE_ISMAKING
                                    || coffeeMakeStateRecorder.state == CoffeeMakeState.COFFEE_DOWN_POWER) {

                                MyLog.W(TAG, " make failed and time is too long,other state is remaining ");

                                if (machineState.hasCupOnShelf()) {
                                    dealRecorder.setMakeSuccess(false);
                                } else {
                                    dealRecorder.setMakeSuccess(true);
                                }

                                //    if (mkCoffeeListenner != null) {
                                isCalculateMaterial = true;
                                if (!isMkingOver) {
                                    mkCoffeeListenner.getMkResult(dealRecorder, dealRecorder.isMakeSuccess(), isCalculateMaterial);
                                }

                                stopCountDownTimer();

                                break;
                                //      }

                            }
                        }

                        break;
                    }
                    if (DealMachineState(machineState)) {

                        if (machineState.getMajorState().getCurStateEnum() == StateEnum.HAS_ERR) {

                            coffeeMakeStateRecorder.state = CoffeeMakeState.COFFEEMAKING_FAILED;
                            //     isCalculateMaterial = false;

                            buffer.append("\n");
                            buffer.append("制作过程中接收到0a : " + machineState.getMajorState().getHighErr_byte());

                        }
                    } else {

                        continue;
                    }


                    if (coffeeMakeStateRecorder.state == null && !isSendMkingComdSuccess) {

                        MyLog.d(TAG, "send mkCoffee comd");

                        Result result = coffMsger.mkCoffee(coffeeFomat.getContainerConfigs());
                        if (result.getCode() == Result.SUCCESS) {

                            coffeeMakeStateRecorder.state = CoffeeMakeState.COFFEE_MAKE_INIT;

                            isSendMkingComdSuccess = true;

                        } else {

                            coffeeMakeStateRecorder.state = CoffeeMakeState.COFFEEMAKING_FAILED;
                            isCalculateMaterial = false;
                            buffer.append("/n");
                            buffer.append("发送咖啡制作指令，返回" + result.getErrDes());
                        }

                    }

                    if (coffeeMakeStateRecorder.state == CoffeeMakeState.COFFEE_MAKE_INIT) {

                        dealStateMakeInit(machineState);

                        MyLog.W(TAG, " cur state is init");
                        continue;
                    }
                    if (coffeeMakeStateRecorder.state == CoffeeMakeState.COFFEE_DOWN_CUP) {

                        dealStateDownCup(machineState);

                        continue;
                    }

                    if (coffeeMakeStateRecorder.state == CoffeeMakeState.COFFEE_ISMAKING) {

                        dealMakingState(machineState);

                        updateProgress();

//                        MyLog.W(TAG, " cur state is making");

                        continue;
                    }

                    if (coffeeMakeStateRecorder.state == CoffeeMakeState.COFFEE_DOWN_POWER) {

                        dealStateDownPower(machineState);

                        updateProgress();

//                        MyLog.W(TAG, " cur state is down power");

                        continue;
                    }

                    if (coffeeMakeStateRecorder.state == CoffeeMakeState.COFFEEFINISHED_CUPNOTAKEN) {
                        //上报交易记录

                        updateProgress();

                        //  if (mkCoffeeListenner != null) {
                        dealRecorder.setMakeSuccess(true);
                        mkCoffeeListenner.getMkResult(dealRecorder, true, isCalculateMaterial);
                        isMkingOver = true;
                        stopCountDownTimer();

                        //    }

//                        MyLog.W(TAG, "cur state is finish ");

                        break;

                    }
                    if (coffeeMakeStateRecorder.state == CoffeeMakeState.COFFEEFINISHED_CUPISTAKEN) {

                        coffeeMakeStateRecorder.state = null;

                        isMkingOver = false;

//                        MyLog.d(TAG, "cur state is take cup ");

                        break;

                    }
                    if (coffeeMakeStateRecorder.state == CoffeeMakeState.COFFEEMAKING_FAILED) {

                        showErr(true);

                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //     if (mkCoffeeListenner != null) {
                                dealRecorder.setMakeSuccess(false);
                                mkCoffeeListenner.getMkResult(dealRecorder, false, isCalculateMaterial);
                                isMkingOver = true;
                                stopCountDownTimer();
                                //        }
                            }
                        }, 4000);


                        MyLog.d(TAG, "cur state is failed ");
                        break;
                    }
                }
            }
        };
        new Thread(runnable).start();

    }


    protected void dealStateDownCup(MachineState machineState) {

        if (machineState.getMajorState().getCurStateEnum() == StateEnum.MAKING) {

            coffeeMakeStateRecorder.state = CoffeeMakeState.COFFEE_ISMAKING;

            MyLog.W(TAG, " cur state is making");
        } else if (machineState.getMajorState().getCurStateEnum() == StateEnum.DOWN_POWER) {

            coffeeMakeStateRecorder.state = CoffeeMakeState.COFFEE_DOWN_POWER;

            MyLog.W(TAG, " cur state is down power");
        } else if (machineState.getMajorState().getCurStateEnum() == StateEnum.FINISH) {

            coffeeMakeStateRecorder.state = CoffeeMakeState.COFFEEFINISHED_CUPNOTAKEN;

            MyLog.W(TAG, " cur state is finish");
        }

    }

    private boolean dealMakingState(MachineState machineState) {

        if (machineState.getMajorState().getCurStateEnum() == StateEnum.DOWN_POWER) {

            coffeeMakeStateRecorder.state = CoffeeMakeState.COFFEE_DOWN_POWER;

            MyLog.W(TAG, " cur state is down power");
            return true;
        } else if (machineState.getMajorState().getCurStateEnum() == StateEnum.FINISH) {

            coffeeMakeStateRecorder.state = CoffeeMakeState.COFFEEFINISHED_CUPNOTAKEN;

            MyLog.W(TAG, " cur state is finish");
            return true;
        }

        return false;
    }

    protected void dealStateDownPower(MachineState machineState) {

        if (machineState.getMajorState().getCurStateEnum() == StateEnum.MAKING) {

            coffeeMakeStateRecorder.state = CoffeeMakeState.COFFEE_ISMAKING;

            MyLog.W(TAG, " cur state is making");
        } else if (machineState.getMajorState().getCurStateEnum() == StateEnum.FINISH) {

            coffeeMakeStateRecorder.state = CoffeeMakeState.COFFEEFINISHED_CUPNOTAKEN;

            MyLog.W(TAG, " cur state is finish");
        }
    }

    protected void dealStateMakeInit(MachineState machineState) {
        if (machineState.getMajorState().getCurStateEnum() == StateEnum.DOWN_CUP) {

            coffeeMakeStateRecorder.state = CoffeeMakeState.COFFEE_DOWN_CUP;

            MyLog.W(TAG, " cur state is down cup");
        }
    }

    private boolean DealMachineState(MachineState curState) {

        if (curState.getResult().getCode() == Result.SUCCESS) {

            lastStateTime = System.currentTimeMillis();
            return true;
        } else {
            if (isTimeOut()) {

                coffeeMakeStateRecorder.state = CoffeeMakeState.COFFEEMAKING_FAILED;
                isCalculateMaterial = false;
            }
            return false;
        }
    }

    private boolean isTimeOut() {

        long curTime = System.currentTimeMillis();
        if (curTime - lastStateTime > MAX_STATE_TIME) {

            buffer.append("\n");
            buffer.append("5s内接收的状态没有更新");
            return true;
        }
        return false;
    }

    private boolean isMkTimeOut() {

        if (System.currentTimeMillis() - totalMakingTime > MAX_TOTAL_MK_TIME) {

            return true;
        }
        return false;
    }

    public void updateProgressAnim() {

        new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                if (!makeSuccess) {
                    for (int i = 0; i <= MAX_MAKING_PROGRESS; i++) {

                        publishProgress(i);

                        if (i == MAX_MAKING_PROGRESS) {
                            break;
                        }
                        if (makeSuccess) {
                            publishProgress(MAX_PROGRESS);
                            break;
                        }
                        Waiter.doWait(CONTAIN_MAKING_PROGRESS_TIME / MAX_MAKING_PROGRESS);
                    }
                } else {
                    publishProgress(MAX_PROGRESS);
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {

                setProgress(values[0]);
                super.onProgressUpdate(values);

            }
        }.execute();
    }

    public void setProgress(int progress) {

        makingViewHolder.mProgressBar.setProgress(progress);
        if (progress < MAX_PROGRESS / 2) {
            makingViewHolder.mTipOne.setVisibility(VISIBLE);
            makingViewHolder.mTipTwo.setVisibility(View.INVISIBLE);
            makingViewHolder.mTipThress.setVisibility(View.INVISIBLE);
        } else if (progress >= MAX_PROGRESS / 2 && progress < MAX_PROGRESS) {
            makingViewHolder.mTipOne.setVisibility(View.INVISIBLE);
            makingViewHolder.mTipTwo.setVisibility(View.VISIBLE);
            makingViewHolder.mTipThress.setVisibility(View.INVISIBLE);
            makingViewHolder.mMakeCenterImage.setImageResource(R.mipmap.making);
        } else {
            makingViewHolder.mTipOne.setVisibility(View.INVISIBLE);
            makingViewHolder.mTipTwo.setVisibility(View.INVISIBLE);
            makingViewHolder.mTipThress.setVisibility(View.VISIBLE);
            makingViewHolder.mMakeCenterImage.setImageResource(R.mipmap.make_finish);

        }
    }

    public void showErr(boolean isErr) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                makingViewHolder.mErrTip.setVisibility(VISIBLE);
                makingViewHolder.mProgressBarLayout.setVisibility(GONE);
                makingViewHolder.mErrTip.setText(buffer.toString());

                MyLog.W(TAG, "making err: " + buffer.toString());
            }
        });

    }

    class MakingViewHolder {
        public View view;
        public TextView mErrTip;
        public LinearLayout mIsMakeLayout;
        public ImageView mMakeCenterImage;
        public TextView mMkTimerCount;
        public TextView mCoffeeName;
        public LinearLayout mProgressBarLayout;
        public ProgressBar mProgressBar;
        public LinearLayout mTextLayout;
        public LinearLayout mTipOne;
        public LinearLayout mTipTwo;
        public LinearLayout mTipThress;


        public MakingViewHolder() {
            initView();
        }

        private void initView() {
            view = LayoutInflater.from(context).inflate(R.layout.make_out_layout, null);
            mErrTip = (TextView) view.findViewById(R.id.errTip);
            mMakeCenterImage = (ImageView) view.findViewById(R.id.makeCenterImage);

            mProgressBarLayout = (LinearLayout) view.findViewById(R.id.progressBarLayout);
            mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
            mTextLayout = (LinearLayout) view.findViewById(R.id.textLayout);
            mTipOne = (LinearLayout) view.findViewById(R.id.tipOne);
            mTipTwo = (LinearLayout) view.findViewById(R.id.tipTwo);
            mTipThress = (LinearLayout) view.findViewById(R.id.tipThress);
            mMkTimerCount = (TextView) view.findViewById(R.id.mkTimerCount);
            mCoffeeName = (TextView) view.findViewById(R.id.coffeeName);

            mTipTwo.setVisibility(View.INVISIBLE);
            mTipThress.setVisibility(View.INVISIBLE);

        }

    }
}
