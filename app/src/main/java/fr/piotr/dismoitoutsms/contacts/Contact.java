package fr.piotr.dismoitoutsms.contacts;

import android.support.annotation.NonNull;

import java.io.Serializable;

/**
 * @author Piotr
 * 
 */
public class Contact implements Comparable<Contact>, Serializable {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 5599844338708510980L;
	int							id;
	String						name;
	String						telephone;
	Integer						photoId;

	public Contact(int id, String name, String telephone, Integer photoId) {

		this.id = id;
		this.name = name;
		this.telephone = telephone;
		this.photoId = photoId;

	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the telephone
	 */
	public String getTelephone() {
		return telephone;
	}

	public int compareTo(@NonNull Contact another) {
		return name.compareTo(another.getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + (name == null ? 0 : name.hashCode());
		result = prime * result + (telephone == null ? 0 : telephone.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Contact other = (Contact) obj;
		if (id != other.id) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (telephone == null) {
			if (other.telephone != null) {
				return false;
			}
		} else if (!telephone.equals(other.telephone)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return getName() + " " + getTelephone();
	}

	/**
	 * @return the photoId
	 */
	public Integer getPhotoId() {
		return photoId;
	}

	public boolean hasAPhoto() {
		return photoId != null && photoId != 0;
	}

	/**
	 * @param photoId
	 *            the photoId to set
	 */
	public void setPhotoId(int photoId) {
		this.photoId = photoId;
	}

}
