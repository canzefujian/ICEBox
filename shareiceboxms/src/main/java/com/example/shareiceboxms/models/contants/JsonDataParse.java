package com.example.shareiceboxms.models.contants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by WH on 2017/12/20.
 */

public class JsonDataParse {
    private static JsonDataParse instance;
    private int totalNum;
    private int curPage;
    private int totalPage;
    private int requestNum;

    public static synchronized JsonDataParse getInstance() {
        if (instance == null) {
            instance = new JsonDataParse();
        }
        return instance;
    }

    /*
    *
    * 解析列表
    * */
    public JSONArray getArrayList(String response) throws JSONException {
        JSONObject jsonObject = new JSONObject(response.toString());
        JSONObject jsonD = jsonObject.getJSONObject("d");
        totalNum = jsonD.getInt("t");
        curPage = jsonD.getInt("p");
        requestNum = jsonD.getInt("n");
        totalPage = totalNum / requestNum + (totalNum % requestNum > 0 ? 1 : 0);
        JSONArray jsonList = jsonD.getJSONArray("list");
        return jsonList;
    }

    public int getTotalNum() {
        return totalNum;
    }

    public void setTotalNum(int totalNum) {
        this.totalNum = totalNum;
    }

    public int getCurPage() {
        return curPage;
    }

    public void setCurPage(int curPage) {
        this.curPage = curPage;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    public int getRequestNum() {
        return requestNum;
    }

    public void setRequestNum(int requestNum) {
        this.requestNum = requestNum;
    }
}