package com.smartpay.merchant.security;

import com.smartpay.merchant.domain.Merchant;
import com.smartpay.merchant.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantUserDetailService implements UserDetailsService {

    private final MerchantRepository merchantRepository;

    @Override
    public Merchant loadUserByUsername(String username) throws UsernameNotFoundException {
        return merchantRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found by username: " + username));
    }
}
