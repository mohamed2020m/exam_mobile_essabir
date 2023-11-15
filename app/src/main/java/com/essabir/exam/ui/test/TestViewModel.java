package com.essabir.exam.ui.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.essabir.exam.classes.Test;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class TestViewModel extends ViewModel {
    private MutableLiveData<List<Test>> testList = new MutableLiveData<>();

    private MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public LiveData<List<Test>> getTestList() {
        return testList;
    }

    public void fetchData(Context context) {
        String url = "http://192.168.0.131:8081/api/v1/tests";

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            response -> {
                Type listType = new TypeToken<List<Test>>() {}.getType();
                Gson gson = new Gson();
                List<Test> tests = gson.fromJson(response.toString(), listType);
                testList.setValue(tests);
            },
            error -> {
                errorLiveData.setValue(error.toString());
                Log.e("Error", error.toString());
            }
        );

        // Instantiate Volley RequestQueue and add the request
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(jsonArrayRequest);
    }
}