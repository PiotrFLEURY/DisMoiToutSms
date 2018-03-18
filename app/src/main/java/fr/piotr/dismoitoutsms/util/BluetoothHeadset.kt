package fr.piotr.dismoitoutsms.util

import java.io.Serializable

/**
 * Created by piotr on 21/01/2018.
 *
 */
data class BluetoothHeadsetDevice(private val profile:String, val address:String, val name:String): Serializable {

    override fun toString(): String {
        return "{profile:$profile, address:$address, name:$name}"
    }

}