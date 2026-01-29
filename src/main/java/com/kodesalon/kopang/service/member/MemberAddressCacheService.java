package com.kodesalon.kopang.service.member;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.kodesalon.kopang.config.Caches;
import com.kodesalon.kopang.domain.Address;
import com.kodesalon.kopang.domain.member.MemberAddressRepository;

@Service
public class MemberAddressCacheService {

	private final MemberAddressRepository memberAddressRepository;

	public MemberAddressCacheService(MemberAddressRepository memberAddressRepository) {
		this.memberAddressRepository = memberAddressRepository;
	}

	@Cacheable(
		cacheManager = Caches.Manager.REDIS,
		value = Caches.Name.MEMBER_ADDRESS,
		key = "#memberNo"
	)
	public Address getDefaultMemberAddress(Long memberNo) {
		return memberAddressRepository.findDefaultByMemberNo(memberNo).getAddress();
	}
}
