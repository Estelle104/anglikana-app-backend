package com.anglikana.backoffice.dto;

/**
 * Comptages d'une entité, calculés sur l'ensemble de ses descendants.
 * Les niveaux égaux ou supérieurs à celui de l'entité valent 0 (une région ne compte pas de régions).
 * {@code clerges} compte les membres DISTINCTS du clergé responsables de l'entité
 * ou de l'une de ses entités descendantes (diocèse → église).
 */
public class StatistiquesEntiteDTO {

    private long regions;
    private long districts;
    private long paroisses;
    private long eglises;
    private long clerges;
    private long ressources;

    public long getRegions() { return regions; }
    public void setRegions(long regions) { this.regions = regions; }

    public long getDistricts() { return districts; }
    public void setDistricts(long districts) { this.districts = districts; }

    public long getParoisses() { return paroisses; }
    public void setParoisses(long paroisses) { this.paroisses = paroisses; }

    public long getEglises() { return eglises; }
    public void setEglises(long eglises) { this.eglises = eglises; }

    public long getClerges() { return clerges; }
    public void setClerges(long clerges) { this.clerges = clerges; }

    public long getRessources() { return ressources; }
    public void setRessources(long ressources) { this.ressources = ressources; }
}
