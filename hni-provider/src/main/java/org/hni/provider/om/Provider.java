package org.hni.provider.om;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.hni.common.om.Persistable;
import org.hni.models.om.Address;

/**
 * A Provider is an entity that provides meals to clients.  There will be
 * one or more ProviderLocations associated with a Provider.  Each provier also
 * has one or more Menus associated with them that can be fulfilled at a location.
 *
 */
@Entity
@Table(name="providers")
public class Provider implements Serializable, Persistable {

	private static final long serialVersionUID = -6983145009930349810L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private Long id;
	
	@Column(name="name") private String name;
	@Column(name="created") private Date created;
	@Column(name="created_by") private Long createdById;

	@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinTable(name = "provider_addresses", joinColumns = { @JoinColumn(name = "provider_id", referencedColumnName = "id") }, inverseJoinColumns = { @JoinColumn(name = "address_id", referencedColumnName = "id") })
	private Set<Address> addresses = new HashSet<>();

	public Provider() {}
	public Provider(Long id) {
		this.id = id;
	}
	
	@Override
	public Long getId() {
		return this.id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Long getCreatedById() {
		return createdById;
	}

	public void setCreatedById(Long createdById) {
		this.createdById = createdById;
	}

	public Set<Address> getAddresses() {
		return addresses;
	}

	public void setAddresses(Set<Address> addresses) {
		this.addresses = addresses;
	}

	public void setId(Long id) {
		this.id = id;
	}

	
}
