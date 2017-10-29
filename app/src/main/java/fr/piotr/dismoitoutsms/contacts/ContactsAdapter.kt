package fr.piotr.dismoitoutsms.contacts

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

import fr.piotr.dismoitoutsms.R
import fr.piotr.dismoitoutsms.util.ContactHelper

class ContactsAdapter(mContext: Context, contacts: Contacts) : ArrayAdapter<Contact>(mContext, R.layout.contact, contacts.asList()) {

    data class ContactViewHolder(var avatar: ImageView, var photo: ImageView, var nom: TextView)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var mConvertView = convertView
        val contact = getItem(position)

        val avatar: ImageView
        val photo: ImageView
        val textViewNom: TextView
        if (mConvertView == null) {

            mConvertView = LayoutInflater.from(context).inflate(R.layout.contact, parent, false)
            avatar = mConvertView.findViewById(R.id.avatar)
            photo = mConvertView.findViewById(R.id.photoContact)
            textViewNom = mConvertView.findViewById(R.id.nom)

            mConvertView.tag = ContactViewHolder(avatar, photo, textViewNom)

        } else {
            val holder = mConvertView.tag as ContactViewHolder
            avatar = holder.avatar
            photo = holder.photo
            textViewNom = holder.nom
        }

        if (contact!!.hasAPhoto()) {
            photo.visibility = View.VISIBLE
            photo.setImageBitmap(ContactHelper.getPhotoContact(context, contact.photoId))
            avatar.visibility = View.GONE
        } else {
            photo.visibility = View.GONE
            avatar.visibility = View.VISIBLE
        }
        textViewNom.text = contact.name

        return mConvertView
    }

}
