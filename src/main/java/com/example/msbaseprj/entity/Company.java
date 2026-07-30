package com.example.msbaseprj.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "companies")
public class Company {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	// @GeneratedValue(strategy = GenerationType.SEQUENCE, generator =
	// "company_seq")
	// @SequenceGenerator(name = "company_seq", sequenceName = "company_seq",
	// allocationSize = 50)
	private Long id;

	private String name;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
