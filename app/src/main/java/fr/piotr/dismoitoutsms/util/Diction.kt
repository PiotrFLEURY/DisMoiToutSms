package fr.piotr.dismoitoutsms.util

enum class Diction {

    // Etape 0, nouveau message reçu
    MESSAGE_RECU_MODE_VIE_PRIVEE,
    // Etape 1, nouveau message reçu
    MESSAGE_RECU,
    // Etape 2 -> Dites lire ou fermer
    LIRE_OU_FERMER,
    // Etape 2 bis -> Dites répondre ou fermer
    REPONDRE_OU_FERMER,
    // Etape 3 -> Vous avez répondu...
    VOUS_AVEZ_REPONDU,
    // Etape 4 -> Dites modifier, envoyer ou fermer
    MODIFIER_ENVOYER_OU_FERMER,
    // Etape 5 -> Message envoyé
    MESSAGE_ENVOYE,

    CONTACTS_TROUVES

}
