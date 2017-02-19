package fr.piotr.dismoitoutsms.contacts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by piotr_000 on 30/10/2016.
 *
 */

public class Contacts implements Serializable, Iterable<Contact> {

    List<Contact> contacts = new ArrayList<>();

    public void add(Contact contact) {
        this.contacts.add(contact);
    }

    @Override
    public Iterator<Contact> iterator() {
        return contacts.iterator();
    }

    public void sort() {
        Collections.sort(contacts);
    }

    public List<Contact> asList(){
        return contacts;
    }

    public boolean contains(Contact contact) {
        return contacts.contains(contact);
    }

    public void remove(Contact contact) {
        this.contacts.remove(contact);
    }

    public void clear() {
        this.contacts.clear();
    }

    public void addAll(Contacts contacts) {
        this.contacts.addAll(contacts.asList());
    }
}
