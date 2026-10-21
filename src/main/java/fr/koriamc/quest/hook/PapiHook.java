package fr.koriamc.quest.hook;

import fr.koriamc.quest.KoriaQuest;

/**
 * Pont vers PlaceholderAPI, chargé uniquement si le plugin est installé.
 */
public final class PapiHook {

    private final QuestExpansion expansion;

    public PapiHook(KoriaQuest plugin) {
        this.expansion = new QuestExpansion(plugin);
        this.expansion.register();
    }

    public void unregister() {
        try {
            expansion.unregister();
        } catch (Throwable ignored) {
            // PlaceholderAPI déjà arrêté
        }
    }
}
