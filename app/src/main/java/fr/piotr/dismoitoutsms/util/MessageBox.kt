package fr.piotr.dismoitoutsms.util

import android.app.AlertDialog
import android.util.Log
import fr.piotr.dismoitoutsms.DisMoiToutSmsApplication
import fr.piotr.dismoitoutsms.R

object EmptyRunnable : Runnable {

    override fun run() {
        Log.d("EmptyRunnable", "nothing to do")
    }

}

object MessageBox {

    fun confirm(title: String = "", message: String,
                ok: Runnable = EmptyRunnable, ko: Runnable = EmptyRunnable) {
        val context = DisMoiToutSmsApplication.INSTANCE.applicationContext
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(context.getString(R.string.oui)) { _, _ -> ok.run() }
        builder.setNegativeButton(context.getString(R.string.non)) { _, _ -> ko.run() }
        val dialog = builder.create()
        dialog.show()
    }

}
