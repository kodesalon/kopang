package com.kodesalon.kopang.domain.member;

import com.kodesalon.kopang.domain.Address;

public class MemberAddress {

	private final Long no;
	private final Long memberNo;
	private final String alias;
	private final Address address;

	public MemberAddress(Long no, Long memberNo, String alias, Address address) {
		this.no = no;
		this.memberNo = memberNo;
		this.alias = alias;
		this.address = address;
	}

	public Address getAddress() {
		return address;
	}
}
