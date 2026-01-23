package com.kodesalon.kopang.domain.member;

public interface MemberAddressRepository {

	MemberAddress findDefaultByMemberNo(Long memberNo);
}
