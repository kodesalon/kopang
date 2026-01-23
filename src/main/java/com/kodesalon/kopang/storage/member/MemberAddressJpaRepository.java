package com.kodesalon.kopang.storage.member;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberAddressJpaRepository extends JpaRepository<MemberAddressJpaEntity, Long> {

	MemberAddressJpaEntity findByMemberNoAndIsDefault(Long memberNo, boolean isDefault);
}
