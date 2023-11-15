package com.essabir.exam.ui.employe;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.essabir.exam.R;
import com.essabir.exam.adapters.EmployeAdapter;
import com.essabir.exam.classes.Employe;
import com.essabir.exam.classes.Service;
import com.essabir.exam.databinding.FragmentEmployeBinding;
import com.essabir.exam.utlis.MultipartRequest;
import com.essabir.exam.utlis.SwipeToDeleteCallback;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class EmployeFragment extends Fragment {
    private FragmentEmployeBinding binding;
    private RecyclerView recyclerView;
    private EmployeAdapter adapter;
    private Bitmap selectedImageBitmap;
    private ImageView add_image;
    private final String URL = "http://192.168.16.170:8081/api/v1";
    private ActivityResultLauncher<String> imagePickerLauncher;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        EmployeViewModel employeViewModel = new ViewModelProvider(this).get(EmployeViewModel.class);

        binding = FragmentEmployeBinding.inflate(inflater, container, false);

        View root = binding.getRoot();
        recyclerView = binding.recycleViewEmploye;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);
        adapter = new EmployeAdapter(requireContext());
        recyclerView.setAdapter(adapter);

        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri result) {
                    if (result != null) {
                        try {
                            selectedImageBitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), result);
                            add_image.setImageBitmap(selectedImageBitmap);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

        // fetch services
        AutoCompleteTextView get_service = binding.getEmployeByService;
        // fetch servcies form db
        String url = URL + "/services";
        List<Service> services = new ArrayList<>();
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest( Request.Method.GET, url, null,
            response -> {
                try {
                    Log.d("get_specialities", response.toString());
                    for (int i = 0; i < response.length(); i++) {
                        Service specialite = new Service();
                        JSONObject specialiteJson = response.getJSONObject(i);
                        specialite.setId(specialiteJson.optLong("id"));
                        specialite.setNom(specialiteJson.optString("nom"));
                        services.add(specialite);
                    }

                    List<String> codesServices = new ArrayList<>();
                    services.stream().forEach(f -> {
                        codesServices.add(f.getNom());
                    });

                    Log.d("code :", codesServices.toString());

                    ArrayAdapter<String> specialiteAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, codesServices);
                    get_service.setAdapter(specialiteAdapter);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> {
                Log.e("Fetch Error", error.toString());
            }
        );
        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(jsonArrayRequest);


        onServiceChange(services);

        // fetch data
        employeViewModel.fetchData(requireContext());

        employeViewModel.getemployeList().observe(getViewLifecycleOwner(), employes -> {
            adapter.setEmployes(employes);
        });

        // enable swipe to delete
        enableSwipeToDeleteAndUndo();

        return root;
    }

    private void onServiceChange(List<Service> servicesList){
        AutoCompleteTextView autoCompleteTextView = binding.getEmployeByService;

        // Create a custom adapter for the AutoCompleteTextView
        ArrayAdapter<Service> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, servicesList);
        autoCompleteTextView.setAdapter(adapter);

        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            // Get the selected item from the AutoCompleteTextView (which is a String in this case)
            String selectedServiceName = (String) parent.getItemAtPosition(position);

            // Now you might need to find the corresponding Service object based on the name
            Service selectedService = findServiceByName(selectedServiceName, servicesList);

            // Perform your Volley request here with the selected service
            getEmployesByService(selectedService.getId());
        });
    }

    private Service findServiceByName(String serviceName, List<Service> servicesList) {
        for (Service service : servicesList) {
            if (service.getNom().equals(serviceName)) {
                return service;
            }
        }
        return null;
    }
    private void getEmployesByService(Long id){
        String url = URL + "/employes/byServices/" + id;

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            response -> {
                Type listType = new TypeToken<List<Employe>>() {}.getType();
                Gson gson = new Gson();
                List<Employe> employees = gson.fromJson(response.toString(), listType);

                if(employees.isEmpty()){
                    adapter.setEmployes(new ArrayList<>());
                    binding.empty.setVisibility(View.VISIBLE);
                }else{
                    adapter.setEmployes(employees);
                    binding.empty.setVisibility(View.GONE);
                }
                Log.d("ta", employees.toString());
            },
            error -> {
                Log.e("Error", error.toString());
            }
        );

        // Instantiate Volley RequestQueue and add the request
        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(jsonArrayRequest);
    }

