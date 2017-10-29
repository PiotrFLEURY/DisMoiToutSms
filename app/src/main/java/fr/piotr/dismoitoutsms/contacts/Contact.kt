package fr.piotr.dismoitoutsms.contacts

import java.io.Serializable

/**
 * @author Piotr
 */
data class Contact(val id: Int = -1, val name: String, val telephone: String, val photoId: Int = 0) : Comparable<Contact>, Serializable {

    override fun compareTo(other: Contact): Int {
        return name.compareTo(other.name)
    }

    fun hasAPhoto(): Boolean {
        return photoId != 0
    }

}
