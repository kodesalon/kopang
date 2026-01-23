package com.kodesalon.kopang.storage.member;

import org.springframework.stereotype.Repository;

import com.kodesalon.kopang.domain.member.MemberAddress;
import com.kodesalon.kopang.domain.member.MemberAddressRepository;

@Repository
public class MemberAddressRepositoryImpl implements MemberAddressRepository {

	private final MemberAddressJpaRepository memberAddressJpaRepository;

	public MemberAddressRepositoryImpl(MemberAddressJpaRepository memberAddressJpaRepository) {
		this.memberAddressJpaRepository = memberAddressJpaRepository;
	}

	@Override
	public MemberAddress findDefaultByMemberNo(Long memberNo) {
		return memberAddressJpaRepository
			.findByMemberNoAndIsDefault(memberNo, true)
			.toDomain();
	}
}