//    private void onServiceChange(){
//        AutoCompleteTextView autoCompleteTextView = binding.getEmployeByService;
//
//        autoCompleteTextView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                // Get the selected item from the AutoCompleteTextView
//                String selectedService = parent.getItemAtPosition(position).toString();
//
//                // Perform your Volley request here with the selected service
////                getEmployesByService(selectedService);
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                // Handle the case where nothing is selected if needed
//            }
//        });
//    }
//
//    private void getEmployesByService(Long id){
//        String url = URL + "/byServices/" + id;
//
//        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
//            Request.Method.GET,
//            url,
//            null,
//            response -> {
//                Type listType = new TypeToken<List<Employe>>() {}.getType();
//                Gson gson = new Gson();
//                List<Employe> Employes = gson.fromJson(response.toString(), listType);
//            },
//            error -> {
//                Log.e("Error", error.toString());
//            }
//        );
//
//        // Instantiate Volley RequestQueue and add the request
//        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
//        requestQueue.add(jsonArrayRequest);
//    }

    public void showAddTestDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.test_add, null);
        dialogBuilder.setView(dialogView);

        TextInputEditText add_name = dialogView.findViewById(R.id.add_name);
        TextInputEditText add_prenom = dialogView.findViewById(R.id.add_prenom);

        add_image = dialogView.findViewById(R.id.add_image);
        // For handling image selection
        add_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });

        TextInputEditText add_dateNaissance = dialogView.findViewById(R.id.dateNaissance);

        add_dateNaissance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(add_dateNaissance, null);
            }
        });

        AutoCompleteTextView add_service = dialogView.findViewById(R.id.add_service);
        // fetch servcies form db
        String url = URL + "/services";
        List<Service> services = new ArrayList<>();
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest( Request.Method.GET, url, null,
            response -> {
                try {
                    Log.d("get_specialities", response.toString());
                    for (int i = 0; i < response.length(); i++) {
                        Service specialite = new Service();
                        JSONObject specialiteJson = response.getJSONObject(i);
                        specialite.setId(specialiteJson.optLong("id"));
                        specialite.setNom(specialiteJson.optString("nom"));
                        services.add(specialite);
                    }

                    List<String> codesServices = new ArrayList<>();
                    services.stream().forEach(f -> {
                        codesServices.add(f.getNom());
                    });

                    Log.d("code :", codesServices.toString());

                    ArrayAdapter<String> specialiteAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, codesServices);
                    add_service.setAdapter(specialiteAdapter);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> {
                Log.e("Fetch Error", error.toString());
            }
        );
        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(jsonArrayRequest);

//        AutoCompleteTextView add_chef = dialogView.findViewById(R.id.chef);
//        // fetch servcies form db
//        String url1 = URL + "/chefs";
//        List<Employe> chefs = new ArrayList<>();
//        JsonArrayRequest jsonArrayRequest1 = new JsonArrayRequest( Request.Method.GET, url1, null,
//            response -> {
//                try {
//                    Log.d("get_chef", response.toString());
//                    for (int i = 0; i < response.length(); i++) {
//                        Employe employe = new Employe();
//                        JSONObject employeJson = response.getJSONObject(i);
//                        employe.setId(employeJson.optLong("id"));
//                        employe.setNom(employeJson.optString("nom"));
//                        employe.setPrenom(employeJson.optString("prenom"));
//                        chefs.add(employe);
//                    }
//
//                    List<String> codeschefs = new ArrayList<>();
//                    chefs.stream().forEach(f -> {
//                        codeschefs.add(f.getNom());
//                    });
//
//                    Log.d("code :", codeschefs.toString());
//
//                    ArrayAdapter<String> specialiteAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, codeschefs);
//                    add_chef.setAdapter(specialiteAdapter);
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            },
//            error -> {
//                Log.e("Fetch Error", error.toString());
//            }
//        );
//        RequestQueue requestQueue1 = Volley.newRequestQueue(requireContext());
//        requestQueue1.add(jsonArrayRequest1);

        AutoCompleteTextView add_chef = dialogView.findViewById(R.id.chef);

        // Fetch chefs from the server
        String url1 = URL + "/employes";
        List<Employe> chefs = new ArrayList<>();

        JsonArrayRequest jsonArrayRequest1 = new JsonArrayRequest(Request.Method.GET, url1, null,
            response -> {
                try {
                    Log.d("get_chef", response.toString());

                    // Populate the chefs list
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject employeJson = response.getJSONObject(i);

                        Employe employe = new Employe();
                        employe.setId(employeJson.optLong("id"));
                        employe.setNom(employeJson.optString("nom"));
                        employe.setPrenom(employeJson.optString("prenom"));

                        // Set other properties as needed

                        chefs.add(employe);
                    }

                    // Create a list of chef names
                    List<String> chefNames = new ArrayList<>();
                    for (Employe chef : chefs) {
                        chefNames.add(chef.getNom());
                    }

                    Log.d("chef names:", chefNames.toString());

                    // Create an ArrayAdapter and set it to the AutoCompleteTextView
                    ArrayAdapter<String> chefAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, chefNames);
                    add_chef.setAdapter(chefAdapter);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> {
                Log.e("Fetch Error", error.toString());
            }
        );

        RequestQueue requestQueue1 = Volley.newRequestQueue(requireContext());
        requestQueue1.add(jsonArrayRequest1);


        // show the dialog
        dialogBuilder.setTitle("Add Employe");
        dialogBuilder.setPositiveButton("Add", (dialog, which) -> {
            String name = add_name.getText().toString().trim();
            String prenom = add_prenom.getText().toString().trim();
            String dateNaissance = add_dateNaissance.getText().toString().trim();
            String service = add_service.getText().toString().trim();
            String chef = add_chef.getText().toString().trim();

            Service chosenService = services.stream().filter( f -> f.getNom().equals(service)).findFirst().get();
            Log.d("chosenSpe", chosenService.getNom());

            Employe chosenChef = chefs.stream().filter( f -> f.getNom().equals(chef)).findFirst().get();
            Log.d("chosenChef", chosenChef.getNom());

            DateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
            try {
                date = dateFormat1.parse(dateNaissance);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }

            Employe newEmploye = new Employe();
            newEmploye.setNom(name);
            newEmploye.setPrenom(prenom);
            newEmploye.setService(chosenService);
            newEmploye.setDateNaissance(date);
            if(chosenChef != null){
                newEmploye.setChef(true);
            }else{
                newEmploye.setChef(false);
            }


            try {
                saveTest(selectedImageBitmap , newEmploye);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            //Notify the adapter about changes in the data
            adapter.notifyDataSetChanged();
        });

        dialogBuilder.setNegativeButton("Cancel", null);

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    private void saveImage(Bitmap imageBitmap, int id, Employe employe) {
        String url = URL + "/employes/" + id + "/image";

        Map<String, String> params = new HashMap<>();
        params.put("id", String.valueOf(id));

        MultipartRequest request = new MultipartRequest(
                Request.Method.POST,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        employe.setPhoto(id +  "/image");
                        adapter.addTest(employe);
                        Toast.makeText(requireContext(), "Employe Created!", Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("VolleyError", "Error during image upload: " + error.toString());
                    }
                },
                imageBitmap,
                "image.jpg",
                params);

        // Instantiate Volley RequestQueue and add the request
        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(request);
    }

    private void saveTest(Bitmap imageBitmap, Employe employe) throws JSONException {
        AtomicInteger testId = new AtomicInteger();
        String url = URL + "/employes";
        JSONObject testJSON = new JSONObject();
        testJSON.put("nom", employe.getNom());
        testJSON.put("prenom", employe.getPrenom());
//        testJSON.put("chef", employe.isChef());
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = dateFormat.format(employe.getDateNaissance());

        testJSON.put("dateNaissance", strDate);

        // Adding Specialite details if Professeur class includes a Specialite object
        Service service = employe.getService();
        if (service != null) {
            JSONObject serviceJson = new JSONObject();
            serviceJson.put("id", service.getId());
            testJSON.put("service", serviceJson);
        }

        // Adding Specialite details if Professeur class includes a Specialite object
//        Employe chef = employe.getChef();
//
//        if (chef != null) {
//            JSONObject chefJson = new JSONObject();
//            chefJson.put("id", chef.getId());
//            testJSON.put("chef", chefJson);
//        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, testJSON,
                response -> {
                    Log.d("resp", response.toString());
                    try {
                        testId.set(response.getInt("id"));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    saveImage(imageBitmap, testId.get(), employe);
                },
                error -> {
                    Log.e("Error", error.toString());
                }
        );

        // Instantiate Volley RequestQueue and add the request
        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(jsonObjectRequest);
    }


    private void enableSwipeToDeleteAndUndo() {
        SwipeToDeleteCallback swipeToDeleteCallback = new SwipeToDeleteCallback(requireContext()) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                final int position = viewHolder.getAdapterPosition();
                final Employe item = adapter.getData().get(position);
                adapter.removeItem(position);

                Snackbar snackbar = Snackbar
                        .make(recyclerView, "Employe was removed.", Snackbar.LENGTH_LONG);

                snackbar.setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        adapter.restoreItem(item, position);
                        recyclerView.scrollToPosition(position);
                    }
                });

                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        if (event == DISMISS_EVENT_TIMEOUT || event == DISMISS_EVENT_SWIPE) {
                            deleteTest(item.getId(), position, item);
                        }
                    }
                });

                snackbar.setActionTextColor(Color.YELLOW);
                snackbar.show();

            }
        };

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchhelper.attachToRecyclerView(recyclerView);
    }

    private void deleteTest(Long id, int position, Employe item) {
        String deleteUrl = URL + "/employes/" + id;

        StringRequest request = new StringRequest(Request.Method.DELETE, deleteUrl,
                response -> {
                    Toast.makeText(requireContext(), response.toString(), Toast.LENGTH_SHORT).show();
                }, error -> {
            Toast.makeText(requireContext(), error.toString(), Toast.LENGTH_SHORT).show();
            Log.e("error", error.toString());
            adapter.restoreItem(item, position);
        }
        ){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<String, String>();
                params.put("id", String.valueOf(id));
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(request);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    private void showDatePicker(TextInputEditText add_dateNaissance, Date initDate) {
        Calendar calendar = Calendar.getInstance();
        int year, month, day;

        if(initDate == null){
            year = calendar.get(Calendar.YEAR);
            month = calendar.get(Calendar.MONTH);
            day = calendar.get(Calendar.DAY_OF_MONTH);
        }else{
            year = initDate.getYear();
            month = initDate.getMonth();
            day = initDate.getDay();
            Log.d("month", String.valueOf(month));
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Update the calendar to the selected date
                    calendar.set(selectedYear, selectedMonth, selectedDay);
                    // Set the selected date to the EditText
                    String selectedDate = selectedYear  + "-" + (selectedMonth + 1) + "-" + selectedDay;
                    add_dateNaissance.setText(selectedDate);
                }, year, month, day);

        datePickerDialog.show();
    }
}