package com.kodesalon.kopang.service.member;

import org.springframework.stereotype.Service;

import com.kodesalon.kopang.domain.member.MemberAddress;
import com.kodesalon.kopang.domain.member.MemberAddressRepository;

@Service
public class MemberAddressService {

	private final MemberAddressRepository memberAddressRepository;

	public MemberAddressService(MemberAddressRepository memberAddressRepository) {
		this.memberAddressRepository = memberAddressRepository;
	}

	public MemberAddress findDefaultMemberAddress(Long memberNo) {
		return memberAddressRepository.findDefaultByMemberNo(memberNo);
	}
}
