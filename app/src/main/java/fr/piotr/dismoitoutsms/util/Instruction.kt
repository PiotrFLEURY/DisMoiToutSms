package fr.piotr.dismoitoutsms.util

/**
 * Created by piotr_000 on 20/03/2016.
 *
 */
enum class Instruction {

    LIRE_FERMER, REPONDRE_FERMER, REPONSE, AJOUTER, MODIFIER_ENVOYER_FERMER, DICTER_CONTACT;

    fun `is`(vararg instructions: Instruction): Boolean {
        return instructions.any { this == it }
    }
}
