package example.jni.com.coffeeseller.factory;


import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import example.jni.com.coffeeseller.views.fragments.BasicFragment;
import example.jni.com.coffeeseller.views.fragments.BuyFragment;
import example.jni.com.coffeeseller.views.fragments.ConfigFragment;
import example.jni.com.coffeeseller.views.fragments.DebugFragment;
import example.jni.com.coffeeseller.views.fragments.HomeFragment;
import example.jni.com.coffeeseller.views.fragments.LoginFragment;
import example.jni.com.coffeeseller.views.fragments.MachineCheckFragment;

/**
 * Created by Administrator on 2018/4/11.
 */

public class FragmentFactory {

    public static FragmentFactory mInstance;
    public static Bundle mSavedInstanceState;
    public static FragmentEnum curPage = null;
    public static FragmentEnum lastPage = null;

    public static synchronized FragmentFactory getInstance() {
        if (mInstance == null) {
            mInstance = new FragmentFactory();
            if (mSavedInstanceState == null) {
                mSavedInstanceState = new Bundle();
            }
        }
        return mInstance;
    }

    public Bundle putLayoutId(int layoutId) {
        mSavedInstanceState.putInt("layout_id", layoutId);
        return mSavedInstanceState;
    }

    public Bundle getSavedBundle() {
        return mSavedInstanceState;
    }

    HashMap<FragmentEnum, BasicFragment> mFragMap;
    ArrayList<BasicFragment> mFragList;

    public FragmentFactory() {

        mFragMap = new HashMap<>();
        mFragList = new ArrayList<>();
    }

    public BasicFragment getFragment(FragmentEnum pageType) {

        BasicFragment mFrag = mFragMap.get(pageType);
        if (mFrag == null) {

            switch (pageType) {
                case ChooseCupNumFragment:
                    mFrag = new BuyFragment();
                    break;
                case MachineCheckFragment:
                    mFrag = new MachineCheckFragment();
                    break;
                case LoginFragment:
                    mFrag = new LoginFragment();
                    break;
                case ConfigFragment:
                    mFrag = new ConfigFragment();
                    break;
                case DebugFragment:
                    mFrag = new DebugFragment();
                    break;
                case HomeFragement:
                    mFrag = new HomeFragment();
            }
            if (mFrag != null) {

                mFragMap.put(pageType, mFrag);
                mFragList.add(mFrag);
            }
        }
        return mFrag;
    }

    public List<BasicFragment> getFragments() {

        return mFragList;

    }
}