package com.essabir.exam.ui.employe;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.essabir.exam.classes.Employe;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class EmployeViewModel extends ViewModel {
    private MutableLiveData<List<Employe>> employeList = new MutableLiveData<>();

    private MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public LiveData<List<Employe>> getemployeList() {
        return employeList;
    }

    public void fetchData(Context context) {
        String url = "http://192.168.16.170:8081/api/v1/employes";

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d("resss", response.toString());
                    Type listType = new TypeToken<List<Employe>>() {}.getType();
                    Gson gson = new Gson();
                    List<Employe> Employes = gson.fromJson(response.toString(), listType);
                    employeList.setValue(Employes);
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