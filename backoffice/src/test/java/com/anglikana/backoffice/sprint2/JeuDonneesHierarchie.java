package com.anglikana.backoffice.sprint2;

import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.Diocese;
import com.anglikana.backoffice.entity.District;
import com.anglikana.backoffice.entity.Eglise;
import com.anglikana.backoffice.entity.Paroisse;
import com.anglikana.backoffice.entity.Region;
import com.anglikana.backoffice.entity.Ressource;
import com.anglikana.backoffice.entity.TypeRessource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Jeu de données isolé, inséré dans la transaction du test (annulée à la fin) :
 *
 * <pre>
 * Diocèse D (resp. A)
 * ├── Région R1 (resp. A)
 * │   ├── District Di1 (resp. B)
 * │   │   └── Paroisse P1 (resp. B)
 * │   │       ├── Église E1 (resp. C)  (47.5, -18.5)  dans le carré de Di1
 * │   │       │     ├── Ressource 1 (resp. C)
 * │   │       │     └── Ressource 2 (resp. X : responsable de ressource uniquement)
 * │   │       ├── Église E2            (48.0, -18.5)  sur la frontière du carré
 * │   │       └── Église E3            (49.5, -18.5)  hors du carré
 * │   └── District Di2 (resp. B)
 * │       └── Paroisse P2 (resp. C)
 * │           └── Église E4 (resp. A)  (47.2, -18.2)  dans le carré de Di1 mais rattachée à Di2
 * └── Région R2 (sans responsable ni enfant)
 * </pre>
 *
 * Carré de Di1 : longitudes 47 → 48, latitudes -19 → -18 (SRID 4326).
 */
public class JeuDonneesHierarchie {

    public static final String CARRE_DI1 =
            "{\"type\":\"Polygon\",\"coordinates\":[[[47,-19],[48,-19],[48,-18],[47,-18],[47,-19]]]}";

    private static final GeometryFactory GEOMETRIE = new GeometryFactory(new PrecisionModel(), 4326);

    public Clerge a, b, c, x;
    public Diocese d;
    public Region r1, r2;
    public District di1, di2;
    public Paroisse p1, p2;
    public Eglise e1, e2, e3, e4;

    public JeuDonneesHierarchie inserer(TestEntityManager em) {
        a = em.persist(Clerge.builder().nom("Test-A").prenom("Alpha").build());
        b = em.persist(Clerge.builder().nom("Test-B").prenom("Bravo").build());
        c = em.persist(Clerge.builder().nom("Test-C").prenom("Charlie").build());
        x = em.persist(Clerge.builder().nom("Test-X").prenom("Xray").build());

        d = em.persist(Diocese.builder().nom("Diocèse Épreuve Sprint Deux").responsable(a).build());
        r1 = em.persist(Region.builder().nom("Épreuve Région Un").diocese(d).responsable(a).build());
        r2 = em.persist(Region.builder().nom("Épreuve Région Deux").diocese(d).build());
        di1 = em.persist(District.builder().nom("Épreuve District Un").region(r1).responsable(b).build());
        di2 = em.persist(District.builder().nom("Épreuve District Deux").region(r1).responsable(b).build());
        p1 = em.persist(Paroisse.builder().nom("Épreuve Paroisse Un").district(di1).responsable(b).build());
        p2 = em.persist(Paroisse.builder().nom("Épreuve Paroisse Deux").district(di2).responsable(c).build());

        e1 = em.persist(Eglise.builder().nom("Épreuve Église Un").paroisse(p1).responsable(c).localisation(point(47.5, -18.5)).build());
        e2 = em.persist(Eglise.builder().nom("Épreuve Église Deux").paroisse(p1).localisation(point(48.0, -18.5)).build());
        e3 = em.persist(Eglise.builder().nom("Épreuve Église Trois").paroisse(p1).localisation(point(49.5, -18.5)).build());
        e4 = em.persist(Eglise.builder().nom("Épreuve Église Quatre").paroisse(p2).responsable(a).localisation(point(47.2, -18.2)).build());

        TypeRessource type = em.persist(TypeRessource.builder().nom("Épreuve École").build());
        em.persist(Ressource.builder().nom("Épreuve Ressource Un").type(type).eglise(e1).responsable(c).build());
        em.persist(Ressource.builder().nom("Épreuve Ressource Deux").type(type).eglise(e1).responsable(x).build());

        em.flush();
        return this;
    }

    private static Point point(double longitude, double latitude) {
        return GEOMETRIE.createPoint(new Coordinate(longitude, latitude));
    }
}
