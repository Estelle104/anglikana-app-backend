package com.anglikana.backoffice.dto;

import java.util.List;

public class ZoneGeographiqueDTO {

    private NiveauHierarchique niveau;
    private Integer id;
    private String nom;
    private String contexte;
    private ZoneFeatureDTO feature;
    /** false tant que le polygone QGIS de la zone n'a pas été importé. */
    private boolean limiteDisponible;
    /** Zones du niveau inférieur rattachées à cette zone et disposant d'une limite (ex. régions d'un diocèse). */
    private List<ZoneFeatureDTO> sousZones;
    /**
     * Églises dont les coordonnées sont à l'intérieur du polygone (frontière incluse).
     * Sans limite disponible : églises rattachées hiérarchiquement à la zone.
     */
    private List<EgliseLocaliseeDTO> eglises;
    /** Églises rattachées à l'entité dans la hiérarchie mais situées hors du polygone (incohérence de données). */
    private List<EgliseLocaliseeDTO> eglisesHorsZone;

    public NiveauHierarchique getNiveau() { return niveau; }
    public void setNiveau(NiveauHierarchique niveau) { this.niveau = niveau; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getContexte() { return contexte; }
    public void setContexte(String contexte) { this.contexte = contexte; }

    public ZoneFeatureDTO getFeature() { return feature; }
    public void setFeature(ZoneFeatureDTO feature) { this.feature = feature; }

    public boolean isLimiteDisponible() { return limiteDisponible; }
    public void setLimiteDisponible(boolean limiteDisponible) { this.limiteDisponible = limiteDisponible; }

    public List<ZoneFeatureDTO> getSousZones() { return sousZones; }
    public void setSousZones(List<ZoneFeatureDTO> sousZones) { this.sousZones = sousZones; }

    public List<EgliseLocaliseeDTO> getEglises() { return eglises; }
    public void setEglises(List<EgliseLocaliseeDTO> eglises) { this.eglises = eglises; }

    public List<EgliseLocaliseeDTO> getEglisesHorsZone() { return eglisesHorsZone; }
    public void setEglisesHorsZone(List<EgliseLocaliseeDTO> eglisesHorsZone) { this.eglisesHorsZone = eglisesHorsZone; }
}
