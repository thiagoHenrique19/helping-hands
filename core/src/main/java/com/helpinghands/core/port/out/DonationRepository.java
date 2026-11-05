package com.helpinghands.core.port.out;

import com.helpinghands.core.domain.Donation;

public interface DonationRepository {

    Donation save(Donation donation);
}
