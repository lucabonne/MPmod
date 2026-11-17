package net.minepiece.qol.essentials;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Calcul pur de l'ordre cible d'un conteneur trié (par rareté ou alphabétique). */
public final class RaritySort {
    private RaritySort() {}

    /**
     * RARITY : clé primaire = rareté (identiques regroupés par itemId).
     * ITEM : clé primaire = nom de l'objet, TOUJOURS A→Z ; secondaire = rareté → chaque
     * objet regroupé avec TOUTES ses raretés à la suite (ex. Boulon épique collé au Boulon
     * légendaire). En mode ITEM, {@code ascending} ne change PAS l'ordre des noms (toujours
     * A→Z) : il choisit seulement le sens des raretés à l'intérieur de chaque groupe.
     */
    public enum Mode { RARITY, ITEM }

    /**
     * Une entrée par slot. {@code empty} = slot vide (toujours placé en fin).
     * {@code rank} = rang de rareté, {@code name} = nom affiché, {@code itemId} = id de
     * registre pour regrouper les identiques.
     */
    public record Entry(boolean empty, int rank, String itemId, String name) {}

    /**
     * Renvoie la liste des index source dans l'ordre cible (permutation stable).
     * Les slots vides finissent toujours en dernier.
     * <ul>
     *   <li>RARITY : {@code ascending} contrôle le sens des raretés (true = commun d'abord).</li>
     *   <li>ITEM : noms TOUJOURS A→Z ; {@code ascending} contrôle le sens des raretés dans
     *       chaque groupe (true = moins rare d'abord, false = plus rare d'abord).</li>
     * </ul>
     */
    public static List<Integer> targetOrder(List<Entry> entries, Mode mode, boolean ascending) {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) idx.add(i);

        Comparator<Integer> cmp = (a, b) -> {
            Entry ea = entries.get(a), eb = entries.get(b);
            if (ea.empty() != eb.empty()) return ea.empty() ? 1 : -1; // vides en dernier
            if (!ea.empty()) {
                if (mode == Mode.RARITY) {
                    int byRank = ascending ? Integer.compare(ea.rank(), eb.rank())
                                           : Integer.compare(eb.rank(), ea.rank());
                    if (byRank != 0) return byRank;
                } else { // ITEM : nom TOUJOURS A→Z, puis rareté (sens = ascending)
                    int byName = ea.name().compareToIgnoreCase(eb.name());
                    if (byName != 0) return byName;
                    int byRank = ascending ? Integer.compare(ea.rank(), eb.rank())  // moins rare d'abord
                                           : Integer.compare(eb.rank(), ea.rank()); // plus rare d'abord
                    if (byRank != 0) return byRank;
                }
                int byId = ea.itemId().compareTo(eb.itemId());          // regroupe les identiques
                if (byId != 0) return byId;
            }
            return Integer.compare(a, b);              // stable
        };
        idx.sort(cmp);
        return idx;
    }
}
