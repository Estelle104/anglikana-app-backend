package com.anglikana.backoffice.dto;

import java.util.List;

public class TableauBordDTO {

    private NoeudHierarchiqueDTO entite;
    /** Ancêtres de l'entité, du diocèse au parent direct. */
    private List<ReferenceEntiteDTO> ancetres;
    /** Niveau des entités listées dans {@code enfants} (null au niveau Église). */
    private NiveauHierarchique niveauEnfants;
    /** Niveaux proposés pour la liste des enfants (les deux niveaux inférieurs au plus). */
    private List<NiveauHierarchique> vuesEnfants;
    private List<NoeudHierarchiqueDTO> enfants;
    private List<ClergeAffecteDTO> clerges;
    /** Renseignés uniquement au niveau Église. */
    private EgliseDTO eglise;
    private List<RessourceDetailleeDTO> ressources;

    public NoeudHierarchiqueDTO getEntite() { return entite; }
    public void setEntite(NoeudHierarchiqueDTO entite) { this.entite = entite; }

    public List<ReferenceEntiteDTO> getAncetres() { return ancetres; }
    public void setAncetres(List<ReferenceEntiteDTO> ancetres) { this.ancetres = ancetres; }

    public NiveauHierarchique getNiveauEnfants() { return niveauEnfants; }
    public void setNiveauEnfants(NiveauHierarchique niveauEnfants) { this.niveauEnfants = niveauEnfants; }

    public List<NiveauHierarchique> getVuesEnfants() { return vuesEnfants; }
    public void setVuesEnfants(List<NiveauHierarchique> vuesEnfants) { this.vuesEnfants = vuesEnfants; }

    public List<NoeudHierarchiqueDTO> getEnfants() { return enfants; }
    public void setEnfants(List<NoeudHierarchiqueDTO> enfants) { this.enfants = enfants; }

    public List<ClergeAffecteDTO> getClerges() { return clerges; }
    public void setClerges(List<ClergeAffecteDTO> clerges) { this.clerges = clerges; }

    public EgliseDTO getEglise() { return eglise; }
    public void setEglise(EgliseDTO eglise) { this.eglise = eglise; }

    public List<RessourceDetailleeDTO> getRessources() { return ressources; }
    public void setRessources(List<RessourceDetailleeDTO> ressources) { this.ressources = ressources; }
}
