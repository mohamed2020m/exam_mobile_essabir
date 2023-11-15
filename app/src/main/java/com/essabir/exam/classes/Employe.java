package com.essabir.exam.classes;

import java.util.Date;
import java.util.List;

public class Employe {
    private Long id;
    private String nom;
    private String prenom;
    private Date dateNaissance;
    private String photo;
    private Service service;
    private boolean chef;

    public Employe() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public Date getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(Date dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public boolean isChef() {
        return chef;
    }

    public void setChef(boolean chef) {
        this.chef = chef;
    }
}
