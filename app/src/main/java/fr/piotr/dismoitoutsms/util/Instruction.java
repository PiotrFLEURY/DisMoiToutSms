package fr.piotr.dismoitoutsms.util;

/**
 * Created by piotr_000 on 20/03/2016.
 *
 */
public enum Instruction {

    REPONDRE_FERMER, REPONSE, MODIFIER_ENVOYER_FERMER, DICTER_CONTACT;

    public boolean is(Instruction ... instructions){
        for (Instruction instruction : instructions) {
            if(this==instruction){
                return true;
            }
        }
        return false;
    }
}
