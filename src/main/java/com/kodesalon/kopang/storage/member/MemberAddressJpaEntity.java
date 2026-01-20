package com.kodesalon.kopang.storage.member;

import com.kodesalon.kopang.domain.Address;
import com.kodesalon.kopang.domain.Coordinate;
import com.kodesalon.kopang.domain.member.MemberAddress;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "member_address")
public class MemberAddressJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long no;

	@Column(nullable = false)
	private Long memberNo;

	private String alias;

	@Column(nullable = false)
	private String zipCode;

	@Column(nullable = false)
	private String address;

	private String detail;

	@Column(nullable = false)
	private Double latitude;

	@Column(nullable = false)
	private Double longitude;

	@Column(nullable = false)
	private Boolean isDefault;

	protected MemberAddressJpaEntity() {
	}

	public MemberAddress toDomain() {
		return new MemberAddress(no, memberNo, alias,
			new Address(zipCode, address, detail, new Coordinate(latitude, longitude)));
	}
}
