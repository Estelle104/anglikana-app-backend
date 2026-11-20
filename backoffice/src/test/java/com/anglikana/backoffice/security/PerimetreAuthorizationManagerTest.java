package com.anglikana.backoffice.security;

import com.anglikana.backoffice.repository.PerimetreRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.util.SimpleMethodInvocation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PerimetreAuthorizationManagerTest {

    /** Méthodes annotées servant de cibles : seules leurs annotations comptent. */
    static class Cibles {

        @LevelRequired(5)
        public void niveauCinq() {
        }

        @NeedAuthorisation(entity = EntiteProtegee.PAROISSE, id = "#id")
        public void paroisseSansCascade(Integer id) {
        }

        @NeedAuthorisation(entity = EntiteProtegee.PAROISSE, id = "#id")
        @CascadeAuthorised
        public void paroisseAvecCascade(Integer id) {
        }

        @NeedAuthorisation(entity = EntiteProtegee.DISTRICT, id = "#dto.districtId")
        @CascadeAuthorised
        public void creationDansUnDistrict(Corps dto) {
        }

        @NeedAuthorisation(entityExpression = "#niveau", id = "#id")
        @CascadeAuthorised
        public void niveauDynamique(String niveau, Integer id) {
        }

        @LevelRequired(3)
        @NeedAuthorisation(entity = EntiteProtegee.PAROISSE, id = "#id")
        @NeedAuthorisation(entity = EntiteProtegee.DISTRICT, id = "#dto.districtId")
        @CascadeAuthorised
        public void deuxCibles(Integer id, Corps dto) {
        }
    }

    public record Corps(Integer districtId) {
        public Integer getDistrictId() { return districtId; }
    }

    private PerimetreRepository perimetreRepository;
    private PerimetreAuthorizationManager manager;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void preparer() {
        perimetreRepository = mock(PerimetreRepository.class);
        AutorisationService service = new AutorisationService(perimetreRepository);
        ObjectProvider<AutorisationService> fournisseur = mock(ObjectProvider.class);
        when(fournisseur.getObject()).thenReturn(service);
        manager = new PerimetreAuthorizationManager(fournisseur);
    }

    // ------------------------------------------------------------------ outils

    private static Authentication utilisateur(Integer clergeId, int niveau, boolean actif) {
        UtilisateurConnecte principal = new UtilisateurConnecte("u", "", actif, clergeId, niveau, null);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    private static Authentication responsable(Integer clergeId, int niveau) {
        return utilisateur(clergeId, niveau, true);
    }

    private boolean accorde(Authentication authentication, String methode, Object... arguments) {
        Method cible = Arrays.stream(Cibles.class.getMethods()).filter(m -> m.getName().equals(methode)).findFirst().orElseThrow();
        return manager.authorize(() -> authentication, new SimpleMethodInvocation(new Cibles(), cible, arguments)).isGranted();
    }

    /** Chaîne paroisse → district → région → diocèse (responsables). */
    private void paroisse(int id, Integer... responsables) {
        when(perimetreRepository.chaineResponsables(EntiteProtegee.PAROISSE, id)).thenReturn(Optional.of(Arrays.asList(responsables)));
    }

    // ------------------------------------------------------------------ niveau

    @Test
    void niveauSuffisantAccepte() {
        assertThat(accorde(responsable(1, 5), "niveauCinq")).isTrue();
    }

    @Test
    void niveauInsuffisantRefuse() {
        assertThat(accorde(responsable(1, 4), "niveauCinq")).isFalse();
    }

    @Test
    void anonymeRefuse() {
        Authentication anonyme = new AnonymousAuthenticationToken("cle", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        assertThat(accorde(anonyme, "niveauCinq")).isFalse();
        assertThat(accorde(null, "paroisseAvecCascade", 1)).isFalse();
    }

    @Test
    void compteDesactiveRefuse() {
        assertThat(accorde(utilisateur(1, 5, false), "niveauCinq")).isFalse();
    }

    // ------------------------------------------------------------------ responsable et cascade

    @Test
    void responsableDirectAccepte() {
        paroisse(1, 3, 3, 1, 1);
        assertThat(accorde(responsable(3, 2), "paroisseSansCascade", 1)).isTrue();
    }

    @Test
    void responsableDUnAncetreAccepteAvecCascadeSeulement() {
        paroisse(1, 7, 3, 1, 1);
        assertThat(accorde(responsable(3, 3), "paroisseAvecCascade", 1)).isTrue();
        assertThat(accorde(responsable(3, 3), "paroisseSansCascade", 1)).isFalse();
    }

    @Test
    void horsPerimetreRefuse() {
        paroisse(2, 4, 4, 2, 2);
        assertThat(accorde(responsable(3, 3), "paroisseAvecCascade", 2)).isFalse();
    }

    @Test
    void niveauxSansResponsableIgnores() {
        paroisse(1, null, null, null, 1);
        assertThat(accorde(responsable(1, 5), "paroisseAvecCascade", 1)).isTrue();
        assertThat(accorde(responsable(9, 5), "paroisseAvecCascade", 1)).isFalse();
    }

    @Test
    void compteSansClergeRefuse() {
        paroisse(1, null, null, null, null);
        assertThat(accorde(responsable(null, 5), "paroisseAvecCascade", 1)).isFalse();
    }

    @Test
    void entiteIntrouvableDonne404() {
        when(perimetreRepository.chaineResponsables(EntiteProtegee.PAROISSE, 404)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> accorde(responsable(3, 3), "paroisseAvecCascade", 404))
                .isInstanceOf(EntiteIntrouvableException.class);
    }

    @Test
    void identifiantNullRefuseSansInterrogerLaBase() {
        assertThat(accorde(responsable(3, 3), "creationDansUnDistrict", new Corps(null))).isFalse();
        verify(perimetreRepository, never()).chaineResponsables(any(), any());
    }

    @Test
    void identifiantLuDansLeCorps() {
        when(perimetreRepository.chaineResponsables(EntiteProtegee.DISTRICT, 1)).thenReturn(Optional.of(List.of(3, 1, 1)));
        assertThat(accorde(responsable(3, 3), "creationDansUnDistrict", new Corps(1))).isTrue();
    }

    // ------------------------------------------------------------------ entité calculée et cibles multiples

    @Test
    void entiteCalculeeDepuisUnParametre() {
        when(perimetreRepository.chaineResponsables(EntiteProtegee.DISTRICT, 1)).thenReturn(Optional.of(List.of(3, 1, 1)));
        assertThat(accorde(responsable(3, 3), "niveauDynamique", "district", 1)).isTrue();
        assertThat(accorde(responsable(3, 3), "niveauDynamique", "DISTRICT", 1)).isTrue();
    }

    @Test
    void entiteCalculeeInconnueRefusee() {
        assertThat(accorde(responsable(3, 3), "niveauDynamique", "eveche", 1)).isFalse();
        verify(perimetreRepository, never()).chaineResponsables(any(), any());
    }

    @Test
    void toutesLesCiblesDoiventEtreAutorisees() {
        paroisse(1, null, 3, 1, 1);
        when(perimetreRepository.chaineResponsables(EntiteProtegee.DISTRICT, 1)).thenReturn(Optional.of(List.of(3, 1, 1)));
        when(perimetreRepository.chaineResponsables(EntiteProtegee.DISTRICT, 2)).thenReturn(Optional.of(List.of(4, 2, 2)));

        assertThat(accorde(responsable(3, 3), "deuxCibles", 1, new Corps(1))).isTrue();
        assertThat(accorde(responsable(3, 3), "deuxCibles", 1, new Corps(2))).isFalse();
        assertThat(accorde(responsable(3, 2), "deuxCibles", 1, new Corps(1))).isFalse();
    }
}
