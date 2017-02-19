package fr.piotr.dismoitoutsms.messages;

import android.support.annotation.NonNull;

import java.util.Date;

import fr.piotr.dismoitoutsms.contacts.Contact;

public class Message implements Comparable<Message> {

	private Date	date;
	private Contact	contact;
	private String	message;

	public Message(Date date, Contact contact, String message) {
		this.date = date;
		this.contact = contact;
		this.message = message;
	}

	public boolean contactIs(Contact contact) {
		return this.contact.equals(contact);
	}

	public void append(String message) {
		this.message += " " + message;
	}

	public Contact getContact() {
		return contact;
	}

	public String getMessage() {
		return message;
	}

	public Date getDate() {
		return date;
	}

	public int compareTo(@NonNull Message another) {
		return this.date.compareTo(another.date);
	}
}
