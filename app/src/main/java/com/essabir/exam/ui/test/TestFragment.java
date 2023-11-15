package com.essabir.exam.ui.test;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.ViewTarget;
import com.essabir.exam.R;
import com.essabir.exam.adapters.TestAdapter;
import com.essabir.exam.classes.Test;
import com.essabir.exam.databinding.FragmentTestBinding;
import com.essabir.exam.utlis.MultipartRequest;
import com.essabir.exam.utlis.SwipeToDeleteCallback;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TestFragment extends Fragment {

    private FragmentTestBinding binding;
    private RecyclerView recyclerView;
    private TestAdapter adapter;
    private Bitmap selectedImageBitmap;
    private ImageView add_image;
    private final String URL = "http://192.168.0.131:8081/api/v1";
    private ActivityResultLauncher<String> imagePickerLauncher;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        TestViewModel testViewModel = new ViewModelProvider(this).get(TestViewModel.class);

        binding = FragmentTestBinding.inflate(inflater, container, false);

        View root = binding.getRoot();
        recyclerView = binding.recycleViewTest;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);
        adapter = new TestAdapter(requireContext());
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

        // fetch data
        testViewModel.fetchData(requireContext());

        testViewModel.getTestList().observe(getViewLifecycleOwner(), tests -> {
            adapter.setTests(tests);
        });

        // update Test
        TestViewModel finalTestViewModel = testViewModel;
        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                View childView = rv.findChildViewUnder(e.getX(), e.getY());
                if (childView != null && e.getAction() == MotionEvent.ACTION_UP) {
                    int position = rv.getChildAdapterPosition(childView);
                    if (position != RecyclerView.NO_POSITION) {
                        Test test = finalTestViewModel.getTestList().getValue().get(position);
                        showUpdateDialog(test);
                    }
                }
                return false;
            }
        });

//        // enable swipe to delete
        enableSwipeToDeleteAndUndo();

        return root;
    }

    public void showAddTestDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.test_add, null);
        dialogBuilder.setView(dialogView);

        TextInputEditText add_name = dialogView.findViewById(R.id.add_name);
        add_image = dialogView.findViewById(R.id.add_image);

        // For handling image selection
        add_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });

        // show the dialog
        dialogBuilder.setTitle("Add Test");
        dialogBuilder.setPositiveButton("Add", (dialog, which) -> {
            String name = add_name.getText().toString().trim();
            Test newTest = new Test();
            newTest.setName(name);

            try {
                saveTest(selectedImageBitmap , newTest);
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

    private void saveImage(Bitmap imageBitmap, int id, Test test) {
        String url = URL + "/tests/" + id + "/image";

        Map<String, String> params = new HashMap<>();
        params.put("id", String.valueOf(id));

        MultipartRequest request = new MultipartRequest(
            Request.Method.POST,
            url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    test.setImageUrl(id +  "/image");
                    adapter.addTest(test);
                    Toast.makeText(requireContext(), "Test Created!", Toast.LENGTH_SHORT).show();
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

    private void saveTest(Bitmap imageBitmap, Test test) throws JSONException {
        AtomicInteger testId = new AtomicInteger();
        String url = URL + "/tests";
        JSONObject testJSON = new JSONObject();
        testJSON.put("name", test.getName());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, testJSON,
            response -> {
                Log.d("resp", response.toString());
                try {
                    testId.set(response.getInt("id"));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                saveImage(imageBitmap, testId.get(), test);
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
                final Test item = adapter.getData().get(position);
                adapter.removeItem(position);

                Snackbar snackbar = Snackbar
                        .make(recyclerView, "Test was removed.", Snackbar.LENGTH_LONG);

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

    private void deleteTest(int id, int position, Test item) {
        String deleteUrl = URL + "/tests/" + id;

        // set loader visible
//        loader.setVisibility(View.VISIBLE);
//        recyclerView.setVisibility(View.GONE);

        StringRequest request = new StringRequest(Request.Method.DELETE, deleteUrl,
                response -> {
//                    loader.setVisibility(View.GONE);
//                    recyclerView.setVisibility(View.VISIBLE);
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


    // update Test
    private void showUpdateDialog(Test test) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.test_edit, null);
        dialogBuilder.setView(dialogView);

        // Initialize UI elements in the dialog layout
        TextInputEditText edit_name = dialogView.findViewById(R.id.edit_name);
        add_image = dialogView.findViewById(R.id.edit_image);

        // Populate the dialog with the student's current information
        edit_name.setText(test.getName());

        if(test.getImageUrl() != null){
            String imgURL = URL + "/tests/" + test.getImageUrl().replace("\\", "");
             Glide
                .with(requireContext())
                .asBitmap()
                .load(imgURL)
                 .diskCacheStrategy(DiskCacheStrategy.NONE)
                 .skipMemoryCache(true)
                .centerCrop()
                .into(add_image);
        }else{
            Glide
                .with(requireContext())
                .load(requireContext().getDrawable(R.drawable.femme))
                .centerCrop()
                .into(add_image);
        }


        // For handling image selection
        add_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });

        dialogBuilder.setTitle("Update Test");
        dialogBuilder.setPositiveButton("Update", (dialog, which) -> {
            // Retrieve the updated information from the dialog
            String name = edit_name.getText().toString().trim();

            test.setName(name);

            try {
                updateTest(test);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });

        dialogBuilder.setNegativeButton("Cancel", null);

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    private void updateTest(Test test) throws JSONException {
        String url = URL + "/tests/" + test.getId();
        JSONObject testJSON = new JSONObject();
        testJSON.put("id", test.getId());
        testJSON.put("name", test.getName());

//        recyclerView.setVisibility(View.GONE);

        JsonObjectRequest  jsonArrayRequest = new JsonObjectRequest(Request.Method.PUT, url, testJSON,
            response -> {
                try {
//                    recyclerView.setVisibility(View.VISIBLE);
                    updateImage(selectedImageBitmap, test.getId(), test);
                    String message = response.getString("message");
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d("res", response.toString());
            },
            error -> {
                Log.e("Error", error.toString());
            }
        );

        // Instantiate Volley RequestQueue and add the request
        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(jsonArrayRequest);
    }

    private void updateImage(Bitmap imageBitmap, int id, Test test) {
        String url = URL + "/tests/" + id + "/image";

        Map<String, String> params = new HashMap<>();
        params.put("id", String.valueOf(id));

        MultipartRequest request = new MultipartRequest(
            Request.Method.PUT,
            url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    // Notify the adapter of the data change
                    adapter.notifyDataSetChanged();
                    Toast.makeText(requireContext(), "Updated!", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }
}