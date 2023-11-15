package com.essabir.exam.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.essabir.exam.R;
import com.essabir.exam.classes.Employe;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class EmployeAdapter extends RecyclerView.Adapter<EmployeAdapter.EmployeViewHolder>{
    private List<Employe> employes;
    private Context context;

    private String url = "http://192.168.16.170:8081/api/v1/employes/";

    public EmployeAdapter(Context context) {
        this.context = context;
    }

    public List<Employe> getemployes() {
        return employes;
    }

    public void addTest(Employe test) {
        // Check if the dataset exists
        if (employes == null) {
            employes = new ArrayList<>();
        }

        // Add the new student to the dataset
        employes.add(test);

        // Notify the adapter that the dataset has changed
        notifyDataSetChanged();
    }

    public void setEmployes(List<Employe> employes) {
        this.employes = employes;
        notifyDataSetChanged();
    }

    public List<Employe> getData(){
        return employes;
    }

    public void removeItem(int position) {
        employes.remove(position);
        notifyItemRemoved(position);
    }

    public void restoreItem(Employe item, int position) {
        employes.add(position, item);
        notifyItemInserted(position);
    }

    @NonNull
    @Override
    public EmployeAdapter.EmployeViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(this.context).inflate(R.layout.employe_list, viewGroup, false);
        final EmployeAdapter.EmployeViewHolder holder = new EmployeAdapter.EmployeViewHolder(v);
        return holder;
    }

    @SuppressLint("ResourceType")
    @Override
    public void onBindViewHolder(@NonNull EmployeAdapter.EmployeViewHolder testViewHolder, int i) {
        testViewHolder.name.setText(employes.get(i).getNom());
//        if(employes.get(i).getService() != null){
//            testViewHolder.service.setText(employes.get(i).getService().getNom());
//        }
//        testViewHolder.date.setText("2033-4-3");

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = dateFormat.format(employes.get(i).getDateNaissance());
        testViewHolder.date.setText(strDate);

        if(employes.get(i).getPhoto() != null){
            String imgURL = url + employes.get(i).getPhoto().replace("\\", "");
            Glide
                .with(context)
                .load(imgURL)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .placeholder(R.raw.image_loading)
                .into(testViewHolder.image);
        }else{
            Glide
                .with(context)
                .load(R.drawable.femme)
                .centerCrop()
                .into(testViewHolder.image);
        }

    }

    @Override
    public int getItemCount() {
        if(employes == null){
            return 0;
        }
        return employes.size();
    }

    public class EmployeViewHolder extends RecyclerView.ViewHolder {
        TextView name, date, service;
        ImageView image;
        CardView parent;
        public EmployeViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name_employe);
            image = itemView.findViewById(R.id.image);
            date = itemView.findViewById(R.id.date);
            service = itemView.findViewById(R.id.service);
            parent = itemView.findViewById(R.id.parent);
        }
    }
}
